package com.kltyton.darwin_soldier.combat;

/**
 * Pure math for the defense minimum reduction feature.
 *
 * <p>The reduction is a flat damage value derived from the victim's defense points:
 * {@code max(0, defensePoints) / 10.0F}. It only fills a shortfall left by the
 * armor/magic/absorption pipeline; stronger existing reductions are preserved.</p>
 */
public final class DefenseReductionMath {
    private DefenseReductionMath() {
    }

    public static float minimumReduction(int defensePoints) {
        return Math.max(0, defensePoints) / 10.0F;
    }

    public static float cappedFinal(float finalAmount, float capturedPreArmorAmount, float minimumReduction) {
        return Math.min(finalAmount, Math.max(0.0F, capturedPreArmorAmount - minimumReduction));
    }
}
