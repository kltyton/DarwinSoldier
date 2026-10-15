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
import java.util.function.Consumer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "10";

    private static final Binding<ActivateHuntingInstinctPacket> ACTIVATE_HUNTING_INSTINCT = new Binding<>("activate_hunting_instinct", ActivateHuntingInstinctPacket::encode, ActivateHuntingInstinctPacket::decode);
    private static final Binding<ActivateStressPacket> ACTIVATE_STRESS = new Binding<>("activate_stress_evolution", ActivateStressPacket::encode, ActivateStressPacket::decode);
    private static final Binding<AllocatePointsPacket> ALLOCATE_POINTS = new Binding<>("allocate_points", AllocatePointsPacket::encode, AllocatePointsPacket::decode);
    private static final Binding<BattleInstinctVisualPacket> BATTLE_INSTINCT_VISUAL = new Binding<>("battle_instinct_visual", BattleInstinctVisualPacket::encode, BattleInstinctVisualPacket::decode);
    private static final Binding<EfficientMetabolismHoldPacket> EFFICIENT_METABOLISM_HOLD = new Binding<>("efficient_metabolism_hold", EfficientMetabolismHoldPacket::encode, EfficientMetabolismHoldPacket::decode);
    private static final Binding<GrowthGainNotificationPacket> GROWTH_GAIN_NOTIFICATION = new Binding<>("growth_gain_notification", GrowthGainNotificationPacket::encode, GrowthGainNotificationPacket::decode);
    private static final Binding<RequestGrowthDataPacket> REQUEST_GROWTH_DATA = new Binding<>("request_growth_data", RequestGrowthDataPacket::encode, RequestGrowthDataPacket::decode);
    private static final Binding<SetBattleInstinctModePacket> SET_BATTLE_INSTINCT_MODE = new Binding<>("set_battle_instinct_mode", SetBattleInstinctModePacket::encode, SetBattleInstinctModePacket::decode);
    private static final Binding<SyncGrowthDataPacket> SYNC_GROWTH_DATA = new Binding<>("sync_growth_data", SyncGrowthDataPacket::encode, SyncGrowthDataPacket::decode);
    private static final Binding<ToggleAbilityPacket> TOGGLE_ABILITY = new Binding<>("toggle_ability", ToggleAbilityPacket::encode, ToggleAbilityPacket::decode);
    private static final Binding<ToggleAdaptationPacket> TOGGLE_ADAPTATION = new Binding<>("toggle_adaptation", ToggleAdaptationPacket::encode, ToggleAdaptationPacket::decode);

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        RuntimeDiagnostics.info("network_register_start", "protocol=" + PROTOCOL_VERSION);
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        ACTIVATE_HUNTING_INSTINCT.registerServer(registrar, (packet, player) -> ActivateHuntingInstinctPacket.handle(packet, () -> player));
        ACTIVATE_STRESS.registerServer(registrar, (packet, player) -> ActivateStressPacket.handle(packet, () -> player));
        ALLOCATE_POINTS.registerServer(registrar, (packet, player) -> AllocatePointsPacket.handle(packet, () -> player));
        BATTLE_INSTINCT_VISUAL.registerClient(registrar, clientHandler(packet -> ClientPayloadHandlers.handleBattleVisual(packet)));
        EFFICIENT_METABOLISM_HOLD.registerServer(registrar, (packet, player) -> EfficientMetabolismHoldPacket.handle(packet, () -> player));
        GROWTH_GAIN_NOTIFICATION.registerClient(registrar, clientHandler(packet -> ClientPayloadHandlers.handleGrowthGain(packet)));
        REQUEST_GROWTH_DATA.registerServer(registrar, (packet, player) -> RequestGrowthDataPacket.handle(packet, () -> player));
        SET_BATTLE_INSTINCT_MODE.registerServer(registrar, (packet, player) -> SetBattleInstinctModePacket.handle(packet, () -> player));
        SYNC_GROWTH_DATA.registerClient(registrar, clientHandler(packet -> ClientPayloadHandlers.handleSync(packet)));
        TOGGLE_ABILITY.registerServer(registrar, (packet, player) -> ToggleAbilityPacket.handle(packet, () -> player));
        TOGGLE_ADAPTATION.registerServer(registrar, (packet, player) -> ToggleAdaptationPacket.handle(packet, () -> player));
        RuntimeDiagnostics.info("network_register_complete", "protocol=" + PROTOCOL_VERSION + " packetCount=11");
    }

    private static <T> IPayloadHandler<Payload<T>> clientHandler(Consumer<T> handler) {
        return (payload, context) -> {
            if (!FMLEnvironment.dist.isClient()) {
                throw new IllegalStateException("Client payload received on a dedicated server");
            }
            context.enqueueWork(() -> handler.accept(payload.message()));
        };
    }

    private record Payload<T>(CustomPacketPayload.Type<Payload<T>> type, T message) implements CustomPacketPayload {
    }

    private static final class Binding<T> {
        private final CustomPacketPayload.Type<Payload<T>> type;
        private final StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec;

        private Binding(String path, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
            type = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Darwin_soldier.MODID, path));
            codec = StreamCodec.of((buffer, payload) -> encoder.accept(payload.message(), buffer),
                    buffer -> new Payload<>(type, decoder.apply(buffer)));
        }

        private Payload<T> wrap(T message) {
            return new Payload<>(type, message);
        }

        private void registerClient(PayloadRegistrar registrar, IPayloadHandler<Payload<T>> handler) {
            registrar.playToClient(type, codec, handler);
        }

        private void registerServer(PayloadRegistrar registrar, BiConsumer<T, ServerPlayer> handler) {
            registrar.playToServer(type, codec,
                    (payload, context) -> handler.accept(payload.message(), (ServerPlayer) context.player()));
        }
    }

    public static void syncTo(ServerPlayer player, PlayerGrowthData data) {
        RuntimeDiagnostics.infoRateLimited("sync-server-" + player.getUUID(), 1000L, "growth_sync_send",
                () -> "player=" + player.getGameProfile().getName() + " uuid=" + player.getUUID()
                        + " points=" + data.getHealthPoints() + "/" + data.getAttackPoints() + "/"
                        + data.getDefensePoints() + "/" + data.getPerceptionPoints() + "/" + data.getNutritionPoints()
                        + " reserves=" + data.getBattleInstinctReserves());
        PacketDistributor.sendToPlayer(player, SYNC_GROWTH_DATA.wrap(SyncGrowthDataPacket.from(player, data)));
    }

    public static void requestSyncFromServer() {
        PacketDistributor.sendToServer(REQUEST_GROWTH_DATA.wrap(RequestGrowthDataPacket.INSTANCE));
    }

    public static void sendGrowthGain(ServerPlayer player, double amount, GrowthGainReason reason) {
        PacketDistributor.sendToPlayer(player, GROWTH_GAIN_NOTIFICATION.wrap(new GrowthGainNotificationPacket(amount, reason)));
    }

    public static void sendAllocation(int health, int attack, int defense, int perception, int nutrition) {
        PacketDistributor.sendToServer(ALLOCATE_POINTS.wrap(new AllocatePointsPacket(health, attack, defense, perception, nutrition)));
    }

    public static void sendAbilityToggle(com.kltyton.darwin_soldier.data.AbilityType ability, boolean enabled) {
        PacketDistributor.sendToServer(TOGGLE_ABILITY.wrap(new ToggleAbilityPacket(ability, enabled)));
    }

    public static void sendAdaptationToggle(String key, boolean enabled) {
        PacketDistributor.sendToServer(TOGGLE_ADAPTATION.wrap(new ToggleAdaptationPacket(key, enabled)));
    }

    public static void activateHuntingInstinct() {
        PacketDistributor.sendToServer(ACTIVATE_HUNTING_INSTINCT.wrap(ActivateHuntingInstinctPacket.INSTANCE));
    }

    public static void activateStressEvolution() {
        PacketDistributor.sendToServer(ACTIVATE_STRESS.wrap(ActivateStressPacket.INSTANCE));
    }

    public static void sendEfficientMetabolismHeld(boolean held) {
        PacketDistributor.sendToServer(EFFICIENT_METABOLISM_HOLD.wrap(new EfficientMetabolismHoldPacket(held)));
    }

    public static void sendBattleInstinctMode(boolean counterEnabled) {
        PacketDistributor.sendToServer(SET_BATTLE_INSTINCT_MODE.wrap(new SetBattleInstinctModePacket(counterEnabled)));
    }

    public static void sendBattleInstinctVisual(ServerPlayer player, Vec3 start, Vec3 end) {
        RuntimeDiagnostics.info("battle_visual_send", "player=" + player.getGameProfile().getName()
                + " entityId=" + player.getId() + " start=" + start + " end=" + end
                + " blockPoseTicks=" + DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get()
                + " trailTicks=" + DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                BATTLE_INSTINCT_VISUAL.wrap(new BattleInstinctVisualPacket(player.getId(), start, end,
                        DarwinConfig.BATTLE_INSTINCT_BLOCK_POSE_TICKS.get(),
                        DarwinConfig.BATTLE_INSTINCT_TRAIL_TICKS.get())));
    }

}
