package com.kltyton.darwin_soldier.combat;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.GrowthAttributes;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MinimumEffectiveDamageTracker {
    private static final Map<WindowKey, DamageWindow> WINDOWS = new HashMap<>();
    private static final Map<UUID, DamageSource> SHIELD_BLOCKS = new HashMap<>();
    private static final ThreadLocal<CorrectionContext> ACTIVE_CORRECTION = new ThreadLocal<>();

    private MinimumEffectiveDamageTracker() {
    }

    static boolean isEligiblePlayerSourceType(Class<?> sourceType) {
        return ServerPlayer.class.isAssignableFrom(sourceType)
                && !FakePlayer.class.isAssignableFrom(sourceType);
    }

    public static boolean isRealPlayerMelee(DamageSource source, ServerPlayer attacker) {
        return isEligiblePlayerSourceType(attacker.getClass())
                && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() == attacker
                && source.getDirectEntity() == attacker;
    }

    public static ServerPlayer resolvePlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player
                && isEligiblePlayerSourceType(player.getClass())) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer player
                && isEligiblePlayerSourceType(player.getClass())) {
            return player;
        }
        return null;
    }

    public static float applyMeleeMinimum(ServerPlayer attacker, float finalDamage) {
        if (finalDamage <= 0.0F) {
            return finalDamage;
        }
        PlayerGrowthData data = GrowthSavedData.get(attacker).getOrCreate(attacker.getUUID());
        if (!data.isEnabled() || !DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_ENABLED.get()) {
            return finalDamage;
        }
        float minimum = CombatMath.minimumEffectiveDamage(data.getAttackPoints(), GrowthAttributes.attackPerPoint(),
                DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_RATIO.get());
        return CombatMath.applyMinimum(finalDamage, minimum);
    }

    public static void recordNonMelee(ServerPlayer attacker, LivingEntity target, DamageSource source, float finalDamage) {
        if (finalDamage <= 0.0F || DarwinDamageTypes.isInternal(source) || isExcludedNonMelee(source)) {
            return;
        }
        PlayerGrowthData data = GrowthSavedData.get(attacker).getOrCreate(attacker.getUUID());
        if (!data.isEnabled() || !DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_ENABLED.get()) {
            return;
        }
        float minimum = CombatMath.minimumEffectiveDamage(data.getAttackPoints(), GrowthAttributes.attackPerPoint(),
                DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_RATIO.get());
        if (minimum <= 0.0F) {
            return;
        }

        ServerLevel level = attacker.serverLevel();
        long now = level.getGameTime();
        WindowKey key = new WindowKey(level.dimension(), attacker.getUUID(), target.getUUID());
        WINDOWS.compute(key, (ignored, existing) -> {
            if (existing == null) {
                long settleAt = now + Math.max(1, DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_WINDOW_TICKS.get());
                return new DamageWindow(settleAt, minimum, finalDamage);
            }
            existing.accumulatedDamage += finalDamage;
            return existing;
        });
    }

    public static void markShieldBlock(LivingEntity target, DamageSource source) {
        SHIELD_BLOCKS.put(target.getUUID(), source);
    }

    public static boolean consumeShieldBlock(LivingEntity target, DamageSource source) {
        DamageSource blockedSource = SHIELD_BLOCKS.get(target.getUUID());
        if (blockedSource != source) {
            return false;
        }
        SHIELD_BLOCKS.remove(target.getUUID());
        return true;
    }

    public static boolean forceCorrectionFinalDamage(LivingDamageEvent event) {
        CorrectionContext context = ACTIVE_CORRECTION.get();
        if (context == null || !event.getSource().is(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION)
                || !context.targetId.equals(event.getEntity().getUUID())) {
            return false;
        }
        event.getEntity().setAbsorptionAmount(context.absorptionBefore);
        event.setAmount(context.requestedDamage);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<WindowKey, DamageWindow>> iterator = WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WindowKey, DamageWindow> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey().dimension);
            if (level == null || level.getGameTime() < entry.getValue().settleAt) {
                continue;
            }
            iterator.remove();
            settle(server, level, entry.getKey(), entry.getValue());
        }
        SHIELD_BLOCKS.clear();
    }

    public static void clearAttacker(UUID attackerId) {
        WINDOWS.keySet().removeIf(key -> key.attackerId.equals(attackerId));
    }

    public static void clearTarget(UUID targetId) {
        WINDOWS.keySet().removeIf(key -> key.targetId.equals(targetId));
        SHIELD_BLOCKS.remove(targetId);
    }

    public static void clearAll() {
        WINDOWS.clear();
        SHIELD_BLOCKS.clear();
        ACTIVE_CORRECTION.remove();
    }

    private static void settle(MinecraftServer server, ServerLevel level, WindowKey key, DamageWindow window) {
        float missing = CombatMath.missingDamage(window.accumulatedDamage, window.minimumDamage);
        if (missing <= 0.0F) {
            return;
        }
        ServerPlayer attacker = server.getPlayerList().getPlayer(key.attackerId);
        if (attacker == null || attacker.serverLevel() != level) {
            return;
        }
        if (!(level.getEntity(key.targetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        DamageSource source = DarwinDamageTypes.minimumDamageCorrection(level, attacker);
        if (target.isInvulnerableTo(source)) {
            return;
        }

        CorrectionContext context = new CorrectionContext(target.getUUID(), missing, target.getAbsorptionAmount());
        ACTIVE_CORRECTION.set(context);
        try {
            target.hurt(source, missing);
        } finally {
            ACTIVE_CORRECTION.remove();
        }
    }

    private static boolean isExcludedNonMelee(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.THORNS)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private record WindowKey(ResourceKey<Level> dimension, UUID attackerId, UUID targetId) {
    }

    private static final class DamageWindow {
        private final long settleAt;
        private final float minimumDamage;
        private float accumulatedDamage;

        private DamageWindow(long settleAt, float minimumDamage, float accumulatedDamage) {
            this.settleAt = settleAt;
            this.minimumDamage = minimumDamage;
            this.accumulatedDamage = accumulatedDamage;
        }
    }

    private record CorrectionContext(UUID targetId, float requestedDamage, float absorptionBefore) {
    }
}
