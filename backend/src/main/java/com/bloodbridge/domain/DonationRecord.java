package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** A completed donation recorded by authorised staff. Request and invitation are null for historical entries. */
@Entity
@Table(name = "donation_records")
@Getter @Setter @NoArgsConstructor
public class DonationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    @OneToOne
    @JoinColumn(name = "invitation_id", unique = true)
    private Invitation invitation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup bloodGroup;

    private int units = 1;
    private LocalDate donatedOn;
    private String recordedBy;
    private Instant createdAt = Instant.now();
}
