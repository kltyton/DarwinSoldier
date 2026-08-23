package com.kltyton.darwin_soldier.ability;

import com.kltyton.darwin_soldier.combat.DarwinDamageTypes;
import com.kltyton.darwin_soldier.combat.InternalInjuryTracker;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class HuntingInstinct {
    private static final int MAX_SECONDARY_TARGETS = 5;

    private HuntingInstinct() {
    }

    public static void activate(ServerPlayer player) {
        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (!canActivate(data, now)) {
            RuntimeDiagnostics.warn("hunting_activate_rejected", "player=" + player.getGameProfile().getName()
                    + " now=" + now + " growthEnabled=" + data.isEnabled() + " feature="
                    + DarwinConfig.HUNT_ENABLED.get() + " unlocked=" + data.isHuntingInstinctUnlocked()
                    + " enabled=" + data.isHuntingInstinctEnabled() + " activeUntil="
                    + data.getHuntingInstinctActiveUntil() + " cooldownUntil=" + data.getHuntingInstinctCooldownUntil());
            ModNetwork.syncTo(player, data);
            return;
        }

        int duration = Math.max(1, DarwinConfig.HUNTING_ACTIVE_SECONDS.get()) * 20;
        data.beginHuntingInstinct(now + duration);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        addMobilityEffect(player, MobEffects.MOVEMENT_SPEED, duration, DarwinConfig.HUNTING_SPEED_LEVEL.get());
        addMobilityEffect(player, MobEffects.JUMP, duration, DarwinConfig.HUNTING_JUMP_LEVEL.get());
        spawnActivationSmoke(player);
        RuntimeDiagnostics.info("hunting_activated", "player=" + player.getGameProfile().getName()
                + " now=" + now + " activeUntil=" + data.getHuntingInstinctActiveUntil()
                + " duration=" + duration + " speedLevel=" + DarwinConfig.HUNTING_SPEED_LEVEL.get()
                + " jumpLevel=" + DarwinConfig.HUNTING_JUMP_LEVEL.get());
        ModNetwork.syncTo(player, data);
    }

    public static boolean tick(ServerPlayer player, PlayerGrowthData data) {
        long now = player.serverLevel().getGameTime();
        if (!data.isHuntingInstinctActive(now)) {
            if (data.getHuntingInstinctActiveUntil() > 0L) {
                finishAndStartCooldown(player, data, now);
                return true;
            }
            return false;
        }
        if (!canUse(data)) {
            clearActive(player, data, now);
            return true;
        }
        return false;
    }

    public static boolean isSlownessImmune(ServerPlayer player, PlayerGrowthData data) {
        return canUse(data) && data.isHuntingInstinctActive(player.serverLevel().getGameTime());
    }

    public static HitResult applyMeleeHit(ServerPlayer attacker, LivingEntity target, float finalDamage) {
        GrowthSavedData savedData = GrowthSavedData.get(attacker);
        PlayerGrowthData data = savedData.getOrCreate(attacker.getUUID());
        long now = attacker.serverLevel().getGameTime();
        if (finalDamage <= 0.0F || !canUse(data) || !data.isHuntingInstinctActive(now)) {
            if (data.isHuntingInstinctActive(now)) {
                RuntimeDiagnostics.warn("hunting_melee_rejected", "player=" + attacker.getGameProfile().getName()
                        + " target=" + target.getType().getDescriptionId() + "#" + target.getId()
                        + " finalDamage=" + finalDamage + " canUse=" + canUse(data)
                        + " activeUntil=" + data.getHuntingInstinctActiveUntil());
            }
            return new HitResult(finalDamage, false);
        }

        finishAndStartCooldown(attacker, data, now);
        float boostedDamage = (float) (finalDamage * DarwinConfig.HUNTING_DAMAGE_MULTIPLIER.get());
        Vec3 impactCenter = target.getBoundingBox().getCenter();
        spawnHitWind(attacker.serverLevel(), impactCenter);
        if (DarwinConfig.HUNTING_INTERNAL_IMPACT_ENABLED.get() && data.isHuntingInternalImpactEnabled()) {
            releaseInternalImpact(attacker, target, impactCenter);
        }
        applyStrongKnockback(attacker, target, finalDamage);
        RuntimeDiagnostics.info("hunting_melee_trigger", "player=" + attacker.getGameProfile().getName()
                + " target=" + target.getType().getDescriptionId() + "#" + target.getId()
                + " baseFinalDamage=" + finalDamage + " boostedDamage=" + boostedDamage
                + " internalImpact=" + (DarwinConfig.HUNTING_INTERNAL_IMPACT_ENABLED.get()
                && data.isHuntingInternalImpactEnabled()) + " cooldownUntil=" + data.getHuntingInstinctCooldownUntil());
        savedData.setDirty();
        ModNetwork.syncTo(attacker, data);
        return new HitResult(boostedDamage, true);
    }

    public static boolean stopWithCooldown(ServerPlayer player, PlayerGrowthData data) {
        long now = player.serverLevel().getGameTime();
        if (!data.isHuntingInstinctActive(now) && data.getHuntingInstinctActiveUntil() <= 0L) {
            return false;
        }
        finishAndStartCooldown(player, data, now);
        return true;
    }

    public static boolean clearRuntime(ServerPlayer player, PlayerGrowthData data, boolean startCooldown) {
        long now = player.serverLevel().getGameTime();
        if (!data.isHuntingInstinctActive(now) && data.getHuntingInstinctActiveUntil() <= 0L) {
            return false;
        }
        if (startCooldown) {
            finishAndStartCooldown(player, data, now);
        } else {
            clearActive(player, data, now);
        }
        return true;
    }

    private static boolean canActivate(PlayerGrowthData data, long now) {
        return canUse(data) && data.getHuntingInstinctCooldownUntil() <= now
                && !data.isHuntingInstinctActive(now);
    }

    private static boolean canUse(PlayerGrowthData data) {
        return data.isEnabled() && DarwinConfig.HUNT_ENABLED.get()
                && data.isHuntingInstinctUnlocked() && data.isHuntingInstinctEnabled();
    }

    private static void finishAndStartCooldown(ServerPlayer player, PlayerGrowthData data, long now) {
        clearActive(player, data, now);
        int seconds = player.getHealth() < player.getMaxHealth() * DarwinConfig.HUNTING_LOW_HEALTH_PERCENT.get()
                ? DarwinConfig.HUNTING_LOW_HEALTH_COOLDOWN_SECONDS.get()
                : DarwinConfig.HUNTING_COOLDOWN_SECONDS.get();
        data.setHuntingInstinctCooldownUntil(now + Math.max(0, seconds) * 20L);
        RuntimeDiagnostics.info("hunting_cooldown_start", "player=" + player.getGameProfile().getName()
                + " now=" + now + " seconds=" + seconds + " cooldownUntil="
                + data.getHuntingInstinctCooldownUntil() + " health=" + player.getHealth() + "/" + player.getMaxHealth());
    }

    private static void clearActive(ServerPlayer player, PlayerGrowthData data, long now) {
        long remaining = Math.max(0L, data.getHuntingInstinctActiveUntil() - now);
        removeOwnedEffect(player, MobEffects.MOVEMENT_SPEED, DarwinConfig.HUNTING_SPEED_LEVEL.get(), remaining);
        removeOwnedEffect(player, MobEffects.JUMP, DarwinConfig.HUNTING_JUMP_LEVEL.get(), remaining);
        data.endHuntingInstinct();
    }

    private static void addMobilityEffect(ServerPlayer player, MobEffect effect, int duration, int level) {
        if (level > 0) {
            player.addEffect(new MobEffectInstance(effect, duration, level - 1, false, false, true));
        }
    }

    private static void removeOwnedEffect(ServerPlayer player, MobEffect effect, int level, long remaining) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && level > 0 && current.getAmplifier() == level - 1
                && current.getDuration() <= remaining + 2L && !current.isAmbient()
                && !current.isVisible() && current.showIcon()) {
            player.removeEffect(effect);
        }
    }

    private static void spawnActivationSmoke(ServerPlayer player) {
        int count = DarwinConfig.HUNTING_ACTIVATION_PARTICLE_COUNT.get();
        if (count > 0) {
            player.serverLevel().sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.1D, player.getZ(), count,
                    0.75D, 0.12D, 0.75D, 0.05D);
        }
    }

    private static void spawnHitWind(ServerLevel level, Vec3 center) {
        int count = DarwinConfig.HUNTING_HIT_PARTICLE_COUNT.get();
        if (count <= 0) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                count, 0.55D, 0.35D, 0.55D, 0.08D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z,
                Math.max(1, count / 3), 0.35D, 0.25D, 0.35D, 0.04D);
    }

    private static void releaseInternalImpact(ServerPlayer attacker, LivingEntity primary, Vec3 center) {
        InternalInjuryTracker.apply(attacker, primary);
        double radius = DarwinConfig.HUNTING_SHOCK_RADIUS.get();
        if (radius <= 0.0D) {
            return;
        }
        float shockDamage = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * DarwinConfig.HUNTING_SHOCK_DAMAGE_MULTIPLIER.get());
        if (shockDamage <= 0.0F) {
            return;
        }

        AABB searchBox = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = attacker.serverLevel().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> isValidSecondaryTarget(attacker, primary, target)
                        && target.getBoundingBox().distanceToSqr(center) <= radius * radius);
        targets.sort(Comparator
                .comparingDouble((LivingEntity target) -> target.getBoundingBox().distanceToSqr(center))
                .thenComparingInt(Entity::getId));
        if (targets.size() > MAX_SECONDARY_TARGETS) {
            targets = targets.subList(0, MAX_SECONDARY_TARGETS);
        }
        int affected = 0;
        for (LivingEntity target : targets) {
            if (target.hurt(DarwinDamageTypes.huntingShock(attacker.serverLevel(), attacker), shockDamage)) {
                InternalInjuryTracker.apply(attacker, target);
                affected++;
            }
        }
        RuntimeDiagnostics.info("hunting_internal_impact", "player=" + attacker.getGameProfile().getName()
                + " primary=" + primary.getType().getDescriptionId() + "#" + primary.getId()
                + " radius=" + radius + " candidates=" + targets.size() + " affected=" + affected
                + " shockDamage=" + shockDamage);
    }

    private static boolean isValidSecondaryTarget(ServerPlayer attacker, LivingEntity primary, LivingEntity target) {
        if (target == attacker || target == primary || !target.isAlive() || target.isSpectator() || !target.isAttackable()) {
            return false;
        }
        if (target instanceof Player player && !attacker.canHarmPlayer(player)) {
            return false;
        }
        if (attacker.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof OwnableEntity ownable) {
            if (attacker.getUUID().equals(ownable.getOwnerUUID())) {
                return false;
            }
            LivingEntity owner = ownable.getOwner();
            if (owner != null && (attacker.isAlliedTo(owner)
                    || owner instanceof Player player && !attacker.canHarmPlayer(player))) {
                return false;
            }
        }
        return true;
    }

    private static void applyStrongKnockback(ServerPlayer attacker, LivingEntity target, float finalDamage) {
        double knockback = Math.min(DarwinConfig.HUNTING_MAX_KNOCKBACK.get(),
                DarwinConfig.HUNTING_BASE_KNOCKBACK.get()
                        + finalDamage / DarwinConfig.HUNTING_KNOCKBACK_PER_DAMAGE.get());
        Vec3 direction = target.position().subtract(attacker.position());
        if (direction.horizontalDistanceSqr() < 0.001D) {
            direction = attacker.getLookAngle();
        }
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z).normalize();
        target.knockback(knockback * 0.4D, -horizontal.x, -horizontal.z);
        target.push(horizontal.x * knockback * 0.08D, 0.35D, horizontal.z * knockback * 0.08D);
        target.hurtMarked = true;
    }

    public record HitResult(float damage, boolean triggered) {
    }
}
