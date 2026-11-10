package com.bloodbridge.service;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.BloodRequestRepository;
import com.bloodbridge.repo.DonationRecordRepository;
import com.bloodbridge.repo.InvitationRepository;
import com.bloodbridge.web.Dtos.AnalyticsDto;
import com.bloodbridge.web.Dtos.DayCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Metric definitions:
 * - responseRate = (ACCEPTED + DECLINED invitations) / all invitations sent, as a percentage.
 * - time to first response = minutes from request creation to the first accept or decline, averaged
 *   over requests that received at least one response.
 * - requestsWithMatches = requests with at least one invitation (an eligible candidate existed).
 * - pendingEmergency = EMERGENCY requests that are OPEN or MATCHING.
 * - lowStock = stock rows at or below their low-stock threshold.
 * - dailyRequests = requests created per UTC day over the last 14 days.
 */
@Service
public class AnalyticsService {
    private final BloodRequestRepository requests;
    private final InvitationRepository invitations;
    private final DonationRecordRepository donations;
    private final InventoryService inventory;

    public AnalyticsService(BloodRequestRepository requests, InvitationRepository invitations,
                            DonationRecordRepository donations, InventoryService inventory) {
        this.requests = requests;
        this.invitations = invitations;
        this.donations = donations;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public AnalyticsDto compute() {
        long invites = invitations.count();
        long responses = invitations.countByStatus(InvitationStatus.ACCEPTED)
                + invitations.countByStatus(InvitationStatus.DECLINED);
        double rate = invites == 0 ? 0 : Math.round(1000.0 * responses / invites) / 10.0;

        List<BloodRequest> all = requests.findAll();
        double avgMinutes = all.stream().filter(r -> r.getFirstResponseAt() != null)
                .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getFirstResponseAt()).toSeconds())
                .average().orElse(Double.NaN) / 60.0;

        Map<String, Long> outcomes = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) outcomes.put(s.name(), requests.countByStatus(s));

        Map<String, Long> byGroup = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) byGroup.put(g.label(), 0L);
        Map<String, Long> byUrgency = new LinkedHashMap<>();
        for (Urgency u : Urgency.values()) byUrgency.put(u.name(), 0L);
        Map<LocalDate, Long> perDay = new TreeMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 13; i >= 0; i--) perDay.put(today.minusDays(i), 0L);
        for (BloodRequest r : all) {
            byGroup.merge(r.getBloodGroup().label(), 1L, Long::sum);
            byUrgency.merge(r.getUrgency().name(), 1L, Long::sum);
            LocalDate day = r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (perDay.containsKey(day)) perDay.merge(day, 1L, Long::sum);
        }
        List<DayCount> daily = new ArrayList<>();
        perDay.forEach((d, c) -> daily.add(new DayCount(d.toString(), c)));

        List<DonationRecord> records = donations.findAll();
        long unitsDonated = records.stream().mapToLong(DonationRecord::getUnits).sum();

        return new AnalyticsDto(all.size(), invitations.countRequestsWithInvitations(), invites, responses,
                rate, Double.isNaN(avgMinutes) ? null : Math.round(avgMinutes * 10) / 10.0, outcomes,
                invitations.countByDeliveryStatus(DeliveryStatus.FAILED),
                requests.countByUrgencyAndStatusIn(Urgency.EMERGENCY,
                        List.of(RequestStatus.OPEN, RequestStatus.MATCHING)),
                inventory.lowStockCount(), records.size(), unitsDonated, byGroup, byUrgency, daily);
    }
}
