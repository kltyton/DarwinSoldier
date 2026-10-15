package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.kltyton.darwin_soldier.client.network.ClientPayloadHandlers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "10";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Darwin_soldier.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        RuntimeDiagnostics.info("network_register_start", "protocol=" + PROTOCOL_VERSION + " firstPacketId=" + packetId);
        CHANNEL.messageBuilder(SyncGrowthDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGrowthDataPacket::encode)
                .decoder(SyncGrowthDataPacket::decode)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSync(packet)))
                .add();
        CHANNEL.messageBuilder(GrowthGainNotificationPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GrowthGainNotificationPacket::encode)
                .decoder(GrowthGainNotificationPacket::decode)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleGrowthGain(packet)))
                .add();
        CHANNEL.messageBuilder(AllocatePointsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AllocatePointsPacket::encode)
                .decoder(AllocatePointsPacket::decode)
                .consumerMainThread((packet, context) -> AllocatePointsPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(RequestGrowthDataPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestGrowthDataPacket::encode)
                .decoder(RequestGrowthDataPacket::decode)
                .consumerMainThread((packet, context) -> RequestGrowthDataPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(ToggleAbilityPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleAbilityPacket::encode)
                .decoder(ToggleAbilityPacket::decode)
                .consumerMainThread((packet, context) -> ToggleAbilityPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(ToggleAdaptationPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleAdaptationPacket::encode)
                .decoder(ToggleAdaptationPacket::decode)
                .consumerMainThread((packet, context) -> ToggleAdaptationPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(ActivateHuntingInstinctPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateHuntingInstinctPacket::encode)
                .decoder(ActivateHuntingInstinctPacket::decode)
                .consumerMainThread((packet, context) -> ActivateHuntingInstinctPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(ActivateStressPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateStressPacket::encode)
                .decoder(ActivateStressPacket::decode)
                .consumerMainThread((packet, context) -> ActivateStressPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(EfficientMetabolismHoldPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EfficientMetabolismHoldPacket::encode)
                .decoder(EfficientMetabolismHoldPacket::decode)
                .consumerMainThread((packet, context) -> EfficientMetabolismHoldPacket.handle(packet, context.get()::getSender))
                .add();
        CHANNEL.messageBuilder(BattleInstinctVisualPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BattleInstinctVisualPacket::encode)
                .decoder(BattleInstinctVisualPacket::decode)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleBattleVisual(packet)))
                .add();
        CHANNEL.messageBuilder(SetBattleInstinctModePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetBattleInstinctModePacket::encode)
                .decoder(SetBattleInstinctModePacket::decode)
                .consumerMainThread((packet, context) -> SetBattleInstinctModePacket.handle(packet, context.get()::getSender))
                .add();
        RuntimeDiagnostics.info("network_register_complete", "protocol=" + PROTOCOL_VERSION + " packetCount=" + packetId);
    }

    public static void syncTo(ServerPlayer player, PlayerGrowthData data) {
        RuntimeDiagnostics.infoRateLimited("sync-server-" + player.getUUID(), 1000L, "growth_sync_send",
                () -> "player=" + player.getGameProfile().getName() + " uuid=" + player.getUUID()
                        + " points=" + data.getHealthPoints() + "/" + data.getAttackPoints() + "/"
                        + data.getDefensePoints() + "/" + data.getPerceptionPoints() + "/" + data.getNutritionPoints()
                        + " reserves=" + data.getBattleInstinctReserves());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncGrowthDataPacket.from(player, data));
    }

    public static void requestSyncFromServer() {
        CHANNEL.sendToServer(RequestGrowthDataPacket.INSTANCE);
    }

    public static void sendGrowthGain(ServerPlayer player, double amount, GrowthGainReason reason) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new GrowthGainNotificationPacket(amount, reason));
    }

    public static void sendAllocation(int health, int attack, int defense, int perception, int nutrition) {
        CHANNEL.sendToServer(new AllocatePointsPacket(health, attack, defense, perception, nutrition));
    }

    public static void sendAbilityToggle(com.kltyton.darwin_soldier.data.AbilityType ability, boolean enabled) {
        CHANNEL.sendToServer(new ToggleAbilityPacket(ability, enabled));
    }

    public static void sendAdaptationToggle(String key, boolean enabled) {
        CHANNEL.sendToServer(new ToggleAdaptationPacket(key, enabled));
    }

    public static void activateHuntingInstinct() {
        CHANNEL.sendToServer(ActivateHuntingInstinctPacket.INSTANCE);
    }

    public static void activateStressEvolution() {
        CHANNEL.sendToServer(ActivateStressPacket.INSTANCE);
    }

    public static void sendEfficientMetabolismHeld(boolean held) {
        CHANNEL.sendToServer(new EfficientMetabolismHoldPacket(held));
    }

    public static void sendBattleInstinctMode(boolean counterEnabled) {
        CHANNEL.sendToServer(new SetBattleInstinctModePacket(counterEnabled));
    }

    public static void sendBattleInstinctVisual(ServerPlayer player, Vec3 start, Vec3 end) {
        RuntimeDiagnostics.info("battle_visual_send", "player=" + player.getGameProfile().getName()
                + " entityId=" + player.getId() + " start=" + start + " end=" + end
                + " blockPoseTicks=" + DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get()
                + " trailTicks=" + DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get());
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BattleInstinctVisualPacket(player.getId(), start, end,
                        DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get(),
                        DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get()));
    }

}
