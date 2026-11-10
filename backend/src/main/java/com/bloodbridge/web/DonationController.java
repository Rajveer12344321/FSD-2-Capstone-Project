package com.bloodbridge.web;

import com.bloodbridge.domain.User;
import com.bloodbridge.service.DonationService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DonationController {
    private final DonationService service;

    public DonationController(DonationService service) {
        this.service = service;
    }

    @PostMapping("/donations")
    @PreAuthorize("hasAuthority('PERM_DONATION_RECORD')")
    public DonationDto record(@AuthenticationPrincipal User actor, @Valid @RequestBody DonationCreate in) {
        return service.record(actor, in.invitationId(), in.units());
    }

    @GetMapping("/donations")
    @PreAuthorize("hasAnyAuthority('PERM_DONATION_RECORD','PERM_REPORT_EXPORT')")
    public List<DonationDto> recent() {
        return service.recent();
    }

    @GetMapping("/donor/impact")
    @PreAuthorize("hasAuthority('PERM_PROFILE_MANAGE')")
    public ImpactDto impact(@AuthenticationPrincipal User donor) {
        return service.impact(donor);
    }
}
