package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "donor_profiles")
@Getter @Setter @NoArgsConstructor
public class DonorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup bloodGroup;

    @Column(nullable = false)
    private String city;

    /** Simulated coordinates. Never exposed to other users. */
    private Double latitude;
    private Double longitude;

    private String phone;
    private boolean available;
    private Instant availabilityConfirmedAt;
    private LocalDate lastDonationDate;
    private boolean contactConsent;
    private boolean verified;
}
