package com.bloodbridge.service;

import com.bloodbridge.config.AppProperties;
import com.bloodbridge.domain.*;
import com.bloodbridge.repo.DonorProfileRepository;
import com.bloodbridge.repo.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Explainable, rule-based matching. Stage 1 filters donors with mandatory rules; stage 2 ranks the
 * remaining candidates with a weighted score (0-100) and records a reason for each factor.
 * No machine learning is used and no outcome is guaranteed.
 */
@Service
public class MatchingService {

    public record Candidate(DonorProfile profile, double score, List<String> reasons, Double distanceKm) {}

    private final DonorProfileRepository profiles;
    private final InvitationRepository invitations;
    private final CompatibilityService compatibility;
    private final AppProperties props;

    public MatchingService(DonorProfileRepository profiles, InvitationRepository invitations,
                           CompatibilityService compatibility, AppProperties props) {
        this.profiles = profiles;
        this.invitations = invitations;
        this.compatibility = compatibility;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<Candidate> rank(BloodRequest request) {
        AppProperties.Matching m = props.matching();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Candidate> result = new ArrayList<>();

        for (DonorProfile p : profiles.findByAvailableTrueAndContactConsentTrue()) {
            User donor = p.getUser();
            // ---- Stage 1: mandatory filters ----
            if (!donor.isActive()) continue;
            if (donor.getId().equals(request.getRequester().getId())) continue;
            if (p.getBloodGroup() == null
                    || !compatibility.isCompatible(p.getBloodGroup(), request.getBloodGroup(), request.getComponent())) continue;
            if (p.getAvailabilityConfirmedAt() == null) continue;
            if (p.getLastDonationDate() != null
                    && ChronoUnit.DAYS.between(p.getLastDonationDate(), today) < m.minDonationIntervalDays()) continue;
            if (invitations.hasCommitment(donor, InvitationStatus.ACCEPTED,
                    List.of(RequestStatus.OPEN, RequestStatus.MATCHING), request)) continue;

            // ---- Stage 2: weighted ranking ----
            Double km = distanceKm(p, request);
            double distanceScore = km == null ? 0 : Math.max(0, 1 - km / m.maxDistanceKm());
            double hoursSince = Duration.between(p.getAvailabilityConfirmedAt(), now).toMinutes() / 60.0;
            double recencyScore = Math.max(0, 1 - hoursSince / m.recencyWindowHours());
            double hoursLeft = Duration.between(now, request.getRequiredBy()).toMinutes() / 60.0;
            boolean reachable = km != null && km / m.assumedSpeedKmh() <= hoursLeft;
            double logisticsScore = reachable ? 1 : 0;
            double verifiedScore = p.isVerified() ? 1 : 0;

            double wSum = m.wDistance() + m.wRecency() + m.wLogistics() + m.wVerified();
            double pDist = 100 * m.wDistance() * distanceScore / wSum;
            double pRec = 100 * m.wRecency() * recencyScore / wSum;
            double pLog = 100 * m.wLogistics() * logisticsScore / wSum;
            double pVer = 100 * m.wVerified() * verifiedScore / wSum;
            double total = pDist + pRec + pLog + pVer;

            List<String> reasons = List.of(
                    km == null ? "Proximity: location unavailable (+0.0)"
                            : String.format(Locale.ROOT, "Proximity: %.1f km from hospital (+%.1f)", km, pDist),
                    String.format(Locale.ROOT, "Availability confirmed %.0f h ago (+%.1f)", hoursSince, pRec),
                    reachable
                            ? String.format(Locale.ROOT, "Logistics: reachable before the deadline (+%.1f)", pLog)
                            : "Logistics: may not reach before the deadline (+0.0)",
                    p.isVerified()
                            ? String.format(Locale.ROOT, "Verified donor profile (+%.1f)", pVer)
                            : "Donor profile not yet verified (+0.0)");
            result.add(new Candidate(p, Math.round(total * 10) / 10.0, reasons,
                    km == null ? null : Math.round(km * 10) / 10.0));
        }
        result.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(c -> c.profile().getId()));
        return result;
    }

    private Double distanceKm(DonorProfile p, BloodRequest r) {
        if (p.getLatitude() == null || p.getLongitude() == null
                || r.getLatitude() == null || r.getLongitude() == null) return null;
        double dLat = Math.toRadians(r.getLatitude() - p.getLatitude());
        double dLon = Math.toRadians(r.getLongitude() - p.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(p.getLatitude())) * Math.cos(Math.toRadians(r.getLatitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
