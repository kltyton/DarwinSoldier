package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.AdaptationRecord;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleAdaptationPacket(String key, boolean enabled) {
    public static void encode(ToggleAdaptationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key);
        buffer.writeBoolean(packet.enabled);
    }

    public static ToggleAdaptationPacket decode(FriendlyByteBuf buffer) {
        return new ToggleAdaptationPacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(ToggleAdaptationPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        AdaptationRecord record = data.getAdaptationRecord(packet.key);
        if (record != null) {
            record.setEnabled(packet.enabled);
            savedData.setDirty();
        }
        ModNetwork.syncTo(player, data);
    }
}
