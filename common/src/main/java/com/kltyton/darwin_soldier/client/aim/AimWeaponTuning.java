package com.kltyton.darwin_soldier.client.aim;

import net.minecraft.util.Mth;

public record AimWeaponTuning(double leadMultiplier, double dropMultiplier, double smooth) {
    public static final double DEFAULT_LEAD = 0.0D;
    public static final double DEFAULT_DROP = 0.0D;
    public static final double DEFAULT_SMOOTH = 0.35D;
    public static final double MIN_LEAD = 0.0D;
    public static final double MAX_LEAD = 3.0D;
    public static final double MIN_DROP = 0.0D;
    public static final double MAX_DROP = 3.0D;
    public static final double MIN_SMOOTH = 0.05D;
    public static final double MAX_SMOOTH = 1.0D;

    public static AimWeaponTuning defaults() {
        return new AimWeaponTuning(DEFAULT_LEAD, DEFAULT_DROP, DEFAULT_SMOOTH);
    }

    public static AimWeaponTuning adaptiveDefaults() {
        return new AimWeaponTuning(1.0D, 1.0D, DEFAULT_SMOOTH);
    }

    public AimWeaponTuning sanitized() {
        return new AimWeaponTuning(
                finiteClamp(leadMultiplier, DEFAULT_LEAD, MIN_LEAD, MAX_LEAD),
                finiteClamp(dropMultiplier, DEFAULT_DROP, MIN_DROP, MAX_DROP),
                finiteClamp(smooth, DEFAULT_SMOOTH, MIN_SMOOTH, MAX_SMOOTH)
        );
    }

    private static double finiteClamp(double value, double fallback, double minimum, double maximum) {
        return Double.isFinite(value) ? Mth.clamp(value, minimum, maximum) : fallback;
    }
}
