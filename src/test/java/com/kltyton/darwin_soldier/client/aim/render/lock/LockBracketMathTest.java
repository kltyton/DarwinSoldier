package com.kltyton.darwin_soldier.client.aim.render.lock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockBracketMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void producesExactlyEightStripsForValidInput() {
        assertEquals(8, standardBracket().size());
    }

    @Test
    void fourCornerFramesAreSymmetricalAroundBothAxes() {
        List<LockBracketMath.Segment> strips = standardBracket();
        assertSymmetricPair(strips.get(0), strips.get(2), true);
        assertSymmetricPair(strips.get(1), strips.get(3), true);
        assertSymmetricPair(strips.get(4), strips.get(6), true);
        assertSymmetricPair(strips.get(5), strips.get(7), true);
        assertSymmetricPair(strips.get(0), strips.get(4), false);
        assertSymmetricPair(strips.get(1), strips.get(5), false);
        assertSymmetricPair(strips.get(2), strips.get(6), false);
        assertSymmetricPair(strips.get(3), strips.get(7), false);
    }

    @Test
    void allStripsStayStrictlyOutsideTargetBounds() {
        double halfWidth = 0.8D;
        double halfHeight = 1.1D;
        double gap = 0.25D;
        double outerX = halfWidth + gap;
        double outerY = halfHeight + gap;
        for (LockBracketMath.Segment strip : LockBracketMath.bracket(halfWidth, halfHeight, gap, 0.75D, 0.06D)) {
            assertTrue(strip.maxX() >= -outerX - EPSILON && strip.minX() <= outerX + EPSILON
                            && strip.maxY() >= -outerY - EPSILON && strip.minY() <= outerY + EPSILON,
                    "strip must lie within the outer frame: " + strip);
            assertFalse(strip.maxX() <= halfWidth + EPSILON && strip.minX() >= -halfWidth - EPSILON
                            && strip.maxY() <= halfHeight + EPSILON && strip.minY() >= -halfHeight - EPSILON,
                    "strip must not sit fully inside the target bounds: " + strip);
        }
    }

    @Test
    void stripsStopAtOuterBoundaryWithoutOvershooting() {
        for (LockBracketMath.Segment strip : standardBracket()) {
            assertTrue(strip.maxX() <= 1.33D + EPSILON, "right edge exceeds outer bound: " + strip);
            assertTrue(strip.minX() >= -1.33D - EPSILON, "left edge exceeds outer bound: " + strip);
            assertTrue(strip.maxY() <= 1.33D + EPSILON, "top edge exceeds outer bound: " + strip);
            assertTrue(strip.minY() >= -1.33D - EPSILON, "bottom edge exceeds outer bound: " + strip);
        }
    }

    @Test
    void stripThicknessIsRespected() {
        for (LockBracketMath.Segment strip : standardBracket()) {
            boolean horizontal = strip.maxY() - strip.minY() <= strip.maxX() - strip.minX();
            if (horizontal) {
                assertEquals(0.06D, strip.maxY() - strip.minY(), EPSILON);
                assertEquals(0.7D, strip.maxX() - strip.minX(), EPSILON);
            } else {
                assertEquals(0.06D, strip.maxX() - strip.minX(), EPSILON);
                assertEquals(0.7D, strip.maxY() - strip.minY(), EPSILON);
            }
        }
    }

    @Test
    void cornerArmsMeetTheFrameCorners() {
        List<LockBracketMath.Segment> strips = standardBracket();
        LockBracketMath.Segment horizontal = strips.get(0);
        LockBracketMath.Segment vertical = strips.get(1);
        assertEquals(0.6D, horizontal.minX(), EPSILON);
        assertEquals(1.3D, horizontal.maxX(), EPSILON);
        assertEquals(1.27D, horizontal.minY(), EPSILON);
        assertEquals(1.33D, horizontal.maxY(), EPSILON);
        assertEquals(1.27D, vertical.minX(), EPSILON);
        assertEquals(1.33D, vertical.maxX(), EPSILON);
        assertEquals(0.6D, vertical.minY(), EPSILON);
        assertEquals(1.3D, vertical.maxY(), EPSILON);
    }

    @Test
    void defaultArmLengthSurvivesSmallWidthGap() {
        List<LockBracketMath.Segment> strips = LockBracketMath.bracket(0.4D, 0.6D,
                LockBracketMath.DEFAULT_GAP, LockBracketMath.DEFAULT_ARM_LENGTH,
                LockBracketMath.DEFAULT_STRIP_THICKNESS);
        LockBracketMath.Segment horizontal = strips.get(0);
        double armX = horizontal.maxX() - horizontal.minX();
        assertTrue(armX > 0.5D, "horizontal arm collapsed to " + armX);
        assertEquals(0.0D, horizontal.minX(), EPSILON);
        assertEquals(0.65D, horizontal.maxX(), EPSILON);
        LockBracketMath.Segment vertical = strips.get(1);
        double armY = vertical.maxY() - vertical.minY();
        assertTrue(armY > 0.5D, "vertical arm collapsed to " + armY);
        assertEquals(0.85D, vertical.maxY(), EPSILON);
    }

    @Test
    void fullDefaultArmLengthIsPreservedWhenFrameAllowsIt() {
        List<LockBracketMath.Segment> strips = LockBracketMath.bracket(0.6D, 0.9D,
                LockBracketMath.DEFAULT_GAP, LockBracketMath.DEFAULT_ARM_LENGTH,
                LockBracketMath.DEFAULT_STRIP_THICKNESS);
        assertEquals(LockBracketMath.DEFAULT_ARM_LENGTH,
                strips.get(0).maxX() - strips.get(0).minX(), EPSILON);
        assertEquals(LockBracketMath.DEFAULT_ARM_LENGTH,
                strips.get(1).maxY() - strips.get(1).minY(), EPSILON);
    }

    @Test
    void armsNeverCrossTheCenterAxis() {
        List<LockBracketMath.Segment> strips = LockBracketMath.bracket(0.1D, 0.1D, 0.25D, 0.75D, 0.06D);
        assertTrue(strips.get(0).minX() >= -EPSILON);
        assertTrue(strips.get(2).maxX() <= EPSILON);
        assertTrue(strips.get(1).minY() >= -EPSILON);
        assertTrue(strips.get(5).maxY() <= EPSILON);
    }

    @Test
    void invalidInputReturnsEmptyBracket() {
        assertTrue(LockBracketMath.bracket(-1.0D, 1.0D, 0.3D, 0.7D, 0.06D).isEmpty());
        assertTrue(LockBracketMath.bracket(1.0D, Double.NaN, 0.3D, 0.7D, 0.06D).isEmpty());
        assertTrue(LockBracketMath.bracket(1.0D, 1.0D, -0.1D, 0.7D, 0.06D).isEmpty());
        assertTrue(LockBracketMath.bracket(1.0D, 1.0D, 0.3D, Double.POSITIVE_INFINITY, 0.06D).isEmpty());
        assertTrue(LockBracketMath.bracket(1.0D, 1.0D, 0.3D, 0.7D, -0.06D).isEmpty());
    }

    @Test
    void pulseGapEndpointsBoundsAndPeriod() {
        double base = 0.25D;
        double amplitude = LockBracketMath.GAP_PULSE_AMPLITUDE;
        double period = LockBracketMath.GAP_PULSE_PERIOD_SECONDS;
        double step = period / 64.0D;
        for (double t = -3.0D * period; t <= 5.0D * period; t += step) {
            assertGapWithinBounds(base, amplitude, t);
        }
        for (double t : new double[]{1.0E9D, 1.0E15D, 1.0E18D, -1.0E12D}) {
            assertGapWithinBounds(base, amplitude, t);
        }
        for (double t : new double[]{0.0D, period, 2.0D * period, -period, 8.0D * period}) {
            assertEquals(base, LockBracketMath.pulseGap(base, t), EPSILON);
        }
        assertEquals(base + amplitude, LockBracketMath.pulseGap(base, period / 4.0D), EPSILON);
        assertEquals(base - amplitude, LockBracketMath.pulseGap(base, 3.0D * period / 4.0D), EPSILON);
        assertEquals(base - amplitude, LockBracketMath.pulseGap(base, -period / 4.0D), EPSILON);
        for (double t : new double[]{0.1D, 0.7D, 1.3D}) {
            assertEquals(LockBracketMath.pulseGap(base, t),
                    LockBracketMath.pulseGap(base, t + period), EPSILON);
            assertEquals(LockBracketMath.pulseGap(base, t),
                    LockBracketMath.pulseGap(base, t + 5.0D * period), EPSILON);
        }
    }

    @Test
    void backingLayerContainsAndMirrorsForegroundLayer() {
        List<LockBracketMath.Segment> backing = LockBracketMath.bracket(1.0D, 1.0D, 0.25D,
                LockBracketMath.DEFAULT_ARM_LENGTH, LockBracketMath.BACKING_STRIP_THICKNESS);
        List<LockBracketMath.Segment> foreground = LockBracketMath.bracket(1.0D, 1.0D, 0.25D,
                LockBracketMath.DEFAULT_ARM_LENGTH, LockBracketMath.FOREGROUND_STRIP_THICKNESS);
        assertEquals(8, backing.size());
        assertEquals(8, foreground.size());
        assertTrue(LockBracketMath.BACKING_STRIP_THICKNESS > LockBracketMath.FOREGROUND_STRIP_THICKNESS);
        assertSymmetricPair(backing.get(0), backing.get(2), true);
        assertSymmetricPair(backing.get(0), backing.get(4), false);
        assertSymmetricPair(foreground.get(0), foreground.get(2), true);
        assertSymmetricPair(foreground.get(0), foreground.get(4), false);
        for (int i = 0; i < backing.size(); i++) {
            LockBracketMath.Segment back = backing.get(i);
            LockBracketMath.Segment front = foreground.get(i);
            assertTrue(back.minX() <= front.minX() + EPSILON && back.maxX() >= front.maxX() - EPSILON,
                    "backing must contain foreground on x: " + back + " vs " + front);
            assertTrue(back.minY() <= front.minY() + EPSILON && back.maxY() >= front.maxY() - EPSILON,
                    "backing must contain foreground on y: " + back + " vs " + front);
        }
    }

    @Test
    void cornerAccentsAreSymmetricalAndCenteredOnOuterCorners() {
        double halfWidth = 0.8D;
        double halfHeight = 1.1D;
        double gap = 0.25D;
        List<LockBracketMath.Segment> nodes = LockBracketMath.cornerAccents(halfWidth, halfHeight, gap,
                LockBracketMath.DEFAULT_ACCENT_SIZE);
        assertEquals(4, nodes.size());
        double outerX = halfWidth + gap;
        double outerY = halfHeight + gap;
        assertCenter(nodes.get(0), outerX, outerY);
        assertCenter(nodes.get(1), -outerX, outerY);
        assertCenter(nodes.get(2), outerX, -outerY);
        assertCenter(nodes.get(3), -outerX, -outerY);
        assertSymmetricPair(nodes.get(0), nodes.get(1), true);
        assertSymmetricPair(nodes.get(0), nodes.get(2), false);
        assertEquals(LockBracketMath.DEFAULT_ACCENT_SIZE, nodes.get(0).maxX() - nodes.get(0).minX(), EPSILON);
        assertEquals(LockBracketMath.DEFAULT_ACCENT_SIZE, nodes.get(0).maxY() - nodes.get(0).minY(), EPSILON);
    }

    @Test
    void cornerAccentsRejectInvalidInput() {
        assertTrue(LockBracketMath.cornerAccents(-1.0D, 1.0D, 0.25D, 0.12D).isEmpty());
        assertTrue(LockBracketMath.cornerAccents(1.0D, Double.NaN, 0.25D, 0.12D).isEmpty());
        assertTrue(LockBracketMath.cornerAccents(1.0D, 1.0D, -0.1D, 0.12D).isEmpty());
        assertTrue(LockBracketMath.cornerAccents(1.0D, 1.0D, 0.25D, Double.POSITIVE_INFINITY).isEmpty());
    }

    private static List<LockBracketMath.Segment> standardBracket() {
        return LockBracketMath.bracket(1.0D, 1.0D, 0.3D, 0.7D, 0.06D);
    }

    private static void assertGapWithinBounds(double base, double amplitude, double t) {
        double gap = LockBracketMath.pulseGap(base, t);
        assertTrue(Double.isFinite(gap), "pulse must stay finite at t=" + t);
        assertTrue(gap >= base - amplitude - EPSILON && gap <= base + amplitude + EPSILON,
                "pulse out of bounds at t=" + t + " gap=" + gap);
    }

    private static void assertCenter(LockBracketMath.Segment segment, double centerX, double centerY) {
        assertEquals(centerX, (segment.minX() + segment.maxX()) / 2.0D, EPSILON);
        assertEquals(centerY, (segment.minY() + segment.maxY()) / 2.0D, EPSILON);
    }

    private static void assertSymmetricPair(LockBracketMath.Segment first,
                                            LockBracketMath.Segment second,
                                            boolean flipAxis) {
        if (flipAxis) {
            assertEquals(-first.maxX(), second.minX(), EPSILON);
            assertEquals(-first.minX(), second.maxX(), EPSILON);
            assertEquals(first.minY(), second.minY(), EPSILON);
            assertEquals(first.maxY(), second.maxY(), EPSILON);
        } else {
            assertEquals(first.minX(), second.minX(), EPSILON);
            assertEquals(first.maxX(), second.maxX(), EPSILON);
            assertEquals(-first.maxY(), second.minY(), EPSILON);
            assertEquals(-first.minY(), second.maxY(), EPSILON);
        }
    }
}
