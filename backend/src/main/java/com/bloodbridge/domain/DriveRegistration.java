package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "drive_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"drive_id", "donor_id"}))
@Getter @Setter @NoArgsConstructor
public class DriveRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "drive_id")
    private BloodDrive drive;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    private Instant createdAt = Instant.now();
}
