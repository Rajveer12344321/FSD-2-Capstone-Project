package com.bloodbridge.repo;

import com.bloodbridge.domain.BloodRequest;
import com.bloodbridge.domain.RequestStatus;
import com.bloodbridge.domain.Urgency;
import com.bloodbridge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByRequesterOrderByCreatedAtDesc(User requester);
    List<BloodRequest> findAllByOrderByCreatedAtDesc();
    List<BloodRequest> findByStatusIn(Collection<RequestStatus> statuses);
    long countByStatus(RequestStatus status);
    long countByUrgencyAndStatusIn(Urgency urgency, Collection<RequestStatus> statuses);
}
