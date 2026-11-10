package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "invitations", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "donor_id"}))
@Getter @Setter @NoArgsConstructor
public class Invitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    private double score;

    /** Simulated distance from the donor to the hospital in km (null when a location is missing). */
    private Double distanceKm;

    /** Ranking explanation, one factor per line. */
    @Column(length = 1500)
    private String reasons;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    private int attempts;
    private Instant createdAt = Instant.now();
    private Instant respondedAt;

    /** Optimistic lock: stops two concurrent answers to the same invitation both succeeding. */
    @Version
    private Long version;
}
