package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AimDirectionTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    void ofNormalizesToUnitLength() {
        AimDirection direction = AimDirection.of(3.0D, 0.0D, 0.0D);
        assertEquals(1.0D, direction.x(), EPSILON);
        assertEquals(0.0D, direction.y(), EPSILON);
        assertEquals(0.0D, direction.z(), EPSILON);
    }

    @Test
    void compactConstructorNormalizesFiniteComponents() {
        AimDirection direction = new AimDirection(2.0D, 0.0D, 0.0D);
        assertEquals(1.0D, direction.x(), EPSILON);
        assertEquals(0.0D, direction.y(), EPSILON);
        assertEquals(0.0D, direction.z(), EPSILON);
    }

    @Test
    void compactConstructorRejectsZeroVector() {
        assertThrows(IllegalArgumentException.class, () -> new AimDirection(0.0D, 0.0D, 0.0D));
    }

    @Test
    void ofRejectsZeroVector() {
        assertThrows(IllegalArgumentException.class, () -> AimDirection.of(0.0D, 0.0D, 0.0D));
    }

    @Test
    void ofRejectsNonFiniteComponents() {
        assertThrows(IllegalArgumentException.class, () -> AimDirection.of(Double.NaN, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> AimDirection.of(Double.POSITIVE_INFINITY, 0.0D, 0.0D));
    }

    @Test
    void angleDegreesToIdenticalDirectionIsZero() {
        AimDirection direction = AimDirection.of(1.0D, 2.0D, 3.0D);
        assertEquals(0.0D, direction.angleDegreesTo(direction), EPSILON);
    }

    @Test
    void angleDegreesToOppositeDirectionIsOneHundredEighty() {
        AimDirection positive = AimDirection.of(1.0D, 0.0D, 0.0D);
        AimDirection negative = AimDirection.of(-1.0D, 0.0D, 0.0D);
        assertEquals(180.0D, positive.angleDegreesTo(negative), EPSILON);
    }

    @Test
    void angleDegreesToIsSymmetric() {
        AimDirection x = AimDirection.of(1.0D, 0.0D, 0.0D);
        AimDirection y = AimDirection.of(0.0D, 1.0D, 0.0D);
        assertEquals(x.angleDegreesTo(y), y.angleDegreesTo(x), EPSILON);
    }

    @Test
    void angleDegreesToOrthogonalIsNinety() {
        AimDirection x = AimDirection.of(1.0D, 0.0D, 0.0D);
        AimDirection z = AimDirection.of(0.0D, 0.0D, 1.0D);
        assertEquals(90.0D, x.angleDegreesTo(z), EPSILON);
    }

    @Test
    void angleDegreesToDiagonalIsFortyFive() {
        AimDirection x = AimDirection.of(1.0D, 0.0D, 0.0D);
        AimDirection diagonal = AimDirection.of(1.0D, 1.0D, 0.0D);
        assertEquals(45.0D, x.angleDegreesTo(diagonal), EPSILON);
    }

    @Test
    void fromYawPitchZeroPointsAlongPositiveX() {
        AimDirection direction = AimDirection.fromYawPitch(0.0D, 0.0D);
        assertEquals(1.0D, direction.x(), EPSILON);
        assertEquals(0.0D, direction.y(), EPSILON);
        assertEquals(0.0D, direction.z(), EPSILON);
    }

    @Test
    void fromYawPitchPositiveYawRotatesTowardPositiveZ() {
        AimDirection direction = AimDirection.fromYawPitch(90.0D, 0.0D);
        assertEquals(0.0D, direction.x(), EPSILON);
        assertEquals(0.0D, direction.y(), EPSILON);
        assertEquals(1.0D, direction.z(), EPSILON);
    }

    @Test
    void fromYawPitchPositivePitchPointsUp() {
        AimDirection direction = AimDirection.fromYawPitch(0.0D, 90.0D);
        assertEquals(0.0D, direction.x(), EPSILON);
        assertEquals(1.0D, direction.y(), EPSILON);
        assertEquals(0.0D, direction.z(), EPSILON);
    }
}
