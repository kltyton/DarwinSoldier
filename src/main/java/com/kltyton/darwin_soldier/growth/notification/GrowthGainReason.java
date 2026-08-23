package com.kltyton.darwin_soldier.growth.notification;

import java.util.Objects;

/** Server-selected explanation for the final growth multiplier shown by the HUD. */
public record GrowthGainReason(Type type, double multiplier) {
    public GrowthGainReason {
        Objects.requireNonNull(type, "type");
    }

    public static GrowthGainReason normal() {
        return new GrowthGainReason(Type.NORMAL, 1.0D);
    }

    public static GrowthGainReason petKill(double multiplier) {
        return new GrowthGainReason(Type.PET_KILL, multiplier);
    }

    public static GrowthGainReason safeCombat(double multiplier) {
        return new GrowthGainReason(Type.SAFE_COMBAT, multiplier);
    }

    public static GrowthGainReason lowHealth(double multiplier) {
        return new GrowthGainReason(Type.LOW_HEALTH, multiplier);
    }

    public boolean special() {
        return type != Type.NORMAL;
    }

    public enum Type {
        NORMAL,
        PET_KILL,
        SAFE_COMBAT,
        LOW_HEALTH
    }
}
