package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.HuntingInstinct;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public final class ActivateHuntingInstinctPacket {
    public static final ActivateHuntingInstinctPacket INSTANCE = new ActivateHuntingInstinctPacket();

    private ActivateHuntingInstinctPacket() {
    }

    public static void encode(ActivateHuntingInstinctPacket packet, FriendlyByteBuf buffer) {
    }

    public static ActivateHuntingInstinctPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ActivateHuntingInstinctPacket packet, Supplier<ServerPlayer> context) {
        ServerPlayer player = context.get();
        if (player != null) {
            HuntingInstinct.activate(player);
        }
    }
}
