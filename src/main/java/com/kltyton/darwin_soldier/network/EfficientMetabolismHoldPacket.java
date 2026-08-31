package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.NutritionAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EfficientMetabolismHoldPacket(boolean held) implements CustomPacketPayload {
    public static final Type<EfficientMetabolismHoldPacket> TYPE = ModNetwork.type("efficient_metabolism_hold");
    public static final StreamCodec<RegistryFriendlyByteBuf, EfficientMetabolismHoldPacket> STREAM_CODEC =
            ModNetwork.codec(EfficientMetabolismHoldPacket::encode, EfficientMetabolismHoldPacket::decode);
    public static void encode(EfficientMetabolismHoldPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.held);
    }

    public static EfficientMetabolismHoldPacket decode(FriendlyByteBuf buffer) {
        return new EfficientMetabolismHoldPacket(buffer.readBoolean());
    }

    public static void handle(EfficientMetabolismHoldPacket packet, IPayloadContext context) {
        NutritionAbilities.setMetabolismHeld((ServerPlayer) context.player(), packet.held);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
