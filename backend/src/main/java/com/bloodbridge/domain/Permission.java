package com.bloodbridge.domain;

/** Fine-grained permissions. Roles are mapped to permissions in the database (editable by an administrator). */
public enum Permission {
    PROFILE_MANAGE("Donor", "Manage own donor profile and availability"),
    DRIVE_REGISTER("Donor", "Register for blood donation drives"),
    INVITATION_RESPOND("Donor", "View and answer donation invitations"),
    REQUEST_CREATE("Requests", "Create blood requests"),
    REQUEST_VIEW_OWN("Requests", "View and manage own requests"),
    REQUEST_VIEW_ALL("Requests", "View all requests and their ranked candidates"),
    REQUEST_MANAGE_ALL("Requests", "Re-run matching, fulfil or cancel any request"),
    QUEUE_VIEW("Coordination", "View the emergency priority queue"),
    URGENCY_REVIEW("Coordination", "Review and change request urgency"),
    DONOR_VERIFY("Coordination", "Verify or unverify donor profiles"),
    ANALYTICS_VIEW("Coordination", "View the analytics dashboard"),
    DELIVERY_RETRY("Coordination", "View failed notification deliveries and retry them"),
    INVENTORY_VIEW("Coordination", "View blood stock levels"),
    INVENTORY_MANAGE("Coordination", "Adjust blood stock and low-stock thresholds"),
    DONATION_RECORD("Coordination", "Record completed donations"),
    DRIVE_MANAGE("Coordination", "Create and cancel blood drives"),
    REPORT_EXPORT("Coordination", "Export CSV reports"),
    USER_VIEW("Administration", "View user accounts"),
    USER_MANAGE("Administration", "Create users, activate or deactivate accounts"),
    ROLE_ASSIGN("Administration", "Change a user's role"),
    PERMISSION_MANAGE("Administration", "Edit the role-permission matrix"),
    AUDIT_VIEW("Administration", "View the security audit log");

    private final String group;
    private final String description;

    Permission(String group, String description) {
        this.group = group;
        this.description = description;
    }

    public String group() { return group; }
    public String description() { return description; }

    /** Spring Security authority string, e.g. PERM_QUEUE_VIEW. */
    public String authority() { return "PERM_" + name(); }
}
