package com.kltyton.darwin_soldier.client.aim.control;

/**
 * Converts raw mouse deltas into player rotation degrees using the exact
 * vanilla 1.20.1 sensitivity chain (MouseHandler.turnPlayer times
 * Entity.turn): per-pixel degrees = (s * 0.6 + 0.2)^3 * 8 * 0.15 where s is
 * the sensitivity option in [0, 1]. Invert-Y applies only to pitch. Any
 * non-finite input (or sensitivity) produces zero rotation.
 */
public final class MouseInputAngleConverter {

    private MouseInputAngleConverter() {
    }

    public static RotationDelta convert(double rawDX, double rawDY,
                                        double sensitivity, boolean invertY) {
        if (!Double.isFinite(rawDX) || !Double.isFinite(rawDY)
                || !Double.isFinite(sensitivity)) {
            return RotationDelta.ZERO;
        }
        double scale = Math.pow(sensitivity * 0.6D + 0.2D, 3.0D) * 8.0D * 0.15D;
        if (!Double.isFinite(scale)) {
            return RotationDelta.ZERO;
        }
        double yaw = rawDX * scale;
        double pitch = rawDY * scale * (invertY ? -1.0D : 1.0D);
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            return RotationDelta.ZERO;
        }
        return new RotationDelta(yaw, pitch);
    }

    /**
     * Angular mouse delta in player rotation degrees.
     *
     * @param yawDegrees   horizontal rotation delta (positive = turn right)
     * @param pitchDegrees vertical rotation delta (positive = look down)
     */
    public record RotationDelta(double yawDegrees, double pitchDegrees) {
        private static final RotationDelta ZERO = new RotationDelta(0.0D, 0.0D);
    }
}
