package com.kltyton.darwin_soldier.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pairs the victim's pre-armor LivingHurtEvent amount with the matching
 * LivingDamageEvent final amount so the flat defense minimum reduction can be
 * applied without leaking stale captures across nested/reentrant events.
 */
public final class DefenseReductionTracker {
    private static final int MAX_CAPTURES_PER_PLAYER = 8;
    private static final long CAPTURE_MAX_AGE_TICKS = 100L;
    private static final Map<UUID, BoundedPairStack<DamageSource>> CAPTURES = new HashMap<>();

    private DefenseReductionTracker() {
    }

    public static void capture(ServerPlayer victim, DamageSource source, float preArmorAmount) {
        if (preArmorAmount <= 0.0F) {
            return;
        }
        long now = victim.serverLevel().getGameTime();
        BoundedPairStack<DamageSource> stack = CAPTURES.computeIfAbsent(victim.getUUID(),
                ignored -> new BoundedPairStack<>(MAX_CAPTURES_PER_PLAYER));
        stack.prune(now, CAPTURE_MAX_AGE_TICKS);
        stack.push(source, preArmorAmount, now);
    }

    public static float applyDefenseReduction(ServerPlayer victim, DamageSource source, float finalAmount,
                                              float minimumReduction) {
        if (finalAmount <= 0.0F || minimumReduction <= 0.0F) {
            return finalAmount;
        }
        BoundedPairStack<DamageSource> stack = CAPTURES.get(victim.getUUID());
        if (stack == null || stack.isEmpty()) {
            return finalAmount;
        }
        stack.prune(victim.serverLevel().getGameTime(), CAPTURE_MAX_AGE_TICKS);
        return stack.consume(source, finalAmount, minimumReduction);
    }

    public static void clear(UUID playerId) {
        CAPTURES.remove(playerId);
    }

    public static void clearAll() {
        CAPTURES.clear();
    }
}
