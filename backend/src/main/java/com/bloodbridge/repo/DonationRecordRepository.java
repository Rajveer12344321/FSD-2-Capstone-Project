package com.bloodbridge.repo;

import com.bloodbridge.domain.BloodRequest;
import com.bloodbridge.domain.DonationRecord;
import com.bloodbridge.domain.Invitation;
import com.bloodbridge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRecordRepository extends JpaRepository<DonationRecord, Long> {
    List<DonationRecord> findByDonorOrderByDonatedOnDesc(User donor);
    List<DonationRecord> findTop100ByOrderByCreatedAtDesc();
    List<DonationRecord> findByRequest(BloodRequest request);
    boolean existsByInvitation(Invitation invitation);
}
