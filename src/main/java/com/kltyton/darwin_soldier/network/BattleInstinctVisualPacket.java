package com.kltyton.darwin_soldier.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record BattleInstinctVisualPacket(int playerId, Vec3 start, Vec3 end, int blockPoseTicks, int trailTicks)
        implements CustomPacketPayload {
    public static final Type<BattleInstinctVisualPacket> TYPE = ModNetwork.type("battle_instinct_visual");
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleInstinctVisualPacket> STREAM_CODEC =
            ModNetwork.codec(BattleInstinctVisualPacket::encode, BattleInstinctVisualPacket::decode);
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
