package com.kltyton.darwin_soldier.combat.adaptation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptationRulesTest {
    @Test
    void adaptationUsesFiveFivePercentStepsAndStopsAtTwentyFivePercent() {
        int level = 0;
        for (int death = 0; death < 8; death++) {
            level = AdaptationRules.nextLevel(level, 5);
        }

        assertEquals(5, level);
        assertEquals(0.05D, reduction(1), 1.0E-9D);
        assertEquals(0.10D, reduction(2), 1.0E-9D);
        assertEquals(0.15D, reduction(3), 1.0E-9D);
        assertEquals(0.20D, reduction(4), 1.0E-9D);
        assertEquals(0.25D, reduction(5), 1.0E-9D);
        assertEquals(0.25D, reduction(99), 1.0E-9D);
    }

    private static double reduction(int level) {
        return AdaptationRules.reductionForLevel(level, 5,
                0.05D, 0.10D, 0.15D, 0.20D, 0.25D);
    }
}
