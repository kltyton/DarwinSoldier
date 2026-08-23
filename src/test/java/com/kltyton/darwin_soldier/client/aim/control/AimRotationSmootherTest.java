package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimRotationSmootherTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    void responseOneMatchesExistingInterpolationSemantics() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);

        AimRotation result = smoother.update(100.0D, 0.0D, 1.0D, 180.0D);

        assertEquals(100.0D, result.yaw(), EPSILON);
        assertEquals(0.0D, result.pitch(), EPSILON);
    }

    @Test
    void responseMovesConfiguredFractionPerTick() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(50.0D, smoother.update(100.0D, 0.0D, 0.5D, 180.0D).yaw(), EPSILON);

        AimRotationSmoother low = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(35.0D, low.update(100.0D, 0.0D, Double.NaN, 180.0D).yaw(), EPSILON);
    }

    @Test
    void responseIsClampedToConfiguredRange() {
        AimRotationSmoother high = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(100.0D, high.update(100.0D, 0.0D, 2.0D, 180.0D).yaw(), EPSILON);

        AimRotationSmoother low = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(5.0D, low.update(100.0D, 0.0D, 0.01D, 180.0D).yaw(), EPSILON);
    }

    @Test
    void perTickStepIsCappedToAvoidSnaps() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(5.0D, smoother.update(100.0D, 0.0D, 1.0D, 5.0D).yaw(), EPSILON);
        assertEquals(10.0D, smoother.update(100.0D, 0.0D, 1.0D, 5.0D).yaw(), EPSILON);
    }

    @Test
    void invalidMaxStepFallsBackToNoPracticalCap() {
        AimRotationSmoother zeroCap = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(100.0D, zeroCap.update(100.0D, 0.0D, 1.0D, 0.0D).yaw(), EPSILON);

        AimRotationSmoother nanCap = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(100.0D, nanCap.update(100.0D, 0.0D, 1.0D, Double.NaN).yaw(), EPSILON);
    }

    @Test
    void neverOvershootsWhileConverging() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        double previous = 0.0D;
        for (int tick = 0; tick < 20; tick++) {
            double yaw = smoother.update(100.0D, 0.0D, 0.5D, 1000.0D).yaw();
            assertTrue(yaw >= previous - EPSILON, "non-monotonic step at tick " + tick);
            assertTrue(yaw <= 100.0D + EPSILON, "overshoot at tick " + tick);
            previous = yaw;
        }
        for (int tick = 0; tick < 60; tick++) {
            smoother.update(100.0D, 0.0D, 0.5D, 1000.0D);
        }
        assertEquals(100.0D, smoother.yaw(), EPSILON);
    }

    @Test
    void convergesMonotonicallyForStaticTarget() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        double previousError = 100.0D;
        for (int tick = 0; tick < 600; tick++) {
            smoother.update(100.0D, 0.0D, 0.05D, 180.0D);
            double error = Angles.angularDistanceDegrees(smoother.yaw(), 100.0D);
            assertTrue(error <= previousError + 1.0E-12D, "error grew at tick " + tick);
            previousError = error;
        }
        assertTrue(previousError < 1.0E-6D);
    }

    @Test
    void wrapAwareShortestPathAcrossBoundary() {
        AimRotationSmoother smoother = new AimRotationSmoother(170.0D, 0.0D);
        assertEquals(-170.0D, smoother.update(-170.0D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);

        AimRotationSmoother reverse = new AimRotationSmoother(-170.0D, 0.0D);
        assertEquals(170.0D, reverse.update(170.0D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
    }

    @Test
    void pitchIsClampedToVerticalRange() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(90.0D, smoother.update(0.0D, 120.0D, 1.0D, 180.0D).pitch(), EPSILON);

        AimRotationSmoother down = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(-90.0D, down.update(0.0D, -120.0D, 1.0D, 180.0D).pitch(), EPSILON);
    }

    @Test
    void noiseBelowToleranceIsFiltered() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D, 0.25D);
        assertEquals(0.0D, smoother.update(0.1D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
        assertEquals(0.0D, smoother.update(-0.1D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
        assertEquals(0.0D, smoother.update(0.05D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
        assertEquals(0.0D, smoother.yaw(), EPSILON);
    }

    @Test
    void noiseAboveToleranceIsFollowed() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D, 0.25D);
        assertEquals(10.0D, smoother.update(10.0D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
    }

    @Test
    void invalidNoiseToleranceFallsBackToDefault() {
        AimRotationSmoother nan = new AimRotationSmoother(0.0D, 0.0D, Double.NaN);
        assertEquals(0.0D, nan.update(0.1D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);

        AimRotationSmoother negative = new AimRotationSmoother(0.0D, 0.0D, -1.0D);
        assertEquals(0.0D, negative.update(0.1D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
    }

    @Test
    void resetClearsStateAndNextUpdateAnchorsWithoutMotion() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(5.0D, smoother.update(100.0D, 0.0D, 1.0D, 5.0D).yaw(), EPSILON);

        smoother.reset();
        assertEquals(100.0D, smoother.update(100.0D, 0.0D, 1.0D, 5.0D).yaw(), EPSILON);
        assertEquals(-40.0D, smoother.update(-40.0D, 0.0D, 1.0D, 180.0D).yaw(), EPSILON);
    }

    @Test
    void yawAndPitchAreInterpolatedIndependently() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        AimRotation result = smoother.update(80.0D, 40.0D, 0.5D, 180.0D);

        assertEquals(40.0D, result.yaw(), EPSILON);
        assertEquals(20.0D, result.pitch(), EPSILON);
        assertEquals(40.0D, smoother.yaw(), EPSILON);
        assertEquals(20.0D, smoother.pitch(), EPSILON);
    }

    @Test
    void frameResponseAtTwentyFpsMatchesPerTickUpdate() {
        AimRotationSmoother frames = new AimRotationSmoother(0.0D, 0.0D);
        for (int frame = 0; frame < 20; frame++) {
            frames.updateFrame(100.0D, 0.0D, 0.5D, 1000.0D, 1.0D / 20.0D);
        }

        AimRotationSmoother ticks = new AimRotationSmoother(0.0D, 0.0D);
        for (int tick = 0; tick < 20; tick++) {
            ticks.update(100.0D, 0.0D, 0.5D, 1000.0D);
        }

        assertEquals(ticks.yaw(), frames.yaw(), 1.0E-9D);
    }

    @Test
    void frameResponseConvergesIdenticallyAtTwentyAndOneHundredFortyFourFps() {
        double target = 100.0D;
        double response = 0.35D;

        AimRotationSmoother twenty = new AimRotationSmoother(0.0D, 0.0D);
        for (int frame = 0; frame < 20; frame++) {
            twenty.updateFrame(target, 0.0D, response, 1000.0D, 1.0D / 20.0D);
        }

        AimRotationSmoother oneFortyFour = new AimRotationSmoother(0.0D, 0.0D);
        for (int frame = 0; frame < 144; frame++) {
            oneFortyFour.updateFrame(target, 0.0D, response, 1000.0D, 1.0D / 144.0D);
        }

        assertEquals(twenty.yaw(), oneFortyFour.yaw(), 1.0E-6D);
        assertTrue(Math.abs(twenty.yaw() - target * (1.0D - Math.pow(0.65D, 20.0D))) < 1.0E-6D,
                "one second of frames must match 20 per-tick response steps");
        assertTrue(oneFortyFour.yaw() <= target + 1.0E-9D, "144 FPS must not overshoot");
    }

    @Test
    void highFrameRateAimMovesInManyBoundedPositiveSubsteps() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        double previous = 0.0D;
        double maxFrameDelta = 0.0D;
        int substeps = 0;
        for (int frame = 0; frame < 144; frame++) {
            double yaw = smoother.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, 1.0D / 144.0D).yaw();
            double delta = yaw - previous;
            assertTrue(delta >= -1.0E-12D, "rotation moved backward at frame " + frame);
            maxFrameDelta = Math.max(maxFrameDelta, delta);
            if (delta > 1.0E-9D) {
                substeps++;
            }
            previous = yaw;
        }
        assertTrue(maxFrameDelta <= 240.0D / 144.0D + 1.0E-9D,
                "frame step must respect 240 deg/s, was " + maxFrameDelta);
        assertTrue(substeps >= 50, "expected many bounded substeps, got " + substeps);
        // Exactly one second under the 240 deg/s cap converges to ~99.93 deg
        // (uncapped 1s response value is 100 * (1 - 0.65^20) ~ 99.982), so a
        // 0.1 deg bound is the correct no-overshoot convergence check.
        assertTrue(smoother.yaw() >= 100.0D - 0.1D,
                "one second at 240 deg/s must nearly converge, was " + smoother.yaw());
        assertTrue(smoother.yaw() <= 100.0D + 1.0E-9D, "must not overshoot");
    }

    @Test
    void twentyFpsAimKeepsTwelveDegreePerTickCeiling() {
        AimRotationSmoother smoother = new AimRotationSmoother(0.0D, 0.0D);
        double previous = 0.0D;
        double maxFrameDelta = 0.0D;
        for (int frame = 0; frame < 20; frame++) {
            double yaw = smoother.updateFrame(100.0D, 0.0D, 1.0D, 240.0D, 1.0D / 20.0D).yaw();
            maxFrameDelta = Math.max(maxFrameDelta, yaw - previous);
            previous = yaw;
        }
        assertEquals(12.0D, maxFrameDelta, EPSILON);
    }

    @Test
    void frameMaxSpeedCapsTotalMotionPerSecond() {
        // wrap(1000) == -80: the cap limits the rate, and both frame rates must
        // reach the wrapped target after one second without overshooting.
        AimRotationSmoother twenty = new AimRotationSmoother(0.0D, 0.0D);
        for (int frame = 0; frame < 20; frame++) {
            twenty.updateFrame(1000.0D, 0.0D, 1.0D, 240.0D, 1.0D / 20.0D);
        }
        assertEquals(-80.0D, twenty.yaw(), 1.0E-9D);

        AimRotationSmoother oneFortyFour = new AimRotationSmoother(0.0D, 0.0D);
        for (int frame = 0; frame < 144; frame++) {
            oneFortyFour.updateFrame(1000.0D, 0.0D, 1.0D, 240.0D, 1.0D / 144.0D);
        }
        assertEquals(-80.0D, oneFortyFour.yaw(), 1.0E-9D);
    }

    @Test
    void frameWrapUsesShortestPath() {
        // 3000 deg/s at 1/144 s = 20.83 deg/frame, so the 20 deg wrap step is
        // not speed-capped and must snap across the boundary in one frame.
        AimRotationSmoother forward = new AimRotationSmoother(170.0D, 0.0D);
        assertEquals(-170.0D, forward.updateFrame(-170.0D, 0.0D, 1.0D, 3000.0D, 1.0D / 144.0D).yaw(), EPSILON);

        AimRotationSmoother reverse = new AimRotationSmoother(-170.0D, 0.0D);
        assertEquals(170.0D, reverse.updateFrame(170.0D, 0.0D, 1.0D, 3000.0D, 1.0D / 144.0D).yaw(), EPSILON);
    }

    @Test
    void framePitchIsClampedToVerticalRange() {
        // 2000 deg/s * 0.05 s = 100 deg/frame, so a 90 deg pitch step is not
        // speed-capped and the vertical clamp is reachable in one frame.
        AimRotationSmoother up = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(90.0D, up.updateFrame(0.0D, 120.0D, 1.0D, 2000.0D,
                AimRotationSmoother.MAX_FRAME_DELTA_SECONDS).pitch(), EPSILON);

        AimRotationSmoother down = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(-90.0D, down.updateFrame(0.0D, -120.0D, 1.0D, 2000.0D,
                AimRotationSmoother.MAX_FRAME_DELTA_SECONDS).pitch(), EPSILON);
    }

    @Test
    void frameInvalidDeltaTimeIsClampedAndNeverProducesNan() {
        AimRotationSmoother nanDelta = new AimRotationSmoother(0.0D, 0.0D);
        AimRotation nanResult = nanDelta.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, Double.NaN);
        assertTrue(Double.isFinite(nanResult.yaw()));
        assertEquals(0.0D, nanResult.yaw(), EPSILON);

        AimRotationSmoother zero = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(0.0D, zero.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, 0.0D).yaw(), EPSILON);

        AimRotationSmoother negative = new AimRotationSmoother(0.0D, 0.0D);
        assertEquals(0.0D, negative.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, -1.0D).yaw(), EPSILON);

        AimRotationSmoother huge = new AimRotationSmoother(0.0D, 0.0D);
        AimRotation hugeClamped = huge.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, 5.0D);
        assertTrue(Double.isFinite(hugeClamped.yaw()));

        AimRotationSmoother reference = new AimRotationSmoother(0.0D, 0.0D);
        reference.updateFrame(100.0D, 0.0D, 0.35D, 240.0D, AimRotationSmoother.MAX_FRAME_DELTA_SECONDS);
        assertEquals(reference.yaw(), hugeClamped.yaw(), EPSILON);
    }
}
