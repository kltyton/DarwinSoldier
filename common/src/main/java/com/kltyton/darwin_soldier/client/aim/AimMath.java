package com.kltyton.darwin_soldier.client.aim;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AimMath {
    private static final double REFERENCE_PROJECTILE_SPEED = 3.0D;
    private static final double REFERENCE_PROJECTILE_GRAVITY = 0.05D;
    private static final double MAX_ESTIMATED_FLIGHT_TICKS = 20.0D;
    /** Per-tick displacement (blocks) above which a motion sample is treated as a teleport. */
    public static final double TELEPORT_THRESHOLD_BLOCKS_PER_TICK = 4.0D;
    /** Per-tick velocity change (blocks/tick) above which adjacent samples are treated as a short jump. */
    public static final double VELOCITY_CHANGE_THRESHOLD_BLOCKS_PER_TICK = 1.0D;
    /** Maximum horizontal acceleration magnitude accepted for prediction, in blocks/tick^2. */
    public static final double MAX_HORIZONTAL_ACCELERATION = 0.12D;
    /** Maximum vertical acceleration component accepted for prediction, in blocks/tick^2. */
    public static final double MAX_VERTICAL_ACCELERATION = 0.2D;
    /** Acceleration contributes to predicted displacement for at most this many ticks. */
    public static final double ACCELERATION_TIME_CAP_TICKS = 6.0D;
    /** Number of recent velocity samples retained by the weighted estimate. */
    private static final int MAX_VELOCITY_SAMPLES = 4;

    private AimMath() {
    }

    public static Vec3 averageVelocity(Collection<Vec3> positions) {
        if (positions.size() < 2) {
            return Vec3.ZERO;
        }
        Vec3 first = null;
        Vec3 last = null;
        for (Vec3 position : positions) {
            if (first == null) {
                first = position;
            }
            last = position;
        }
        return last.subtract(first).scale(1.0D / (positions.size() - 1));
    }

    public static double estimatedFlightTicks(double distance) {
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            return 0.0D;
        }
        return Mth.clamp(distance / REFERENCE_PROJECTILE_SPEED, 0.0D, MAX_ESTIMATED_FLIGHT_TICKS);
    }

    public static double dropCompensation(double distance, double multiplier) {
        if (!Double.isFinite(distance) || distance <= 0.0D || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return 0.0D;
        }
        double flightTicks = estimatedFlightTicks(distance);
        return 0.5D * REFERENCE_PROJECTILE_GRAVITY * flightTicks * flightTicks * multiplier;
    }

    public static double motionStability(Collection<Vec3> positions) {
        if (positions.size() < 3) {
            return positions.size() < 2 ? 0.0D : 1.0D;
        }
        List<Vec3> samples = List.copyOf(positions);
        Vec3 average = averageVelocity(samples);
        if (average.lengthSqr() < 1.0E-6D) {
            return 0.0D;
        }
        Vec3 direction = average.normalize();
        double total = 0.0D;
        int count = 0;
        for (int index = 1; index < samples.size(); index++) {
            Vec3 delta = samples.get(index).subtract(samples.get(index - 1));
            if (delta.lengthSqr() < 1.0E-6D) {
                continue;
            }
            total += Mth.clamp(delta.normalize().dot(direction), -1.0D, 1.0D);
            count++;
        }
        return count == 0 ? 0.0D : Mth.clamp((total / count + 1.0D) * 0.5D, 0.15D, 1.0D);
    }

    /**
     * Estimates the current per-tick velocity and acceleration from consecutive
     * position samples. Each interval is treated as one tick. The velocity is a
     * recency-weighted average of the last up to 4 per-tick displacements; the
     * acceleration is a recency-weighted average of the adjacent differences of
     * those displacements. The estimate is rejected (deterministically) when any
     * sample is null/non-finite, when any per-tick displacement exceeds the
     * teleport threshold, when adjacent per-tick velocities change by more than
     * {@link #VELOCITY_CHANGE_THRESHOLD_BLOCKS_PER_TICK}, or when fewer than two
     * samples are available.
     */
    public static TargetMotionEstimate estimateMotion(Collection<Vec3> positions) {
        if (positions == null || positions.size() < 2) {
            return TargetMotionEstimate.INVALID;
        }
        List<Vec3> samples = new ArrayList<>(positions);
        List<Vec3> velocities = new ArrayList<>();
        for (int index = 1; index < samples.size(); index++) {
            Vec3 previous = samples.get(index - 1);
            Vec3 current = samples.get(index);
            if (previous == null || current == null || !isFinite(previous) || !isFinite(current)) {
                return TargetMotionEstimate.INVALID;
            }
            Vec3 step = current.subtract(previous);
            if (!isFinite(step)
                    || step.lengthSqr() > TELEPORT_THRESHOLD_BLOCKS_PER_TICK * TELEPORT_THRESHOLD_BLOCKS_PER_TICK) {
                return TargetMotionEstimate.INVALID;
            }
            velocities.add(step);
        }
        for (int index = 1; index < velocities.size(); index++) {
            Vec3 velocityChange = velocities.get(index).subtract(velocities.get(index - 1));
            if (velocityChange.lengthSqr()
                    > VELOCITY_CHANGE_THRESHOLD_BLOCKS_PER_TICK * VELOCITY_CHANGE_THRESHOLD_BLOCKS_PER_TICK) {
                return TargetMotionEstimate.INVALID;
            }
        }

        int velocityCount = Math.min(MAX_VELOCITY_SAMPLES, velocities.size());
        List<Vec3> recentVelocities = velocities.subList(velocities.size() - velocityCount, velocities.size());
        Vec3 velocity = weightedAverage(recentVelocities);
        Vec3 acceleration = Vec3.ZERO;
        if (velocityCount >= 2) {
            List<Vec3> accelerations = new ArrayList<>();
            for (int index = 1; index < recentVelocities.size(); index++) {
                accelerations.add(recentVelocities.get(index).subtract(recentVelocities.get(index - 1)));
            }
            acceleration = clampAcceleration(weightedAverage(accelerations));
        }
        if (!isFinite(velocity) || !isFinite(acceleration)) {
            return TargetMotionEstimate.INVALID;
        }
        return new TargetMotionEstimate(velocity, acceleration, true);
    }

    /**
     * Predicted displacement over {@code flightTicks}: {@code v*t + 0.5*a*t^2}.
     * The velocity term always uses the full flight time, while the acceleration
     * term is capped to {@link #ACCELERATION_TIME_CAP_TICKS} ticks so the
     * quadratic component cannot explode over long ranges. Returns zero for
     * non-finite input.
     */
    public static Vec3 predictedDisplacement(Vec3 velocity, Vec3 acceleration, double flightTicks) {
        Vec3 safeVelocity = velocity == null ? Vec3.ZERO : velocity;
        Vec3 safeAcceleration = acceleration == null ? Vec3.ZERO : acceleration;
        if (!Double.isFinite(flightTicks) || flightTicks <= 0.0D
                || !isFinite(safeVelocity) || !isFinite(safeAcceleration)) {
            return Vec3.ZERO;
        }
        double cappedTicks = Math.min(flightTicks, ACCELERATION_TIME_CAP_TICKS);
        return safeVelocity.scale(flightTicks)
                .add(safeAcceleration.scale(0.5D * cappedTicks * cappedTicks));
    }

    private static Vec3 weightedAverage(List<Vec3> samples) {
        double totalWeight = 0.0D;
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (int index = 0; index < samples.size(); index++) {
            double weight = index + 1.0D;
            Vec3 sample = samples.get(index);
            totalWeight += weight;
            x += sample.x * weight;
            y += sample.y * weight;
            z += sample.z * weight;
        }
        if (totalWeight <= 0.0D) {
            return Vec3.ZERO;
        }
        return new Vec3(x / totalWeight, y / totalWeight, z / totalWeight);
    }

    private static Vec3 clampAcceleration(Vec3 acceleration) {
        Vec3 horizontal = new Vec3(acceleration.x, 0.0D, acceleration.z);
        double maxHorizontalSquared = MAX_HORIZONTAL_ACCELERATION * MAX_HORIZONTAL_ACCELERATION;
        if (horizontal.lengthSqr() > maxHorizontalSquared) {
            horizontal = horizontal.normalize().scale(MAX_HORIZONTAL_ACCELERATION);
        }
        double vertical = Mth.clamp(acceleration.y, -MAX_VERTICAL_ACCELERATION, MAX_VERTICAL_ACCELERATION);
        return new Vec3(horizontal.x, vertical, horizontal.z);
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
