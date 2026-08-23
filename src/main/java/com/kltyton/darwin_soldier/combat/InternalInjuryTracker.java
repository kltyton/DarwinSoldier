package com.kltyton.darwin_soldier.combat;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class InternalInjuryTracker {
    private static final Map<UUID, InjuryState> INJURIES = new HashMap<>();

    private InternalInjuryTracker() {
    }

    public static void apply(ServerPlayer attacker, LivingEntity target) {
        int interval = Math.max(1, DarwinConfig.HUNTING_INJURY_INTERVAL_TICKS.get());
        long durationTicks = Math.max(0L, DarwinConfig.HUNTING_INJURY_DURATION_SECONDS.get()) * 20L;
        int applications = (int) Math.min(Integer.MAX_VALUE, durationTicks / interval);
        float damage = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * DarwinConfig.HUNTING_INJURY_DAMAGE_MULTIPLIER.get());
        if (applications <= 0 || damage <= 0.0F) {
            INJURIES.remove(target.getUUID());
            return;
        }

        long now = attacker.serverLevel().getGameTime();
        INJURIES.put(target.getUUID(), new InjuryState(
                attacker.serverLevel().dimension(), attacker.getUUID(), now + interval,
                now + durationTicks, applications, damage));
    }

    public static float applyVulnerability(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || source.is(DarwinDamageTypes.INTERNAL_INJURY)
                || source.is(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION)) {
            return amount;
        }
        InjuryState state = INJURIES.get(target.getUUID());
        if (state == null || !target.level().dimension().equals(state.dimension)
                || target.level().getGameTime() >= state.expiresAt) {
            return amount;
        }
        return (float) (amount * (1.0D + DarwinConfig.HUNTING_VULNERABILITY_BONUS.get()));
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, InjuryState>> iterator = INJURIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, InjuryState> entry = iterator.next();
            InjuryState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now < state.nextDamageAt) {
                continue;
            }
            ServerPlayer attacker = server.getPlayerList().getPlayer(state.attackerId);
            if (attacker == null || attacker.serverLevel() != level
                    || !(level.getEntity(entry.getKey()) instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            target.hurt(DarwinDamageTypes.internalInjury(level, attacker), state.damagePerApplication);
            state.remainingApplications--;
            state.nextDamageAt += Math.max(1, DarwinConfig.HUNTING_INJURY_INTERVAL_TICKS.get());
            if (state.remainingApplications <= 0 || now >= state.expiresAt) {
                iterator.remove();
            }
        }
    }

    public static void clearAttacker(UUID attackerId) {
        INJURIES.entrySet().removeIf(entry -> entry.getValue().attackerId.equals(attackerId));
    }

    public static void clearTarget(UUID targetId) {
        INJURIES.remove(targetId);
    }

    public static void clearAll() {
        INJURIES.clear();
    }

    private static final class InjuryState {
        private final ResourceKey<Level> dimension;
        private final UUID attackerId;
        private long nextDamageAt;
        private final long expiresAt;
        private int remainingApplications;
        private final float damagePerApplication;

        private InjuryState(ResourceKey<Level> dimension, UUID attackerId, long nextDamageAt,
                            long expiresAt, int remainingApplications, float damagePerApplication) {
            this.dimension = dimension;
            this.attackerId = attackerId;
            this.nextDamageAt = nextDamageAt;
            this.expiresAt = expiresAt;
            this.remainingApplications = remainingApplications;
            this.damagePerApplication = damagePerApplication;
        }
    }
}
