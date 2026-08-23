package com.kltyton.darwin_soldier.client.aim;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimWeaponSettingsStoreMigrationTest {
    @Test
    void schemaTwoAdaptiveAndTrajectoryFlagsAreResetOnce() {
        JsonObject legacy = storedAdaptiveSettings();

        AimWeaponSettings migrated = AimWeaponSettingsStore.decodeSettings(legacy, 2);

        assertEquals(AimMode.MANUAL, migrated.mode());
        assertFalse(migrated.trajectoryVisible());
    }

    @Test
    void schemaThreeExplicitPlayerChoicesPersist() {
        JsonObject current = storedAdaptiveSettings();

        AimWeaponSettings decoded = AimWeaponSettingsStore.decodeSettings(current, 3);

        assertEquals(AimMode.ADAPTIVE, decoded.mode());
        assertTrue(decoded.trajectoryVisible());
    }

    private static JsonObject storedAdaptiveSettings() {
        JsonObject value = new JsonObject();
        value.addProperty("mode", AimMode.ADAPTIVE.name());
        value.addProperty("trajectoryVisible", true);
        return value;
    }
}
