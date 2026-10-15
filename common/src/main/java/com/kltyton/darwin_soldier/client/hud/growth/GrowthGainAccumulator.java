package com.kltyton.darwin_soldier.client.hud.growth;

import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;

/** Mutable client-only state for one refreshed/merged growth notification. */
public final class GrowthGainAccumulator {
    static final long MERGE_WINDOW_MILLIS = 1_000L;
    static final long HOLD_MILLIS = 1_000L;
    static final long DISPLAY_MILLIS = 1_500L;

    private double amount;
    private GrowthGainReason reason = GrowthGainReason.normal();
    private long lastUpdateMillis;
    private boolean active;

    public void accept(double awardedGrowth, GrowthGainReason awardedReason, long nowMillis) {
        if (!Double.isFinite(awardedGrowth) || awardedGrowth <= 0.0D) {
            return;
        }
        boolean merge = active && nowMillis >= lastUpdateMillis
                && nowMillis - lastUpdateMillis <= MERGE_WINDOW_MILLIS;
        amount = merge ? amount + awardedGrowth : awardedGrowth;
        reason = awardedReason;
        lastUpdateMillis = nowMillis;
        active = true;
    }

    public DisplayState snapshot(long nowMillis) {
        if (!active) {
            return DisplayState.hidden();
        }
        long elapsed = Math.max(0L, nowMillis - lastUpdateMillis);
        if (elapsed >= DISPLAY_MILLIS) {
            return DisplayState.hidden();
        }
        float alpha = elapsed <= HOLD_MILLIS
                ? 1.0F
                : (DISPLAY_MILLIS - elapsed) / (float) (DISPLAY_MILLIS - HOLD_MILLIS);
        return new DisplayState(true, amount, reason, alpha);
    }

    public void clear() {
        active = false;
        amount = 0.0D;
        reason = GrowthGainReason.normal();
        lastUpdateMillis = 0L;
    }

    public record DisplayState(boolean visible, double amount, GrowthGainReason reason, float alpha) {
        private static DisplayState hidden() {
            return new DisplayState(false, 0.0D, GrowthGainReason.normal(), 0.0F);
        }
    }
}
