package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseInputAngleConverterTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void defaultSensitivityUsesExactVanillaPerPixelScale() {
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(1.0D, 1.0D, 0.5D, false);

        // (0.5 * 0.6 + 0.2)^3 * 8 * 0.15 = 0.15 degrees per pixel.
        assertEquals(0.15D, delta.yawDegrees(), EPSILON);
        assertEquals(0.15D, delta.pitchDegrees(), EPSILON);
    }

    @Test
    void invertYNegatesOnlyPitch() {
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(2.0D, 3.0D, 0.5D, true);

        assertEquals(0.30D, delta.yawDegrees(), EPSILON);
        assertEquals(-0.45D, delta.pitchDegrees(), EPSILON);
    }

    @Test
    void negativeRawInputRotatesTheOppositeDirection() {
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(-1.0D, -1.0D, 0.5D, false);

        assertEquals(-0.15D, delta.yawDegrees(), EPSILON);
        assertEquals(-0.15D, delta.pitchDegrees(), EPSILON);
    }

    @Test
    void zeroSensitivityStillUsesTheVanillaMinimumScale() {
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(10.0D, 10.0D, 0.0D, false);

        double scale = Math.pow(0.2D, 3.0D) * 8.0D * 0.15D;
        assertEquals(10.0D * scale, delta.yawDegrees(), EPSILON);
        assertEquals(10.0D * scale, delta.pitchDegrees(), EPSILON);
    }

    @Test
    void nonFiniteInputProducesZeroRotation() {
        MouseInputAngleConverter.RotationDelta nan =
                MouseInputAngleConverter.convert(Double.NaN, 1.0D, 0.5D, false);
        MouseInputAngleConverter.RotationDelta infinity =
                MouseInputAngleConverter.convert(1.0D, Double.POSITIVE_INFINITY, 0.5D, false);
        MouseInputAngleConverter.RotationDelta badSensitivity =
                MouseInputAngleConverter.convert(1.0D, 1.0D, Double.NaN, false);

        assertEquals(0.0D, nan.yawDegrees(), EPSILON);
        assertEquals(0.0D, nan.pitchDegrees(), EPSILON);
        assertEquals(0.0D, infinity.yawDegrees(), EPSILON);
        assertEquals(0.0D, infinity.pitchDegrees(), EPSILON);
        assertEquals(0.0D, badSensitivity.yawDegrees(), EPSILON);
        assertEquals(0.0D, badSensitivity.pitchDegrees(), EPSILON);
    }

    @Test
    void mouseDownProducesPositivePitchLikeVanilla() {
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(0.0D, 5.0D, 0.5D, false);

        assertTrue(delta.pitchDegrees() > 0.0D);
    }

    @Test
    void scaledOverflowFromFiniteRawInputProducesZeroRotation() {
        // scale = (2.0 * 0.6 + 0.2)^3 * 8 * 0.15 = 3.2928, so MAX * scale overflows.
        MouseInputAngleConverter.RotationDelta delta =
                MouseInputAngleConverter.convert(Double.MAX_VALUE, Double.MAX_VALUE, 2.0D, false);

        assertEquals(0.0D, delta.yawDegrees(), EPSILON);
        assertEquals(0.0D, delta.pitchDegrees(), EPSILON);
    }
}
