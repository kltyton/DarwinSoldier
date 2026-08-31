package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GrowthGainNotificationPacket(double amount, GrowthGainReason reason) implements CustomPacketPayload {
    public static final Type<GrowthGainNotificationPacket> TYPE = ModNetwork.type("growth_gain_notification");
    public static final StreamCodec<RegistryFriendlyByteBuf, GrowthGainNotificationPacket> STREAM_CODEC =
            ModNetwork.codec(GrowthGainNotificationPacket::encode, GrowthGainNotificationPacket::decode);
    public static void encode(GrowthGainNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.amount);
        buffer.writeEnum(packet.reason.type());
        buffer.writeDouble(packet.reason.multiplier());
    }

    public static GrowthGainNotificationPacket decode(FriendlyByteBuf buffer) {
        double amount = buffer.readDouble();
        GrowthGainReason.Type type = buffer.readEnum(GrowthGainReason.Type.class);
        return new GrowthGainNotificationPacket(amount, new GrowthGainReason(type, buffer.readDouble()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
