package com.bloodbridge.service;

import com.bloodbridge.config.AppProperties;
import com.bloodbridge.domain.*;
import com.bloodbridge.repo.BloodRequestRepository;
import com.bloodbridge.repo.InvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class InvitationService {
    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitations;
    private final BloodRequestRepository requests;
    private final MatchingService matching;
    private final NotificationService notifications;
    private final AppProperties props;

    public InvitationService(InvitationRepository invitations, BloodRequestRepository requests,
                             MatchingService matching, NotificationService notifications, AppProperties props) {
        this.invitations = invitations;
        this.requests = requests;
        this.matching = matching;
        this.notifications = notifications;
        this.props = props;
    }

    /** Invites the best not-yet-invited candidates until the target number of active invitations is reached. */
    @Transactional
    public int inviteForRequest(BloodRequest req) {
        AppProperties.Matching m = props.matching();
        if (invitations.countByRequestAndStatus(req, InvitationStatus.ACCEPTED) >= req.getUnitsRequired()) return 0;
        int target = Math.min(m.maxInvitesPerRequest(), req.getUnitsRequired() * m.inviteMultiplier());
        long active = invitations.countByRequestAndStatusIn(req,
                List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED));
        int slots = (int) (target - active);
        int created = 0;
        if (slots <= 0) return 0;
        for (MatchingService.Candidate c : matching.rank(req)) {
            if (created >= slots) break;
            User donor = c.profile().getUser();
            if (invitations.existsByRequestAndDonor(req, donor)) continue;
            Invitation inv = new Invitation();
            inv.setRequest(req);
            inv.setDonor(donor);
            inv.setScore(c.score());
            inv.setDistanceKm(c.distanceKm());
            inv.setReasons(String.join("\n", c.reasons()));
            invitations.save(inv);
            notifications.notifyUser(donor, String.format("Blood request #%d: %s %s needed at %s, %s (%s). Open BloodBridge to accept or decline.",
                    req.getId(), req.getBloodGroup().label(), pretty(req.getComponent()),
                    req.getHospital(), req.getCity(), req.getUrgency()));
            deliver(inv);
            created++;
        }
        return created;
    }

    /** One delivery attempt. Records SENT or FAILED and increments the attempt counter. */
    @Transactional
    public void deliver(Invitation inv) {
        inv.setAttempts(inv.getAttempts() + 1);
        BloodRequest r = inv.getRequest();
        try {
            notifications.sendEmail(inv.getDonor(), "BloodBridge: blood request #" + r.getId(),
                    String.format("A %s request for %s %s (%d unit(s)) at %s, %s needs donors. "
                                    + "This is a preliminary match, not medical clearance. Log in to BloodBridge to respond.",
                            r.getUrgency(), r.getBloodGroup().label(), pretty(r.getComponent()),
                            r.getUnitsRequired(), r.getHospital(), r.getCity()));
            inv.setDeliveryStatus(DeliveryStatus.SENT);
        } catch (Exception e) {
            log.warn("Delivery failed for invitation {} (attempt {}): {}", inv.getId(), inv.getAttempts(), e.getMessage());
            inv.setDeliveryStatus(DeliveryStatus.FAILED);
        }
        invitations.save(inv);
    }

    /** Scheduled retry of failed deliveries, limited to the configured maximum attempts. */
    @Transactional
    public int retryFailed() {
        int count = 0;
        for (Invitation inv : invitations.findByDeliveryStatusAndAttemptsLessThan(
                DeliveryStatus.FAILED, props.notifications().maxAttempts())) {
            if (inv.getStatus() == InvitationStatus.PENDING) {
                deliver(inv);
                count++;
            }
        }
        return count;
    }

    /** Administrator-triggered retry of a single failed invitation. */
    @Transactional
    public void retryOne(Long invitationId) {
        Invitation inv = invitations.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (inv.getStatus() != InvitationStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation is no longer pending");
        deliver(inv);
    }

    /**
     * Records a donor's answer. Each invitation can be answered once: a second answer gets 409, and the
     * optimistic lock (@Version) rejects two concurrent answers.
     */
    @Transactional
    public Invitation respond(Long invitationId, User actor, boolean accept) {
        Invitation inv = invitations.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!inv.getDonor().getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invitation belongs to another donor");
        BloodRequest req = inv.getRequest();
        if (req.getStatus() != RequestStatus.OPEN && req.getStatus() != RequestStatus.MATCHING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is no longer active");
        if (inv.getStatus() != InvitationStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This invitation has already been answered");

        Instant now = Instant.now();
        inv.setStatus(accept ? InvitationStatus.ACCEPTED : InvitationStatus.DECLINED);
        inv.setRespondedAt(now);
        invitations.save(inv);
        if (req.getFirstResponseAt() == null) {
            req.setFirstResponseAt(now);
            requests.save(req);
        }
        notifications.notifyUser(req.getRequester(), String.format("A donor %s your request #%d (%s %s at %s).",
                accept ? "accepted" : "declined", req.getId(), req.getBloodGroup().label(),
                pretty(req.getComponent()), req.getHospital()));
        return inv;
    }

    /** Marks all still-pending invitations of a request as CLOSED and tells the donors. */
    @Transactional
    public void closePending(BloodRequest req, String reason) {
        for (Invitation inv : invitations.findByRequestAndStatus(req, InvitationStatus.PENDING)) {
            inv.setStatus(InvitationStatus.CLOSED);
            invitations.save(inv);
            notifications.notifyUser(inv.getDonor(), "Blood request #" + req.getId() + " is no longer active (" + reason + ").");
        }
    }

    private String pretty(BloodComponent c) {
        return c.name().toLowerCase().replace('_', ' ');
    }
}
