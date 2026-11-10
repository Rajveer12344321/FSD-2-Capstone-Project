package com.bloodbridge.web;

import com.bloodbridge.domain.User;
import com.bloodbridge.service.DriveService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drives")
public class DriveController {
    private final DriveService service;

    public DriveController(DriveService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_DRIVE_REGISTER','PERM_DRIVE_MANAGE')")
    public List<DriveDto> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DRIVE_MANAGE')")
    public DriveDto create(@AuthenticationPrincipal User actor, @Valid @RequestBody DriveCreate in) {
        return service.create(actor, in);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_DRIVE_MANAGE')")
    public DriveDto cancel(@AuthenticationPrincipal User actor, @PathVariable Long id) {
        return service.cancel(actor, id);
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAuthority('PERM_DRIVE_REGISTER')")
    public DriveDto register(@AuthenticationPrincipal User donor, @PathVariable Long id) {
        return service.register(donor, id);
    }

    @DeleteMapping("/{id}/register")
    @PreAuthorize("hasAuthority('PERM_DRIVE_REGISTER')")
    public DriveDto unregister(@AuthenticationPrincipal User donor, @PathVariable Long id) {
        return service.unregister(donor, id);
    }
}
