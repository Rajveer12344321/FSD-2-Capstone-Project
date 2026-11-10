package com.bloodbridge.repo;

import com.bloodbridge.domain.BloodDrive;
import com.bloodbridge.domain.DriveRegistration;
import com.bloodbridge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriveRegistrationRepository extends JpaRepository<DriveRegistration, Long> {
    long countByDrive(BloodDrive drive);
    long countByDonor(User donor);
    boolean existsByDriveAndDonor(BloodDrive drive, User donor);
    Optional<DriveRegistration> findByDriveAndDonor(BloodDrive drive, User donor);
    List<DriveRegistration> findByDrive(BloodDrive drive);
}
