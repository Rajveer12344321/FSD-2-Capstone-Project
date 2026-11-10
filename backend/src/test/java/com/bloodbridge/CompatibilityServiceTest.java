package com.bloodbridge;

import com.bloodbridge.domain.BloodComponent;
import com.bloodbridge.domain.BloodGroup;
import com.bloodbridge.service.CompatibilityService;
import org.junit.jupiter.api.Test;

import static com.bloodbridge.domain.BloodComponent.*;
import static com.bloodbridge.domain.BloodGroup.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityServiceTest {
    private final CompatibilityService svc = new CompatibilityService();

    @Test
    void redCellsOnegativeIsUniversalDonor() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertTrue(svc.isCompatible(O_NEG, recipient, RED_CELLS), "O- to " + recipient);
        }
    }

    @Test
    void redCellsRhPositiveDonorCannotGiveToRhNegative() {
        assertFalse(svc.isCompatible(O_POS, O_NEG, RED_CELLS));
        assertFalse(svc.isCompatible(A_POS, A_NEG, RED_CELLS));
        assertTrue(svc.isCompatible(A_NEG, A_POS, RED_CELLS));
    }

    @Test
    void redCellsAboRules() {
        assertFalse(svc.isCompatible(A_POS, O_POS, RED_CELLS));
        assertFalse(svc.isCompatible(B_POS, A_POS, RED_CELLS));
        assertTrue(svc.isCompatible(A_POS, AB_POS, RED_CELLS));
        assertFalse(svc.isCompatible(AB_POS, A_POS, RED_CELLS));
    }

    @Test
    void plasmaFollowsReverseAboRule() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertTrue(svc.isCompatible(AB_NEG, recipient, PLASMA), "AB plasma to " + recipient);
        }
        assertFalse(svc.isCompatible(O_POS, A_POS, PLASMA));
        assertTrue(svc.isCompatible(A_POS, O_POS, PLASMA));
        assertTrue(svc.isCompatible(O_NEG, O_POS, PLASMA));
    }

    @Test
    void wholeBloodAndPlateletsNeedIdenticalAbo() {
        assertTrue(svc.isCompatible(A_POS, A_POS, WHOLE_BLOOD));
        assertFalse(svc.isCompatible(O_NEG, A_POS, WHOLE_BLOOD));
        assertFalse(svc.isCompatible(A_POS, A_NEG, WHOLE_BLOOD));
        assertTrue(svc.isCompatible(A_NEG, A_POS, PLATELETS));
        assertFalse(svc.isCompatible(O_POS, A_POS, PLATELETS));
    }
}
