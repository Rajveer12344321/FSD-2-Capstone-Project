package com.bloodbridge.service;

import com.bloodbridge.domain.BloodRequest;
import com.bloodbridge.domain.RequestStatus;
import com.bloodbridge.repo.BloodRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Application-level request coordination queue (not clinical triage). Order: urgency
 * (Emergency, Urgent, Routine), then earliest required-by deadline, then earliest submission time,
 * then lowest id as a stable tie-breaker.
 */
@Service
public class QueueService {

    public static final Comparator<BloodRequest> ORDER =
            Comparator.comparingInt((BloodRequest r) -> r.getUrgency().ordinal())
                    .thenComparing(BloodRequest::getRequiredBy)
                    .thenComparing(BloodRequest::getCreatedAt)
                    .thenComparing(BloodRequest::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final BloodRequestRepository requests;
    private final InvitationService invitationService;
    private final ReentrantLock lock = new ReentrantLock();

    public QueueService(BloodRequestRepository requests, InvitationService invitationService) {
        this.requests = requests;
        this.invitationService = invitationService;
    }

    /** Orders the given requests by priority using a PriorityQueue. */
    public static List<BloodRequest> prioritise(Collection<BloodRequest> items) {
        PriorityQueue<BloodRequest> pq = new PriorityQueue<>(Math.max(1, items.size()), ORDER);
        pq.addAll(items);
        List<BloodRequest> ordered = new ArrayList<>();
        while (!pq.isEmpty()) ordered.add(pq.poll());
        return ordered;
    }

    @Transactional(readOnly = true)
    public List<BloodRequest> activeQueue() {
        return prioritise(requests.findByStatusIn(List.of(RequestStatus.OPEN, RequestStatus.MATCHING)));
    }

    /**
     * Matches and invites for every OPEN request, highest priority first. A request becomes MATCHING once
     * at least one donor was invited; otherwise it stays OPEN and is retried on the next run.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processPending() {
        lock.lock();
        try {
            int processed = 0;
            for (BloodRequest req : prioritise(requests.findByStatusIn(List.of(RequestStatus.OPEN)))) {
                int invited = invitationService.inviteForRequest(req);
                if (invited > 0) {
                    req.setStatus(RequestStatus.MATCHING);
                    requests.save(req);
                    processed++;
                }
            }
            return processed;
        } finally {
            lock.unlock();
        }
    }
}
