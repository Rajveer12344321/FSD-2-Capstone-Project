package com.bloodbridge.service;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.*;
import com.bloodbridge.web.Dtos.BadgeDto;
import com.bloodbridge.web.Dtos.DonationDto;
import com.bloodbridge.web.Dtos.ImpactDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Records completed donations (by authorised staff only), updates the donor's last-donation date so the
 * donation interval filter applies, closes fully served requests, and derives donor badges from the history.
 */
@Service
public class DonationService {
    private final InvitationRepository invitations;
    private final DonationRecordRepository donations;
    private final DonorProfileRepository profiles;
    private final BloodRequestRepository requests;
    private final DriveRegistrationRepository registrations;
    private final InvitationService invitationService;
    private final NotificationService notifications;
    private final AuditService audit;

    public DonationService(InvitationRepository invitations, DonationRecordRepository donations,
                           DonorProfileRepository profiles, BloodRequestRepository requests,
                           DriveRegistrationRepository registrations, InvitationService invitationService,
                           NotificationService notifications, AuditService audit) {
        this.invitations = invitations;
        this.donations = donations;
        this.profiles = profiles;
        this.requests = requests;
        this.registrations = registrations;
        this.invitationService = invitationService;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public DonationDto record(User actor, Long invitationId, int units) {
        Invitation inv = invitations.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (inv.getStatus() != InvitationStatus.ACCEPTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an accepted invitation can be recorded as a donation");
        if (donations.existsByInvitation(inv))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A donation is already recorded for this invitation");
        BloodRequest req = inv.getRequest();
        if (req.getStatus() == RequestStatus.CANCELLED || req.getStatus() == RequestStatus.EXPIRED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The request is no longer active");
        DonorProfile profile = profiles.findByUser(inv.getDonor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Donor has no profile"));

        DonationRecord d = new DonationRecord();
        d.setDonor(inv.getDonor());
        d.setRequest(req);
        d.setInvitation(inv);
        d.setBloodGroup(profile.getBloodGroup());
        d.setUnits(units);
        d.setDonatedOn(LocalDate.now(ZoneOffset.UTC));
        d.setRecordedBy(actor.getEmail());
        d = donations.save(d);

        profile.setLastDonationDate(d.getDonatedOn());
        profiles.save(profile);

        notifications.notifyUser(inv.getDonor(), "Thank you. Your donation for request #" + req.getId() + " was recorded.");
        notifications.notifyUser(req.getRequester(), "A donation was recorded for your request #" + req.getId() + ".");

        int total = donations.findByRequest(req).stream().mapToInt(DonationRecord::getUnits).sum();
        if (total >= req.getUnitsRequired() && req.getStatus() == RequestStatus.MATCHING) {
            req.setStatus(RequestStatus.FULFILLED);
            requests.save(req);
            invitationService.closePending(req, "request fulfilled");
            notifications.notifyUser(req.getRequester(), "Your request #" + req.getId() + " is now fulfilled.");
        }
        audit.log(actor, "DONATION_RECORDED", "invitation#" + invitationId, units + " unit(s)");
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DonationDto> recent() {
        return donations.findTop100ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ImpactDto impact(User donor) {
        List<DonationRecord> history = donations.findByDonorOrderByDonatedOnDesc(donor);
        long count = history.size();
        long units = history.stream().mapToLong(DonationRecord::getUnits).sum();
        LocalDate last = history.isEmpty() ? null : history.get(0).getDonatedOn();
        long drives = registrations.countByDonor(donor);
        boolean verified = profiles.findByUser(donor).map(DonorProfile::isVerified).orElse(false);
        List<BadgeDto> badges = List.of(
                new BadgeDto("FIRST_DROP", "First drop", "Complete your first recorded donation", count >= 1),
                new BadgeDto("REGULAR", "Regular", "Three recorded donations", count >= 3),
                new BadgeDto("CHAMPION", "Champion", "Five recorded donations", count >= 5),
                new BadgeDto("HERO", "Hero", "Ten recorded donations", count >= 10),
                new BadgeDto("VERIFIED", "Verified", "Profile verified by a coordinator", verified),
                new BadgeDto("DRIVE_JOINER", "Drive joiner", "Register for a blood drive", drives >= 1));
        return new ImpactDto(count, units, last, drives, badges, history.stream().map(this::toDto).toList());
    }

    private DonationDto toDto(DonationRecord d) {
        return new DonationDto(d.getId(), RequestService.shortName(d.getDonor().getName()), d.getBloodGroup(),
                d.getUnits(), d.getDonatedOn(), d.getRequest() == null ? null : d.getRequest().getId(),
                d.getRecordedBy());
    }
}
