package com.bloodbridge.service;

import com.bloodbridge.domain.BloodDrive;
import com.bloodbridge.domain.DriveRegistration;
import com.bloodbridge.domain.Permission;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.BloodDriveRepository;
import com.bloodbridge.repo.DriveRegistrationRepository;
import com.bloodbridge.web.Dtos.DriveCreate;
import com.bloodbridge.web.Dtos.DriveDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** Blood donation drives (camps): staff publish them, donors register within capacity. */
@Service
public class DriveService {
    private final BloodDriveRepository drives;
    private final DriveRegistrationRepository registrations;
    private final PermissionService permissions;
    private final NotificationService notifications;
    private final AuditService audit;

    public DriveService(BloodDriveRepository drives, DriveRegistrationRepository registrations,
                        PermissionService permissions, NotificationService notifications, AuditService audit) {
        this.drives = drives;
        this.registrations = registrations;
        this.permissions = permissions;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DriveDto> list(User user) {
        List<BloodDrive> list = permissions.has(user, Permission.DRIVE_MANAGE)
                ? drives.findAllByOrderByDriveDateDesc()
                : drives.findByCancelledFalseAndDriveDateAfterOrderByDriveDateAsc(Instant.now());
        return list.stream().map(d -> toDto(d, user)).toList();
    }

    @Transactional
    public DriveDto create(User actor, DriveCreate in) {
        BloodDrive d = new BloodDrive();
        d.setTitle(in.title().trim());
        d.setVenue(in.venue().trim());
        d.setCity(in.city().trim());
        d.setDriveDate(in.driveDate());
        d.setCapacity(in.capacity());
        d.setDescription(in.description() == null || in.description().isBlank() ? null : in.description().trim());
        d.setCreatedBy(actor.getEmail());
        d = drives.save(d);
        audit.log(actor, "DRIVE_CREATED", "drive#" + d.getId(), d.getTitle());
        return toDto(d, actor);
    }

    @Transactional
    public DriveDto cancel(User actor, Long id) {
        BloodDrive d = find(id);
        if (!d.isCancelled()) {
            d.setCancelled(true);
            drives.save(d);
            for (DriveRegistration r : registrations.findByDrive(d)) {
                notifications.notifyUser(r.getDonor(), "The blood drive \"" + d.getTitle() + "\" was cancelled.");
            }
            audit.log(actor, "DRIVE_CANCELLED", "drive#" + id, d.getTitle());
        }
        return toDto(d, actor);
    }

    @Transactional
    public DriveDto register(User donor, Long id) {
        BloodDrive d = find(id);
        if (d.isCancelled() || d.getDriveDate().isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This drive is closed");
        if (registrations.existsByDriveAndDonor(d, donor))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already registered for this drive");
        if (registrations.countByDrive(d) >= d.getCapacity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This drive is full");
        DriveRegistration r = new DriveRegistration();
        r.setDrive(d);
        r.setDonor(donor);
        registrations.save(r);
        notifications.notifyUser(donor, "You are registered for \"" + d.getTitle() + "\" at " + d.getVenue() + ", " + d.getCity() + ".");
        return toDto(d, donor);
    }

    @Transactional
    public DriveDto unregister(User donor, Long id) {
        BloodDrive d = find(id);
        registrations.findByDriveAndDonor(d, donor).ifPresent(registrations::delete);
        return toDto(d, donor);
    }

    private BloodDrive find(Long id) {
        return drives.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drive not found"));
    }

    private DriveDto toDto(BloodDrive d, User viewer) {
        return new DriveDto(d.getId(), d.getTitle(), d.getVenue(), d.getCity(), d.getDriveDate(), d.getCapacity(),
                registrations.countByDrive(d), d.getDescription(), d.isCancelled(),
                registrations.existsByDriveAndDonor(d, viewer));
    }
}
