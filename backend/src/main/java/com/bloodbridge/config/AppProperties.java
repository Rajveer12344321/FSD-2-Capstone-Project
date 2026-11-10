package com.bloodbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bloodbridge")
public record AppProperties(
        boolean seedDemoData,
        List<String> allowedOrigins,
        Jwt jwt,
        Admin admin,
        Matching matching,
        Notifications notifications) {

    public record Jwt(String secret, long expiryMinutes) {}

    public record Admin(String email, String password) {}

    /** All weights are relative; the score is normalised to 0-100. Values are prototype defaults. */
    public record Matching(double wDistance, double wRecency, double wLogistics, double wVerified,
                           double maxDistanceKm, int recencyWindowHours, int minDonationIntervalDays,
                           double assumedSpeedKmh, int inviteMultiplier, int maxInvitesPerRequest) {}

    public record Notifications(boolean emailEnabled, String from, int maxAttempts) {}
}
