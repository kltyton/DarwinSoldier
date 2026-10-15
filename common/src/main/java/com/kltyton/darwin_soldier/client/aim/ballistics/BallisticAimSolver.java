package com.kltyton.darwin_soldier.client.aim.ballistics;

import com.kltyton.darwin_soldier.client.aim.AimMath;
import com.kltyton.darwin_soldier.client.aim.AimWeaponTuning;
import com.kltyton.darwin_soldier.client.aim.TargetMotionEstimate;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BallisticAimSolver {
    private static final double MAX_FLIGHT_TICKS = 200.0D;
    private static final double SEARCH_STEP_TICKS = 0.25D;
    private static final double MIN_SEARCH_TICKS = 0.01D;
    private static final int ROOT_REFINEMENT_PASSES = 32;

    private BallisticAimSolver() {
    }

    public static AimSolution solveAdaptive(
            Vec3 origin,
            Vec3 visiblePoint,
            List<Vec3> targetPositions,
            ProjectileBallistics ballistics,
            AimWeaponTuning tuning
    ) {
        ProjectileBallistics safe = ballistics == null ? null : ballistics.sanitized();
        return solveAdaptive(origin, visiblePoint, targetPositions,
                safe == null ? 0.0D : safe.speed(), Vec3.ZERO,
                safe == null ? 0.0D : safe.gravity(), VanillaProjectilePhysics.AIR_DRAG,
                safe == null || safe.hitscan(), tuning);
    }

    public static AimSolution solveAdaptive(
            ProjectileLaunchState launch,
            boolean inWater,
            Vec3 visiblePoint,
            List<Vec3> targetPositions,
            AimWeaponTuning tuning
    ) {
        return solveAdaptive(
                launch.origin(),
                visiblePoint,
                targetPositions,
                launch.launchSpeed(),
                launch.inheritedVelocity(),
                launch.gravity(),
                launch.drag(inWater),
                launch.hitscan(),
                tuning
        );
    }

    public static AimSolution solveManual(
            Vec3 origin,
            Vec3 visiblePoint,
            List<Vec3> targetPositions,
            AimWeaponTuning tuning
    ) {
        TargetMotionEstimate estimate = AimMath.estimateMotion(targetPositions);
        double distance = origin.distanceTo(visiblePoint);
        double flightTicks = AimMath.estimatedFlightTicks(distance);
        Vec3 predictedTarget = visiblePoint.add(AimMath.predictedDisplacement(
                estimate.velocity(), estimate.acceleration(), flightTicks)
                .scale(tuning.leadMultiplier()));
        Vec3 aimPoint = predictedTarget.add(0.0D,
                AimMath.dropCompensation(distance, tuning.dropMultiplier()), 0.0D);
        return new AimSolution(aimPoint, estimate.velocity(), flightTicks, tuning.smooth(),
                estimate.acceleration(), predictedTarget);
    }

    private static AimSolution solveAdaptive(
            Vec3 origin,
            Vec3 visiblePoint,
            List<Vec3> targetPositions,
            double launchSpeed,
            Vec3 inheritedVelocity,
            double gravity,
            double drag,
            boolean hitscan,
            AimWeaponTuning tuning
    ) {
        TargetMotionEstimate estimate = AimMath.estimateMotion(targetPositions);
        Vec3 targetVelocity = estimate.velocity();
        Vec3 targetAcceleration = estimate.acceleration();
        double targetSpeed = targetVelocity.length();
        double interpolation = adaptiveInterpolation(tuning.smooth(), targetSpeed);
        if (hitscan || !Double.isFinite(launchSpeed) || launchSpeed <= 0.0D) {
            return new AimSolution(visiblePoint, targetVelocity, 0.0D, interpolation,
                    targetAcceleration, visiblePoint);
        }

        double leadWeight = tuning.leadMultiplier();
        FlightCandidate solution = findEarliestSolution(origin, visiblePoint, targetVelocity,
                targetAcceleration, leadWeight, launchSpeed, inheritedVelocity, gravity, drag,
                tuning.dropMultiplier());
        if (solution == null || solution.aimVelocity().lengthSqr() < 1.0E-10D) {
            return new AimSolution(visiblePoint, targetVelocity, 0.0D, interpolation,
                    targetAcceleration, visiblePoint);
        }

        Vec3 aimDirection = solution.aimVelocity().normalize();
        Vec3 targetOffset = solution.predictedTarget().subtract(origin);
        double targetHorizontal = Math.hypot(targetOffset.x, targetOffset.z);
        double directionHorizontal = Math.hypot(aimDirection.x, aimDirection.z);
        double lookDistance = directionHorizontal > 1.0E-8D
                ? Math.max(1.0D, targetHorizontal) / directionHorizontal
                : Math.max(1.0D, targetOffset.length());
        Vec3 aimPoint = origin.add(aimDirection.scale(lookDistance));
        return new AimSolution(aimPoint, targetVelocity, solution.flightTicks(), interpolation,
                targetAcceleration, solution.predictedTarget());
    }

    private static FlightCandidate findEarliestSolution(
            Vec3 origin,
            Vec3 visiblePoint,
            Vec3 targetVelocity,
            Vec3 targetAcceleration,
            double leadWeight,
            double launchSpeed,
            Vec3 inheritedVelocity,
            double gravity,
            double drag,
            double dropMultiplier
    ) {
        FlightCandidate previous = candidate(origin, visiblePoint, targetVelocity, targetAcceleration,
                leadWeight, launchSpeed, inheritedVelocity, gravity, drag, dropMultiplier, MIN_SEARCH_TICKS);
        for (double time = MIN_SEARCH_TICKS + SEARCH_STEP_TICKS;
             time <= MAX_FLIGHT_TICKS;
             time += SEARCH_STEP_TICKS) {
            FlightCandidate current = candidate(origin, visiblePoint, targetVelocity, targetAcceleration,
                    leadWeight, launchSpeed, inheritedVelocity, gravity, drag, dropMultiplier, time);
            if (previous.speedError() > 0.0D && current.speedError() <= 0.0D) {
                double lower = previous.flightTicks();
                double upper = current.flightTicks();
                for (int pass = 0; pass < ROOT_REFINEMENT_PASSES; pass++) {
                    double middle = (lower + upper) * 0.5D;
                    FlightCandidate refined = candidate(origin, visiblePoint, targetVelocity, targetAcceleration,
                            leadWeight, launchSpeed, inheritedVelocity, gravity, drag, dropMultiplier, middle);
                    if (refined.speedError() > 0.0D) {
                        lower = middle;
                    } else {
                        upper = middle;
                    }
                }
                return candidate(origin, visiblePoint, targetVelocity, targetAcceleration,
                        leadWeight, launchSpeed, inheritedVelocity, gravity, drag, dropMultiplier,
                        (lower + upper) * 0.5D);
            }
            previous = current;
        }
        return null;
    }

    private static FlightCandidate candidate(
            Vec3 origin,
            Vec3 visiblePoint,
            Vec3 targetVelocity,
            Vec3 targetAcceleration,
            double leadWeight,
            double launchSpeed,
            Vec3 inheritedVelocity,
            double gravity,
            double drag,
            double dropMultiplier,
            double flightTicks
    ) {
        Vec3 predictedTarget = visiblePoint.add(AimMath.predictedDisplacement(
                targetVelocity, targetAcceleration, flightTicks).scale(leadWeight));
        double distanceFactor = VanillaProjectilePhysics.dragDistanceFactor(flightTicks, drag);
        double drop = VanillaProjectilePhysics.gravityDrop(flightTicks, gravity, drag) * dropMultiplier;
        Vec3 requiredTotalVelocity = predictedTarget.subtract(origin)
                .add(0.0D, drop, 0.0D)
                .scale(1.0D / Math.max(1.0E-9D, distanceFactor));
        Vec3 aimVelocity = requiredTotalVelocity.subtract(inheritedVelocity);
        return new FlightCandidate(flightTicks, predictedTarget, aimVelocity,
                aimVelocity.length() - launchSpeed);
    }

    private static double adaptiveInterpolation(double configuredSmooth, double targetSpeed) {
        double smoothingAmount = Mth.clamp(configuredSmooth - Math.min(0.25D, targetSpeed * 0.12D),
                AimWeaponTuning.MIN_SMOOTH, AimWeaponTuning.MAX_SMOOTH - 0.05D);
        return Mth.clamp(1.0D - smoothingAmount,
                AimWeaponTuning.MIN_SMOOTH, AimWeaponTuning.MAX_SMOOTH);
    }

    private record FlightCandidate(
            double flightTicks,
            Vec3 predictedTarget,
            Vec3 aimVelocity,
            double speedError
    ) {
    }
}
