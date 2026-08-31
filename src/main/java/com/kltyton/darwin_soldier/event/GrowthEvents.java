package com.kltyton.darwin_soldier.event;

import com.kltyton.darwin_soldier.ability.GrowthAbilities;
import com.kltyton.darwin_soldier.ability.HuntingInstinct;
import com.kltyton.darwin_soldier.ability.NutritionAbilities;
import com.kltyton.darwin_soldier.combat.DarwinDamageTypes;
import com.kltyton.darwin_soldier.combat.DefenseReductionMath;
import com.kltyton.darwin_soldier.combat.DefenseReductionTracker;
import com.kltyton.darwin_soldier.combat.InternalInjuryTracker;
import com.kltyton.darwin_soldier.combat.MinimumEffectiveDamageTracker;
import com.kltyton.darwin_soldier.command.DarwinCommand;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.CombatParticipationTracker;
import com.kltyton.darwin_soldier.data.GrowthAttributes;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import com.kltyton.darwin_soldier.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.UUID;

public final class GrowthEvents {
    private static final long ENTITY_DAMAGE_WINDOW_TICKS = 30L * 20L;

    private GrowthEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        markNutritionCombat(event.getEntity(), event.getSource());

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        boolean invulnerable = GrowthAbilities.isBattleInstinctInvulnerable(player, data);
        if (!invulnerable && DarwinDamageTypes.isInternal(event.getSource())) {
            return;
        }
        if (invulnerable
                || GrowthAbilities.tryTriggerBattleInstinct(player, event.getSource(), event.getAmount(), data)) {
            GrowthAbilities.clearBattleInstinctHitFeedback(player);
            event.setCanceled(true);
            savedData.setDirty();
            ModNetwork.syncTo(player, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        markNutritionCombat(event.getEntity(), event.getSource());

        if (event.getEntity() instanceof ServerPlayer player) {
            GrowthSavedData savedData = GrowthSavedData.get(player);
            PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
            if (GrowthAbilities.isBattleInstinctInvulnerable(player, data)) {
                GrowthAbilities.clearBattleInstinctHitFeedback(player);
                event.setAmount(0.0F);
                event.setCanceled(true);
                return;
            }
            if (DarwinDamageTypes.isInternal(event.getSource())) {
                return;
            }

            DamageSource source = event.getSource();
            if (GrowthAbilities.tryTriggerBattleInstinct(player, source, event.getAmount(), data)) {
                GrowthAbilities.clearBattleInstinctHitFeedback(player);
                event.setAmount(0.0F);
                event.setCanceled(true);
                savedData.setDirty();
                ModNetwork.syncTo(player, data);
                return;
            }

            Entity causingEntity = source.getEntity();
            Entity directEntity = source.getDirectEntity();
            if (causingEntity != null || directEntity != null) {
                data.setLastEntityDamageGameTime(player.serverLevel().getGameTime());
                event.setAmount(GrowthAbilities.applyAdaptationReduction(player, source, event.getAmount(), data));
                savedData.setDirty();
            }
        }

        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer victim
                && !DarwinDamageTypes.isInternal(event.getSource()) && event.getAmount() > 0.0F) {
            PlayerGrowthData victimData = GrowthSavedData.get(victim).getOrCreate(victim.getUUID());
            if (victimData.isEnabled() && victimData.getDefensePoints() > 0) {
                DefenseReductionTracker.capture(victim, event.getSource(), event.getAmount());
            }
        }

        if (DarwinDamageTypes.isInternal(event.getSource())) {
            return;
        }

        if (event.getAmount() > 0.0F && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            GrowthAbilities.applyArmorPiercing(attacker, event.getEntity(), event.getSource());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        if (GrowthAbilities.isBattleInstinctInvulnerable(player, data)) {
            event.setCanceled(true);
            GrowthAbilities.clearBattleInstinctHitFeedback(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        event.setAmount(GrowthAbilities.applyHealingAmplification(player, data, event.getAmount()));
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        GrowthAbilities.clearArmorPiercingModifier(event.getEntity());

        if (!DarwinDamageTypes.isInternal(event.getSource())
                && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            GrowthSavedData savedData = GrowthSavedData.get(attacker);
            PlayerGrowthData data = savedData.getOrCreate(attacker.getUUID());
            long now = attacker.serverLevel().getGameTime();
            if (!data.isBattleInstinctCounterActiveFor(event.getEntity().getId(), now)) {
                event.setNewDamage(GrowthAbilities.applyCriticalDamage(attacker, event.getNewDamage()));
            }
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }
        if (DarwinDamageTypes.isInternal(event.getSource())) {
            return;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (GrowthAbilities.reduceStressCooldownFromDamage(player, data, event.getSource())) {
            savedData.setDirty();
            ModNetwork.syncTo(player, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDamageDefenseReduction(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || event.getNewDamage() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION) || DarwinDamageTypes.isInternal(source)) {
            return;
        }
        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        if (!data.isEnabled()) {
            return;
        }
        event.setNewDamage(DefenseReductionTracker.applyDefenseReduction(player, source, event.getNewDamage(),
                DefenseReductionMath.minimumReduction(data.getDefensePoints())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalOutgoingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION)) {
            MinimumEffectiveDamageTracker.forceCorrectionFinalDamage(event);
            if (event.getNewDamage() > 0.0F) {
                CombatParticipationTracker.recordDamage(event.getEntity(), source);
            }
            return;
        }
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (source.is(DarwinDamageTypes.INTERNAL_INJURY)) {
            CombatParticipationTracker.recordDamage(event.getEntity(), source);
            return;
        }
        if (source.is(DarwinDamageTypes.HUNTING_SHOCK)) {
            event.setNewDamage(InternalInjuryTracker.applyVulnerability(event.getEntity(), source, event.getNewDamage()));
            CombatParticipationTracker.recordDamage(event.getEntity(), source);
            return;
        }

        ServerPlayer attacker = MinimumEffectiveDamageTracker.resolvePlayer(source);
        float amount = event.getNewDamage();
        if (attacker != null) {
            boolean shieldBlocked = MinimumEffectiveDamageTracker.consumeShieldBlock(event.getEntity(), source);
            PlayerGrowthData data = GrowthSavedData.get(attacker).getOrCreate(attacker.getUUID());
            long now = attacker.serverLevel().getGameTime();
            boolean battleCounter = data.isBattleInstinctCounterActiveFor(event.getEntity().getId(), now);
            if (battleCounter) {
                data.endBattleInstinctCounter();
            } else {
                amount = InternalInjuryTracker.applyVulnerability(event.getEntity(), source, amount);
                if (!shieldBlocked && MinimumEffectiveDamageTracker.isRealPlayerMelee(source, attacker)) {
                    amount = MinimumEffectiveDamageTracker.applyMeleeMinimum(attacker, amount);
                    amount = HuntingInstinct.applyMeleeHit(attacker, event.getEntity(), amount).damage();
                } else if (!shieldBlocked) {
                    MinimumEffectiveDamageTracker.recordNonMelee(attacker, event.getEntity(), source, amount);
                }
                event.setNewDamage(amount);
            }
        }
        CombatParticipationTracker.recordDamage(event.getEntity(), source);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getEntity().level().isClientSide && event.getBlocked() && event.getBlockedDamage() > 0.0F) {
            MinimumEffectiveDamageTracker.markShieldBlock(event.getEntity(), event.getDamageSource());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        GrowthAbilities.clearArmorPiercingModifier(dead);
        MinimumEffectiveDamageTracker.clearTarget(dead.getUUID());
        InternalInjuryTracker.clearTarget(dead.getUUID());

        if (dead instanceof ServerPlayer deadPlayer) {
            CombatParticipationTracker.consume(deadPlayer);
            GrowthSavedData savedData = GrowthSavedData.get(deadPlayer);
            PlayerGrowthData data = savedData.getOrCreate(deadPlayer.getUUID());
            HuntingInstinct.clearRuntime(deadPlayer, data, true);
            MinimumEffectiveDamageTracker.clearAttacker(deadPlayer.getUUID());
            DefenseReductionTracker.clear(deadPlayer.getUUID());
            InternalInjuryTracker.clearAttacker(deadPlayer.getUUID());
            NutritionAbilities.clearRuntime(data);
            GrowthAbilities.tryRecordAdaptationDeath(deadPlayer, event.getSource(), data);
            savedData.setDirty();
            ModNetwork.syncTo(deadPlayer, data);
            return;
        }
        if (dead instanceof Player) {
            CombatParticipationTracker.consume(dead);
            return;
        }

        CombatParticipationTracker.recordFinalSource(dead, event.getSource());
        MinecraftServer server = ((ServerLevel) dead.level()).getServer();
        for (CombatParticipationTracker.Participation participation : CombatParticipationTracker.consume(dead)) {
            ServerPlayer participant = server.getPlayerList().getPlayer(participation.playerId());
            awardGrowth(server, participant, participation.playerId(), dead,
                    participation.direct());
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity living) {
            CombatParticipationTracker.clear(living);
            MinimumEffectiveDamageTracker.clearTarget(living.getUUID());
            DefenseReductionTracker.clear(living.getUUID());
            InternalInjuryTracker.clearTarget(living.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeDiagnostics.info("player_login", "player=" + player.getGameProfile().getName()
                    + " uuid=" + player.getUUID() + " dimension=" + player.level().dimension().location());
            syncAndApply(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAndApply(player, event.isWasDeath());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeDiagnostics.info("player_dimension_change", "player=" + player.getGameProfile().getName()
                    + " uuid=" + player.getUUID() + " from=" + event.getFrom().location()
                    + " to=" + event.getTo().location());
            clearAbilityRuntime(player);
            syncAndApply(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAbilityRuntime(player);
            syncAndApply(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeDiagnostics.info("player_logout", "player=" + player.getGameProfile().getName()
                    + " uuid=" + player.getUUID() + " dimension=" + player.level().dimension().location());
            clearAbilityRuntime(player);
            MinimumEffectiveDamageTracker.clearAttacker(player.getUUID());
            DefenseReductionTracker.clear(player.getUUID());
            InternalInjuryTracker.clearAttacker(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (player.tickCount % 20 == 0) {
            GrowthAttributes.reconcileProtectedHealth(player, data);
        }
        if (GrowthAbilities.isBattleInstinctInvulnerable(player, data)) {
            GrowthAbilities.clearBattleInstinctHitFeedback(player);
        }
        if (GrowthAbilities.tickBattleInstinctReserve(player, data)) {
            savedData.setDirty();
            ModNetwork.syncTo(player, data);
        }
        if (HuntingInstinct.tick(player, data)) {
            savedData.setDirty();
            ModNetwork.syncTo(player, data);
        }
        NutritionAbilities.TickResult nutritionTick = NutritionAbilities.tick(player, data);
        if (nutritionTick.dataChanged()) {
            savedData.setDirty();
        }
        if (nutritionTick.syncRequired()) {
            ModNetwork.syncTo(player, data);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinimumEffectiveDamageTracker.tick(event.getServer());
        InternalInjuryTracker.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onSlownessApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() != net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        if (HuntingInstinct.isSlownessImmune(player, data)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinimumEffectiveDamageTracker.clearAll();
        DefenseReductionTracker.clearAll();
        InternalInjuryTracker.clearAll();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DarwinCommand.register(event.getDispatcher());
    }

    private static void syncAndApply(ServerPlayer player, boolean fillHealth) {
        PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        RuntimeDiagnostics.info("player_state_apply", "player=" + player.getGameProfile().getName()
                + " uuid=" + player.getUUID() + " fillHealth=" + fillHealth + " enabled=" + data.isEnabled()
                + " totalPoints=" + data.getTotalPoints() + " points=" + data.getHealthPoints() + "/"
                + data.getAttackPoints() + "/" + data.getDefensePoints() + "/" + data.getPerceptionPoints()
                + "/" + data.getNutritionPoints() + " unlocks=" + data.isDamageAdaptationUnlocked() + "/"
                + data.isHuntingInstinctUnlocked() + "/" + data.isStressEvolutionUnlocked() + "/"
                + data.isSuperPerceptionUnlocked() + "/" + data.isBattleInstinctUnlocked()
                + " reserves=" + data.getBattleInstinctReserves());
        GrowthAttributes.apply(player, data);
        if (fillHealth && data.isEnabled()) {
            GrowthAttributes.fillHealthToMax(player);
        }
        NutritionAbilities.TickResult nutritionTick = NutritionAbilities.tick(player, data);
        if (nutritionTick.dataChanged()) {
            GrowthSavedData.get(player).setDirty();
        }
        ModNetwork.syncTo(player, data);
    }

    private static void clearAbilityRuntime(ServerPlayer player) {
        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        if (HuntingInstinct.clearRuntime(player, data, true)) {
            savedData.setDirty();
        }
        MinimumEffectiveDamageTracker.clearAttacker(player.getUUID());
        InternalInjuryTracker.clearAttacker(player.getUUID());
        NutritionAbilities.clearRuntime(data);
    }

    private static void markNutritionCombat(LivingEntity target, DamageSource source) {
        if (DarwinDamageTypes.isInternal(source) || source.getEntity() == null && source.getDirectEntity() == null) {
            return;
        }
        if (target instanceof ServerPlayer victim) {
            NutritionAbilities.markCombat(victim);
        }
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target) {
            NutritionAbilities.markCombat(attacker);
        }
    }

    private static void awardGrowth(MinecraftServer server, ServerPlayer player, UUID playerId,
                                    LivingEntity dead, boolean directParticipation) {
        GrowthSavedData savedData = GrowthSavedData.get(server);
        PlayerGrowthData data = savedData.getOrCreate(playerId);
        if (!data.isEnabled()) {
            return;
        }

        double participationMultiplier = directParticipation ? 1.0D : DarwinConfig.PET_GROWTH_MULTIPLIER.get();
        GrowthGainReason reason = directParticipation
                ? GrowthGainReason.normal()
                : GrowthGainReason.petKill(participationMultiplier);
        double gain = classifyGrowth(dead) * participationMultiplier;
        long now = server.overworld().getGameTime();
        if (now - data.getLastEntityDamageGameTime() > ENTITY_DAMAGE_WINDOW_TICKS) {
            double multiplier = DarwinConfig.ANTI_AFK_MULTIPLIER.get();
            gain *= multiplier;
            reason = GrowthGainReason.safeCombat(multiplier);
        }
        if (player != null && player.getHealth() <= player.getMaxHealth() * 0.2F) {
            double multiplier = DarwinConfig.LOW_HEALTH_MULTIPLIER.get();
            gain *= multiplier;
            reason = GrowthGainReason.lowHealth(multiplier);
        }
        data.addGrowth(gain);
        savedData.setDirty();
        if (player != null) {
            ModNetwork.syncTo(player, data);
            if (gain > 0.0D) {
                ModNetwork.sendGrowthGain(player, gain, reason);
            }
        }
    }

    private static double classifyGrowth(LivingEntity entity) {
        if (isBoss(entity)) {
            return DarwinConfig.BOSS_GROWTH.get();
        }
        if (entity instanceof NeutralMob) {
            return DarwinConfig.NEUTRAL_GROWTH.get();
        }
        if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER) {
            return DarwinConfig.HOSTILE_GROWTH.get();
        }
        return DarwinConfig.PEACEFUL_GROWTH.get();
    }

    private static boolean isBoss(LivingEntity entity) {
        return GrowthAbilities.isBossOrHighHealth(entity);
    }
}
