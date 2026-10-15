package com.kltyton.darwin_soldier.client.ui.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarwinConfigKuiScreenTest {
    @Test
    void numericSpinnerStepPreservesConfiguredDecimalPrecision() {
        assertEquals("1", DarwinConfigKuiScreen.numberStep(5));
        assertEquals("0.1", DarwinConfigKuiScreen.numberStep(1.0D));
        assertEquals("0.01", DarwinConfigKuiScreen.numberStep(0.05D));
        assertEquals("0.001", DarwinConfigKuiScreen.numberStep(0.015D));
        assertEquals("0.0001", DarwinConfigKuiScreen.numberStep(0.0001D));
    }

    @Test
    void internalMigrationCategoryIsNotUserEditable() {
        assertFalse(DarwinConfigKuiScreen.isVisibleCategory("migration"));
        assertTrue(DarwinConfigKuiScreen.isVisibleCategory("abilities"));
    }

    @Test
    void legacyCamelCaseConfigIdsResolveToExistingSnakeCaseTranslations() {
        assertEquals("config.darwin_soldier.option.peaceful_growth",
                DarwinConfigKuiScreen.optionTranslationKey("peacefulGrowth"));
        assertEquals("config.darwin_soldier.option.points_per_attribute_point",
                DarwinConfigKuiScreen.optionTranslationKey("pointsPerAttributePoint"));
        assertEquals("config.darwin_soldier.option.boss_health_threshold",
                DarwinConfigKuiScreen.optionTranslationKey("boss_health_threshold"));
    }
}
