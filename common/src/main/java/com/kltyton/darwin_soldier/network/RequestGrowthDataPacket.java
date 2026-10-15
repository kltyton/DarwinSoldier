package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public final class RequestGrowthDataPacket {
    public static final RequestGrowthDataPacket INSTANCE = new RequestGrowthDataPacket();

    private RequestGrowthDataPacket() {
    }

    public static void encode(RequestGrowthDataPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestGrowthDataPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestGrowthDataPacket packet, Supplier<ServerPlayer> context) {
        ServerPlayer player = context.get();
        if (player == null) {
            return;
        }

        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        ModNetwork.syncTo(player, data);
    }
}
