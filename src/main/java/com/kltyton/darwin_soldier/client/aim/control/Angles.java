package com.kltyton.darwin_soldier.client.aim.control;

/**
 * Minecraft-independent angle helpers shared by the aim-control components.
 * All angles are in degrees; yaw wraps into {@code [-180, 180)}.
 */
public final class Angles {

    private Angles() {
    }

    /**
     * Normalizes an angle into {@code [-180, 180)} (180 maps to -180).
     */
    public static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    /**
     * Shortest wrap-aware angular distance between two angles, in {@code [0, 180]}.
     */
    public static double angularDistanceDegrees(double from, double to) {
        return Math.abs(wrapDegrees(to - from));
    }

    public static double clamp(double value, double minimum, double maximum) {
        return Math.min(maximum, Math.max(minimum, value));
    }

    /**
     * Clamps a finite value into {@code [minimum, maximum]}; non-finite values
     * fall back to {@code fallback} instead of propagating NaN/infinity.
     */
    public static double sanitizeRange(double value, double fallback, double minimum, double maximum) {
        return Double.isFinite(value) ? clamp(value, minimum, maximum) : fallback;
    }
}
