package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RequestGrowthDataPacket implements CustomPacketPayload {
    public static final RequestGrowthDataPacket INSTANCE = new RequestGrowthDataPacket();
    public static final Type<RequestGrowthDataPacket> TYPE = ModNetwork.type("request_growth_data");
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGrowthDataPacket> STREAM_CODEC =
            ModNetwork.codec(RequestGrowthDataPacket::encode, RequestGrowthDataPacket::decode);

    private RequestGrowthDataPacket() {
    }

    public static void encode(RequestGrowthDataPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestGrowthDataPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestGrowthDataPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        ModNetwork.syncTo(player, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
