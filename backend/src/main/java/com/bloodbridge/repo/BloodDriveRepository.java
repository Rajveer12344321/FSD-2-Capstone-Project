package com.bloodbridge.repo;

import com.bloodbridge.domain.BloodDrive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BloodDriveRepository extends JpaRepository<BloodDrive, Long> {
    List<BloodDrive> findAllByOrderByDriveDateDesc();
    List<BloodDrive> findByCancelledFalseAndDriveDateAfterOrderByDriveDateAsc(Instant after);
}
