package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.GrowthAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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

    public static void handle(ActivateStressPacket packet, Supplier<NetworkEvent.Context> context) {
        if (context.get().getSender() != null) {
            GrowthAbilities.activateStressEvolution(context.get().getSender());
        }
    }
}
