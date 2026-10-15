package com.kltyton.darwin_soldier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;


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

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
