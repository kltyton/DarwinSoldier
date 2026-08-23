package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetBattleInstinctModePacket(boolean counterEnabled) {
    public static void encode(SetBattleInstinctModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.counterEnabled);
    }

    public static SetBattleInstinctModePacket decode(FriendlyByteBuf buffer) {
        return new SetBattleInstinctModePacket(buffer.readBoolean());
    }

    public static void handle(SetBattleInstinctModePacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (!data.isBattleInstinctUnlocked()) {
            RuntimeDiagnostics.warn("battle_instinct_mode_rejected", "player="
                    + player.getGameProfile().getName() + " reason=locked requestedCounter="
                    + packet.counterEnabled);
            ModNetwork.syncTo(player, data);
            return;
        }

        data.setBattleInstinctCounterEnabled(packet.counterEnabled);
        savedData.setDirty();
        RuntimeDiagnostics.info("battle_instinct_mode_applied", "player="
                + player.getGameProfile().getName() + " counterEnabled=" + packet.counterEnabled);
        ModNetwork.syncTo(player, data);
    }
}
