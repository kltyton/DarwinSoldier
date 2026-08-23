package com.kltyton.darwin_soldier.client.aim.ballistics;

import com.kltyton.darwin_soldier.client.aim.AimWeaponTuning;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticAimSolverTest {
    @Test
    void adaptiveSolverUsesLearnedGravityAndSecondDistanceCorrection() {
        AimSolution solution = BallisticAimSolver.solveAdaptive(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 30.0D),
                List.of(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO),
                ProjectileBallistics.detected("test:grenade", 3.0D, 0.05D),
                AimWeaponTuning.adaptiveDefaults()
        );

        assertTrue(solution.aimPoint().y > 2.0D);
        assertTrue(solution.aimPoint().y < 2.5D);
        assertTrue(solution.flightTicks() > 10.0D);
    }

    @Test
    void hitscanForcesZeroFlightLeadAndDrop() {
        Vec3 visible = new Vec3(2.0D, 4.0D, 20.0D);
        AimSolution solution = BallisticAimSolver.solveAdaptive(
                Vec3.ZERO,
                visible,
                List.of(Vec3.ZERO, new Vec3(1.0D, 0.0D, 0.0D)),
                ProjectileBallistics.hitscanProfile(),
                AimWeaponTuning.adaptiveDefaults()
        );

        assertEquals(visible, solution.aimPoint());
        assertEquals(0.0D, solution.flightTicks(), 1.0E-9D);
    }

    @Test
    void fastTargetsReceiveFasterTrackingInterpolation() {
        AimSolution stationary = BallisticAimSolver.solveAdaptive(
                Vec3.ZERO, new Vec3(0.0D, 0.0D, 20.0D),
                List.of(Vec3.ZERO, Vec3.ZERO), ProjectileBallistics.hitscanProfile(),
                AimWeaponTuning.adaptiveDefaults());
        AimSolution moving = BallisticAimSolver.solveAdaptive(
                Vec3.ZERO, new Vec3(0.0D, 0.0D, 20.0D),
                List.of(Vec3.ZERO, new Vec3(2.0D, 0.0D, 0.0D)), ProjectileBallistics.hitscanProfile(),
                AimWeaponTuning.adaptiveDefaults());

        assertTrue(moving.interpolation() > stationary.interpolation());
    }

    @Test
    void adaptiveLaunchStateUsesTheSamePhysicsAsTheTrajectoryPreview() {
        Vec3 origin = new Vec3(0.0D, 1.5D, 0.0D);
        Vec3 inheritedVelocity = new Vec3(0.15D, 0.0D, 0.0D);
        ProjectileLaunchState launch = new ProjectileLaunchState(
                "test:arrow",
                origin,
                new Vec3(0.15D, 0.0D, 3.0D),
                inheritedVelocity,
                0.05D,
                VanillaProjectilePhysics.AIR_DRAG,
                VanillaProjectilePhysics.ARROW_WATER_DRAG,
                false
        );
        Vec3 target = new Vec3(0.0D, 1.5D, 30.0D);

        AimSolution solution = BallisticAimSolver.solveAdaptive(
                launch,
                false,
                target,
                List.of(target, target, target),
                AimWeaponTuning.adaptiveDefaults()
        );
        Vec3 aimDirection = solution.aimPoint().subtract(origin).normalize();
        Vec3 initialVelocity = aimDirection.scale(launch.launchSpeed()).add(inheritedVelocity);
        Vec3 simulated = VanillaProjectilePhysics.project(
                origin,
                initialVelocity,
                solution.flightTicks(),
                launch.airDrag(),
                launch.gravity()
        );

        assertTrue(aimDirection.x < 0.0D);
        assertTrue(simulated.distanceTo(target) < 0.02D,
                () -> "simulated=" + simulated + " target=" + target + " solution=" + solution);
    }

    @Test
    void unreachableLowChargeShotFallsBackWithoutExtremePitchCorrection() {
        Vec3 visible = new Vec3(0.0D, 1.5D, 30.0D);
        ProjectileLaunchState launch = new ProjectileLaunchState(
                "test:weak_arrow",
                new Vec3(0.0D, 1.5D, 0.0D),
                new Vec3(0.0D, 0.0D, 0.1D),
                Vec3.ZERO,
                0.05D,
                VanillaProjectilePhysics.AIR_DRAG,
                VanillaProjectilePhysics.ARROW_WATER_DRAG,
                false
        );

        AimSolution solution = BallisticAimSolver.solveAdaptive(
                launch, false, visible, List.of(visible, visible), AimWeaponTuning.adaptiveDefaults());

        assertEquals(visible, solution.aimPoint());
        assertEquals(0.0D, solution.flightTicks(), 1.0E-9D);
    }

    @Test
    void manualSolverAppliesAccelerationToPredictedTarget() {
        Vec3 visible = new Vec3(0.0D, 1.5D, 30.0D);
        AimSolution solution = BallisticAimSolver.solveManual(
                new Vec3(0.0D, 1.5D, 0.0D),
                visible,
                List.of(
                        new Vec3(0.0D, 1.5D, 30.0D),
                        new Vec3(0.0D, 1.5D, 30.0D),
                        new Vec3(1.0D, 1.5D, 30.0D),
                        new Vec3(3.0D, 1.5D, 30.0D)
                ),
                AimWeaponTuning.adaptiveDefaults()
        );

        assertTrue(solution.targetAcceleration().x > 0.0D);
        assertTrue(solution.predictedTarget().x > visible.x);
        assertEquals(30.0D, solution.predictedTarget().z, 1.0E-9D);
    }

    @Test
    void adaptivePredictedTargetIncludesAccelerationTerm() {
        Vec3 origin = new Vec3(0.0D, 1.5D, 0.0D);
        Vec3 visible = new Vec3(0.0D, 1.5D, 30.0D);
        AimSolution solution = BallisticAimSolver.solveAdaptive(
                origin,
                visible,
                List.of(
                        new Vec3(0.0D, 1.5D, 30.0D),
                        new Vec3(0.0D, 1.5D, 30.0D),
                        new Vec3(1.0D, 1.5D, 30.0D),
                        new Vec3(3.0D, 1.5D, 30.0D)
                ),
                ProjectileBallistics.detected("test:grenade", 3.0D, 0.05D),
                AimWeaponTuning.adaptiveDefaults()
        );

        assertTrue(solution.targetAcceleration().x > 0.0D);
        assertTrue(solution.flightTicks() > 0.0D);
        assertTrue(solution.predictedTarget().x > solution.targetVelocity().x * solution.flightTicks(),
                () -> "velocity-only lead would be " + (solution.targetVelocity().x * solution.flightTicks())
                        + " but predictedTarget.x=" + solution.predictedTarget().x);
    }
}
