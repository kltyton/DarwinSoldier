package com.kltyton.darwin_soldier.client.aim.render.trajectory;

/**
 * Pure shrink animation state for trajectory cubes.
 *
 * <p>While the trajectory preview stays eligible for the same weapon, the radius
 * scale eases from 1.0 to {@value #TARGET_SCALE} over {@value #SHRINK_DURATION_NANOS}
 * nanoseconds (1.5 seconds, i.e. 30 game ticks at 20 tps) using smoothstep, driven by
 * an explicit monotonic clock so progress is independent of render frame rate.
 * Any ineligible frame or weapon identity change resets the scale back to 1.0
 * immediately. Repeating the same timestamp produces no progress.
 */
public final class TrajectoryShrinkState {
    static final long SHRINK_DURATION_NANOS = (long) (1.5D * 1_000_000_000L);
    static final double TARGET_SCALE = 0.35D;

    private String weaponId;
    private long startNanos;

    public double update(String weaponId, boolean eligible, long nowNanos) {
        if (!eligible || weaponId == null || !weaponId.equals(this.weaponId)) {
            this.weaponId = weaponId;
            startNanos = nowNanos;
            return 1.0D;
        }
        return currentScale(nowNanos);
    }

    private double currentScale(long nowNanos) {
        long elapsedNanos = nowNanos - startNanos;
        if (elapsedNanos <= 0L) {
            return 1.0D;
        }
        if (elapsedNanos >= SHRINK_DURATION_NANOS) {
            return TARGET_SCALE;
        }
        double x = elapsedNanos / (double) SHRINK_DURATION_NANOS;
        double smooth = x * x * (3.0D - 2.0D * x);
        return 1.0D - smooth * (1.0D - TARGET_SCALE);
    }
}
