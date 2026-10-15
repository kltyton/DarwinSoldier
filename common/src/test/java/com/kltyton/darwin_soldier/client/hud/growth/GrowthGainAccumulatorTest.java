package com.kltyton.darwin_soldier.client.hud.growth;

import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthGainAccumulatorTest {
    @Test
    void gainsWithinOneSecondMergeAndUseTheLastSettlementReason() {
        GrowthGainAccumulator accumulator = new GrowthGainAccumulator();
        accumulator.accept(1.0D, GrowthGainReason.normal(), 0L);
        accumulator.accept(1.0D, GrowthGainReason.normal(), 500L);
        accumulator.accept(3.0D, GrowthGainReason.lowHealth(3.0D), 900L);

        GrowthGainAccumulator.DisplayState state = accumulator.snapshot(900L);

        assertTrue(state.visible());
        assertEquals(5.0D, state.amount(), 1.0E-9D);
        assertEquals(GrowthGainReason.Type.LOW_HEALTH, state.reason().type());
        assertEquals(3.0D, state.reason().multiplier(), 1.0E-9D);
    }

    @Test
    void settlementAfterMergeWindowStartsANewDisplay() {
        GrowthGainAccumulator accumulator = new GrowthGainAccumulator();
        accumulator.accept(2.0D, GrowthGainReason.petKill(0.5D), 0L);

        accumulator.accept(0.5D, GrowthGainReason.normal(), 1001L);

        GrowthGainAccumulator.DisplayState state = accumulator.snapshot(1001L);
        assertEquals(0.5D, state.amount(), 1.0E-9D);
        assertEquals(GrowthGainReason.Type.NORMAL, state.reason().type());
    }

    @Test
    void displayHoldsForOneSecondThenFadesOutByOneAndAHalfSeconds() {
        GrowthGainAccumulator accumulator = new GrowthGainAccumulator();
        accumulator.accept(1.0D, GrowthGainReason.safeCombat(0.1D), 100L);

        assertEquals(1.0F, accumulator.snapshot(1100L).alpha(), 1.0E-6F);
        assertEquals(0.5F, accumulator.snapshot(1350L).alpha(), 1.0E-6F);
        assertFalse(accumulator.snapshot(1600L).visible());
    }

    @Test
    void invalidOrNonPositiveNotificationsAreIgnored() {
        GrowthGainAccumulator accumulator = new GrowthGainAccumulator();

        accumulator.accept(0.0D, GrowthGainReason.normal(), 0L);
        accumulator.accept(Double.NaN, GrowthGainReason.normal(), 1L);

        assertFalse(accumulator.snapshot(1L).visible());
    }

    @Test
    void numberFormattingRemovesFloatingPointNoiseAndTrailingZeros() {
        assertEquals("5", GrowthGainFormatter.format(5.0D));
        assertEquals("0.3", GrowthGainFormatter.format(0.30000004D));
        assertEquals("1.24", GrowthGainFormatter.format(1.236D));
        assertEquals("0.1", GrowthGainFormatter.format(0.1D));
    }
}
