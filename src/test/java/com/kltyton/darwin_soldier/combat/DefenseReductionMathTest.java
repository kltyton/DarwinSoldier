package com.kltyton.darwin_soldier.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefenseReductionMathTest {
    @Test
    void flatReductionIsOneTenthOfPositiveDefensePoints() {
        assertEquals(5.0F, DefenseReductionMath.minimumReduction(50), 0.0001F);
        assertEquals(1.0F, DefenseReductionMath.minimumReduction(10), 0.0001F);
        assertEquals(0.0F, DefenseReductionMath.minimumReduction(0), 0.0001F);
    }

    @Test
    void negativePointsYieldZeroReduction() {
        assertEquals(0.0F, DefenseReductionMath.minimumReduction(-5), 0.0001F);
        assertEquals(40.0F, DefenseReductionMath.cappedFinal(40.0F, 100.0F, 0.0F), 0.0001F);
    }

    @Test
    void raw100Final40Minimum10KeepsStrongerExistingReduction() {
        assertEquals(40.0F, DefenseReductionMath.cappedFinal(40.0F, 100.0F, 10.0F), 0.0001F);
    }

    @Test
    void raw100Final95Minimum10FillsShortfallTo90() {
        assertEquals(90.0F, DefenseReductionMath.cappedFinal(95.0F, 100.0F, 10.0F), 0.0001F);
    }

    @Test
    void raw3Final3Minimum5CanReduceToZero() {
        assertEquals(0.0F, DefenseReductionMath.cappedFinal(3.0F, 3.0F, 5.0F), 0.0001F);
    }
}
