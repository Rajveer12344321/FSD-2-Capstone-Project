package com.bloodbridge;

import com.bloodbridge.domain.BloodRequest;
import com.bloodbridge.domain.Urgency;
import com.bloodbridge.service.QueueService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueOrderingTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private BloodRequest req(long id, Urgency u, long deadlineHours, long createdMinutes) {
        BloodRequest r = new BloodRequest();
        r.setId(id);
        r.setUrgency(u);
        r.setRequiredBy(BASE.plusSeconds(deadlineHours * 3600));
        r.setCreatedAt(BASE.plusSeconds(createdMinutes * 60));
        return r;
    }

    @Test
    void ordersByUrgencyThenDeadlineThenSubmissionThenId() {
        BloodRequest routineEarly = req(1, Urgency.ROUTINE, 1, 0);
        BloodRequest urgentLate = req(2, Urgency.URGENT, 10, 0);
        BloodRequest urgentSoon = req(3, Urgency.URGENT, 5, 0);
        BloodRequest emergencyLate = req(4, Urgency.EMERGENCY, 48, 0);
        BloodRequest tieB = req(6, Urgency.ROUTINE, 100, 5);
        BloodRequest tieA = req(5, Urgency.ROUTINE, 100, 5);
        BloodRequest tieEarlierSubmit = req(7, Urgency.ROUTINE, 100, 1);

        List<BloodRequest> ordered = QueueService.prioritise(
                List.of(routineEarly, urgentLate, urgentSoon, emergencyLate, tieB, tieA, tieEarlierSubmit));

        assertEquals(List.of(4L, 3L, 2L, 1L, 7L, 5L, 6L), ordered.stream().map(BloodRequest::getId).toList());
    }
}
