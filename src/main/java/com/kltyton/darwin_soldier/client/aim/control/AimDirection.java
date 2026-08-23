package com.kltyton.darwin_soldier.client.aim.control;

/**
 * Immutable normalized 3D direction used for virtual-cursor and candidate
 * comparisons. Pure Java; deliberately independent of Minecraft vectors.
 */
public record AimDirection(double x, double y, double z) {

    private static final double MIN_LENGTH_SQUARED = 1.0E-12D;

    public AimDirection {
        requireFinite(x);
        requireFinite(y);
        requireFinite(z);
        requireNonZero(x, y, z);
        double scale = 1.0D / Math.sqrt(x * x + y * y + z * z);
        x *= scale;
        y *= scale;
        z *= scale;
    }

    /**
     * Creates a normalized direction from arbitrary finite components.
     *
     * @throws IllegalArgumentException for zero or non-finite components
     */
    public static AimDirection of(double x, double y, double z) {
        return new AimDirection(x, y, z);
    }

    /**
     * Builds a direction from yaw/pitch in degrees. Yaw 0 is +X rotating toward
     * +Z, pitch 0 is horizontal and positive pitch points up.
     */
    public static AimDirection fromYawPitch(double yawDegrees, double pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitch);
        return new AimDirection(
                cosPitch * Math.cos(yaw),
                Math.sin(pitch),
                cosPitch * Math.sin(yaw));
    }

    /**
     * Angular distance to another direction in degrees, in {@code [0, 180]}.
     */
    public double angleDegreesTo(AimDirection other) {
        double dot = x * other.x + y * other.y + z * other.z;
        return Math.toDegrees(Math.acos(clampDot(dot)));
    }

    private static double clampDot(double value) {
        return Math.min(1.0D, Math.max(-1.0D, value));
    }

    private static void requireFinite(double component) {
        if (!Double.isFinite(component)) {
            throw new IllegalArgumentException("AimDirection components must be finite");
        }
    }

    private static void requireNonZero(double x, double y, double z) {
        if (x * x + y * y + z * z < MIN_LENGTH_SQUARED) {
            throw new IllegalArgumentException("AimDirection must not be the zero vector");
        }
    }
}
