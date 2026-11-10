package com.bloodbridge.service;

import com.bloodbridge.domain.BloodComponent;
import com.bloodbridge.domain.BloodGroup;
import org.springframework.stereotype.Service;

/**
 * Prototype blood-group compatibility rules per component. These are simplified, textbook-level
 * rules for demonstration only and must be validated by qualified professionals before real use.
 */
@Service
public class CompatibilityService {

    /** True if a donor of {@code donor} group may preliminarily be considered for a {@code recipient}. */
    public boolean isCompatible(BloodGroup donor, BloodGroup recipient, BloodComponent component) {
        return switch (component) {
            case WHOLE_BLOOD -> donor.abo().equals(recipient.abo()) && rhOk(donor, recipient);
            case RED_CELLS -> redCellAbo(donor.abo(), recipient.abo()) && rhOk(donor, recipient);
            // Plasma follows the reverse ABO rule; Rh is not considered.
            case PLASMA -> redCellAbo(recipient.abo(), donor.abo());
            case PLATELETS -> donor.abo().equals(recipient.abo());
        };
    }

    private boolean rhOk(BloodGroup donor, BloodGroup recipient) {
        return !donor.rhPositive() || recipient.rhPositive();
    }

    private boolean redCellAbo(String donor, String recipient) {
        return switch (donor) {
            case "O" -> true;
            case "A" -> recipient.equals("A") || recipient.equals("AB");
            case "B" -> recipient.equals("B") || recipient.equals("AB");
            case "AB" -> recipient.equals("AB");
            default -> false;
        };
    }
}
