package com.kltyton.darwin_soldier.client.aim.render.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-independent bracket layout math for the camera-facing lock bracket.
 * The bracket is a Titanfall-2-style frame of four corner brackets built from
 * eight thin strips (two per corner) and rendered in two layers: a thicker
 * dark backing plus a thinner amber-gold foreground, with small pale-gold
 * accent nodes on the four outer corners. Coordinates are expressed in the
 * local camera plane: x is the camera-right axis, y is the camera-up axis,
 * and the origin is the locked target center. Strips form a symmetric
 * four-corner frame whose arms run inward from the outer frame by up to
 * {@code armLength}, clamped only so they never cross the center axis. A pure
 * sine pulse breathes the gap between the target bounds and the frame.
 */
public final class LockBracketMath {
    /** Default gap between the target bounds and the inner bracket edge. */
    public static final double DEFAULT_GAP = 0.25D;
    /** Default arm length of each corner strip, measured toward the center. */
    public static final double DEFAULT_ARM_LENGTH = 0.75D;
    /** Default strip thickness of the original single-layer frame. */
    public static final double DEFAULT_STRIP_THICKNESS = 0.06D;
    /** Backing strip thickness: the dark plate visually backing the frame. */
    public static final double BACKING_STRIP_THICKNESS = 0.10D;
    /** Foreground strip thickness: the thin bright frame over the backing. */
    public static final double FOREGROUND_STRIP_THICKNESS = 0.05D;
    /** Side length of the pale-gold corner accent nodes. */
    public static final double DEFAULT_ACCENT_SIZE = 0.12D;
    /** Breathing amplitude added to and subtracted from the base gap. */
    public static final double GAP_PULSE_AMPLITUDE = 0.025D;
    /** Breathing period in seconds for one full gap expansion cycle. */
    public static final double GAP_PULSE_PERIOD_SECONDS = 1.6D;
    /** Full circle in radians (Java 17 has no {@code Math.TAU}). */
    private static final double TWO_PI = 2.0D * Math.PI;

    private LockBracketMath() {
    }

    /**
     * Returns the eight strip rectangles for a bracket centered at the origin.
     * The target occupies {@code [-halfWidth, halfWidth]} on x and
     * {@code [-halfHeight, halfHeight]} on y; every strip lies outside that
     * rectangle when {@code gap > 0}. Non-finite or negative input is rejected
     * by returning an empty list.
     *
     * @param halfWidth   half of the target width (block units)
     * @param halfHeight  half of the target height (block units)
     * @param gap         space between the target bounds and the bracket
     * @param armLength   length of each corner arm, measured toward the center
     * @param thickness   strip thickness
     * @return the eight strips, or an empty list for invalid input
     */
    public static List<Segment> bracket(double halfWidth, double halfHeight,
                                        double gap, double armLength, double thickness) {
        if (!finiteNonNegative(halfWidth) || !finiteNonNegative(halfHeight)
                || !finiteNonNegative(gap) || !finiteNonNegative(armLength)
                || !finiteNonNegative(thickness)) {
            return Collections.emptyList();
        }
        double outerX = halfWidth + gap;
        double outerY = halfHeight + gap;
        double armX = Math.min(armLength, outerX);
        double armY = Math.min(armLength, outerY);
        double halfThickness = thickness * 0.5D;
        double innerX = outerX - armX;
        double innerY = outerY - armY;

        List<Segment> strips = new ArrayList<>(8);
        // Top-left corner: horizontal arm plus vertical arm.
        strips.add(new Segment(innerX, outerY - halfThickness, outerX, outerY + halfThickness));
        strips.add(new Segment(outerX - halfThickness, innerY, outerX + halfThickness, outerY));
        // Top-right corner.
        strips.add(new Segment(-outerX, outerY - halfThickness, -innerX, outerY + halfThickness));
        strips.add(new Segment(-outerX - halfThickness, innerY, -outerX + halfThickness, outerY));
        // Bottom-left corner.
        strips.add(new Segment(innerX, -outerY - halfThickness, outerX, -outerY + halfThickness));
        strips.add(new Segment(outerX - halfThickness, -outerY, outerX + halfThickness, -innerY));
        // Bottom-right corner.
        strips.add(new Segment(-outerX, -outerY - halfThickness, -innerX, -outerY + halfThickness));
        strips.add(new Segment(-outerX - halfThickness, -outerY, -outerX + halfThickness, -innerY));
        return strips;
    }

    /**
     * Returns the four small accent nodes centered on the outer corners of the
     * frame at {@code (±(halfWidth + gap), ±(halfHeight + gap))}. Non-finite or
     * negative input is rejected by returning an empty list.
     *
     * @param halfWidth  half of the target width (block units)
     * @param halfHeight half of the target height (block units)
     * @param gap        space between the target bounds and the frame
     * @param size       side length of each square accent node
     * @return the four accent nodes, or an empty list for invalid input
     */
    public static List<Segment> cornerAccents(double halfWidth, double halfHeight,
                                              double gap, double size) {
        if (!finiteNonNegative(halfWidth) || !finiteNonNegative(halfHeight)
                || !finiteNonNegative(gap) || !finiteNonNegative(size)) {
            return Collections.emptyList();
        }
        double outerX = halfWidth + gap;
        double outerY = halfHeight + gap;
        double halfSize = size * 0.5D;
        List<Segment> nodes = new ArrayList<>(4);
        nodes.add(new Segment(outerX - halfSize, outerY - halfSize, outerX + halfSize, outerY + halfSize));
        nodes.add(new Segment(-outerX - halfSize, outerY - halfSize, -outerX + halfSize, outerY + halfSize));
        nodes.add(new Segment(outerX - halfSize, -outerY - halfSize, outerX + halfSize, -outerY + halfSize));
        nodes.add(new Segment(-outerX - halfSize, -outerY - halfSize, -outerX + halfSize, -outerY + halfSize));
        return nodes;
    }

    /**
     * Deterministic breathing gap at the given wall-clock time. The gap
     * oscillates sinusoidally between {@code baseGap - GAP_PULSE_AMPLITUDE} and
     * {@code baseGap + GAP_PULSE_AMPLITUDE} with period
     * {@code GAP_PULSE_PERIOD_SECONDS}. {@link Math#IEEEremainder} folds the
     * time into a single half-period, so the function stays finite and bounded
     * for arbitrarily large inputs.
     *
     * @param baseGap     base gap in block units
     * @param timeSeconds wall-clock time in seconds
     * @return the breathing gap in block units
     */
    public static double pulseGap(double baseGap, double timeSeconds) {
        double phase = Math.IEEEremainder(timeSeconds, GAP_PULSE_PERIOD_SECONDS);
        return baseGap + GAP_PULSE_AMPLITUDE
                * Math.sin(TWO_PI * phase / GAP_PULSE_PERIOD_SECONDS);
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }

    /**
     * A single bracket strip as an axis-aligned rectangle in the camera plane.
     *
     * @param minX left edge on the camera-right axis
     * @param minY bottom edge on the camera-up axis
     * @param maxX right edge on the camera-right axis
     * @param maxY top edge on the camera-up axis
     */
    public record Segment(double minX, double minY, double maxX, double maxY) {
    }
}
