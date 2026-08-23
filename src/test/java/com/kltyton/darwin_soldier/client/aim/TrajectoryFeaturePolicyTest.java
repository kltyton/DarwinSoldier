package com.kltyton.darwin_soldier.client.aim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajectoryFeaturePolicyTest {
    @Test
    void availabilityRequiresDarwinAndPerWeaponOptIn() {
        assertFalse(TrajectoryFeaturePolicy.isAvailable(false, false));
        assertFalse(TrajectoryFeaturePolicy.isAvailable(false, true));
        assertFalse(TrajectoryFeaturePolicy.isAvailable(true, false));
        assertTrue(TrajectoryFeaturePolicy.isAvailable(true, true));
    }

    @Test
    void edibleItemsAreNeverTrajectoryWeapons() {
        assertFalse(TrajectoryFeaturePolicy.isEligibleWeapon(true));
        assertTrue(TrajectoryFeaturePolicy.isEligibleWeapon(false));
    }

    @Test
    void usingAnotherHandDoesNotTriggerTheMainHandPreview() {
        assertFalse(TrajectoryFeaturePolicy.shouldTrigger(false, true, true, false, false));
        assertFalse(TrajectoryFeaturePolicy.shouldTrigger(false, true, false, true, false));
        assertFalse(TrajectoryFeaturePolicy.shouldTrigger(false, true, false, false, true));
        assertTrue(TrajectoryFeaturePolicy.shouldTrigger(true, true, true, false, false));
    }

    @Test
    void idleUseKeyCanPreviewAnExplicitlyEnabledThrowable() {
        assertTrue(TrajectoryFeaturePolicy.shouldTrigger(false, false, true, false, false));
    }
}
