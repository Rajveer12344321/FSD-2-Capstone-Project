package com.bloodbridge.repo;

import com.bloodbridge.domain.DonorProfile;
import com.bloodbridge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {
    Optional<DonorProfile> findByUser(User user);
    Optional<DonorProfile> findByUserId(Long userId);
    List<DonorProfile> findByAvailableTrueAndContactConsentTrue();
}
