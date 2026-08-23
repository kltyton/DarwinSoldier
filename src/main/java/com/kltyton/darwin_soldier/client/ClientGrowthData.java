package com.kltyton.darwin_soldier.client;

import com.kltyton.darwin_soldier.network.SyncGrowthDataPacket;
import com.kltyton.darwin_soldier.nutrition.NutritionClientState;

import java.util.List;

public final class ClientGrowthData {
    private static boolean enabled;
    private static double currentGrowth;
    private static double totalGrowth;
    private static int totalPoints;
    private static int healthPoints;
    private static int attackPoints;
    private static int defensePoints;
    private static int perceptionPoints;
    private static int cumulativeHealthPoints;
    private static int cumulativeAttackPoints;
    private static int cumulativeDefensePoints;
    private static int cumulativePerceptionPoints;
    private static double healthPerPoint = 1.5D;
    private static double attackPerPoint = 0.4D;
    private static double defensePerPoint = 0.25D;
    private static boolean derivedAttributesEnabled = true;
    private static int healingHealthPoints = 5;
    private static double healingPerStep = 0.01D;
    private static int armorPierceAttackPoints = 15;
    private static double armorPiercePerStep = 1.0D;
    private static int toughnessDefensePoints = 5;
    private static double toughnessPerStep = 1.0D;
    private static boolean minimumDamageEnabled = true;
    private static double minimumDamageRatio = 0.75D;
    private static int damageAdaptationUnlockPoints = 5;
    private static int huntingInstinctUnlockPoints = 5;
    private static int stressEvolutionUnlockPoints = 5;
    private static int superPerceptionUnlockPoints = 5;
    private static int battleInstinctUnlockPoints = 10;
    private static int efficientMetabolismUnlockPoints = 5;
    private static int nutritionFullnessUnlockPoints = 10;
    private static boolean nutritionFeatureEnabled;
    private static int nutritionPoints;
    private static int cumulativeNutritionPoints;
    private static int nutritionMaximumPoints = 10;
    private static int nutritionMaximumFood = 20;
    private static double nutritionFoodBonusPerPoint = 0.20D;
    private static double nutritionSaturationBonusPerPoint = 0.20D;
    private static boolean efficientMetabolismUnlocked;
    private static boolean efficientMetabolismEnabled;
    private static boolean efficientMetabolismActive;
    private static boolean nutritionFullnessUnlocked;
    private static boolean nutritionFullnessEnabled;
    private static boolean nutritionFullnessActive;
    private static boolean thirstLinked;
    private static double efficientMetabolismHealingPercent = 0.015D;
    private static int efficientMetabolismFoodCost = 1;
    private static double efficientMetabolismThirstCost = 0.5D;
    private static double nutritionFullnessThreshold = 0.80D;
    private static int nutritionFullnessHasteLevel = 2;
    private static float nutritionFullnessAbsorptionCap;
    private static boolean damageAdaptationUnlocked;
    private static boolean huntingInstinctUnlocked;
    private static boolean stressEvolutionUnlocked;
    private static boolean superPerceptionUnlocked;
    private static boolean battleInstinctUnlocked;
    private static boolean adaptationFeatureEnabled = true;
    private static boolean huntingInstinctFeatureEnabled = true;
    private static boolean huntingInternalImpactFeatureEnabled = true;
    private static boolean stressEvolutionFeatureEnabled = true;
    private static boolean battleInstinctFeatureEnabled = true;
    private static boolean damageAdaptationEnabled;
    private static boolean huntingInstinctEnabled;
    private static boolean huntingInternalImpactEnabled = true;
    private static boolean stressEvolutionEnabled;
    private static boolean superPerceptionEnabled;
    private static boolean battleInstinctEnabled;
    private static boolean battleInstinctCounterEnabled = true;
    private static int battleInstinctReserves;
    private static int battleInstinctMaxReserves;
    private static long huntingInstinctCooldownUntil;
    private static long huntingInstinctActiveUntil;
    private static long stressEvolutionCooldownUntil;
    private static List<SyncGrowthDataPacket.AdaptationEntry> adaptationEntries = List.of();
    private static long revision;

    private ClientGrowthData() {
    }

