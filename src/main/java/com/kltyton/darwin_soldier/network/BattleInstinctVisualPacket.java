package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.client.battle.BattleInstinctClientVisuals;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BattleInstinctVisualPacket(int playerId, Vec3 start, Vec3 end, int blockPoseTicks, int trailTicks) {
    public static void encode(BattleInstinctVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.playerId());
        writeVec3(buffer, packet.start());
        writeVec3(buffer, packet.end());
        buffer.writeVarInt(packet.blockPoseTicks());
        buffer.writeVarInt(packet.trailTicks());
    }

    public static BattleInstinctVisualPacket decode(FriendlyByteBuf buffer) {
        return new BattleInstinctVisualPacket(buffer.readVarInt(), readVec3(buffer), readVec3(buffer),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(BattleInstinctVisualPacket packet, Supplier<NetworkEvent.Context> context) {
        RuntimeDiagnostics.info("battle_visual_receive", "playerId=" + packet.playerId()
                + " start=" + packet.start() + " end=" + packet.end()
                + " blockPoseTicks=" + packet.blockPoseTicks() + " trailTicks=" + packet.trailTicks());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BattleInstinctClientVisuals.start(packet));
        context.get().setPacketHandled(true);
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
