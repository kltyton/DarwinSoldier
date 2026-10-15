package com.kltyton.darwin_soldier.data;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerGrowthDataTest {
    @BeforeAll
    static void loadDefaultConfig() {
        CommentedConfig config = CommentedConfig.inMemory();
        DarwinConfig.SPEC.correct(config);
        DarwinConfig.SPEC.setConfig(config);
    }

    @Test
    void counterKillReserveRestorationDoesNotResetRecoveryTimer() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setBattleInstinctReserves(2);
        data.setBattleInstinctLastReserveGameTime(1234L);

        assertEquals(1, data.restoreBattleInstinctReserves(1));
        assertEquals(3, data.getBattleInstinctReserves());
        assertEquals(1234L, data.getBattleInstinctLastReserveGameTime());
    }

    @Test
    void counterKillReserveRestorationNeverExceedsConfiguredMaximum() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setBattleInstinctReserves(Integer.MAX_VALUE);

        assertEquals(0, data.restoreBattleInstinctReserves(1));
        assertEquals(5, data.getBattleInstinctReserves());
    }

    @Test
    void abilitiesRelockWhenCurrentAttributesDropAndUnlockAgainWhenRestored() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setAttackPointsDebug(5);
        data.setDefensePointsDebug(5);
        data.setHealthPointsDebug(5);
        data.setPerceptionPointsDebug(10);
        data.setNutritionPointsDebug(10);

        assertTrue(data.isHuntingInstinctUnlocked());
        assertTrue(data.isDamageAdaptationUnlocked());
        assertTrue(data.isStressEvolutionUnlocked());
        assertTrue(data.isSuperPerceptionUnlocked());
        assertTrue(data.isBattleInstinctUnlocked());
        assertTrue(data.getNutrition().isEfficientMetabolismUnlocked());
        assertTrue(data.getNutrition().isNutritionFullnessUnlocked());

        data.setAttackPointsDebug(4);
        data.setDefensePointsDebug(4);
        data.setHealthPointsDebug(4);
        data.setPerceptionPointsDebug(4);
        data.setNutritionPointsDebug(4);

        assertFalse(data.isHuntingInstinctUnlocked());
        assertFalse(data.isDamageAdaptationUnlocked());
        assertFalse(data.isStressEvolutionUnlocked());
        assertFalse(data.isSuperPerceptionUnlocked());
        assertFalse(data.isBattleInstinctUnlocked());
        assertFalse(data.getNutrition().isEfficientMetabolismUnlocked());
        assertFalse(data.getNutrition().isNutritionFullnessUnlocked());
    }

    @Test
    void cumulativePerceptionIsHistoricalOnlyAndDoesNotUnlockSuperPerception() {
        PlayerGrowthData data = new PlayerGrowthData();

        data.setCumulativePerceptionPoints(5);

        assertEquals(0, data.getPerceptionPoints());
        assertEquals(5, data.getCumulativePerceptionPoints());
        assertFalse(data.isSuperPerceptionUnlocked());

        data.setPerceptionPointsDebug(5);
        assertTrue(data.isSuperPerceptionUnlocked());
    }

    @Test
    void restoringBattleInstinctRequirementDoesNotRefillSpentReserves() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setPerceptionPointsDebug(10);
        data.setBattleInstinctReserves(2);

        data.setPerceptionPointsDebug(0);
        data.setPerceptionPointsDebug(10);

        assertTrue(data.isBattleInstinctUnlocked());
        assertEquals(2, data.getBattleInstinctReserves());
    }

    @Test
    void battleInstinctModePersistsAndOldSavesDefaultToNormalMode() {
        PlayerGrowthData data = new PlayerGrowthData();
        assertTrue(data.isBattleInstinctCounterEnabled());

        data.setBattleInstinctCounterEnabled(false);
        assertFalse(PlayerGrowthData.load(data.save()).isBattleInstinctCounterEnabled());

        assertTrue(PlayerGrowthData.load(new CompoundTag()).isBattleInstinctCounterEnabled());
    }
}
