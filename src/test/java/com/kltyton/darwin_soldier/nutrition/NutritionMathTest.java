package com.kltyton.darwin_soldier.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionMathTest {
    @Test
    void steakExamplesMatchTheNutritionSpecification() {
        assertEquals(20, NutritionMath.maximumFood(20, 2, 0));
        assertEquals(30, NutritionMath.maximumFood(20, 2, 5));
        assertEquals(40, NutritionMath.maximumFood(20, 2, 10));

        assertEquals(20, NutritionMath.maximumCapacity(20, 2, 0));
        assertEquals(30, NutritionMath.maximumCapacity(20, 2, 5));
        assertEquals(40, NutritionMath.maximumCapacity(20, 2, 10));

        assertEquals(8, NutritionMath.scaleIntegral(8, 0, 0.20D, 0.0D).amount());
        assertEquals(16, NutritionMath.scaleIntegral(8, 5, 0.20D, 0.0D).amount());
        assertEquals(24, NutritionMath.scaleIntegral(8, 10, 0.20D, 0.0D).amount());
    }

    @Test
    void fractionalIntegralRestorationCarriesWithoutLosingValue() {
        NutritionMath.IntegralScaling first = NutritionMath.scaleIntegral(1, 1, 0.20D, 0.0D);
        assertEquals(1, first.amount());
        assertEquals(0.20D, first.remainder(), 1.0E-9D);

        NutritionMath.IntegralScaling fifth = first;
        for (int i = 1; i < 5; i++) {
            fifth = NutritionMath.scaleIntegral(1, 1, 0.20D, fifth.remainder());
        }
        assertEquals(2, fifth.amount());
        assertEquals(0.0D, fifth.remainder(), 1.0E-9D);
    }

    @Test
    void absorptionExamplesMatchTheNutritionSpecification() {
        assertEquals(10.2F, NutritionMath.absorptionCap(10.0D, 0.01D, 20.0F), 1.0E-5F);
        assertEquals(11.0F, NutritionMath.absorptionCap(10.0D, 0.01D, 100.0F), 1.0E-5F);
        assertEquals(20.0F, NutritionMath.absorptionCap(10.0D, 0.01D, 1000.0F), 1.0E-5F);
        assertEquals(110.0F, NutritionMath.absorptionCap(10.0D, 0.01D, 10000.0F), 1.0E-5F);
    }

    @Test
    void fullnessThresholdIsInclusive() {
        assertTrue(NutritionMath.meetsThreshold(32.0D, 40.0D, 0.80D));
        assertFalse(NutritionMath.meetsThreshold(31.99D, 40.0D, 0.80D));
        assertFalse(NutritionMath.meetsThreshold(20.0D, 40.0D, 0.80D));
    }
}
