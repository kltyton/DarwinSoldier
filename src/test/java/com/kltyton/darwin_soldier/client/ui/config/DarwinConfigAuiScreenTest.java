package com.kltyton.darwin_soldier.client.ui.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarwinConfigAuiScreenTest {
    @Test
    void numericSpinnerStepPreservesConfiguredDecimalPrecision() {
        assertEquals("1", DarwinConfigAuiScreen.numberStep(5));
        assertEquals("0.1", DarwinConfigAuiScreen.numberStep(1.0D));
        assertEquals("0.01", DarwinConfigAuiScreen.numberStep(0.05D));
        assertEquals("0.001", DarwinConfigAuiScreen.numberStep(0.015D));
        assertEquals("0.0001", DarwinConfigAuiScreen.numberStep(0.0001D));
    }

    @Test
    void internalMigrationCategoryIsNotUserEditable() {
        assertFalse(DarwinConfigAuiScreen.isVisibleCategory("migration"));
        assertTrue(DarwinConfigAuiScreen.isVisibleCategory("abilities"));
    }

    @Test
    void legacyCamelCaseConfigIdsResolveToExistingSnakeCaseTranslations() {
        assertEquals("config.darwin_soldier.option.peaceful_growth",
                DarwinConfigAuiScreen.optionTranslationKey("peacefulGrowth"));
        assertEquals("config.darwin_soldier.option.points_per_attribute_point",
                DarwinConfigAuiScreen.optionTranslationKey("pointsPerAttributePoint"));
        assertEquals("config.darwin_soldier.option.boss_health_threshold",
                DarwinConfigAuiScreen.optionTranslationKey("boss_health_threshold"));
    }
}
