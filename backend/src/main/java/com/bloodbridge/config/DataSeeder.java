package com.bloodbridge.config;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.BloodDriveRepository;
import com.bloodbridge.repo.DonationRecordRepository;
import com.bloodbridge.repo.DonorProfileRepository;
import com.bloodbridge.repo.UserRepository;
import com.bloodbridge.service.InventoryService;
import com.bloodbridge.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Creates the administrator account and, optionally, clearly synthetic demonstration users. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@12345";

    private final UserRepository users;
    private final DonorProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final PermissionService permissions;
    private final InventoryService inventory;
    private final BloodDriveRepository drives;
    private final DonationRecordRepository donations;

    public DataSeeder(UserRepository users, DonorProfileRepository profiles, PasswordEncoder encoder,
                      AppProperties props, PermissionService permissions, InventoryService inventory,
                      BloodDriveRepository drives, DonationRecordRepository donations) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.props = props;
        this.permissions = permissions;
        this.inventory = inventory;
        this.drives = drives;
        this.donations = donations;
    }

    @Override
    @Transactional
    public void run(String... args) {
        permissions.seedDefaults();
        inventory.seedIfEmpty(props.seedDemoData());
        String adminEmail = props.admin().email().toLowerCase();
        if (!users.existsByEmail(adminEmail)) {
            create("Administrator", adminEmail, props.admin().password(), Role.ADMIN);
            log.info("Created administrator account {}", adminEmail);
        }
        if ("Admin@12345".equals(props.admin().password()))
            log.warn("The default administrator password is in use. Set ADMIN_PASSWORD before any shared deployment.");
        if (props.jwt().secret().startsWith("dev-only-secret"))
            log.warn("The default JWT secret is in use. Set JWT_SECRET before any shared deployment.");

        if (!props.seedDemoData() || users.existsByEmail("requester@demo.bloodbridge.local")) return;

        create("Demo Requester", "requester@demo.bloodbridge.local", DEMO_PASSWORD, Role.REQUESTER);
        create("Demo Coordinator", "coordinator@demo.bloodbridge.local", DEMO_PASSWORD, Role.MODERATOR);

        // Simulated locations around Chandigarh tricity. All people and numbers are synthetic.
        Object[][] donors = {
                {"Demo Donor One", BloodGroup.O_NEG, "Chandigarh", 30.7333, 76.7794, true},
                {"Demo Donor Two", BloodGroup.O_POS, "Chandigarh", 30.7410, 76.7680, false},
                {"Demo Donor Three", BloodGroup.A_POS, "Mohali", 30.7046, 76.7179, true},
                {"Demo Donor Four", BloodGroup.A_NEG, "Panchkula", 30.6942, 76.8606, false},
                {"Demo Donor Five", BloodGroup.B_POS, "Zirakpur", 30.6425, 76.8173, true},
                {"Demo Donor Six", BloodGroup.B_NEG, "Kharar", 30.7469, 76.6469, false},
                {"Demo Donor Seven", BloodGroup.AB_POS, "Chandigarh", 30.7200, 76.8000, true},
                {"Demo Donor Eight", BloodGroup.AB_NEG, "Mohali", 30.7100, 76.7300, false},
                {"Demo Donor Nine", BloodGroup.O_POS, "Panchkula", 30.7000, 76.8500, true},
                {"Demo Donor Ten", BloodGroup.O_NEG, "Mohali", 30.6900, 76.7400, false},
                {"Demo Donor Eleven", BloodGroup.A_POS, "Chandigarh", 30.7500, 76.7900, false},
                {"Demo Donor Twelve", BloodGroup.B_POS, "Chandigarh", 30.7250, 76.7700, true},
        };
        int i = 1;
        for (Object[] d : donors) {
            User u = create((String) d[0], String.format("donor%02d@demo.bloodbridge.local", i), DEMO_PASSWORD, Role.DONOR);
            DonorProfile p = new DonorProfile();
            p.setUser(u);
            p.setBloodGroup((BloodGroup) d[1]);
            p.setCity((String) d[2]);
            p.setLatitude((Double) d[3]);
            p.setLongitude((Double) d[4]);
            p.setVerified((Boolean) d[5]);
            p.setPhone("+91-00000-000" + String.format("%02d", i));
            p.setAvailable(true);
            p.setContactConsent(true);
            p.setAvailabilityConfirmedAt(Instant.now().minus(i * 6L, ChronoUnit.HOURS));
            p.setLastDonationDate(i % 4 == 0 ? LocalDate.now().minusDays(30) : LocalDate.now().minusDays(200));
            profiles.save(p);
            if (i <= 3) {
                DonationRecord past = new DonationRecord();
                past.setDonor(u);
                past.setBloodGroup(p.getBloodGroup());
                past.setUnits(1);
                past.setDonatedOn(LocalDate.now().minusDays(200L + i * 30L));
                past.setRecordedBy("seed");
                donations.save(past);
            }
            i++;
        }
        BloodDrive d1 = new BloodDrive();
        d1.setTitle("Community Blood Drive");
        d1.setVenue("Sector 17 Community Hall");
        d1.setCity("Chandigarh");
        d1.setDriveDate(Instant.now().plus(7, ChronoUnit.DAYS));
        d1.setCapacity(60);
        d1.setDescription("Walk-in and pre-registered donors welcome. Demonstration event with synthetic data.");
        d1.setCreatedBy("seed");
        drives.save(d1);
        BloodDrive d2 = new BloodDrive();
        d2.setTitle("Campus Donation Camp");
        d2.setVenue("University Auditorium");
        d2.setCity("Mohali");
        d2.setDriveDate(Instant.now().plus(14, ChronoUnit.DAYS));
        d2.setCapacity(120);
        d2.setDescription("Organised with a partner blood bank. Demonstration event with synthetic data.");
        d2.setCreatedBy("seed");
        drives.save(d2);
        log.info("Seeded synthetic demonstration data (password for demo accounts: {})", DEMO_PASSWORD);
    }

    private User create(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        return users.save(u);
    }
}
