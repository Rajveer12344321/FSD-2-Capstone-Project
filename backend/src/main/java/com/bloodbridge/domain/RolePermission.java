package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_name", "permission_name"}))
@Getter @Setter @NoArgsConstructor
public class RolePermission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_name", nullable = false)
    private Permission permission;

    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }
}
