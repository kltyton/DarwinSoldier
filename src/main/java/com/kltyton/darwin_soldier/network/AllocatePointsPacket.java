package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.GrowthAttributes;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AllocatePointsPacket(int healthPoints, int attackPoints, int defensePoints, int perceptionPoints,
                                   int nutritionPoints) {
    public static void encode(AllocatePointsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.healthPoints);
        buffer.writeVarInt(packet.attackPoints);
        buffer.writeVarInt(packet.defensePoints);
        buffer.writeVarInt(packet.perceptionPoints);
        buffer.writeVarInt(packet.nutritionPoints);
    }

    public static AllocatePointsPacket decode(FriendlyByteBuf buffer) {
        return new AllocatePointsPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(AllocatePointsPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (!data.isEnabled()) {
            RuntimeDiagnostics.warn("allocation_rejected", "player=" + player.getGameProfile().getName()
                    + " uuid=" + player.getUUID() + " reason=growth_disabled");
            ModNetwork.syncTo(player, data);
            return;
        }

        String before = data.getHealthPoints() + "/" + data.getAttackPoints() + "/" + data.getDefensePoints()
                + "/" + data.getPerceptionPoints() + "/" + data.getNutritionPoints();
        boolean accepted = data.allocate(packet.healthPoints, packet.attackPoints, packet.defensePoints,
                packet.perceptionPoints, packet.nutritionPoints);
        RuntimeDiagnostics.info(accepted ? "allocation_applied" : "allocation_rejected",
                "player=" + player.getGameProfile().getName() + " uuid=" + player.getUUID()
                        + " before=" + before + " requested=" + packet.healthPoints + "/" + packet.attackPoints
                        + "/" + packet.defensePoints + "/" + packet.perceptionPoints + "/" + packet.nutritionPoints
                        + " totalPoints=" + data.getTotalPoints() + " accepted=" + accepted);
        if (accepted) {
            savedData.setDirty();
            GrowthAttributes.apply(player, data);
            com.kltyton.darwin_soldier.nutrition.NutritionFood.clampToMaximum(player, data);
        }
        ModNetwork.syncTo(player, data);
    }
}
