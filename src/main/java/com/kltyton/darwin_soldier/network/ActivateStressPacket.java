package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.GrowthAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ActivateStressPacket implements CustomPacketPayload {
    public static final ActivateStressPacket INSTANCE = new ActivateStressPacket();
    public static final Type<ActivateStressPacket> TYPE = ModNetwork.type("activate_stress_evolution");
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateStressPacket> STREAM_CODEC =
            ModNetwork.codec(ActivateStressPacket::encode, ActivateStressPacket::decode);

    private ActivateStressPacket() {
    }

    public static void encode(ActivateStressPacket packet, FriendlyByteBuf buffer) {
    }

    public static ActivateStressPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ActivateStressPacket packet, IPayloadContext context) {
        GrowthAbilities.activateStressEvolution((ServerPlayer) context.player());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
