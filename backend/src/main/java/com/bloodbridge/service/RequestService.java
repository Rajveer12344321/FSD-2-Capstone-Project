package com.bloodbridge.service;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.BloodRequestRepository;
import com.bloodbridge.repo.DonationRecordRepository;
import com.bloodbridge.repo.DonorProfileRepository;
import com.bloodbridge.repo.InvitationRepository;
import com.bloodbridge.web.Dtos.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class RequestService {

    /** Published after a request is saved; handled once the transaction commits. */
    public record RequestCreatedEvent(Long requestId) {}

    private final BloodRequestRepository requests;
    private final InvitationRepository invitations;
    private final DonorProfileRepository profiles;
    private final InvitationService invitationService;
    private final NotificationService notifications;
    private final ApplicationEventPublisher events;
    private final PermissionService permissions;
    private final AuditService audit;
    private final DonationRecordRepository donations;

    public RequestService(BloodRequestRepository requests, InvitationRepository invitations,
                          DonorProfileRepository profiles, InvitationService invitationService,
                          NotificationService notifications, ApplicationEventPublisher events,
                          PermissionService permissions, AuditService audit,
                          DonationRecordRepository donations) {
        this.requests = requests;
        this.invitations = invitations;
        this.profiles = profiles;
        this.invitationService = invitationService;
        this.notifications = notifications;
        this.events = events;
        this.permissions = permissions;
        this.audit = audit;
        this.donations = donations;
    }

    @Transactional
    public Long create(User requester, RequestCreate in) {
        BloodRequest r = new BloodRequest();
        r.setRequester(requester);
        r.setBloodGroup(in.bloodGroup());
        r.setComponent(in.component());
        r.setUnitsRequired(in.unitsRequired());
        r.setHospital(in.hospital().trim());
        r.setCity(in.city().trim());
        r.setLatitude(in.latitude());
        r.setLongitude(in.longitude());
        r.setUrgency(in.urgency());
        r.setRequiredBy(in.requiredBy());
        r = requests.save(r);
        events.publishEvent(new RequestCreatedEvent(r.getId()));
        return r.getId();
    }

    @Transactional(readOnly = true)
    public List<RequestDto> list(User user) {
        List<BloodRequest> list = permissions.has(user, Permission.REQUEST_VIEW_ALL)
                ? requests.findAllByOrderByCreatedAtDesc()
                : requests.findByRequesterOrderByCreatedAtDesc(user);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetailDto detail(Long id, User user) {
        BloodRequest r = load(id, user, false);
        boolean owner = r.getRequester().getId().equals(user.getId());
        List<CandidateDto> candidates = invitations.findByRequestOrderByScoreDesc(r).stream()
                .map(i -> toCandidate(i, owner)).toList();
        return new RequestDetailDto(toDto(r), candidates);
    }

    /** Manual transitions: MATCHING to FULFILLED, and OPEN or MATCHING to CANCELLED. */
    @Transactional
    public void changeStatus(Long id, User user, RequestStatus target) {
        BloodRequest r = load(id, user, true);
        boolean active = r.getStatus() == RequestStatus.OPEN || r.getStatus() == RequestStatus.MATCHING;
        if (target == RequestStatus.CANCELLED && active) {
            r.setStatus(RequestStatus.CANCELLED);
            invitationService.closePending(r, "cancelled by requester");
        } else if (target == RequestStatus.FULFILLED && r.getStatus() == RequestStatus.MATCHING) {
            r.setStatus(RequestStatus.FULFILLED);
            invitationService.closePending(r, "request fulfilled");
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transition " + r.getStatus() + " -> " + target + " is not allowed");
        }
        requests.save(r);
        audit.log(user, "REQUEST_STATUS", "request#" + r.getId(), "set to " + target);
    }

    @Transactional
    public void rematch(Long id, User user) {
        BloodRequest r = load(id, user, true);
        if (r.getStatus() != RequestStatus.OPEN && r.getStatus() != RequestStatus.MATCHING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not active");
        int invited = invitationService.inviteForRequest(r);
        if (invited > 0 && r.getStatus() == RequestStatus.OPEN) {
            r.setStatus(RequestStatus.MATCHING);
            requests.save(r);
        }
    }

    @Transactional
    public void changeUrgency(Long id, Urgency urgency, User actor) {
        BloodRequest r = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        Urgency before = r.getUrgency();
        r.setUrgency(urgency);
        requests.save(r);
        audit.log(actor, "URGENCY_CHANGED", "request#" + id, before + " -> " + urgency);
    }

    /** Marks overdue active requests EXPIRED and closes their pending invitations. */
    @Transactional
    public int expireOverdue() {
        Instant now = Instant.now();
        int count = 0;
        for (BloodRequest r : requests.findByStatusIn(List.of(RequestStatus.OPEN, RequestStatus.MATCHING))) {
            if (r.getRequiredBy().isBefore(now)) {
                r.setStatus(RequestStatus.EXPIRED);
                requests.save(r);
                invitationService.closePending(r, "deadline passed");
                notifications.notifyUser(r.getRequester(), "Your request #" + r.getId() + " expired at its deadline.");
                count++;
            }
        }
        return count;
    }

    /** Owners always have access; others need REQUEST_VIEW_ALL to read and REQUEST_MANAGE_ALL to change. */
    private BloodRequest load(Long id, User user, boolean forWrite) {
        BloodRequest r = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        boolean owner = r.getRequester().getId().equals(user.getId());
        Permission needed = forWrite ? Permission.REQUEST_MANAGE_ALL : Permission.REQUEST_VIEW_ALL;
        if (!owner && !permissions.has(user, needed))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this request");
        return r;
    }

    public RequestDto toDto(BloodRequest r) {
        return new RequestDto(r.getId(), r.getBloodGroup(), r.getComponent(), r.getUnitsRequired(), r.getHospital(),
                r.getCity(), r.getUrgency(), r.getRequiredBy(), r.getStatus(), r.getCreatedAt(),
                r.getRequester().getName(), r.getRequester().getId(), invitations.countByRequest(r),
                invitations.countByRequestAndStatus(r, InvitationStatus.ACCEPTED));
    }

    /** Donor name is shortened, exact location is never shown, and a phone number only appears to the
     *  requester who owns the request, after the donor accepted and gave consent. */
    private CandidateDto toCandidate(Invitation i, boolean viewerIsOwner) {
        DonorProfile p = profiles.findByUser(i.getDonor()).orElse(null);
        String phone = (viewerIsOwner && p != null && i.getStatus() == InvitationStatus.ACCEPTED && p.isContactConsent())
                ? p.getPhone() : null;
        List<String> reasons = i.getReasons() == null ? List.of() : List.of(i.getReasons().split("\n"));
        return new CandidateDto(i.getId(), shortName(i.getDonor().getName()),
                p == null ? null : p.getBloodGroup(), p == null ? null : p.getCity(), i.getScore(), reasons,
                i.getStatus(), i.getDeliveryStatus(), i.getAttempts(), p != null && p.isVerified(), phone,
                i.getDistanceKm(), donations.existsByInvitation(i));
    }

    public static String shortName(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
