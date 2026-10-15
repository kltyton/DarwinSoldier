package com.kltyton.darwin_soldier.combat;

public final class CombatMath {
    private CombatMath() {
    }

    public static float minimumEffectiveDamage(int attackPoints, double attackPerPoint, double minimumRatio) {
        return (float) (Math.max(0, attackPoints) * Math.max(0.0D, attackPerPoint) * Math.max(0.0D, minimumRatio));
    }

    public static float applyMinimum(float actualDamage, float minimumDamage) {
        return actualDamage <= 0.0F ? actualDamage : Math.max(actualDamage, Math.max(0.0F, minimumDamage));
    }

    public static float missingDamage(float accumulatedDamage, float minimumDamage) {
        return Math.max(0.0F, minimumDamage - Math.max(0.0F, accumulatedDamage));
    }
}
