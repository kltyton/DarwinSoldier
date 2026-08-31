package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetBattleInstinctModePacket(boolean counterEnabled) implements CustomPacketPayload {
    public static final Type<SetBattleInstinctModePacket> TYPE = ModNetwork.type("set_battle_instinct_mode");
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBattleInstinctModePacket> STREAM_CODEC =
            ModNetwork.codec(SetBattleInstinctModePacket::encode, SetBattleInstinctModePacket::decode);
    public static void encode(SetBattleInstinctModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.counterEnabled);
    }

    public static SetBattleInstinctModePacket decode(FriendlyByteBuf buffer) {
        return new SetBattleInstinctModePacket(buffer.readBoolean());
    }

    public static void handle(SetBattleInstinctModePacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (!data.isBattleInstinctUnlocked()) {
            RuntimeDiagnostics.warn("battle_instinct_mode_rejected", "player="
                    + player.getGameProfile().getName() + " reason=locked requestedCounter="
                    + packet.counterEnabled);
            ModNetwork.syncTo(player, data);
            return;
        }

        data.setBattleInstinctCounterEnabled(packet.counterEnabled);
        savedData.setDirty();
        RuntimeDiagnostics.info("battle_instinct_mode_applied", "player="
                + player.getGameProfile().getName() + " counterEnabled=" + packet.counterEnabled);
        ModNetwork.syncTo(player, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
