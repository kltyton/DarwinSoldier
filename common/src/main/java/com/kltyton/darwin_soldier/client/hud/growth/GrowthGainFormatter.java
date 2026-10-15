package com.kltyton.darwin_soldier.client.hud.growth;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Stable HUD formatting for growth amounts and multipliers. */
public final class GrowthGainFormatter {
    private GrowthGainFormatter() {
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
