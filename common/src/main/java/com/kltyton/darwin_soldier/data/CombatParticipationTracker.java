package com.kltyton.darwin_soldier.data;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatParticipationTracker {
    private static final Map<LivingEntity, ParticipationList> PARTICIPANTS = new WeakHashMap<>();
    private static final Set<String> OWNER_METHOD_NAMES = Set.of(
            "getOwnerUUID", "getOwnerId", "getOwner", "getTrueOwner", "getSummoner", "getCaster"
    );
    private static final Map<Class<?>, List<Method>> OWNER_METHOD_CACHE = new ConcurrentHashMap<>();

    private CombatParticipationTracker() {
    }

    public static void recordDamage(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        ParticipantSource participant = resolveParticipant(source);
        if (participant == null) {
            return;
        }

        long now = level.getGameTime();
        PARTICIPANTS.computeIfAbsent(target, ignored -> new ParticipationList())
                .record(participant.playerId(), participant.direct(), now);
    }

    public static void recordFinalSource(LivingEntity target, DamageSource source) {
        if (target.level() instanceof ServerLevel level) {
            ParticipantSource participant = resolveParticipant(source);
            if (participant != null) {
                PARTICIPANTS.computeIfAbsent(target, ignored -> new ParticipationList())
                        .recordImmediately(participant.playerId(), participant.direct(), level.getGameTime());
            }
        }
    }

    public static List<Participation> consume(LivingEntity target) {
        ParticipationList list = PARTICIPANTS.remove(target);
        if (list == null) {
            return List.of();
        }
        return list.snapshot(target.level().getGameTime());
    }

    public static void clear(LivingEntity target) {
        PARTICIPANTS.remove(target);
    }

    private static ParticipantSource resolveParticipant(DamageSource source) {
        Entity causing = source.getEntity();
        if (causing instanceof ServerPlayer player) {
            return new ParticipantSource(player.getUUID(), true);
        }
        UUID projectilePlayer = resolveProjectilePlayer(causing);
        if (projectilePlayer == null) {
            projectilePlayer = resolveProjectilePlayer(source.getDirectEntity());
        }
        if (projectilePlayer != null) {
            return new ParticipantSource(projectilePlayer, true);
        }

        UUID ownerId = resolveOwnerId(causing);
        if (ownerId == null) {
            ownerId = resolveOwnerId(source.getDirectEntity());
        }
        if (ownerId == null) {
            return null;
        }
        return new ParticipantSource(ownerId, false);
    }

    private static UUID resolveProjectilePlayer(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null;
    }

    private static UUID resolveOwnerId(Entity entity) {
        return resolveOwnerId(entity, 0);
    }

    private static UUID resolveOwnerId(Entity entity, int depth) {
        if (depth > 4) {
            return null;
        }
        if (entity == null) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            return player.getUUID();
        }
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != entity) {
                UUID projectileOwner = resolveOwnerId(owner, depth + 1);
                if (projectileOwner != null) {
                    return projectileOwner;
                }
            }
        }
        if (entity instanceof OwnableEntity ownable) {
            return ownable.getOwnerUUID();
        }
        for (Method method : OWNER_METHOD_CACHE.computeIfAbsent(entity.getClass(), CombatParticipationTracker::findOwnerMethods)) {
            try {
                Object value = method.invoke(entity);
                if (value instanceof UUID uuid) {
                    return uuid;
                }
                if (value instanceof Entity owner && owner != entity) {
                    UUID ownerId = resolveOwnerId(owner, depth + 1);
                    if (ownerId != null) {
                        return ownerId;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Unsupported or inaccessible owner APIs are safely skipped.
            }
        }
        return null;
    }

    private static List<Method> findOwnerMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 && OWNER_METHOD_NAMES.contains(method.getName())
                    && (UUID.class.isAssignableFrom(method.getReturnType()) || Entity.class.isAssignableFrom(method.getReturnType()))) {
                methods.add(method);
            }
        }
        return List.copyOf(methods);
    }

    public record Participation(UUID playerId, boolean direct) {
    }

    private record ParticipantSource(UUID playerId, boolean direct) {
    }

    private static final class ParticipationList {
        private final LinkedHashMap<UUID, Entry> entries = new LinkedHashMap<>(16, 0.75F, true);

        private void record(UUID playerId, boolean direct, long now) {
            cleanup(now);
            Entry existing = entries.get(playerId);
            long refreshTicks = DarwinConfig.PARTICIPATION_REFRESH_INTERVAL_SECONDS.get() * 20L;
            if (existing != null && !direct && now - existing.lastRefresh() < refreshTicks) {
                return;
            }
            if (existing != null && direct && existing.direct() && now - existing.lastRefresh() < refreshTicks) {
                return;
            }
            entries.put(playerId, new Entry(existing != null && existing.direct() || direct, now));
            trimToLimit();
        }

        private void recordImmediately(UUID playerId, boolean direct, long now) {
            cleanup(now);
            Entry existing = entries.get(playerId);
            entries.put(playerId, new Entry(existing != null && existing.direct() || direct, now));
            trimToLimit();
        }

        private List<Participation> snapshot(long now) {
            cleanup(now);
            List<Participation> result = new ArrayList<>(entries.size());
            entries.forEach((uuid, entry) -> result.add(new Participation(uuid, entry.direct())));
            return result;
        }

        private void cleanup(long now) {
            long expireTicks = DarwinConfig.PARTICIPATION_EXPIRE_SECONDS.get() * 20L;
            entries.entrySet().removeIf(entry -> now - entry.getValue().lastRefresh() > expireTicks);
        }

        private void trimToLimit() {
            int maxPlayers = DarwinConfig.PARTICIPATION_MAX_PLAYERS.get();
            while (entries.size() > maxPlayers) {
                UUID oldest = entries.keySet().iterator().next();
                entries.remove(oldest);
            }
        }
    }

    private record Entry(boolean direct, long lastRefresh) {
    }
}
