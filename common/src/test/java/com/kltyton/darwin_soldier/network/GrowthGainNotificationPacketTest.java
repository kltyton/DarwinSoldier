package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowthGainNotificationPacketTest {
    @Test
    void packetRoundTripPreservesFinalGainAndServerSelectedReason() {
        GrowthGainNotificationPacket original = new GrowthGainNotificationPacket(
                0.30000004D, GrowthGainReason.safeCombat(0.1D));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        GrowthGainNotificationPacket.encode(original, buffer);
        GrowthGainNotificationPacket decoded = GrowthGainNotificationPacket.decode(buffer);

        assertEquals(original.amount(), decoded.amount(), 0.0D);
        assertEquals(original.reason(), decoded.reason());
    }
}
