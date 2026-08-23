package com.kltyton.darwin_soldier.ability;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.AdaptationRecord;
import com.kltyton.darwin_soldier.data.GrowthAttributes;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class GrowthAbilities {
    private static final long BATTLE_INSTINCT_COUNTER_WINDOW_TICKS = 2L;
    private static final UUID ARMOR_PIERCE_MODIFIER_ID = UUID.fromString("313ca6b2-0996-45a7-a38f-a935be286c55");
    private static final DustParticleOptions STRESS_SHOCKWAVE_PARTICLE = new DustParticleOptions(new Vector3f(0.95F, 0.95F, 0.95F), 1.25F);
    private static final int[][] BATTLE_INSTINCT_DODGE_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {0, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    private GrowthAbilities() {
    }

    public static boolean tickBattleInstinctReserve(ServerPlayer player, PlayerGrowthData data) {
        int maxReserves = Math.max(0, DarwinConfig.BATTLE_INSTINCT_MAX_RESERVES.get());
        int currentReserves = data.getBattleInstinctReserves();
        boolean dirty = false;
        if (currentReserves > maxReserves) {
            data.setBattleInstinctReserves(maxReserves);
            currentReserves = maxReserves;
            dirty = true;
        }

        if (!data.isEnabled() || !data.isBattleInstinctUnlocked() || maxReserves <= 0) {
            return dirty;
        }
        if (currentReserves >= maxReserves) {
            return dirty;
        }

        long now = player.serverLevel().getGameTime();
        long lastRecover = data.getBattleInstinctLastReserveGameTime();
        if (lastRecover <= 0L) {
            data.setBattleInstinctLastReserveGameTime(now);
            return true;
        }

        long interval = battleInstinctRecoverIntervalTicks(player);
        if (now - lastRecover < interval) {
            return dirty;
        }

        int recovered = (int) ((now - lastRecover) / interval);
        data.setBattleInstinctReserves(Math.min(maxReserves, currentReserves + Math.max(1, recovered)));
        data.setBattleInstinctLastReserveGameTime(now);
        return true;
    }

    public static boolean isBattleInstinctInvulnerable(ServerPlayer player, PlayerGrowthData data) {
        return data.getBattleInstinctInvulnerableUntil() > player.serverLevel().getGameTime();
    }

    public static boolean tryTriggerBattleInstinct(ServerPlayer player, DamageSource source, float amount, PlayerGrowthData data) {
        if (amount <= 0.0F) {
            RuntimeDiagnostics.infoRateLimited("battle-reject-amount-" + player.getUUID(), 2000L,
                    "battle_instinct_rejected", () -> "player=" + player.getGameProfile().getName()
                            + " reason=non_positive_damage amount=" + amount + " source=" + source.getMsgId());
            return false;
        }
        if (!canUseBattleInstinct(data)) {
            RuntimeDiagnostics.infoRateLimited("battle-reject-state-" + player.getUUID(), 2000L,
                    "battle_instinct_rejected", () -> "player=" + player.getGameProfile().getName()
                            + " reason=state enabled=" + data.isEnabled() + " feature="
                            + DarwinConfig.BATTLE_INSTINCT_ENABLED.get() + " unlocked=" + data.isBattleInstinctUnlocked()
                            + " abilityEnabled=" + data.isBattleInstinctEnabled()
                            + " reserves=" + data.getBattleInstinctReserves() + " source=" + source.getMsgId());
            return false;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (data.getBattleInstinctInvulnerableUntil() > now) {
            RuntimeDiagnostics.infoRateLimited("battle-invulnerable-" + player.getUUID(), 500L,
                    "battle_instinct_invulnerable_cancel", () -> "player=" + player.getGameProfile().getName()
                            + " now=" + now + " until=" + data.getBattleInstinctInvulnerableUntil()
                            + " amount=" + amount + " source=" + source.getMsgId());
            return true;
        }
        LivingEntity attacker = resolveBattleInstinctAttacker(player, source);
        if (attacker == null) {
            RuntimeDiagnostics.warn("battle_instinct_rejected", "player=" + player.getGameProfile().getName()
                    + " reason=no_living_attacker amount=" + amount + " source=" + source.getMsgId()
                    + " direct=" + describeEntity(source.getDirectEntity()) + " owner=" + describeEntity(source.getEntity()));
            return false;
        }
        int reservesBefore = data.getBattleInstinctReserves();
        if (!data.consumeBattleInstinctReserve(now)) {
            RuntimeDiagnostics.warn("battle_instinct_rejected", "player=" + player.getGameProfile().getName()
                    + " reason=reserve_consume_failed reservesBefore=" + reservesBefore);
            return false;
        }

        data.setBattleInstinctInvulnerableUntil(now + DarwinConfig.BATTLE_INSTINCT_INVULNERABILITY_TICKS.get());
        Vec3 start = player.position();
        spawnBattleInstinctSmoke(player);
        clearBattleInstinctHitFeedback(player);

        Vec3 landing = findBattleInstinctLanding(player, attacker);
        if (landing != null) {
            player.teleportTo(level, landing.x, landing.y, landing.z, player.getYRot(), player.getXRot());
        }
        RuntimeDiagnostics.info("battle_instinct_trigger", "player=" + player.getGameProfile().getName()
                + " uuid=" + player.getUUID() + " source=" + source.getMsgId() + " amount=" + amount
                + " attacker=" + describeEntity(attacker) + " reserves=" + reservesBefore + "->"
                + data.getBattleInstinctReserves() + " start=" + start + " landing="
                + (landing == null ? "none" : landing.toString()) + " end=" + player.position()
                + " invulnerableUntil=" + data.getBattleInstinctInvulnerableUntil());

        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                DarwinConfig.BATTLE_INSTINCT_BLOCK_SOUND_VOLUME.get().floatValue(),
                DarwinConfig.BATTLE_INSTINCT_BLOCK_SOUND_PITCH.get().floatValue());
        ModNetwork.sendBattleInstinctVisual(player, start, player.position());

        if (data.isBattleInstinctCounterEnabled()) {
            boolean counterInRange = isWithinBattleInstinctCounterRange(player, attacker);
            double counterRange = player.getAttributeValue(ForgeMod.ENTITY_REACH.get())
                    + DarwinConfig.BATTLE_INSTINCT_COUNTER_RANGE_PADDING.get();
            double counterDistance = Math.sqrt(distanceToBoxSqr(player.getEyePosition(), attacker.getBoundingBox()));
            RuntimeDiagnostics.info("battle_instinct_counter_check", "player=" + player.getGameProfile().getName()
                    + " attacker=" + describeEntity(attacker) + " distanceToBox=" + counterDistance
                    + " range=" + counterRange + " inRange=" + counterInRange);
            if (counterInRange) {
                lookAt(player, attacker);
                counterattack(player, attacker, data);
            }
        } else {
            data.endBattleInstinctCounter();
            RuntimeDiagnostics.info("battle_instinct_dodge_only", "player=" + player.getGameProfile().getName()
                    + " attacker=" + describeEntity(attacker) + " viewAndCounterSkipped=true");
        }
        clearBattleInstinctHitFeedback(player);
        return true;
    }

    public static void tryRecordAdaptationDeath(ServerPlayer deadPlayer, DamageSource source, PlayerGrowthData data) {
        if (!DarwinConfig.ADAPTATION_ENABLED.get() || !data.isDamageAdaptationUnlocked() || !data.isDamageAdaptationEnabled()) {
            return;
        }

        AdaptationTarget target = resolveAdaptationTarget(source);
        if (target == null || target.playerTarget() && !DarwinConfig.ADAPTATION_PVP_ENABLED.get()) {
            return;
        }

        AdaptationRecord existing = data.getAdaptationRecord(target.key());
        int previousLevel = existing == null ? 0 : existing.getLevel();
        AdaptationRecord record = data.recordAdaptationDeath(target.key(), target.displayName(), target.targetId(), target.playerTarget());
        if (record.getLevel() > previousLevel) {
            spawnAdaptationLevelFeedback(deadPlayer);
        }
    }

    public static float applyAdaptationReduction(ServerPlayer player, DamageSource source, float amount, PlayerGrowthData data) {
        if (!DarwinConfig.ADAPTATION_ENABLED.get() || !data.isDamageAdaptationUnlocked() || !data.isDamageAdaptationEnabled()) {
            return amount;
        }

        AdaptationTarget target = resolveAdaptationTarget(source);
        if (target == null || target.playerTarget() && !DarwinConfig.ADAPTATION_PVP_ENABLED.get()) {
            return amount;
        }

        AdaptationRecord record = data.getAdaptationRecord(target.key());
        if (record == null || !record.isEnabled() || record.getLevel() <= 0) {
            return amount;
        }

        double reduction = Mth.clamp(DarwinConfig.adaptationReductionForLevel(record.getLevel()), 0.0D, 1.0D);
        return (float) (amount * (1.0D - reduction));
    }

    public static float applyHealingAmplification(ServerPlayer player, PlayerGrowthData data, float amount) {
        if (amount <= 0.0F) {
            return amount;
        }
        double amplification = GrowthAttributes.getHealingAmplification(data);
        if (amplification <= 0.0D) {
            return amount;
        }
        return (float) (amount * (1.0D + amplification));
    }

    public static void applyArmorPiercing(ServerPlayer attacker, LivingEntity target, DamageSource source) {
        if (target == attacker || target.level().isClientSide || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        clearArmorPiercingModifier(target);
        PlayerGrowthData data = GrowthSavedData.get(attacker).getOrCreate(attacker.getUUID());
        double pierce = GrowthAttributes.getArmorPierce(data);
        if (pierce <= 0.0D) {
            return;
        }

        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }

        double effectivePierce = Math.min(Math.max(0.0D, target.getAttributeValue(Attributes.ARMOR)), pierce);
        if (effectivePierce <= 0.0D) {
            return;
        }

        armor.addTransientModifier(new AttributeModifier(
                ARMOR_PIERCE_MODIFIER_ID,
                "Darwin soldier armor pierce",
                -effectivePierce,
                AttributeModifier.Operation.ADDITION
        ));
        attacker.getServer().execute(() -> clearArmorPiercingModifier(target));
    }

    public static void clearArmorPiercingModifier(LivingEntity target) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_PIERCE_MODIFIER_ID);
        }
    }

    public static void clearBattleInstinctHitFeedback(ServerPlayer player) {
        player.hurtTime = 0;
        player.hurtDuration = 0;
        player.setArrowCount(0);
    }

    public static float applyCriticalDamage(ServerPlayer attacker, float amount) {
        if (amount <= 0.0F) {
            return amount;
        }
        PlayerGrowthData data = GrowthSavedData.get(attacker).getOrCreate(attacker.getUUID());
        if (!data.isEnabled()) {
            return amount;
        }

        double chance = Mth.clamp(DarwinConfig.BASE_CRITICAL_CHANCE.get()
                + data.getPerceptionPoints() * DarwinConfig.PERCEPTION_CRITICAL_CHANCE_PER_POINT.get(), 0.0D, 1.0D);
        if (attacker.getRandom().nextDouble() >= chance) {
            return amount;
        }
        double multiplier = Math.max(1.0D, DarwinConfig.BASE_CRITICAL_DAMAGE.get()
                + data.getPerceptionPoints() * DarwinConfig.PERCEPTION_CRITICAL_DAMAGE_PER_POINT.get());
        return (float) (amount * multiplier);
    }

    public static void activateStressEvolution(ServerPlayer player) {
        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (!DarwinConfig.EVOLUTION_ENABLED.get() || !data.isStressEvolutionUnlocked() || !data.isStressEvolutionEnabled() || data.getStressEvolutionCooldownUntil() > now) {
            ModNetwork.syncTo(player, data);
            return;
        }

        removeHarmfulEffects(player);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DarwinConfig.STRESS_RESISTANCE_SECONDS.get() * 20, 0));
        int utilityDuration = DarwinConfig.STRESS_UTILITY_EFFECT_SECONDS.get() * 20;
        if (utilityDuration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, utilityDuration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, utilityDuration, 0));
        }
        playStressEvolutionVisuals(player);
        releaseStressShock(player);

        data.setStressEvolutionCooldownUntil(now + DarwinConfig.STRESS_COOLDOWN_SECONDS.get() * 20L);
        savedData.setDirty();
        ModNetwork.syncTo(player, data);
    }

    public static boolean reduceStressCooldownFromDamage(ServerPlayer player, PlayerGrowthData data, DamageSource source) {
        long now = player.serverLevel().getGameTime();
        if (!data.isStressEvolutionUnlocked() || data.getStressEvolutionCooldownUntil() <= now || !isStressCooldownReducingDamage(source)) {
            return false;
        }
        if (now - data.getStressEvolutionLastCooldownReductionTime() < DarwinConfig.STRESS_DAMAGE_COOLDOWN_REDUCTION_INTERVAL_SECONDS.get() * 20L) {
            return false;
        }

        data.setStressEvolutionLastCooldownReductionTime(now);
        data.setStressEvolutionCooldownUntil(Math.max(now,
                data.getStressEvolutionCooldownUntil() - DarwinConfig.STRESS_DAMAGE_COOLDOWN_REDUCTION_SECONDS.get() * 20L));
        return true;
    }

    public static boolean isBossOrHighHealth(LivingEntity entity) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }
        EntityType<?> type = entity.getType();
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
            return true;
        }
        return entity.getAttributeValue(Attributes.MAX_HEALTH) >= DarwinConfig.BOSS_HEALTH_THRESHOLD.get();
    }

    public static boolean isStressCooldownReducingDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }

        String msgId = source.getMsgId();
        if ("magic".equals(msgId) || "wither".equals(msgId) || "starve".equals(msgId) || "outOfWorld".equals(msgId)
                || "lava".equals(msgId) || "inFire".equals(msgId) || "onFire".equals(msgId) || "drown".equals(msgId)
                || "fall".equals(msgId) || "freeze".equals(msgId) || "cactus".equals(msgId)
                || "sweetBerryBush".equals(msgId) || "stalagmite".equals(msgId) || "hotFloor".equals(msgId)
                || "dryOut".equals(msgId) || "inWall".equals(msgId) || "cramming".equals(msgId)) {
            return false;
        }

        return source.getEntity() != null || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    private static boolean canUseBattleInstinct(PlayerGrowthData data) {
        return data.isEnabled()
                && DarwinConfig.BATTLE_INSTINCT_ENABLED.get()
                && data.isBattleInstinctUnlocked()
                && data.isBattleInstinctEnabled()
                && data.getBattleInstinctReserves() > 0;
    }

    private static long battleInstinctRecoverIntervalTicks(ServerPlayer player) {
        boolean lowHealth = player.getHealth() < player.getMaxHealth() * DarwinConfig.BATTLE_INSTINCT_LOW_HEALTH_PERCENT.get();
        int seconds = lowHealth
                ? DarwinConfig.BATTLE_INSTINCT_LOW_HEALTH_RECOVER_SECONDS.get()
                : DarwinConfig.BATTLE_INSTINCT_RECOVER_SECONDS.get();
        return Math.max(1L, seconds) * 20L;
    }

    private static LivingEntity resolveBattleInstinctAttacker(ServerPlayer player, DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof LivingEntity living && living != player && living.isAlive()) {
            return living;
        }
        entity = source.getDirectEntity();
        if (entity instanceof LivingEntity living && living != player && living.isAlive()) {
            return living;
        }
        return null;
    }

    private static Vec3 findBattleInstinctLanding(ServerPlayer player, LivingEntity attacker) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        Vec3 away = base.subtract(attacker.position());
        Vec3 horizontalAway = new Vec3(away.x, 0.0D, away.z);
        if (horizontalAway.lengthSqr() < 0.001D) {
            horizontalAway = player.getLookAngle().scale(-1.0D);
        }
        Vec3 awayNormal = new Vec3(horizontalAway.x, 0.0D, horizontalAway.z).normalize();

        List<Vec3> candidates = new ArrayList<>();
        for (int[] offset : BATTLE_INSTINCT_DODGE_OFFSETS) {
            candidates.add(base.add(offset[0], 0.0D, offset[1]));
        }
        candidates.sort(Comparator.comparingDouble(candidate -> -candidate.subtract(base).normalize().dot(awayNormal)));

        for (Vec3 candidate : candidates) {
            if (isSafeBattleInstinctLanding(level, player, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSafeBattleInstinctLanding(ServerLevel level, ServerPlayer player, Vec3 position) {
        BlockPos feet = BlockPos.containing(position.x, position.y, position.z);
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos below = feet.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        if (isUnsafeBattleInstinctBlock(belowState)
                || isUnsafeBattleInstinctBlock(level.getBlockState(feet))
                || isUnsafeBattleInstinctBlock(level.getBlockState(feet.above()))) {
            return false;
        }

        AABB targetBox = player.getBoundingBox().move(position.subtract(player.position()));
        if (!level.noCollision(player, targetBox)) {
            return false;
        }
        return level.getEntities(player, targetBox, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    private static boolean isUnsafeBattleInstinctBlock(BlockState state) {
        return state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA);
    }

    private static void spawnBattleInstinctSmoke(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                12, 0.35D, 0.35D, 0.35D, 0.02D);
    }

    private static void playStressEvolutionVisuals(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double y = player.getY() + player.getBbHeight() * 0.5D;
        int explosionParticles = DarwinConfig.STRESS_VISUAL_EXPLOSION_PARTICLES.get();
        if (explosionParticles > 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), y, player.getZ(),
                    explosionParticles, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.05F);

        int shockwaveParticles = DarwinConfig.STRESS_VISUAL_SHOCKWAVE_PARTICLES.get();
        if (shockwaveParticles <= 0) {
            return;
        }
        double radius = Math.max(0.5D, DarwinConfig.STRESS_SHOCK_RANGE.get());
        for (int i = 0; i < shockwaveParticles; i++) {
            double angle = Math.PI * 2.0D * i / shockwaveParticles;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(STRESS_SHOCKWAVE_PARTICLE, x, player.getY() + 0.15D, z,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    private static void spawnAdaptationLevelFeedback(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void lookAt(ServerPlayer player, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yRot = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        float xRot = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0D / Math.PI)));
        player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), yRot, xRot);
        player.setYHeadRot(yRot);
    }

    private static boolean isWithinBattleInstinctCounterRange(ServerPlayer player, LivingEntity target) {
        double range = player.getAttributeValue(ForgeMod.ENTITY_REACH.get())
                + DarwinConfig.BATTLE_INSTINCT_COUNTER_RANGE_PADDING.get();
        return distanceToBoxSqr(player.getEyePosition(), target.getBoundingBox()) <= range * range;
    }

    private static double distanceToBoxSqr(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void counterattack(ServerPlayer player, LivingEntity target, PlayerGrowthData data) {
        if (!target.isAlive()) {
            RuntimeDiagnostics.info("battle_instinct_counter_skipped", "player=" + player.getGameProfile().getName()
                    + " target=" + describeEntity(target) + " reason=target_not_alive");
            return;
        }

        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * DarwinConfig.BATTLE_INSTINCT_COUNTER_DAMAGE_MULTIPLIER.get());
        if (damage <= 0.0F) {
            RuntimeDiagnostics.info("battle_instinct_counter_skipped", "player=" + player.getGameProfile().getName()
                    + " target=" + describeEntity(target) + " reason=non_positive_damage damage=" + damage);
            return;
        }

        boolean wasAlive = target.isAlive();
        int reservesBefore = data.getBattleInstinctReserves();
        float healthBefore = target.getHealth();
        long now = player.serverLevel().getGameTime();
        data.beginBattleInstinctCounter(target.getId(), now + BATTLE_INSTINCT_COUNTER_WINDOW_TICKS);
        boolean hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        if (!hurt) {
            data.endBattleInstinctCounter();
        }
        if (wasAlive && target.isDeadOrDying()) {
            data.restoreBattleInstinctReserves(DarwinConfig.BATTLE_INSTINCT_COUNTER_KILL_RESERVES.get());
        }
        RuntimeDiagnostics.info("battle_instinct_counter_result", "player=" + player.getGameProfile().getName()
                + " target=" + describeEntity(target) + " requestedDamage=" + damage + " hurt=" + hurt
                + " health=" + healthBefore + "->" + target.getHealth() + " killed="
                + (wasAlive && target.isDeadOrDying()) + " reserves=" + reservesBefore + "->"
                + data.getBattleInstinctReserves());
    }

    private static String describeEntity(Entity entity) {
        if (entity == null) {
            return "null";
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return (id == null ? entity.getType().toString() : id.toString()) + "#" + entity.getId();
    }

    private static void removeHarmfulEffects(ServerPlayer player) {
        List<MobEffect> toRemove = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        toRemove.forEach(player::removeEffect);
    }

    private static void releaseStressShock(ServerPlayer player) {
        double range = DarwinConfig.STRESS_SHOCK_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                target -> target != player && target.isAlive() && target.distanceTo(player) <= range);
        for (LivingEntity target : targets) {
            if (isIntimidationImmune(player, target)) {
                continue;
            }
            boolean enhanced = target.getHealth() < player.getHealth() && !isBossOrHighHealth(target);
            double knockback = enhanced ? DarwinConfig.STRESS_ENHANCED_KNOCKBACK.get() : DarwinConfig.STRESS_NORMAL_KNOCKBACK.get();
            applyShockKnockback(player, target, knockback);
            int slowSeconds = enhanced ? DarwinConfig.STRESS_ENHANCED_SLOW_SECONDS.get() : DarwinConfig.STRESS_NORMAL_SLOW_SECONDS.get();
            int amplifier = Math.max(0, (enhanced ? DarwinConfig.STRESS_ENHANCED_SLOW_LEVEL.get() : DarwinConfig.STRESS_NORMAL_SLOW_LEVEL.get()) - 1);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowSeconds * 20, amplifier));
        }
    }

    private static boolean isIntimidationImmune(ServerPlayer player, LivingEntity target) {
        if (target instanceof TamableAnimal tamable && player.getUUID().equals(tamable.getOwnerUUID())) {
            return true;
        }
        if (target instanceof AbstractHorse horse && player.getUUID().equals(horse.getOwnerUUID())) {
            return true;
        }
        if (target instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null) {
            return false;
        }
        String idText = id.toString().toLowerCase(Locale.ROOT);
        return DarwinConfig.INTIMIDATION_IMMUNITY.get().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(idText::equals);
    }

    private static void applyShockKnockback(ServerPlayer player, LivingEntity target, double knockback) {
        Vec3 direction = target.position().subtract(player.position());
        if (direction.horizontalDistanceSqr() < 0.001D) {
            direction = player.getLookAngle();
        }
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z).normalize();
        target.knockback(knockback * 0.45D, -horizontal.x, -horizontal.z);
        target.hurtMarked = true;
    }

    private static AdaptationTarget resolveAdaptationTarget(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity == null) {
            entity = source.getDirectEntity();
        }
        if (entity instanceof Player player) {
            String uuid = player.getUUID().toString();
            return new AdaptationTarget("player:" + uuid, player.getGameProfile().getName(), uuid, true);
        }
        if (entity instanceof LivingEntity living) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
            if (id == null) {
                return null;
            }
            String idText = id.toString();
            return new AdaptationTarget("entity:" + idText, living.getType().getDescription().getString(), idText, false);
        }
        return null;
    }

    private record AdaptationTarget(String key, String displayName, String targetId, boolean playerTarget) {
    }
}
