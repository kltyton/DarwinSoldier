package com.kltyton.darwin_soldier.client.aim;

import net.minecraft.world.phys.Vec3;

/**
 * Current estimate of a target's per-tick motion, derived from recent position
 * samples. {@code velocity} is the weighted current velocity in blocks/tick and
 * {@code acceleration} the weighted, clamped acceleration in blocks/tick^2.
 * {@code valid} is false when there are too few samples, non-finite data, or a
 * per-tick displacement that exceeds the teleport threshold.
 */
public record TargetMotionEstimate(Vec3 velocity, Vec3 acceleration, boolean valid) {
    public static final TargetMotionEstimate INVALID =
            new TargetMotionEstimate(Vec3.ZERO, Vec3.ZERO, false);
}
