package com.kltyton.darwin_soldier.client.aim.control;

/**
 * Accumulates manual yaw/pitch mouse deltas into a radial offset and converts
 * dead-zone crossings into exactly one switch intent per fresh capture.
 *
 * <p>A failed switch preserves the accumulated offset but keeps the crossing
 * consumed; only new nonzero input re-arms the controller. A successful switch
 * or session reset clears the offset. The offset is clamped radially to
 * {@link #MAX_OFFSET_DEGREES}.
 */
public final class AimInputController {

    public static final double DEAD_ZONE_DEGREES = 6.0D;
    public static final double MAX_OFFSET_DEGREES = 45.0D;

    private static final double MIN_INPUT_SQUARED = 1.0E-18D;

    private double yawOffset;
    private double pitchOffset;
    private boolean armed = true;

    /**
     * Adds manual angular input. Non-finite input is ignored. Any nonzero input
     * counts as a fresh capture and re-arms the controller.
     */
    public void addInput(double yawDeltaDegrees, double pitchDeltaDegrees) {
        if (!Double.isFinite(yawDeltaDegrees) || !Double.isFinite(pitchDeltaDegrees)) {
            return;
        }
        yawOffset += yawDeltaDegrees;
        pitchOffset += pitchDeltaDegrees;
        if (Math.hypot(yawDeltaDegrees, pitchDeltaDegrees) > Math.sqrt(MIN_INPUT_SQUARED)) {
            armed = true;
        }
        clampOffsetToMaximum();
    }

    /**
     * Returns exactly one intent when the armed radial offset crosses the dead
     * zone, then consumes the crossing until fresh input arrives.
     */
    public boolean pollSwitchIntent() {
        if (!armed || radialOffsetDegrees() < DEAD_ZONE_DEGREES) {
            return false;
        }
        armed = false;
        return true;
    }

    /**
     * Records that the switch attempt failed. The accumulated offset is kept so
     * that later new input advances it; the consumed crossing stays consumed.
     */
    public void markSwitchFailed() {
        armed = false;
    }

    /** Clears the accumulated offset and starts a fresh capture (session reset). */
    public void reset() {
        yawOffset = 0.0D;
        pitchOffset = 0.0D;
        armed = true;
    }

    public double yawOffsetDegrees() {
        return yawOffset;
    }

    public double pitchOffsetDegrees() {
        return pitchOffset;
    }

    public double radialOffsetDegrees() {
        return Math.hypot(yawOffset, pitchOffset);
    }

    private void clampOffsetToMaximum() {
        double yawMagnitude = Math.abs(yawOffset);
        double pitchMagnitude = Math.abs(pitchOffset);
        if (yawMagnitude * yawMagnitude + pitchMagnitude * pitchMagnitude
                <= MAX_OFFSET_DEGREES * MAX_OFFSET_DEGREES) {
            return;
        }
        // Overflow-safe radial clamp: normalize by the largest component so
        // Math.hypot never sees components whose product would overflow.
        double maxComponent = Math.max(yawMagnitude, pitchMagnitude);
        double unitYaw = yawOffset / maxComponent;
        double unitPitch = pitchOffset / maxComponent;
        double scale = (MAX_OFFSET_DEGREES / maxComponent) / Math.hypot(unitYaw, unitPitch);
        yawOffset *= scale;
        pitchOffset *= scale;
    }
}
