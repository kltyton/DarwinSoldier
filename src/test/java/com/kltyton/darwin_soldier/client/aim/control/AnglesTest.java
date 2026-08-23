package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnglesTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    void wrapDegreesMapsPositiveOverflowToNegativeSide() {
        assertEquals(-170.0D, Angles.wrapDegrees(190.0D), EPSILON);
        assertEquals(-180.0D, Angles.wrapDegrees(540.0D), EPSILON);
        assertEquals(0.0D, Angles.wrapDegrees(720.0D), EPSILON);
    }

    @Test
    void wrapDegreesMapsNegativeOverflowToPositiveSide() {
        assertEquals(170.0D, Angles.wrapDegrees(-190.0D), EPSILON);
        assertEquals(-180.0D, Angles.wrapDegrees(-540.0D), EPSILON);
    }

    @Test
    void wrapDegreesKeepsNormalizedValuesStable() {
        assertEquals(0.0D, Angles.wrapDegrees(0.0D), EPSILON);
        assertEquals(90.0D, Angles.wrapDegrees(90.0D), EPSILON);
        assertEquals(-90.0D, Angles.wrapDegrees(-90.0D), EPSILON);
    }

    @Test
    void angularDistanceIsWrapAwareShortestPath() {
        assertEquals(20.0D, Angles.angularDistanceDegrees(170.0D, -170.0D), EPSILON);
        assertEquals(20.0D, Angles.angularDistanceDegrees(-170.0D, 170.0D), EPSILON);
        assertEquals(180.0D, Angles.angularDistanceDegrees(0.0D, 180.0D), EPSILON);
        assertEquals(0.0D, Angles.angularDistanceDegrees(350.0D, -10.0D), EPSILON);
    }

    @Test
    void clampKeepsValueInsideBounds() {
        assertEquals(5.0D, Angles.clamp(5.0D, 0.0D, 10.0D), EPSILON);
        assertEquals(0.0D, Angles.clamp(-3.0D, 0.0D, 10.0D), EPSILON);
        assertEquals(10.0D, Angles.clamp(12.0D, 0.0D, 10.0D), EPSILON);
    }

    @Test
    void sanitizeRangeClampsFiniteValuesAndFallsBackForNonFinite() {
        assertEquals(0.5D, Angles.sanitizeRange(0.5D, 0.35D, 0.05D, 1.0D), EPSILON);
        assertEquals(1.0D, Angles.sanitizeRange(2.0D, 0.35D, 0.05D, 1.0D), EPSILON);
        assertEquals(0.05D, Angles.sanitizeRange(0.01D, 0.35D, 0.05D, 1.0D), EPSILON);
        assertEquals(0.35D, Angles.sanitizeRange(Double.NaN, 0.35D, 0.05D, 1.0D), EPSILON);
        assertEquals(0.35D, Angles.sanitizeRange(Double.POSITIVE_INFINITY, 0.35D, 0.05D, 1.0D), EPSILON);
    }
}
