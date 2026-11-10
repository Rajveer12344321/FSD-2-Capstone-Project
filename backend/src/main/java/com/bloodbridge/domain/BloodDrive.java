package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "blood_drives")
@Getter @Setter @NoArgsConstructor
public class BloodDrive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String city;

    @Column(name = "drive_date", nullable = false)
    private Instant driveDate;

    private int capacity;

    @Column(length = 500)
    private String description;

    private boolean cancelled;
    private String createdBy;
    private Instant createdAt = Instant.now();
}
