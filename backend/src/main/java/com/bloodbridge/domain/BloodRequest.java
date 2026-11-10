package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "blood_requests")
@Getter @Setter @NoArgsConstructor
public class BloodRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodComponent component;

    private int unitsRequired;

    @Column(nullable = false)
    private String hospital;

    @Column(nullable = false)
    private String city;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(nullable = false)
    private Instant requiredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.OPEN;

    private Instant createdAt = Instant.now();

    /** Time of the first donor answer (accept or decline). Used for the response-time metric. */
    private Instant firstResponseAt;
}
