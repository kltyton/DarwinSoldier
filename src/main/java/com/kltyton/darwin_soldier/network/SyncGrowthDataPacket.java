package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.NutritionAbilities;
import com.kltyton.darwin_soldier.compat.thirst.ThirstCompat;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.AdaptationRecord;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record SyncGrowthDataPacket(
        boolean enabled,
        double currentGrowth,
        double totalGrowth,
        int totalPoints,
        int healthPoints,
        int attackPoints,
        int defensePoints,
        int perceptionPoints,
        int cumulativeHealthPoints,
        int cumulativeAttackPoints,
        int cumulativeDefensePoints,
        int cumulativePerceptionPoints,
        NutritionEntry nutrition,
        AttributeSettings attributeSettings,
        boolean adaptationFeatureEnabled,
        boolean huntingInstinctFeatureEnabled,
        boolean huntingInternalImpactFeatureEnabled,
        boolean stressEvolutionFeatureEnabled,
        boolean battleInstinctFeatureEnabled,
        boolean damageAdaptationUnlocked,
        boolean huntingInstinctUnlocked,
        boolean stressEvolutionUnlocked,
        boolean superPerceptionUnlocked,
        boolean battleInstinctUnlocked,
        boolean damageAdaptationEnabled,
        boolean huntingInstinctEnabled,
        boolean huntingInternalImpactEnabled,
        boolean stressEvolutionEnabled,
        boolean superPerceptionEnabled,
        boolean battleInstinctEnabled,
        boolean battleInstinctCounterEnabled,
        int battleInstinctReserves,
        int battleInstinctMaxReserves,
        long huntingInstinctCooldownUntil,
        long huntingInstinctActiveUntil,
        long stressEvolutionCooldownUntil,
        List<AdaptationEntry> adaptationEntries
) implements CustomPacketPayload {
    public static final Type<SyncGrowthDataPacket> TYPE = ModNetwork.type("sync_growth_data");
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGrowthDataPacket> STREAM_CODEC =
            ModNetwork.codec(SyncGrowthDataPacket::encode, SyncGrowthDataPacket::decode);
    public static SyncGrowthDataPacket from(ServerPlayer player, PlayerGrowthData data) {
        return new SyncGrowthDataPacket(
                data.isEnabled(),
                data.getCurrentGrowth(),
                data.getTotalGrowth(),
                data.getTotalPoints(),
                data.getHealthPoints(),
                data.getAttackPoints(),
                data.getDefensePoints(),
                data.getPerceptionPoints(),
                data.getCumulativeHealthPoints(),
                data.getCumulativeAttackPoints(),
                data.getCumulativeDefensePoints(),
                data.getCumulativePerceptionPoints(),
                NutritionEntry.from(player, data),
                AttributeSettings.from(),
                DarwinConfig.ADAPTATION_ENABLED.get(),
                DarwinConfig.HUNT_ENABLED.get(),
                DarwinConfig.HUNTING_INTERNAL_IMPACT_ENABLED.get(),
                DarwinConfig.EVOLUTION_ENABLED.get(),
                DarwinConfig.BATTLE_INSTINCT_ENABLED.get(),
                data.isDamageAdaptationUnlocked(),
                data.isHuntingInstinctUnlocked(),
                data.isStressEvolutionUnlocked(),
                data.isSuperPerceptionUnlocked(),
                data.isBattleInstinctUnlocked(),
                data.isDamageAdaptationEnabled(),
                data.isHuntingInstinctEnabled(),
                data.isHuntingInternalImpactEnabled(),
                data.isStressEvolutionEnabled(),
                data.isSuperPerceptionEnabled(),
                data.isBattleInstinctEnabled(),
                data.isBattleInstinctCounterEnabled(),
                data.getBattleInstinctReserves(),
                DarwinConfig.BATTLE_INSTINCT_MAX_RESERVES.get(),
                data.getHuntingInstinctCooldownUntil(),
                data.getHuntingInstinctActiveUntil(),
                data.getStressEvolutionCooldownUntil(),
                data.getAdaptationRecords().stream().map(AdaptationEntry::from).toList()
        );
    }

    public static void encode(SyncGrowthDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
        buffer.writeDouble(packet.currentGrowth);
        buffer.writeDouble(packet.totalGrowth);
        buffer.writeVarInt(packet.totalPoints);
        buffer.writeVarInt(packet.healthPoints);
        buffer.writeVarInt(packet.attackPoints);
        buffer.writeVarInt(packet.defensePoints);
        buffer.writeVarInt(packet.perceptionPoints);
        buffer.writeVarInt(packet.cumulativeHealthPoints);
        buffer.writeVarInt(packet.cumulativeAttackPoints);
        buffer.writeVarInt(packet.cumulativeDefensePoints);
        buffer.writeVarInt(packet.cumulativePerceptionPoints);
        packet.nutrition.encode(buffer);
        packet.attributeSettings.encode(buffer);
        buffer.writeBoolean(packet.adaptationFeatureEnabled);
        buffer.writeBoolean(packet.huntingInstinctFeatureEnabled);
        buffer.writeBoolean(packet.huntingInternalImpactFeatureEnabled);
        buffer.writeBoolean(packet.stressEvolutionFeatureEnabled);
        buffer.writeBoolean(packet.battleInstinctFeatureEnabled);
        buffer.writeBoolean(packet.damageAdaptationUnlocked);
        buffer.writeBoolean(packet.huntingInstinctUnlocked);
        buffer.writeBoolean(packet.stressEvolutionUnlocked);
        buffer.writeBoolean(packet.superPerceptionUnlocked);
        buffer.writeBoolean(packet.battleInstinctUnlocked);
        buffer.writeBoolean(packet.damageAdaptationEnabled);
        buffer.writeBoolean(packet.huntingInstinctEnabled);
        buffer.writeBoolean(packet.huntingInternalImpactEnabled);
        buffer.writeBoolean(packet.stressEvolutionEnabled);
        buffer.writeBoolean(packet.superPerceptionEnabled);
        buffer.writeBoolean(packet.battleInstinctEnabled);
        buffer.writeBoolean(packet.battleInstinctCounterEnabled);
        buffer.writeVarInt(packet.battleInstinctReserves);
        buffer.writeVarInt(packet.battleInstinctMaxReserves);
        buffer.writeLong(packet.huntingInstinctCooldownUntil);
        buffer.writeLong(packet.huntingInstinctActiveUntil);
        buffer.writeLong(packet.stressEvolutionCooldownUntil);
        buffer.writeVarInt(packet.adaptationEntries.size());
        for (AdaptationEntry entry : packet.adaptationEntries) {
            entry.encode(buffer);
        }
    }

    public static SyncGrowthDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncGrowthDataPacket(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                NutritionEntry.decode(buffer),
                AttributeSettings.decode(buffer),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                decodeAdaptations(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static List<AdaptationEntry> decodeAdaptations(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        com.google.common.collect.ImmutableList.Builder<AdaptationEntry> entries = com.google.common.collect.ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            entries.add(AdaptationEntry.decode(buffer));
        }
        return entries.build();
    }

    public record AttributeSettings(
            double healthPerPoint,
            double attackPerPoint,
            double defensePerPoint,
            boolean derivedAttributesEnabled,
            int healingHealthPoints,
            double healingPerStep,
            int armorPierceAttackPoints,
            double armorPiercePerStep,
            int toughnessDefensePoints,
            double toughnessPerStep,
            boolean minimumDamageEnabled,
            double minimumDamageRatio,
            int damageAdaptationUnlockPoints,
            int huntingInstinctUnlockPoints,
            int stressEvolutionUnlockPoints,
            int superPerceptionUnlockPoints,
            int battleInstinctUnlockPoints,
            int efficientMetabolismUnlockPoints,
            int nutritionFullnessUnlockPoints
    ) {
        private static AttributeSettings from() {
            return new AttributeSettings(
                    DarwinConfig.HEALTH_PER_POINT.get(),
                    DarwinConfig.ATTACK_PER_POINT.get(),
                    DarwinConfig.DEFENSE_PER_POINT.get(),
                    DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.get(),
                    DarwinConfig.HEALING_AMPLIFICATION_HEALTH_POINTS.get(),
                    DarwinConfig.HEALING_AMPLIFICATION_PER_STEP.get(),
                    DarwinConfig.ARMOR_PIERCE_ATTACK_POINTS.get(),
                    DarwinConfig.ARMOR_PIERCE_PER_STEP.get(),
                    DarwinConfig.ARMOR_TOUGHNESS_DEFENSE_POINTS.get(),
                     DarwinConfig.ARMOR_TOUGHNESS_PER_STEP.get(),
                     DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_ENABLED.get(),
                     DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_RATIO.get(),
                     DarwinConfig.DAMAGE_ADAPTATION_UNLOCK_POINTS.get(),
                     DarwinConfig.HUNTING_INSTINCT_UNLOCK_POINTS.get(),
                     DarwinConfig.STRESS_EVOLUTION_UNLOCK_POINTS.get(),
                     DarwinConfig.SUPER_PERCEPTION_UNLOCK_POINTS.get(),
                     DarwinConfig.BATTLE_INSTINCT_UNLOCK_POINTS.get(),
                     DarwinConfig.EFFICIENT_METABOLISM_UNLOCK_POINTS.get(),
                     DarwinConfig.NUTRITION_FULLNESS_UNLOCK_POINTS.get());
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeDouble(healthPerPoint);
            buffer.writeDouble(attackPerPoint);
            buffer.writeDouble(defensePerPoint);
            buffer.writeBoolean(derivedAttributesEnabled);
            buffer.writeVarInt(healingHealthPoints);
            buffer.writeDouble(healingPerStep);
            buffer.writeVarInt(armorPierceAttackPoints);
            buffer.writeDouble(armorPiercePerStep);
            buffer.writeVarInt(toughnessDefensePoints);
            buffer.writeDouble(toughnessPerStep);
            buffer.writeBoolean(minimumDamageEnabled);
            buffer.writeDouble(minimumDamageRatio);
            buffer.writeVarInt(damageAdaptationUnlockPoints);
            buffer.writeVarInt(huntingInstinctUnlockPoints);
            buffer.writeVarInt(stressEvolutionUnlockPoints);
            buffer.writeVarInt(superPerceptionUnlockPoints);
            buffer.writeVarInt(battleInstinctUnlockPoints);
            buffer.writeVarInt(efficientMetabolismUnlockPoints);
            buffer.writeVarInt(nutritionFullnessUnlockPoints);
        }

        private static AttributeSettings decode(FriendlyByteBuf buffer) {
            return new AttributeSettings(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),
                    buffer.readVarInt(), buffer.readDouble(), buffer.readVarInt(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readDouble(), buffer.readBoolean(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
    }

    public record NutritionEntry(
            boolean featureEnabled,
            int points,
            int cumulativePoints,
            int maximumPoints,
            int maximumFood,
            double foodBonusPerPoint,
            double saturationBonusPerPoint,
            boolean efficientMetabolismUnlocked,
            boolean efficientMetabolismEnabled,
            boolean efficientMetabolismActive,
            boolean nutritionFullnessUnlocked,
            boolean nutritionFullnessEnabled,
            boolean nutritionFullnessActive,
            boolean thirstLinked,
            double efficientHealingPercent,
            int efficientFoodCost,
            double efficientThirstCost,
            double fullnessThreshold,
            int fullnessHasteLevel,
            float absorptionCap
    ) {
        private static NutritionEntry from(ServerPlayer player, PlayerGrowthData data) {
            ThirstCompat.ThirstSnapshot thirst = NutritionAbilities.thirstSnapshot(player, data);
            return new NutritionEntry(
                    DarwinConfig.NUTRITION_ENABLED.get(),
                    data.getNutritionPoints(),
                    data.getCumulativeNutritionPoints(),
                    DarwinConfig.NUTRITION_MAX_POINTS.get(),
                    com.kltyton.darwin_soldier.nutrition.NutritionFood.maximumFood(data.getNutritionPoints()),
                    DarwinConfig.NUTRITION_FOOD_RESTORE_BONUS_PER_POINT.get(),
                    DarwinConfig.NUTRITION_SATURATION_RESTORE_BONUS_PER_POINT.get(),
                    data.getNutrition().isEfficientMetabolismUnlocked(),
                    data.getNutrition().isEfficientMetabolismEnabled(),
                    data.getNutrition().isEfficientMetabolismActive(),
                    data.getNutrition().isNutritionFullnessUnlocked(),
                    data.getNutrition().isNutritionFullnessEnabled(),
                    data.getNutrition().isNutritionFullnessActive(),
                    thirst.linked(),
                    DarwinConfig.EFFICIENT_METABOLISM_HEAL_MAX_HEALTH_PERCENT.get(),
                    DarwinConfig.EFFICIENT_METABOLISM_FOOD_COST.get(),
                    DarwinConfig.EFFICIENT_METABOLISM_THIRST_COST.get(),
                    DarwinConfig.NUTRITION_FULLNESS_THRESHOLD.get(),
                    DarwinConfig.NUTRITION_FULLNESS_HASTE_LEVEL.get(),
                    NutritionAbilities.absorptionCap(player)
            );
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(featureEnabled);
            buffer.writeVarInt(points);
            buffer.writeVarInt(cumulativePoints);
            buffer.writeVarInt(maximumPoints);
            buffer.writeVarInt(maximumFood);
            buffer.writeDouble(foodBonusPerPoint);
            buffer.writeDouble(saturationBonusPerPoint);
            buffer.writeBoolean(efficientMetabolismUnlocked);
            buffer.writeBoolean(efficientMetabolismEnabled);
            buffer.writeBoolean(efficientMetabolismActive);
            buffer.writeBoolean(nutritionFullnessUnlocked);
            buffer.writeBoolean(nutritionFullnessEnabled);
            buffer.writeBoolean(nutritionFullnessActive);
            buffer.writeBoolean(thirstLinked);
            buffer.writeDouble(efficientHealingPercent);
            buffer.writeVarInt(efficientFoodCost);
            buffer.writeDouble(efficientThirstCost);
            buffer.writeDouble(fullnessThreshold);
            buffer.writeVarInt(fullnessHasteLevel);
            buffer.writeFloat(absorptionCap);
        }

        private static NutritionEntry decode(FriendlyByteBuf buffer) {
            return new NutritionEntry(
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readDouble(),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readVarInt(),
                    buffer.readFloat()
            );
        }
    }

    public record AdaptationEntry(String key, String displayName, String targetId, boolean playerTarget, int level, int reductionPercent, boolean enabled) {
        public static AdaptationEntry from(AdaptationRecord record) {
            return new AdaptationEntry(
                    record.getKey(),
                    record.getDisplayName(),
                    record.getTargetId(),
                    record.isPlayerTarget(),
                    record.getLevel(),
                    (int) Math.round(DarwinConfig.adaptationReductionForLevel(record.getLevel()) * 100.0D),
                    record.isEnabled()
            );
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUtf(key);
            buffer.writeUtf(displayName);
            buffer.writeUtf(targetId);
            buffer.writeBoolean(playerTarget);
            buffer.writeVarInt(level);
            buffer.writeVarInt(reductionPercent);
            buffer.writeBoolean(enabled);
        }

        private static AdaptationEntry decode(FriendlyByteBuf buffer) {
            return new AdaptationEntry(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean()
            );
        }
    }
}
