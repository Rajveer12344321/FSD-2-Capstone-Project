package com.bloodbridge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blood_group", "component_type"}))
@Getter @Setter @NoArgsConstructor
public class InventoryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false)
    private BloodComponent component;

    private int units;
    private int lowThreshold = 5;
    private Instant updatedAt = Instant.now();
}
