package com.kltyton.darwin_soldier.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionDataCombatTest {
    @Test
    void absorptionRefillWaitsForTheFullOutOfCombatDelay() {
        NutritionData data = new NutritionData();
        data.markCombat(100L);

        assertFalse(data.canRefillAbsorptionAfterCombat(299L, 200L));
        assertTrue(data.canRefillAbsorptionAfterCombat(300L, 200L));
    }

    @Test
    void anotherCombatHitRestartsTheDelayAndRuntimeClearRemovesIt() {
        NutritionData data = new NutritionData();
        data.markCombat(100L);
        data.markCombat(250L);

        assertFalse(data.canRefillAbsorptionAfterCombat(449L, 200L));
        assertTrue(data.canRefillAbsorptionAfterCombat(450L, 200L));

        data.clearRuntime();
        assertTrue(data.canRefillAbsorptionAfterCombat(250L, 200L));
    }
}