    public static void update(SyncGrowthDataPacket packet) {
        enabled = packet.enabled();
        currentGrowth = packet.currentGrowth();
        totalGrowth = packet.totalGrowth();
        totalPoints = packet.totalPoints();
        healthPoints = packet.healthPoints();
        attackPoints = packet.attackPoints();
        defensePoints = packet.defensePoints();
        perceptionPoints = packet.perceptionPoints();
        cumulativeHealthPoints = packet.cumulativeHealthPoints();
        cumulativeAttackPoints = packet.cumulativeAttackPoints();
        cumulativeDefensePoints = packet.cumulativeDefensePoints();
        cumulativePerceptionPoints = packet.cumulativePerceptionPoints();
        SyncGrowthDataPacket.AttributeSettings attributes = packet.attributeSettings();
        healthPerPoint = attributes.healthPerPoint();
        attackPerPoint = attributes.attackPerPoint();
        defensePerPoint = attributes.defensePerPoint();
        derivedAttributesEnabled = attributes.derivedAttributesEnabled();
        healingHealthPoints = attributes.healingHealthPoints();
        healingPerStep = attributes.healingPerStep();
        armorPierceAttackPoints = attributes.armorPierceAttackPoints();
        armorPiercePerStep = attributes.armorPiercePerStep();
        toughnessDefensePoints = attributes.toughnessDefensePoints();
        toughnessPerStep = attributes.toughnessPerStep();
        minimumDamageEnabled = attributes.minimumDamageEnabled();
        minimumDamageRatio = attributes.minimumDamageRatio();
        damageAdaptationUnlockPoints = attributes.damageAdaptationUnlockPoints();
        huntingInstinctUnlockPoints = attributes.huntingInstinctUnlockPoints();
        stressEvolutionUnlockPoints = attributes.stressEvolutionUnlockPoints();
        superPerceptionUnlockPoints = attributes.superPerceptionUnlockPoints();
        battleInstinctUnlockPoints = attributes.battleInstinctUnlockPoints();
        efficientMetabolismUnlockPoints = attributes.efficientMetabolismUnlockPoints();
        nutritionFullnessUnlockPoints = attributes.nutritionFullnessUnlockPoints();
        SyncGrowthDataPacket.NutritionEntry nutrition = packet.nutrition();
        nutritionFeatureEnabled = nutrition.featureEnabled();
        nutritionPoints = nutrition.points();
        cumulativeNutritionPoints = nutrition.cumulativePoints();
        nutritionMaximumPoints = nutrition.maximumPoints();
        nutritionMaximumFood = nutrition.maximumFood();
        nutritionFoodBonusPerPoint = nutrition.foodBonusPerPoint();
        nutritionSaturationBonusPerPoint = nutrition.saturationBonusPerPoint();
        efficientMetabolismUnlocked = nutrition.efficientMetabolismUnlocked();
        efficientMetabolismEnabled = nutrition.efficientMetabolismEnabled();
        efficientMetabolismActive = nutrition.efficientMetabolismActive();
        nutritionFullnessUnlocked = nutrition.nutritionFullnessUnlocked();
        nutritionFullnessEnabled = nutrition.nutritionFullnessEnabled();
        nutritionFullnessActive = nutrition.nutritionFullnessActive();
        thirstLinked = nutrition.thirstLinked();
        efficientMetabolismHealingPercent = nutrition.efficientHealingPercent();
        efficientMetabolismFoodCost = nutrition.efficientFoodCost();
        efficientMetabolismThirstCost = nutrition.efficientThirstCost();
        nutritionFullnessThreshold = nutrition.fullnessThreshold();
        nutritionFullnessHasteLevel = nutrition.fullnessHasteLevel();
        nutritionFullnessAbsorptionCap = nutrition.absorptionCap();
        NutritionClientState.update(enabled, nutritionFeatureEnabled, nutritionPoints, nutritionMaximumFood,
                nutritionFoodBonusPerPoint, nutritionSaturationBonusPerPoint);
        adaptationFeatureEnabled = packet.adaptationFeatureEnabled();
        huntingInstinctFeatureEnabled = packet.huntingInstinctFeatureEnabled();
        huntingInternalImpactFeatureEnabled = packet.huntingInternalImpactFeatureEnabled();
        stressEvolutionFeatureEnabled = packet.stressEvolutionFeatureEnabled();
        battleInstinctFeatureEnabled = packet.battleInstinctFeatureEnabled();
        damageAdaptationUnlocked = packet.damageAdaptationUnlocked();
        huntingInstinctUnlocked = packet.huntingInstinctUnlocked();
        stressEvolutionUnlocked = packet.stressEvolutionUnlocked();
        superPerceptionUnlocked = packet.superPerceptionUnlocked();
        battleInstinctUnlocked = packet.battleInstinctUnlocked();
        damageAdaptationEnabled = packet.damageAdaptationEnabled();
        huntingInstinctEnabled = packet.huntingInstinctEnabled();
        huntingInternalImpactEnabled = packet.huntingInternalImpactEnabled();
        stressEvolutionEnabled = packet.stressEvolutionEnabled();
        superPerceptionEnabled = packet.superPerceptionEnabled();
        battleInstinctEnabled = packet.battleInstinctEnabled();
        battleInstinctCounterEnabled = packet.battleInstinctCounterEnabled();
        battleInstinctReserves = packet.battleInstinctReserves();
        battleInstinctMaxReserves = packet.battleInstinctMaxReserves();
        huntingInstinctCooldownUntil = packet.huntingInstinctCooldownUntil();
        huntingInstinctActiveUntil = packet.huntingInstinctActiveUntil();
        stressEvolutionCooldownUntil = packet.stressEvolutionCooldownUntil();
        adaptationEntries = List.copyOf(packet.adaptationEntries());
        revision++;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getCurrentGrowth() {
        return currentGrowth;
    }

    public static double getTotalGrowth() {
        return totalGrowth;
    }

    public static int getTotalPoints() {
        return totalPoints;
    }

    public static int getUsedPoints() {
        return healthPoints + attackPoints + defensePoints + perceptionPoints + nutritionPoints;
    }

    public static int getRemainingPoints() {
        return Math.max(0, totalPoints - getUsedPoints());
    }

    public static int getHealthPoints() {
        return healthPoints;
    }

    public static int getAttackPoints() {
        return attackPoints;
    }

    public static int getDefensePoints() {
        return defensePoints;
    }

    public static int getPerceptionPoints() {
        return perceptionPoints;
    }

    public static int getCumulativeHealthPoints() {
        return cumulativeHealthPoints;
    }

    public static int getCumulativeAttackPoints() {
        return cumulativeAttackPoints;
    }

    public static int getCumulativeDefensePoints() {
        return cumulativeDefensePoints;
    }

    public static int getCumulativePerceptionPoints() {
        return cumulativePerceptionPoints;
    }

    public static double getHealthPerPoint() {
        return healthPerPoint;
    }

    public static double getAttackPerPoint() {
        return attackPerPoint;
    }

    public static double getDefensePerPoint() {
        return defensePerPoint;
    }

    public static boolean isDerivedAttributesEnabled() {
        return derivedAttributesEnabled;
    }

    public static int getHealingHealthPoints() {
        return healingHealthPoints;
    }

    public static double getHealingPerStep() {
        return healingPerStep;
    }

    public static int getArmorPierceAttackPoints() {
        return armorPierceAttackPoints;
    }

    public static double getArmorPiercePerStep() {
        return armorPiercePerStep;
    }

    public static int getToughnessDefensePoints() {
        return toughnessDefensePoints;
    }

    public static double getToughnessPerStep() {
        return toughnessPerStep;
    }

    public static double getMinimumEffectiveDamage(int attackPoints) {
        return minimumDamageEnabled ? Math.max(0, attackPoints) * attackPerPoint * minimumDamageRatio : 0.0D;
    }

    public static boolean isNutritionFeatureEnabled() {
        return nutritionFeatureEnabled;
    }

    public static int getNutritionPoints() {
        return nutritionPoints;
    }

    public static int getCumulativeNutritionPoints() {
        return cumulativeNutritionPoints;
    }

    public static int getNutritionMaximumPoints() {
        return nutritionMaximumPoints;
    }

    public static int getNutritionMaximumFood() {
        return nutritionMaximumFood;
    }

    public static boolean isEfficientMetabolismUnlocked() {
        return efficientMetabolismUnlocked;
    }

    public static boolean isEfficientMetabolismEnabled() {
        return efficientMetabolismEnabled;
    }

    public static boolean isEfficientMetabolismActive() {
        return efficientMetabolismActive;
    }

    public static boolean isNutritionFullnessUnlocked() {
        return nutritionFullnessUnlocked;
    }

    public static boolean isNutritionFullnessEnabled() {
        return nutritionFullnessEnabled;
    }

    public static boolean isNutritionFullnessActive() {
        return nutritionFullnessActive;
    }

    public static boolean isThirstLinked() {
        return thirstLinked;
    }

    public static double getEfficientMetabolismHealingPercent() {
        return efficientMetabolismHealingPercent;
    }

    public static int getEfficientMetabolismFoodCost() {
        return efficientMetabolismFoodCost;
    }

    public static double getEfficientMetabolismThirstCost() {
        return efficientMetabolismThirstCost;
    }

    public static double getNutritionFullnessThreshold() {
        return nutritionFullnessThreshold;
    }

    public static int getNutritionFullnessHasteLevel() {
        return nutritionFullnessHasteLevel;
    }

    public static float getNutritionFullnessAbsorptionCap() {
        return nutritionFullnessAbsorptionCap;
    }

    public static long getRevision() {
        return revision;
    }

    public static boolean isDamageAdaptationUnlocked() {
        return damageAdaptationUnlocked;
    }

    public static int getDamageAdaptationUnlockPoints() {
        return damageAdaptationUnlockPoints;
    }

    public static int getHuntingInstinctUnlockPoints() {
        return huntingInstinctUnlockPoints;
    }

    public static int getStressEvolutionUnlockPoints() {
        return stressEvolutionUnlockPoints;
    }

    public static int getSuperPerceptionUnlockPoints() {
        return superPerceptionUnlockPoints;
    }

    public static int getBattleInstinctUnlockPoints() {
        return battleInstinctUnlockPoints;
    }

    public static int getEfficientMetabolismUnlockPoints() {
        return efficientMetabolismUnlockPoints;
    }

    public static int getNutritionFullnessUnlockPoints() {
        return nutritionFullnessUnlockPoints;
    }

    public static boolean isHuntingInstinctUnlocked() {
        return huntingInstinctUnlocked;
    }

    public static boolean isDamageAdaptationEnabled() {
        return damageAdaptationEnabled;
    }

    public static boolean isStressEvolutionUnlocked() {
        return stressEvolutionUnlocked;
    }

    public static boolean isHuntingInstinctEnabled() {
        return huntingInstinctEnabled;
    }

    public static boolean isHuntingInternalImpactEnabled() {
        return huntingInternalImpactEnabled;
    }

    public static boolean isAdaptationFeatureEnabled() {
        return adaptationFeatureEnabled;
    }

    public static boolean isHuntingInstinctFeatureEnabled() {
        return huntingInstinctFeatureEnabled;
    }

    public static boolean isHuntingInternalImpactFeatureEnabled() {
        return huntingInternalImpactFeatureEnabled;
    }

    public static boolean isStressEvolutionFeatureEnabled() {
        return stressEvolutionFeatureEnabled;
    }

    public static boolean isBattleInstinctFeatureEnabled() {
        return battleInstinctFeatureEnabled;
    }

    public static boolean isStressEvolutionEnabled() {
        return stressEvolutionEnabled;
    }

    public static boolean isSuperPerceptionUnlocked() {
        return superPerceptionUnlocked;
    }

    public static boolean isSuperPerceptionEnabled() {
        return superPerceptionEnabled;
    }

    public static boolean isBattleInstinctUnlocked() {
        return battleInstinctUnlocked;
    }

    public static boolean isBattleInstinctEnabled() {
        return battleInstinctEnabled;
    }

    public static boolean isBattleInstinctCounterEnabled() {
        return battleInstinctCounterEnabled;
    }

    public static int getBattleInstinctReserves() {
        return battleInstinctReserves;
    }

    public static int getBattleInstinctMaxReserves() {
        return battleInstinctMaxReserves;
    }

    public static long getHuntingInstinctCooldownUntil() {
        return huntingInstinctCooldownUntil;
    }

    public static long getHuntingInstinctActiveUntil() {
        return huntingInstinctActiveUntil;
    }

    public static long getStressEvolutionCooldownUntil() {
        return stressEvolutionCooldownUntil;
    }

    public static List<SyncGrowthDataPacket.AdaptationEntry> getAdaptationEntries() {
        return adaptationEntries;
    }
}
