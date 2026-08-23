package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.client.hud.growth.GrowthGainHud;
import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GrowthGainNotificationPacket(double amount, GrowthGainReason reason) {
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

    public static void handle(GrowthGainNotificationPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GrowthGainHud.accept(packet));
        context.get().setPacketHandled(true);
    }
}
