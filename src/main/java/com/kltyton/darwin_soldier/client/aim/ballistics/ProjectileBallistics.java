package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.util.Mth;

public record ProjectileBallistics(String projectileType, double speed, double gravity, boolean hitscan) {
    public static final double REFERENCE_SPEED = 3.0D;
    public static final double REFERENCE_GRAVITY = 0.05D;

    public static ProjectileBallistics reference() {
        return new ProjectileBallistics("minecraft:generic_projectile", REFERENCE_SPEED, REFERENCE_GRAVITY, false);
    }

    public static ProjectileBallistics detected(String projectileType, double speed, double gravity) {
        return new ProjectileBallistics(projectileType, speed, gravity, false).sanitized();
    }

    public static ProjectileBallistics hitscanProfile() {
        return new ProjectileBallistics("hitscan", 0.0D, 0.0D, true);
    }

    public ProjectileBallistics sanitized() {
        if (hitscan) {
            return hitscanProfile();
        }
        String type = projectileType == null || projectileType.isBlank()
                ? "minecraft:generic_projectile"
                : projectileType;
        double safeSpeed = Double.isFinite(speed) ? Mth.clamp(speed, 0.01D, 20.0D) : REFERENCE_SPEED;
        double safeGravity = Double.isFinite(gravity) ? Mth.clamp(gravity, 0.0D, 2.0D) : REFERENCE_GRAVITY;
        return new ProjectileBallistics(type, safeSpeed, safeGravity, false);
    }
}
