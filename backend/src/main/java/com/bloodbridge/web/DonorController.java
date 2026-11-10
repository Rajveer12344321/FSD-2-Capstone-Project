package com.bloodbridge.web;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.DonationRecordRepository;
import com.bloodbridge.repo.DonorProfileRepository;
import com.bloodbridge.repo.InvitationRepository;
import com.bloodbridge.service.InvitationService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DonorController {
    private final DonorProfileRepository profiles;
    private final InvitationRepository invitations;
    private final InvitationService invitationService;
    private final DonationRecordRepository donations;

    public DonorController(DonorProfileRepository profiles, InvitationRepository invitations,
                           InvitationService invitationService, DonationRecordRepository donations) {
        this.profiles = profiles;
        this.invitations = invitations;
        this.invitationService = invitationService;
        this.donations = donations;
    }

    @GetMapping("/donor/profile")
    @PreAuthorize("hasAuthority('PERM_PROFILE_MANAGE')")
    public DonorProfileDto profile(@AuthenticationPrincipal User user) {
        return profiles.findByUser(user).map(this::toDto)
                .orElse(new DonorProfileDto(false, null, null, null, null, null, false, null, null, false, false));
    }

    @PutMapping("/donor/profile")
    @PreAuthorize("hasAuthority('PERM_PROFILE_MANAGE')")
    @Transactional
    public DonorProfileDto update(@AuthenticationPrincipal User user, @Valid @RequestBody DonorProfileUpdate in) {
        DonorProfile p = profiles.findByUser(user).orElseGet(() -> {
            DonorProfile created = new DonorProfile();
            created.setUser(user);
            return created;
        });
        p.setBloodGroup(in.bloodGroup());
        p.setCity(in.city().trim());
        p.setLatitude(in.latitude());
        p.setLongitude(in.longitude());
        p.setPhone(in.phone() == null || in.phone().isBlank() ? null : in.phone().trim());
        p.setLastDonationDate(in.lastDonationDate());
        p.setContactConsent(in.contactConsent());
        if (in.available() && (!p.isAvailable() || p.getAvailabilityConfirmedAt() == null)) {
            p.setAvailabilityConfirmedAt(Instant.now());
        }
        p.setAvailable(in.available());
        return toDto(profiles.save(p));
    }

    /** Donor re-confirms they are still available; updates the confirmation timestamp used for ranking. */
    @PostMapping("/donor/profile/confirm-availability")
    @PreAuthorize("hasAuthority('PERM_PROFILE_MANAGE')")
    @Transactional
    public DonorProfileDto confirm(@AuthenticationPrincipal User user) {
        DonorProfile p = profiles.findByUser(user)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Save your donor profile first"));
        p.setAvailable(true);
        p.setAvailabilityConfirmedAt(Instant.now());
        return toDto(profiles.save(p));
    }

    @GetMapping("/donor/invitations")
    @PreAuthorize("hasAuthority('PERM_INVITATION_RESPOND')")
    @Transactional(readOnly = true)
    public List<InvitationDto> invitations(@AuthenticationPrincipal User user) {
        return invitations.findByDonorOrderByCreatedAtDesc(user).stream().map(this::toInvitationDto).toList();
    }

    @PostMapping("/invitations/{id}/respond")
    @PreAuthorize("hasAuthority('PERM_INVITATION_RESPOND')")
    public InvitationDto respond(@AuthenticationPrincipal User user, @PathVariable Long id,
                                 @RequestBody RespondRequest in) {
        return toInvitationDto(invitationService.respond(id, user, in.accept()));
    }

    private InvitationDto toInvitationDto(Invitation i) {
        BloodRequest r = i.getRequest();
        return new InvitationDto(i.getId(), r.getId(), r.getBloodGroup(), r.getComponent(), r.getUnitsRequired(),
                r.getHospital(), r.getCity(), r.getUrgency(), r.getRequiredBy(), r.getStatus(), i.getStatus(),
                i.getCreatedAt(), i.getRespondedAt(), donations.existsByInvitation(i));
    }

    private DonorProfileDto toDto(DonorProfile p) {
        return new DonorProfileDto(true, p.getBloodGroup(), p.getCity(), p.getLatitude(), p.getLongitude(),
                p.getPhone(), p.isAvailable(), p.getAvailabilityConfirmedAt(), p.getLastDonationDate(),
                p.isContactConsent(), p.isVerified());
    }
}
