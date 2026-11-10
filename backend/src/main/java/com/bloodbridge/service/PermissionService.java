package com.bloodbridge.service;

import com.bloodbridge.domain.Permission;
import com.bloodbridge.domain.Role;
import com.bloodbridge.domain.RolePermission;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.RolePermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bloodbridge.domain.Permission.*;

/**
 * Role-based access control. The role to permission matrix lives in the role_permissions table and can be edited
 * by an administrator. ADMIN always has the fixed administrator set and cannot be edited, so nobody can lock
 * themselves out. Permissions are loaded into authorities by the JWT filter on every request, so changes take
 * effect immediately.
 */
@Service
public class PermissionService {

    public static final Set<Permission> ADMIN_PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            REQUEST_VIEW_ALL, REQUEST_MANAGE_ALL, QUEUE_VIEW, URGENCY_REVIEW, DONOR_VERIFY, ANALYTICS_VIEW,
            DELIVERY_RETRY, INVENTORY_VIEW, INVENTORY_MANAGE, DONATION_RECORD, DRIVE_MANAGE, REPORT_EXPORT,
            USER_VIEW, USER_MANAGE, ROLE_ASSIGN, PERMISSION_MANAGE, AUDIT_VIEW));

    public static final Map<Role, Set<Permission>> DEFAULTS = Map.of(
            Role.DONOR, EnumSet.of(PROFILE_MANAGE, INVITATION_RESPOND, DRIVE_REGISTER),
            Role.REQUESTER, EnumSet.of(REQUEST_CREATE, REQUEST_VIEW_OWN),
            Role.MODERATOR, EnumSet.of(REQUEST_VIEW_ALL, QUEUE_VIEW, URGENCY_REVIEW, DONOR_VERIFY,
                    ANALYTICS_VIEW, DELIVERY_RETRY, INVENTORY_VIEW, INVENTORY_MANAGE, DONATION_RECORD,
                    DRIVE_MANAGE, REPORT_EXPORT, USER_VIEW));

    private final RolePermissionRepository repo;
    private volatile Map<Role, Set<Permission>> cache = Map.of();

    public PermissionService(RolePermissionRepository repo) {
        this.repo = repo;
    }

    /** Writes the default matrix the first time the application starts with an empty table. */
    @Transactional
    public void seedDefaults() {
        if (repo.count() == 0) {
            List<RolePermission> rows = new ArrayList<>();
            DEFAULTS.forEach((role, perms) -> perms.forEach(p -> rows.add(new RolePermission(role, p))));
            repo.saveAll(rows);
        }
        reload();
    }

    public synchronized void reload() {
        Map<Role, Set<Permission>> map = new EnumMap<>(Role.class);
        for (RolePermission rp : repo.findAll()) {
            map.computeIfAbsent(rp.getRole(), r -> EnumSet.noneOf(Permission.class)).add(rp.getPermission());
        }
        map.put(Role.ADMIN, ADMIN_PERMISSIONS);
        cache = map;
    }

    public Set<Permission> forRole(Role role) {
        if (cache.isEmpty()) reload();
        return cache.getOrDefault(role, Set.of());
    }

    public boolean has(User user, Permission permission) {
        return forRole(user.getRole()).contains(permission);
    }

    public Map<Role, Set<Permission>> matrix() {
        Map<Role, Set<Permission>> out = new EnumMap<>(Role.class);
        for (Role r : Role.values()) out.put(r, forRole(r));
        return out;
    }

    @Transactional
    public void update(Role role, Set<Permission> permissions) {
        if (role == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The administrator role is fixed and cannot be edited");
        repo.deleteAllForRole(role);
        List<RolePermission> rows = new ArrayList<>();
        permissions.forEach(p -> rows.add(new RolePermission(role, p)));
        repo.saveAll(rows);
        reload();
    }
}
