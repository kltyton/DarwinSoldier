package com.kltyton.darwin_soldier.client.aim;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimMathTest {
    @Test
    void averageVelocityUsesAllSampleIntervals() {
        Vec3 velocity = AimMath.averageVelocity(List.of(
                new Vec3(1.0D, 2.0D, 3.0D),
                new Vec3(2.0D, 2.0D, 5.0D),
                new Vec3(3.0D, 2.0D, 7.0D)
        ));

        assertEquals(1.0D, velocity.x, 1.0E-9D);
        assertEquals(0.0D, velocity.y, 1.0E-9D);
        assertEquals(2.0D, velocity.z, 1.0E-9D);
    }

    @Test
    void dropCompensationScalesWithDistanceAndPlayerMultiplier() {
        assertEquals(0.0D, AimMath.dropCompensation(30.0D, 0.0D), 1.0E-9D);
        assertEquals(2.5D, AimMath.dropCompensation(30.0D, 1.0D), 1.0E-9D);
        assertEquals(5.0D, AimMath.dropCompensation(30.0D, 2.0D), 1.0E-9D);
    }

    @Test
    void estimatedFlightTimeIsBoundedForVeryLongRanges() {
        assertEquals(20.0D, AimMath.estimatedFlightTicks(1_000.0D), 1.0E-9D);
    }

    @Test
    void directionChangesReduceMotionStability() {
        double stable = AimMath.motionStability(List.of(
                Vec3.ZERO,
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(2.0D, 0.0D, 0.0D),
                new Vec3(3.0D, 0.0D, 0.0D)
        ));
        double changing = AimMath.motionStability(List.of(
                Vec3.ZERO,
                new Vec3(1.0D, 0.0D, 0.0D),
                Vec3.ZERO,
                new Vec3(1.0D, 0.0D, 0.0D)
        ));

        assertEquals(1.0D, stable, 1.0E-9D);
        assertTrue(changing < stable);
    }

    @Test
    void constantVelocityEstimateHasNearZeroAcceleration() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(2.0D, 0.0D, 0.0D),
                new Vec3(3.0D, 0.0D, 0.0D)
        ));

        assertTrue(estimate.valid());
        assertEquals(1.0D, estimate.velocity().x, 1.0E-9D);
        assertEquals(0.0D, estimate.acceleration().lengthSqr(), 1.0E-12D);
    }

    @Test
    void acceleratingTargetProducesForwardAcceleration() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.05D, 0.0D, 0.0D),
                new Vec3(0.15D, 0.0D, 0.0D)
        ));

        assertTrue(estimate.valid());
        assertTrue(estimate.acceleration().x > 0.0D);
        assertTrue(estimate.acceleration().x <= AimMath.MAX_HORIZONTAL_ACCELERATION);
    }

    @Test
    void suddenStopProducesReverseAcceleration() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(2.0D, 0.0D, 0.0D),
                new Vec3(3.0D, 0.0D, 0.0D),
                new Vec3(3.0D, 0.0D, 0.0D)
        ));

        assertTrue(estimate.valid());
        assertTrue(estimate.acceleration().x < 0.0D);
    }

    @Test
    void turnProducesLateralAcceleration() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.3D, 0.0D, 0.0D),
                new Vec3(0.6D, 0.0D, 0.0D),
                new Vec3(0.9D, 0.0D, 0.3D),
                new Vec3(1.2D, 0.0D, 0.9D)
        ));

        assertTrue(estimate.valid());
        assertTrue(estimate.acceleration().z > 0.0D);
    }

    @Test
    void teleportSampleRejectsEstimate() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(10.0D, 0.0D, 0.0D)
        ));

        assertFalse(estimate.valid());
        assertEquals(0.0D, estimate.velocity().lengthSqr(), 1.0E-12D);
        assertEquals(0.0D, estimate.acceleration().lengthSqr(), 1.0E-12D);
    }

    @Test
    void shortJumpBelowTeleportThresholdRejectsEstimate() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(2.0D, 0.0D, 0.0D),
                new Vec3(5.0D, 0.0D, 0.0D),
                new Vec3(6.0D, 0.0D, 0.0D),
                new Vec3(7.0D, 0.0D, 0.0D)
        ));

        assertFalse(estimate.valid());
        assertEquals(0.0D, estimate.velocity().lengthSqr(), 1.0E-12D);
        assertEquals(0.0D, estimate.acceleration().lengthSqr(), 1.0E-12D);
    }

    @Test
    void sustainedHighSpeedSamplesRemainValid() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(2.0D, 0.0D, 0.0D),
                new Vec3(4.0D, 0.0D, 0.0D),
                new Vec3(6.0D, 0.0D, 0.0D),
                new Vec3(8.0D, 0.0D, 0.0D)
        ));

        assertTrue(estimate.valid());
        assertEquals(2.0D, estimate.velocity().x, 1.0E-9D);
        assertEquals(0.0D, estimate.acceleration().lengthSqr(), 1.0E-12D);
    }

    @Test
    void nonFiniteSampleRejectsEstimate() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(Double.NaN, 0.0D, 0.0D)
        ));

        assertFalse(estimate.valid());
    }

    @Test
    void predictedDisplacementCapsAccelerationAtSixTicks() {
        Vec3 velocity = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 acceleration = new Vec3(2.0D, 0.0D, 0.0D);

        Vec3 atSix = AimMath.predictedDisplacement(velocity, acceleration, 6.0D);
        Vec3 atTwenty = AimMath.predictedDisplacement(velocity, acceleration, 20.0D);

        assertEquals(42.0D, atSix.x, 1.0E-9D);
        assertEquals(56.0D, atTwenty.x, 1.0E-9D);
        assertTrue(atTwenty.x < 420.0D);
    }

    @Test
    void excessiveAccelerationIsClamped() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.2D, 0.3D, 0.2D),
                new Vec3(0.7D, 0.0D, 0.7D)
        ));

        assertTrue(estimate.valid());
        double horizontal = Math.hypot(estimate.acceleration().x, estimate.acceleration().z);
        assertTrue(horizontal <= AimMath.MAX_HORIZONTAL_ACCELERATION + 1.0E-9D);
        assertTrue(Math.abs(estimate.acceleration().y) <= AimMath.MAX_VERTICAL_ACCELERATION + 1.0E-9D);
    }

    @Test
    void velocityWeightsRecentSamplesHighest() {
        TargetMotionEstimate estimate = AimMath.estimateMotion(List.of(
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.1D, 0.0D, 0.0D),
                new Vec3(0.3D, 0.0D, 0.0D),
                new Vec3(0.6D, 0.0D, 0.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(1.8D, 0.0D, 0.0D)
        ));

        assertEquals(0.52D, estimate.velocity().x, 1.0E-9D);
    }

    @Test
    void predictedDisplacementIgnoresInvalidInput() {
        assertEquals(0.0D, AimMath.predictedDisplacement(
                new Vec3(Double.NaN, 0.0D, 0.0D), Vec3.ZERO, 10.0D).lengthSqr(), 1.0E-12D);
        assertEquals(0.0D, AimMath.predictedDisplacement(
                Vec3.ZERO, Vec3.ZERO, Double.POSITIVE_INFINITY).lengthSqr(), 1.0E-12D);
    }
}
