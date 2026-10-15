package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimInputControllerTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    void belowDeadZoneProducesNoSwitchIntent() {
        AimInputController controller = new AimInputController();
        controller.addInput(3.0D, 4.0D);

        assertFalse(controller.pollSwitchIntent());
        assertEquals(3.0D, controller.yawOffsetDegrees(), EPSILON);
        assertEquals(4.0D, controller.pitchOffsetDegrees(), EPSILON);
        assertEquals(5.0D, controller.radialOffsetDegrees(), EPSILON);
    }

    @Test
    void atDeadZoneBoundaryCrossesOnEitherAxis() {
        AimInputController yawOnly = new AimInputController();
        yawOnly.addInput(6.0D, 0.0D);
        assertTrue(yawOnly.pollSwitchIntent());

        AimInputController pitchOnly = new AimInputController();
        pitchOnly.addInput(0.0D, 6.0D);
        assertTrue(pitchOnly.pollSwitchIntent());
    }

    @Test
    void crossingEmitsExactlyOneIntentUntilFreshInput() {
        AimInputController controller = new AimInputController();
        controller.addInput(8.0D, 0.0D);

        assertTrue(controller.pollSwitchIntent());
        assertFalse(controller.pollSwitchIntent());
        assertFalse(controller.pollSwitchIntent());

        controller.addInput(1.0D, 0.0D);
        assertTrue(controller.pollSwitchIntent());
    }

    @Test
    void failedSwitchPreservesOffsetAndRequiresNewInput() {
        AimInputController controller = new AimInputController();
        controller.addInput(8.0D, 2.0D);
        assertTrue(controller.pollSwitchIntent());

        controller.markSwitchFailed();
        assertFalse(controller.pollSwitchIntent());
        assertEquals(8.0D, controller.yawOffsetDegrees(), EPSILON);
        assertEquals(2.0D, controller.pitchOffsetDegrees(), EPSILON);

        controller.addInput(0.5D, 0.0D);
        assertTrue(controller.pollSwitchIntent());
        assertEquals(8.5D, controller.yawOffsetDegrees(), EPSILON);
    }

    @Test
    void zeroInputDoesNotReArmConsumedCapture() {
        AimInputController controller = new AimInputController();
        controller.addInput(8.0D, 0.0D);
        assertTrue(controller.pollSwitchIntent());

        controller.addInput(0.0D, 0.0D);
        assertFalse(controller.pollSwitchIntent());
    }

    @Test
    void successfulSwitchClearsAccumulatedOffset() {
        AimInputController controller = new AimInputController();
        controller.addInput(10.0D, 4.0D);
        assertTrue(controller.pollSwitchIntent());

        controller.reset();
        assertEquals(0.0D, controller.yawOffsetDegrees(), EPSILON);
        assertEquals(0.0D, controller.pitchOffsetDegrees(), EPSILON);
        assertEquals(0.0D, controller.radialOffsetDegrees(), EPSILON);
        assertFalse(controller.pollSwitchIntent());
    }

    @Test
    void sessionResetRequiresFreshCrossingFromZero() {
        AimInputController controller = new AimInputController();
        controller.addInput(5.0D, 0.0D);
        assertFalse(controller.pollSwitchIntent());

        controller.addInput(1.5D, 0.0D);
        assertTrue(controller.pollSwitchIntent());
    }

    @Test
    void offsetClampsRadiallyToFortyFiveDegrees() {
        AimInputController controller = new AimInputController();
        controller.addInput(60.0D, 0.0D);

        assertEquals(45.0D, controller.yawOffsetDegrees(), EPSILON);
        assertEquals(45.0D, controller.radialOffsetDegrees(), EPSILON);

        AimInputController diagonal = new AimInputController();
        diagonal.addInput(60.0D, 60.0D);
        assertEquals(45.0D / Math.sqrt(2.0D), diagonal.yawOffsetDegrees(), EPSILON);
        assertEquals(45.0D / Math.sqrt(2.0D), diagonal.pitchOffsetDegrees(), EPSILON);
        assertEquals(45.0D, diagonal.radialOffsetDegrees(), EPSILON);
        assertTrue(diagonal.pollSwitchIntent());
    }

    @Test
    void nonFiniteInputIsIgnored() {
        AimInputController controller = new AimInputController();
        controller.addInput(Double.NaN, 0.0D);
        controller.addInput(Double.POSITIVE_INFINITY, 1.0D);

        assertEquals(0.0D, controller.yawOffsetDegrees(), EPSILON);
        assertEquals(0.0D, controller.pitchOffsetDegrees(), EPSILON);
        assertFalse(controller.pollSwitchIntent());
    }

    @Test
    void hugeFiniteInputClampsToExactMaximumWithoutNaN() {
        AimInputController singleAxis = new AimInputController();
        singleAxis.addInput(Double.MAX_VALUE, 0.0D);
        assertTrue(Double.isFinite(singleAxis.yawOffsetDegrees()));
        assertTrue(Double.isFinite(singleAxis.pitchOffsetDegrees()));
        assertTrue(Double.isFinite(singleAxis.radialOffsetDegrees()));
        assertEquals(45.0D, singleAxis.yawOffsetDegrees(), EPSILON);
        assertEquals(45.0D, singleAxis.radialOffsetDegrees(), EPSILON);

        AimInputController pair = new AimInputController();
        pair.addInput(Double.MAX_VALUE, Double.MAX_VALUE);
        assertTrue(Double.isFinite(pair.yawOffsetDegrees()));
        assertTrue(Double.isFinite(pair.pitchOffsetDegrees()));
        assertTrue(Double.isFinite(pair.radialOffsetDegrees()));
        assertEquals(45.0D / Math.sqrt(2.0D), pair.yawOffsetDegrees(), EPSILON);
        assertEquals(45.0D / Math.sqrt(2.0D), pair.pitchOffsetDegrees(), EPSILON);
        assertEquals(45.0D, pair.radialOffsetDegrees(), EPSILON);
        assertTrue(pair.pollSwitchIntent());
    }

    @Test
    void repeatedHugeFiniteInputStaysFiniteAndClamped() {
        AimInputController controller = new AimInputController();
        controller.addInput(Double.MAX_VALUE, 0.0D);
        controller.addInput(Double.MAX_VALUE, Double.MAX_VALUE);
        controller.addInput(0.0D, Double.MAX_VALUE);

        assertTrue(Double.isFinite(controller.yawOffsetDegrees()));
        assertTrue(Double.isFinite(controller.pitchOffsetDegrees()));
        assertTrue(Double.isFinite(controller.radialOffsetDegrees()));
        assertTrue(controller.radialOffsetDegrees() <= 45.0D + EPSILON);
        assertTrue(controller.pollSwitchIntent());
    }
}
