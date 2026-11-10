package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", length = 150)
    private String actor;

    @Column(name = "action_name", length = 60, nullable = false)
    private String action;

    @Column(name = "target_ref", length = 150)
    private String target;

    @Column(name = "detail_text", length = 500)
    private String detail;

    private Instant createdAt = Instant.now();
}
