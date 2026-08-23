package com.kltyton.darwin_soldier.client.aim.control;

/**
 * Stateful, wrap-aware yaw/pitch smoother. Per tick it applies the existing
 * configured interpolation semantics ({@code current + wrap(target - current)
 * * response}) with response in {@code [0.05, 1]}, capped by a maximum step so
 * a large target jump cannot snap the camera. The frame API converts the
 * configured per-tick response into an equivalent per-frame response so
 * convergence is independent of frame rate. It is non-overshooting and
 * converges monotonically for a static target, and it filters desired-angle
 * jitter below a noise tolerance. {@link #reset()} clears state at session
 * end; the next update anchors to the target without moving.
 */
public final class AimRotationSmoother {

    public static final double MIN_RESPONSE = 0.05D;
    public static final double MAX_RESPONSE = 1.0D;
    public static final double DEFAULT_RESPONSE = 0.35D;
    public static final double DEFAULT_NOISE_TOLERANCE_DEGREES = 0.25D;
    public static final double MAX_PITCH_DEGREES = 90.0D;
    /**
     * Per-frame delta time is clamped to this so a rendering pause (or a
     * clock glitch) cannot turn one frame into a snap.
     */
    public static final double MAX_FRAME_DELTA_SECONDS = 0.05D;

    private static final double MAX_STEP_FALLBACK_DEGREES = 180.0D;
    private static final double TICKS_PER_SECOND = 20.0D;

    private double yaw;
    private double pitch;
    private boolean hasState;
    private double lastDesiredYaw;
    private double lastDesiredPitch;
    private boolean hasLastDesired;
    private final double noiseToleranceDegrees;

    public AimRotationSmoother(double initialYaw, double initialPitch) {
        this(initialYaw, initialPitch, DEFAULT_NOISE_TOLERANCE_DEGREES);
    }

    public AimRotationSmoother(double initialYaw, double initialPitch, double noiseToleranceDegrees) {
        yaw = Angles.wrapDegrees(initialYaw);
        pitch = Angles.clamp(initialPitch, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);
        hasState = true;
        lastDesiredYaw = yaw;
        lastDesiredPitch = pitch;
        hasLastDesired = true;
        this.noiseToleranceDegrees = Double.isFinite(noiseToleranceDegrees) && noiseToleranceDegrees >= 0.0D
                ? noiseToleranceDegrees
                : DEFAULT_NOISE_TOLERANCE_DEGREES;
    }

    /**
     * Advances the smoother by one tick toward the desired rotation.
     *
     * @param targetYaw             desired yaw in degrees (any value; wrapped)
     * @param targetPitch           desired pitch in degrees (clamped to [-90, 90])
     * @param response              configured interpolation response in [0.05, 1]
     * @param maxStepPerTickDegrees maximum per-tick angular motion on either axis
     */
    public AimRotation update(
            double targetYaw,
            double targetPitch,
            double response,
            double maxStepPerTickDegrees) {
        double clampedResponse = Angles.sanitizeRange(
                response, DEFAULT_RESPONSE, MIN_RESPONSE, MAX_RESPONSE);
        double maxStep = Double.isFinite(maxStepPerTickDegrees) && maxStepPerTickDegrees > 0.0D
                ? maxStepPerTickDegrees
                : MAX_STEP_FALLBACK_DEGREES;
        return advance(targetYaw, targetPitch, clampedResponse, maxStep);
    }

    /**
     * Advances the smoother by one rendered frame toward the desired rotation.
     * The configured per-tick response {@code r} is converted into the frame
     * response {@code 1 - (1 - r)^(deltaSeconds * 20)} so one second of frames
     * converges exactly like 20 client ticks, regardless of frame rate. Motion
     * is capped to {@code maxDegreesPerSecond * deltaSeconds} so the
     * 12 degrees-per-tick ceiling (240 deg/s at 20 ticks/s) holds at any frame
     * rate. Delta time is clamped to {@link #MAX_FRAME_DELTA_SECONDS};
     * non-finite, zero, or negative delta time produces no motion and never
     * propagates NaN.
     *
     * @param targetYaw           desired yaw in degrees (any value; wrapped)
     * @param targetPitch         desired pitch in degrees (clamped to [-90, 90])
     * @param response            configured per-tick interpolation response in [0.05, 1]
     * @param maxDegreesPerSecond maximum angular speed on either axis in degrees per second
     * @param deltaSeconds        elapsed render-frame time in seconds
     */
    public AimRotation updateFrame(
            double targetYaw,
            double targetPitch,
            double response,
            double maxDegreesPerSecond,
            double deltaSeconds) {
        double clampedResponse = Angles.sanitizeRange(
                response, DEFAULT_RESPONSE, MIN_RESPONSE, MAX_RESPONSE);
        double clampedDelta = sanitizeFrameDelta(deltaSeconds);
        double frameResponse = 1.0D - Math.pow(
                1.0D - clampedResponse, clampedDelta * TICKS_PER_SECOND);
        double maxStep = Double.isFinite(maxDegreesPerSecond) && maxDegreesPerSecond > 0.0D
                ? maxDegreesPerSecond * clampedDelta
                : MAX_STEP_FALLBACK_DEGREES;
        return advance(targetYaw, targetPitch, frameResponse, maxStep);
    }

    private static double sanitizeFrameDelta(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0D) {
            return 0.0D;
        }
        return Math.min(deltaSeconds, MAX_FRAME_DELTA_SECONDS);
    }

    private AimRotation advance(
            double targetYaw,
            double targetPitch,
            double response,
            double maxStep) {
        double desiredYaw = Angles.wrapDegrees(targetYaw);
        double desiredPitch = Angles.clamp(targetPitch, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);

        if (hasLastDesired) {
            if (Angles.angularDistanceDegrees(lastDesiredYaw, desiredYaw) <= noiseToleranceDegrees) {
                desiredYaw = lastDesiredYaw;
            }
            if (Angles.angularDistanceDegrees(lastDesiredPitch, desiredPitch) <= noiseToleranceDegrees) {
                desiredPitch = lastDesiredPitch;
            }
        }
        lastDesiredYaw = desiredYaw;
        lastDesiredPitch = desiredPitch;
        hasLastDesired = true;

        if (!hasState) {
            yaw = desiredYaw;
            pitch = desiredPitch;
            hasState = true;
            return new AimRotation(yaw, pitch);
        }

        double deltaYaw = Angles.wrapDegrees(desiredYaw - yaw);
        yaw = Angles.wrapDegrees(yaw + Angles.clamp(deltaYaw * response, -maxStep, maxStep));

        double deltaPitch = Angles.wrapDegrees(desiredPitch - pitch);
        pitch = Angles.clamp(
                pitch + Angles.clamp(deltaPitch * response, -maxStep, maxStep),
                -MAX_PITCH_DEGREES,
                MAX_PITCH_DEGREES);

        return new AimRotation(yaw, pitch);
    }

    /** Clears all state; the next update anchors to its target (session end). */
    public void reset() {
        hasState = false;
        hasLastDesired = false;
    }

    public double yaw() {
        return yaw;
    }

    public double pitch() {
        return pitch;
    }
}
