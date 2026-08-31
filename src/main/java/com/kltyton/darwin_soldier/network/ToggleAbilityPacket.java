package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.HuntingInstinct;
import com.kltyton.darwin_soldier.data.AbilityType;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleAbilityPacket(AbilityType ability, boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleAbilityPacket> TYPE = ModNetwork.type("toggle_ability");
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleAbilityPacket> STREAM_CODEC =
            ModNetwork.codec(ToggleAbilityPacket::encode, ToggleAbilityPacket::decode);
    public static void encode(ToggleAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.ability);
        buffer.writeBoolean(packet.enabled);
    }

    public static ToggleAbilityPacket decode(FriendlyByteBuf buffer) {
        return new ToggleAbilityPacket(buffer.readEnum(AbilityType.class), buffer.readBoolean());
    }

    public static void handle(ToggleAbilityPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (!data.isAbilityUnlocked(packet.ability)) {
            RuntimeDiagnostics.warn("ability_toggle_rejected", "player=" + player.getGameProfile().getName()
                    + " uuid=" + player.getUUID() + " ability=" + packet.ability + " requested=" + packet.enabled
                    + " reason=locked points=" + data.getHealthPoints() + "/" + data.getAttackPoints() + "/"
                    + data.getDefensePoints() + "/" + data.getPerceptionPoints() + "/" + data.getNutritionPoints());
            ModNetwork.syncTo(player, data);
            return;
        }

        switch (packet.ability) {
            case DAMAGE_ADAPTATION -> data.setDamageAdaptationEnabled(packet.enabled);
            case HUNTING_INSTINCT -> {
                if (!packet.enabled) {
                    HuntingInstinct.stopWithCooldown(player, data);
                }
                data.setHuntingInstinctEnabled(packet.enabled);
            }
            case HUNTING_INTERNAL_IMPACT -> data.setHuntingInternalImpactEnabled(packet.enabled);
            case STRESS_EVOLUTION -> data.setStressEvolutionEnabled(packet.enabled);
            case SUPER_PERCEPTION -> data.setSuperPerceptionEnabled(packet.enabled);
            case BATTLE_INSTINCT -> data.setBattleInstinctEnabled(packet.enabled);
            case EFFICIENT_METABOLISM -> data.getNutrition().setEfficientMetabolismEnabled(packet.enabled);
            case NUTRITION_FULLNESS -> data.getNutrition().setNutritionFullnessEnabled(packet.enabled);
        }
        RuntimeDiagnostics.info("ability_toggle_applied", "player=" + player.getGameProfile().getName()
                + " uuid=" + player.getUUID() + " ability=" + packet.ability + " enabled=" + packet.enabled);
        savedData.setDirty();
        ModNetwork.syncTo(player, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
