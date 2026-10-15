package com.kltyton.darwin_soldier.client.aim;

/** Pure eligibility rules shared by trajectory rendering and ballistics learning. */
public final class TrajectoryFeaturePolicy {
    private TrajectoryFeaturePolicy() {
    }

    public static boolean isAvailable(boolean darwinEnabled, boolean trajectoryVisible) {
        return darwinEnabled && trajectoryVisible;
    }

    public static boolean isEligibleWeapon(boolean edible) {
        return !edible;
    }

    public static boolean shouldTrigger(boolean mainHandUse, boolean usingAnyItem, boolean useKeyDown,
                                        boolean attackKeyDown, boolean aimingActive) {
        if (usingAnyItem && !mainHandUse) {
            return false;
        }
        return mainHandUse || useKeyDown || attackKeyDown || aimingActive;
    }
}
