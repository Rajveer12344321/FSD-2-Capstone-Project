package com.bloodbridge.repo;

import com.bloodbridge.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findByRequestOrderByScoreDesc(BloodRequest request);
    List<Invitation> findByDonorOrderByCreatedAtDesc(User donor);
    List<Invitation> findByRequestAndStatus(BloodRequest request, InvitationStatus status);
    List<Invitation> findByDeliveryStatus(DeliveryStatus deliveryStatus);
    List<Invitation> findByDeliveryStatusAndAttemptsLessThan(DeliveryStatus deliveryStatus, int attempts);

    boolean existsByRequestAndDonor(BloodRequest request, User donor);
    long countByRequest(BloodRequest request);
    long countByRequestAndStatus(BloodRequest request, InvitationStatus status);
    long countByRequestAndStatusIn(BloodRequest request, Collection<InvitationStatus> statuses);
    long countByStatus(InvitationStatus status);
    long countByDeliveryStatus(DeliveryStatus deliveryStatus);

    @Query("select count(distinct i.request.id) from Invitation i")
    long countRequestsWithInvitations();

    /** True if the donor already accepted a different request that is still active. */
    @Query("select count(i) > 0 from Invitation i where i.donor = :donor and i.status = :status "
            + "and i.request.status in :statuses and i.request <> :request")
    boolean hasCommitment(@Param("donor") User donor,
                          @Param("status") InvitationStatus status,
                          @Param("statuses") Collection<RequestStatus> statuses,
                          @Param("request") BloodRequest request);
}
