package com.kltyton.darwin_soldier.client.aim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AimWeaponSettingsTest {
    @Test
    void newWeaponsDefaultToManualWithTrajectoryHidden() {
        AimWeaponSettings defaults = AimWeaponSettings.defaults();

        assertEquals(AimMode.MANUAL, defaults.mode());
        assertFalse(defaults.trajectoryVisible());
    }
}
