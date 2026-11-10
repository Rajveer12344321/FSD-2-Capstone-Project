package com.bloodbridge.web;

import com.bloodbridge.domain.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Request and response payloads. Entities are never returned directly. */
public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotNull Role role) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserDto(Long id, String name, String email, Role role, boolean active, List<String> permissions) {
        public static UserDto of(User u, Collection<Permission> perms) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(),
                    perms.stream().map(Enum::name).sorted().toList());
        }
    }

    public record AuthResponse(String token, UserDto user) {}

    public record DonorProfileUpdate(@NotNull BloodGroup bloodGroup,
                                     @NotBlank @Size(max = 100) String city,
                                     @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                                     @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
                                     @Size(max = 30) String phone,
                                     boolean available,
                                     @PastOrPresent LocalDate lastDonationDate,
                                     boolean contactConsent) {}

    public record DonorProfileDto(boolean exists, BloodGroup bloodGroup, String city, Double latitude,
                                  Double longitude, String phone, boolean available,
                                  Instant availabilityConfirmedAt, LocalDate lastDonationDate,
                                  boolean contactConsent, boolean verified) {}

    public record RequestCreate(@NotNull BloodGroup bloodGroup,
                                @NotNull BloodComponent component,
                                @Min(1) @Max(20) int unitsRequired,
                                @NotBlank @Size(max = 150) String hospital,
                                @NotBlank @Size(max = 100) String city,
                                @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                                @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
                                @NotNull Urgency urgency,
                                @NotNull @Future Instant requiredBy) {}

    public record RequestDto(Long id, BloodGroup bloodGroup, BloodComponent component, int unitsRequired,
                             String hospital, String city, Urgency urgency, Instant requiredBy,
                             RequestStatus status, Instant createdAt, String requesterName, Long requesterId,
                             long invitedCount, long acceptedCount) {}

    public record CandidateDto(Long invitationId, String donorLabel, BloodGroup bloodGroup, String city,
                               double score, List<String> reasons, InvitationStatus status,
                               DeliveryStatus deliveryStatus, int attempts, boolean verified, String phone,
                               Double distanceKm, boolean donated) {}

    public record RequestDetailDto(RequestDto request, List<CandidateDto> candidates) {}

    public record InvitationDto(Long id, Long requestId, BloodGroup bloodGroup, BloodComponent component,
                                int unitsRequired, String hospital, String city, Urgency urgency,
                                Instant requiredBy, RequestStatus requestStatus, InvitationStatus status,
                                Instant createdAt, Instant respondedAt, boolean donated) {}

    public record RespondRequest(boolean accept) {}

    public record StatusUpdate(@NotNull RequestStatus status) {}

    public record UrgencyUpdate(@NotNull Urgency urgency) {}

    public record FlagUpdate(boolean value) {}

    public record NotificationDto(Long id, String message, boolean seen, Instant createdAt) {}

    public record QueueItemDto(int position, RequestDto request) {}

    public record AdminUserDto(Long id, String name, String email, Role role, boolean active,
                               Boolean verified, Instant createdAt) {}

    public record CreateUserRequest(@NotBlank @Size(max = 100) String name,
                                    @NotBlank @Email String email,
                                    @NotBlank @Size(min = 8, max = 100) String password,
                                    @NotNull Role role) {}

    public record RoleUpdate(@NotNull Role role) {}

    public record PermissionInfo(String name, String group, String description) {}

    public record RolePermissionsDto(Role role, boolean editable, List<String> permissions) {}

    public record PermissionMatrixDto(List<PermissionInfo> permissions, List<RolePermissionsDto> roles) {}

    public record PermissionUpdate(@NotNull List<String> permissions) {}

    public record AuditDto(Long id, String actor, String action, String target, String detail, Instant createdAt) {}

    public record FailedDeliveryDto(Long invitationId, Long requestId, String donorLabel, int attempts) {}

    public record DayCount(String date, long count) {}

    public record AnalyticsDto(long totalRequests, long requestsWithMatches, long invitationsSent,
                               long responsesReceived, double responseRatePercent,
                               Double avgMinutesToFirstResponse, Map<String, Long> outcomes,
                               long deliveryFailures, long pendingEmergencyRequests,
                               long lowStockCount, long totalDonations, long unitsDonated,
                               Map<String, Long> requestsByGroup, Map<String, Long> requestsByUrgency,
                               List<DayCount> dailyRequests) {}

    // ---- blood stock ----
    public record InventoryDto(Long id, BloodGroup bloodGroup, BloodComponent component, int units,
                               int lowThreshold, boolean low, Instant updatedAt) {}

    public record InventoryAdjust(@NotNull @Min(-500) @Max(500) Integer delta,
                                  @NotBlank @Size(max = 200) String reason) {}

    public record ThresholdUpdate(@Min(0) @Max(1000) int lowThreshold) {}

    // ---- donations and impact ----
    public record DonationCreate(@NotNull Long invitationId, @Min(1) @Max(5) int units) {}

    public record DonationDto(Long id, String donorLabel, BloodGroup bloodGroup, int units,
                              java.time.LocalDate donatedOn, Long requestId, String recordedBy) {}

    public record BadgeDto(String code, String label, String description, boolean earned) {}

    public record ImpactDto(long donations, long units, java.time.LocalDate lastDonation, long drivesJoined,
                            List<BadgeDto> badges, List<DonationDto> history) {}

    // ---- blood drives ----
    public record DriveCreate(@NotBlank @Size(max = 120) String title,
                              @NotBlank @Size(max = 150) String venue,
                              @NotBlank @Size(max = 100) String city,
                              @NotNull @Future Instant driveDate,
                              @Min(1) @Max(1000) int capacity,
                              @Size(max = 500) String description) {}

    public record DriveDto(Long id, String title, String venue, String city, Instant driveDate, int capacity,
                           long registered, String description, boolean cancelled, boolean registeredByMe) {}
}
