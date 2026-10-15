package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.GrowthAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public final class ActivateStressPacket {
    public static final ActivateStressPacket INSTANCE = new ActivateStressPacket();

    private ActivateStressPacket() {
    }

    public static void encode(ActivateStressPacket packet, FriendlyByteBuf buffer) {
    }

    public static ActivateStressPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ActivateStressPacket packet, Supplier<ServerPlayer> context) {
        if (context.get() != null) {
            GrowthAbilities.activateStressEvolution(context.get());
        }
    }
}
