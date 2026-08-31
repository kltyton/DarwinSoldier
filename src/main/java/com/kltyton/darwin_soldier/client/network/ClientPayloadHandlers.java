package com.kltyton.darwin_soldier.client.network;

import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.battle.BattleInstinctClientVisuals;
import com.kltyton.darwin_soldier.client.hud.growth.GrowthGainHud;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.network.BattleInstinctVisualPacket;
import com.kltyton.darwin_soldier.network.GrowthGainNotificationPacket;
import com.kltyton.darwin_soldier.network.SyncGrowthDataPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleSync(SyncGrowthDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientGrowthData.update(packet);
            RuntimeDiagnostics.infoRateLimited("sync-client", 1000L, "growth_sync_receive",
                    () -> "enabled=" + packet.enabled() + " totalPoints=" + packet.totalPoints()
                            + " current=" + packet.healthPoints() + "/" + packet.attackPoints() + "/"
                            + packet.defensePoints() + "/" + packet.perceptionPoints() + "/" + packet.nutrition().points()
                            + " unlocks=" + packet.damageAdaptationUnlocked() + "/" + packet.huntingInstinctUnlocked()
                            + "/" + packet.stressEvolutionUnlocked() + "/" + packet.superPerceptionUnlocked()
                            + "/" + packet.battleInstinctUnlocked() + "/"
                            + packet.nutrition().efficientMetabolismUnlocked() + "/"
                            + packet.nutrition().nutritionFullnessUnlocked()
                            + " features=" + packet.adaptationFeatureEnabled() + "/"
                            + packet.huntingInstinctFeatureEnabled() + "/" + packet.stressEvolutionFeatureEnabled()
                            + "/" + packet.battleInstinctFeatureEnabled() + "/" + packet.nutrition().featureEnabled()
                            + " reserves=" + packet.battleInstinctReserves() + "/" + packet.battleInstinctMaxReserves());
        });
    }

    public static void handleGrowthGain(GrowthGainNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> GrowthGainHud.accept(packet));
    }

    public static void handleBattleVisual(BattleInstinctVisualPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            RuntimeDiagnostics.info("battle_visual_receive", "playerId=" + packet.playerId()
                    + " start=" + packet.start() + " end=" + packet.end()
                    + " blockPoseTicks=" + packet.blockPoseTicks() + " trailTicks=" + packet.trailTicks());
            BattleInstinctClientVisuals.start(packet);
        });
    }
}
