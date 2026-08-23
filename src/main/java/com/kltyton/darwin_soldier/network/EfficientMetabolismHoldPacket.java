package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.ability.NutritionAbilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EfficientMetabolismHoldPacket(boolean held) {
    public static void encode(EfficientMetabolismHoldPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.held);
    }

    public static EfficientMetabolismHoldPacket decode(FriendlyByteBuf buffer) {
        return new EfficientMetabolismHoldPacket(buffer.readBoolean());
    }

    public static void handle(EfficientMetabolismHoldPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            NutritionAbilities.setMetabolismHeld(player, packet.held);
        }
    }
}
