package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.AdaptationRecord;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleAdaptationPacket(String key, boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleAdaptationPacket> TYPE = ModNetwork.type("toggle_adaptation");
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleAdaptationPacket> STREAM_CODEC =
            ModNetwork.codec(ToggleAdaptationPacket::encode, ToggleAdaptationPacket::decode);
    public static void encode(ToggleAdaptationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key);
        buffer.writeBoolean(packet.enabled);
    }

    public static ToggleAdaptationPacket decode(FriendlyByteBuf buffer) {
        return new ToggleAdaptationPacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(ToggleAdaptationPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        AdaptationRecord record = data.getAdaptationRecord(packet.key);
        if (record != null) {
            record.setEnabled(packet.enabled);
            savedData.setDirty();
        }
        ModNetwork.syncTo(player, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
