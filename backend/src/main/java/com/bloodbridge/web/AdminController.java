package com.bloodbridge.web;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.DonorProfileRepository;
import com.bloodbridge.repo.InvitationRepository;
import com.bloodbridge.repo.UserRepository;
import com.bloodbridge.service.*;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Coordination and administration endpoints. Every endpoint is guarded by a specific permission. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AnalyticsService analytics;
    private final QueueService queueService;
    private final RequestService requestService;
    private final InvitationService invitationService;
    private final InvitationRepository invitations;
    private final UserRepository userRepo;
    private final DonorProfileRepository profiles;
    private final PermissionService permissions;
    private final AuditService audit;
    private final PasswordEncoder encoder;

    public AdminController(AnalyticsService analytics, QueueService queueService, RequestService requestService,
                           InvitationService invitationService, InvitationRepository invitations,
                           UserRepository userRepo, DonorProfileRepository profiles,
                           PermissionService permissions, AuditService audit, PasswordEncoder encoder) {
        this.analytics = analytics;
        this.queueService = queueService;
        this.requestService = requestService;
        this.invitationService = invitationService;
        this.invitations = invitations;
        this.userRepo = userRepo;
        this.profiles = profiles;
        this.permissions = permissions;
        this.audit = audit;
        this.encoder = encoder;
    }

    // ---------- coordination ----------

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS_VIEW')")
    public AnalyticsDto analytics() {
        return analytics.compute();
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('PERM_QUEUE_VIEW')")
    @Transactional(readOnly = true)
    public List<QueueItemDto> queue() {
        AtomicInteger pos = new AtomicInteger(1);
        return queueService.activeQueue().stream()
                .map(r -> new QueueItemDto(pos.getAndIncrement(), requestService.toDto(r))).toList();
    }

    @PatchMapping("/requests/{id}/urgency")
    @PreAuthorize("hasAuthority('PERM_URGENCY_REVIEW')")
    public void urgency(@AuthenticationPrincipal User actor, @PathVariable Long id,
                        @Valid @RequestBody UrgencyUpdate in) {
        requestService.changeUrgency(id, in.urgency(), actor);
    }

    @PatchMapping("/donors/{userId}/verify")
    @PreAuthorize("hasAuthority('PERM_DONOR_VERIFY')")
    @Transactional
    public void verify(@AuthenticationPrincipal User actor, @PathVariable Long userId, @RequestBody FlagUpdate in) {
        DonorProfile p = profiles.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor profile not found"));
        p.setVerified(in.value());
        profiles.save(p);
        audit.log(actor, in.value() ? "DONOR_VERIFIED" : "DONOR_UNVERIFIED", "user#" + userId, null);
    }

    @GetMapping("/failed-deliveries")
    @PreAuthorize("hasAuthority('PERM_DELIVERY_RETRY')")
    @Transactional(readOnly = true)
    public List<FailedDeliveryDto> failed() {
        return invitations.findByDeliveryStatus(DeliveryStatus.FAILED).stream()
                .map(i -> new FailedDeliveryDto(i.getId(), i.getRequest().getId(),
                        RequestService.shortName(i.getDonor().getName()), i.getAttempts())).toList();
    }

    @PostMapping("/invitations/{id}/retry")
    @PreAuthorize("hasAuthority('PERM_DELIVERY_RETRY')")
    public void retry(@AuthenticationPrincipal User actor, @PathVariable Long id) {
        invitationService.retryOne(id);
        audit.log(actor, "DELIVERY_RETRY", "invitation#" + id, null);
    }

    // ---------- users and roles ----------

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('PERM_USER_VIEW')")
    @Transactional(readOnly = true)
    public List<AdminUserDto> users() {
        return userRepo.findAll().stream().map(u -> new AdminUserDto(u.getId(), u.getName(), u.getEmail(),
                u.getRole(), u.isActive(),
                u.getRole() == Role.DONOR ? profiles.findByUser(u).map(DonorProfile::isVerified).orElse(null) : null,
                u.getCreatedAt())).toList();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    @Transactional
    public void createUser(@AuthenticationPrincipal User actor, @Valid @RequestBody CreateUserRequest in) {
        if (in.role() == Role.ADMIN && actor.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can create administrators");
        String email = in.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        User u = new User();
        u.setName(in.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(in.password()));
        u.setRole(in.role());
        u = userRepo.save(u);
        audit.log(actor, "USER_CREATED", "user#" + u.getId(), email + " as " + in.role());
    }

    @PatchMapping("/users/{id}/active")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    @Transactional
    public void setActive(@AuthenticationPrincipal User actor, @PathVariable Long id, @RequestBody FlagUpdate in) {
        User u = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own account status");
        if (u.getRole() == Role.ADMIN && actor.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can change an administrator");
        u.setActive(in.value());
        userRepo.save(u);
        audit.log(actor, in.value() ? "USER_ACTIVATED" : "USER_DEACTIVATED", "user#" + id, u.getEmail());
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('PERM_ROLE_ASSIGN')")
    @Transactional
    public void setRole(@AuthenticationPrincipal User actor, @PathVariable Long id, @Valid @RequestBody RoleUpdate in) {
        User u = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        if ((u.getRole() == Role.ADMIN || in.role() == Role.ADMIN) && actor.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can grant or remove the administrator role");
        Role before = u.getRole();
        u.setRole(in.role());
        userRepo.save(u);
        audit.log(actor, "ROLE_CHANGED", "user#" + id, u.getEmail() + ": " + before + " -> " + in.role());
    }

    // ---------- permission matrix and audit ----------

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_PERMISSION_MANAGE')")
    public PermissionMatrixDto matrix() {
        List<PermissionInfo> all = Arrays.stream(Permission.values())
                .map(p -> new PermissionInfo(p.name(), p.group(), p.description())).toList();
        List<RolePermissionsDto> roles = Arrays.stream(Role.values())
                .map(r -> new RolePermissionsDto(r, r != Role.ADMIN,
                        permissions.forRole(r).stream().map(Enum::name).sorted().toList())).toList();
        return new PermissionMatrixDto(all, roles);
    }

    @PutMapping("/permissions/{role}")
    @PreAuthorize("hasAuthority('PERM_PERMISSION_MANAGE')")
    public PermissionMatrixDto updateRole(@AuthenticationPrincipal User actor, @PathVariable Role role,
                                          @Valid @RequestBody PermissionUpdate in) {
        if (actor.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can edit permissions");
        Set<Permission> wanted = EnumSet.noneOf(Permission.class);
        for (String name : in.permissions()) {
            try {
                wanted.add(Permission.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown permission: " + name);
            }
        }
        permissions.update(role, wanted);
        audit.log(actor, "PERMISSIONS_UPDATED", "role " + role, wanted.toString());
        return matrix();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('PERM_AUDIT_VIEW')")
    public List<AuditDto> auditLog() {
        return audit.recent();
    }
}
