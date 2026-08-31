package com.kltyton.darwin_soldier.network;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.client.network.ClientPayloadHandlers;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "10";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        RuntimeDiagnostics.info("network_register_start", "protocol=" + PROTOCOL_VERSION);
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        IPayloadHandler<SyncGrowthDataPacket> syncHandler = FMLEnvironment.dist.isClient()
                ? ClientPayloadHandlers::handleSync : (payload, context) -> {};
        IPayloadHandler<GrowthGainNotificationPacket> notificationHandler = FMLEnvironment.dist.isClient()
                ? ClientPayloadHandlers::handleGrowthGain : (payload, context) -> {};
        IPayloadHandler<BattleInstinctVisualPacket> visualHandler = FMLEnvironment.dist.isClient()
                ? ClientPayloadHandlers::handleBattleVisual : (payload, context) -> {};

        registrar.playToClient(SyncGrowthDataPacket.TYPE, SyncGrowthDataPacket.STREAM_CODEC, syncHandler);
        registrar.playToClient(GrowthGainNotificationPacket.TYPE, GrowthGainNotificationPacket.STREAM_CODEC, notificationHandler);
        registrar.playToClient(BattleInstinctVisualPacket.TYPE, BattleInstinctVisualPacket.STREAM_CODEC, visualHandler);
        registrar.playToServer(AllocatePointsPacket.TYPE, AllocatePointsPacket.STREAM_CODEC, AllocatePointsPacket::handle);
        registrar.playToServer(RequestGrowthDataPacket.TYPE, RequestGrowthDataPacket.STREAM_CODEC, RequestGrowthDataPacket::handle);
        registrar.playToServer(ToggleAbilityPacket.TYPE, ToggleAbilityPacket.STREAM_CODEC, ToggleAbilityPacket::handle);
        registrar.playToServer(ToggleAdaptationPacket.TYPE, ToggleAdaptationPacket.STREAM_CODEC, ToggleAdaptationPacket::handle);
        registrar.playToServer(ActivateHuntingInstinctPacket.TYPE, ActivateHuntingInstinctPacket.STREAM_CODEC, ActivateHuntingInstinctPacket::handle);
        registrar.playToServer(ActivateStressPacket.TYPE, ActivateStressPacket.STREAM_CODEC, ActivateStressPacket::handle);
        registrar.playToServer(EfficientMetabolismHoldPacket.TYPE, EfficientMetabolismHoldPacket.STREAM_CODEC, EfficientMetabolismHoldPacket::handle);
        registrar.playToServer(SetBattleInstinctModePacket.TYPE, SetBattleInstinctModePacket.STREAM_CODEC, SetBattleInstinctModePacket::handle);
        RuntimeDiagnostics.info("network_register_complete", "protocol=" + PROTOCOL_VERSION + " packetCount=11");
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Darwin_soldier.MODID, path));
    }

    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
        return StreamCodec.of((buffer, payload) -> encoder.accept(payload, buffer), decoder::apply);
    }

    public static void syncTo(ServerPlayer player, PlayerGrowthData data) {
        RuntimeDiagnostics.infoRateLimited("sync-server-" + player.getUUID(), 1000L, "growth_sync_send",
                () -> "player=" + player.getGameProfile().getName() + " uuid=" + player.getUUID()
                        + " points=" + data.getHealthPoints() + "/" + data.getAttackPoints() + "/"
                        + data.getDefensePoints() + "/" + data.getPerceptionPoints() + "/" + data.getNutritionPoints()
                        + " reserves=" + data.getBattleInstinctReserves());
        PacketDistributor.sendToPlayer(player, SyncGrowthDataPacket.from(player, data));
    }

    public static void requestSyncFromServer() {
        PacketDistributor.sendToServer(RequestGrowthDataPacket.INSTANCE);
    }

    public static void sendGrowthGain(ServerPlayer player, double amount, GrowthGainReason reason) {
        PacketDistributor.sendToPlayer(player, new GrowthGainNotificationPacket(amount, reason));
    }

    public static void sendAllocation(int health, int attack, int defense, int perception, int nutrition) {
        PacketDistributor.sendToServer(new AllocatePointsPacket(health, attack, defense, perception, nutrition));
    }

    public static void sendAbilityToggle(com.kltyton.darwin_soldier.data.AbilityType ability, boolean enabled) {
        PacketDistributor.sendToServer(new ToggleAbilityPacket(ability, enabled));
    }

    public static void sendAdaptationToggle(String key, boolean enabled) {
        PacketDistributor.sendToServer(new ToggleAdaptationPacket(key, enabled));
    }

    public static void activateHuntingInstinct() {
        PacketDistributor.sendToServer(ActivateHuntingInstinctPacket.INSTANCE);
    }

    public static void activateStressEvolution() {
        PacketDistributor.sendToServer(ActivateStressPacket.INSTANCE);
    }

    public static void sendEfficientMetabolismHeld(boolean held) {
        PacketDistributor.sendToServer(new EfficientMetabolismHoldPacket(held));
    }

    public static void sendBattleInstinctMode(boolean counterEnabled) {
        PacketDistributor.sendToServer(new SetBattleInstinctModePacket(counterEnabled));
    }

    public static void sendBattleInstinctVisual(ServerPlayer player, Vec3 start, Vec3 end) {
        RuntimeDiagnostics.info("battle_visual_send", "player=" + player.getGameProfile().getName()
                + " entityId=" + player.getId() + " start=" + start + " end=" + end
                + " blockPoseTicks=" + DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get()
                + " trailTicks=" + DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new BattleInstinctVisualPacket(player.getId(), start, end,
                        DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get(),
                        DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get()));
    }

}
