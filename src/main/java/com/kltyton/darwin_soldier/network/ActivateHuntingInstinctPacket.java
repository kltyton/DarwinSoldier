package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.HuntingInstinct;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ActivateHuntingInstinctPacket implements CustomPacketPayload {
    public static final ActivateHuntingInstinctPacket INSTANCE = new ActivateHuntingInstinctPacket();
    public static final Type<ActivateHuntingInstinctPacket> TYPE = ModNetwork.type("activate_hunting_instinct");
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateHuntingInstinctPacket> STREAM_CODEC =
            ModNetwork.codec(ActivateHuntingInstinctPacket::encode, ActivateHuntingInstinctPacket::decode);

    private ActivateHuntingInstinctPacket() {
    }

    public static void encode(ActivateHuntingInstinctPacket packet, FriendlyByteBuf buffer) {
    }

    public static ActivateHuntingInstinctPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ActivateHuntingInstinctPacket packet, IPayloadContext context) {
        HuntingInstinct.activate((ServerPlayer) context.player());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
