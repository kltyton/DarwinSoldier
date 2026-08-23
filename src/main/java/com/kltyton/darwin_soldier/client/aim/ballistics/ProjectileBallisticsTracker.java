package com.kltyton.darwin_soldier.client.aim.ballistics;

import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettings;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettingsStore;
import com.kltyton.darwin_soldier.client.aim.TrajectoryFeaturePolicy;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProjectileBallisticsTracker {
    private static final int DETECTION_WINDOW_TICKS = 8;
    private static final int SAMPLE_WINDOW_TICKS = 8;
    private static final int MINIMUM_SPEED_SAMPLES = 3;
    private static final int MINIMUM_GRAVITY_SAMPLES = 2;
    private static final int PROJECTILE_CONFIRMATIONS = 2;
    private static final double DISCOVERY_RADIUS = 16.0D;
    private static final double MINIMUM_DIRECTION_DOT = 0.35D;
    private static final Map<Integer, SampleTrack> TRACKS = new HashMap<>();
    private static final Map<ResourceLocation, ProfileCandidate> PROFILE_CANDIDATES = new HashMap<>();
    private static final Set<Integer> SEEN_PROJECTILES = new HashSet<>();

    private static Object levelIdentity;
    private static PendingTrigger pending;
    private static ResourceLocation useWeapon;
    private static boolean useWeaponEligible;
    private static boolean wasUsingItem;

    private ProjectileBallisticsTracker() {
    }

    public static void noteTrigger(Minecraft minecraft, ItemStack weapon) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!TrajectoryFeaturePolicy.isEligibleWeapon(weapon.isEdible())
                || WeaponLaunchResolver.isSupported(weapon)) {
            return;
        }
        ResourceLocation weaponId = AimWeaponSettingsStore.weaponId(weapon);
        if (weaponId == null || !canLearn(weaponId) || AimWeaponSettingsStore.get(weaponId).ballistics() != null) {
            return;
        }
        arm(minecraft.player, weaponId, minecraft.level.getGameTime());
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            reset(null);
            return;
        }
        if (!ClientGrowthData.isEnabled()) {
            reset(minecraft.level);
            return;
        }
        if (levelIdentity != minecraft.level) {
            reset(minecraft.level);
        }

        long now = minecraft.level.getGameTime();
        trackUseRelease(player, now);
        discoverProjectile(player, now);
        sampleProjectiles(minecraft, now);
        if (pending != null && now > pending.deadline()) {
            ResourceLocation weaponId = pending.weaponId();
            pending = null;
            RuntimeDiagnostics.info("projectile_ballistics_miss", "weapon=" + weaponId
                    + " reason=no_owned_projectile windowTicks=" + DETECTION_WINDOW_TICKS);
        }
    }

    private static void trackUseRelease(LocalPlayer player, long now) {
        if (player.isUsingItem()) {
            boolean mainHandUse = player.getUsedItemHand() == InteractionHand.MAIN_HAND;
            ResourceLocation current = mainHandUse
                    ? AimWeaponSettingsStore.weaponId(player.getMainHandItem())
                    : null;
            if (mainHandUse && (!wasUsingItem || !Objects.equals(current, useWeapon))) {
                useWeapon = current;
                useWeaponEligible = TrajectoryFeaturePolicy.isEligibleWeapon(player.getUseItem().isEdible());
            } else if (!mainHandUse && wasUsingItem) {
                useWeapon = null;
                useWeaponEligible = false;
            }
            if (pending != null && mainHandUse && Objects.equals(pending.weaponId(), current)) {
                pending = null;
            }
            wasUsingItem = true;
            return;
        }
        if (wasUsingItem && useWeaponEligible && useWeapon != null && canLearn(useWeapon)
                && AimWeaponSettingsStore.get(useWeapon).ballistics() == null) {
            ItemStack releasedItem = player.getMainHandItem();
            if (Objects.equals(AimWeaponSettingsStore.weaponId(releasedItem), useWeapon)
                    && !WeaponLaunchResolver.isSupported(releasedItem)) {
                arm(player, useWeapon, now);
            }
        }
        wasUsingItem = false;
        useWeapon = null;
        useWeaponEligible = false;
    }

    private static void discoverProjectile(LocalPlayer player, long now) {
        if (pending == null || now > pending.deadline()) {
            return;
        }
        AABB search = player.getBoundingBox().inflate(DISCOVERY_RADIUS);
        List<Projectile> projectiles = player.level().getEntitiesOfClass(Projectile.class, search,
                projectile -> projectile.tickCount <= 4 && belongsToTrigger(player, projectile, pending));
        for (Projectile projectile : projectiles) {
            if (!SEEN_PROJECTILES.add(projectile.getId())) {
                continue;
            }
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(projectile.getType());
            TRACKS.put(projectile.getId(), new SampleTrack(
                    pending.weaponId(),
                    type == null ? projectile.getType().toString() : type.toString(),
                    now,
                    projectile.position()
            ));
            RuntimeDiagnostics.info("projectile_ballistics_observe", "weapon=" + pending.weaponId()
                    + " projectile=" + (type == null ? projectile.getType() : type)
                    + " entityId=" + projectile.getId());
            pending = null;
            break;
        }
    }

    private static void sampleProjectiles(Minecraft minecraft, long now) {
        Iterator<Map.Entry<Integer, SampleTrack>> iterator = TRACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, SampleTrack> entry = iterator.next();
            SampleTrack track = entry.getValue();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof Projectile projectile && !projectile.isRemoved()) {
                track.sample(projectile.position());
            }
            boolean expired = now - track.startedAt() >= SAMPLE_WINDOW_TICKS;
            if (!track.ready() && !expired && entity != null) {
                continue;
            }
            iterator.remove();
            if (!track.ready()) {
                RuntimeDiagnostics.info("projectile_ballistics_discard", "weapon=" + track.weaponId()
                        + " reason=incomplete_samples speedSamples=" + track.speedCount()
                        + " gravitySamples=" + track.gravityCount());
                continue;
            }
            ProjectileBallistics learned = track.finish();
            if (learned == null || !canLearn(track.weaponId())
                    || AimWeaponSettingsStore.get(track.weaponId()).ballistics() != null) {
                continue;
            }
            confirmProfile(track, learned);
        }
    }

    private static boolean belongsToTrigger(LocalPlayer player, Projectile projectile, PendingTrigger trigger) {
        Entity owner = projectile.getOwner();
        if (owner != null) {
            return owner.getUUID().equals(player.getUUID());
        }
        Vec3 offset = projectile.position().subtract(trigger.origin());
        double distance = offset.length();
        if (distance > DISCOVERY_RADIUS) {
            return false;
        }
        return distance < 0.25D || offset.scale(1.0D / distance).dot(trigger.direction()) >= MINIMUM_DIRECTION_DOT;
    }

    private static void confirmProfile(SampleTrack track, ProjectileBallistics learned) {
        ProfileCandidate previous = PROFILE_CANDIDATES.get(track.weaponId());
        if (previous == null || !previous.profile().projectileType().equals(learned.projectileType())) {
            PROFILE_CANDIDATES.put(track.weaponId(), new ProfileCandidate(learned, 1));
            RuntimeDiagnostics.info("projectile_ballistics_confirmation", "weapon=" + track.weaponId()
                    + " projectile=" + learned.projectileType() + " confirmations=1/" + PROJECTILE_CONFIRMATIONS);
            return;
        }

        int confirmations = previous.confirmations() + 1;
        ProjectileBallistics combined = ProjectileBallistics.detected(learned.projectileType(),
                (previous.profile().speed() + learned.speed()) * 0.5D,
                (previous.profile().gravity() + learned.gravity()) * 0.5D);
        if (confirmations < PROJECTILE_CONFIRMATIONS) {
            PROFILE_CANDIDATES.put(track.weaponId(), new ProfileCandidate(combined, confirmations));
            return;
        }

        PROFILE_CANDIDATES.remove(track.weaponId());
        AimWeaponSettingsStore.putBallistics(track.weaponId(), combined);
        RuntimeDiagnostics.info("projectile_ballistics_cached", "weapon=" + track.weaponId()
                + " projectile=" + combined.projectileType() + " speed=" + combined.speed()
                + " gravity=" + combined.gravity() + " confirmations=" + confirmations
                + " speedSamples=" + track.speedCount() + " gravitySamples=" + track.gravityCount());
    }

    private static void arm(LocalPlayer player, ResourceLocation weaponId, long now) {
        if (!canLearn(weaponId)) {
            return;
        }
        if (pending != null && now <= pending.deadline() && !pending.weaponId().equals(weaponId)) {
            RuntimeDiagnostics.infoRateLimited("projectile-trigger-busy", 500L,
                    "projectile_ballistics_trigger_deferred", () -> "armedWeapon=" + pending.weaponId()
                            + " ignoredWeapon=" + weaponId + " remainingTicks=" + (pending.deadline() - now));
            return;
        }
        Vec3 direction = player.getLookAngle().normalize();
        pending = new PendingTrigger(weaponId, now, now + DETECTION_WINDOW_TICKS,
                player.getEyePosition().add(direction.scale(0.25D)), direction);
        RuntimeDiagnostics.infoRateLimited("projectile-trigger-" + weaponId, 500L,
                "projectile_ballistics_trigger", () -> "weapon=" + weaponId + " deadline="
                        + (now + DETECTION_WINDOW_TICKS));
    }

    private static void reset(Object newLevelIdentity) {
        levelIdentity = newLevelIdentity;
        pending = null;
        useWeapon = null;
        useWeaponEligible = false;
        wasUsingItem = false;
        TRACKS.clear();
        PROFILE_CANDIDATES.clear();
        SEEN_PROJECTILES.clear();
    }

    public static void resetLearning(ResourceLocation weaponId) {
        PROFILE_CANDIDATES.remove(weaponId);
        if (pending != null && pending.weaponId().equals(weaponId)) {
            pending = null;
        }
        TRACKS.values().removeIf(track -> track.weaponId().equals(weaponId));
    }

    private static boolean canLearn(ResourceLocation weaponId) {
        AimWeaponSettings settings = AimWeaponSettingsStore.get(weaponId);
        return TrajectoryFeaturePolicy.isAvailable(ClientGrowthData.isEnabled(), settings.trajectoryVisible());
    }

    private record PendingTrigger(ResourceLocation weaponId, long armedAt, long deadline, Vec3 origin, Vec3 direction) {
    }

    private record ProfileCandidate(ProjectileBallistics profile, int confirmations) {
    }

    private static final class SampleTrack {
        private final ResourceLocation weaponId;
        private final String projectileType;
        private final long startedAt;
        private final List<Double> speeds = new ArrayList<>();
        private final List<Double> gravities = new ArrayList<>();
        private Vec3 previousPosition;
        private Double previousVerticalSpeed;

        private SampleTrack(ResourceLocation weaponId, String projectileType, long startedAt, Vec3 position) {
            this.weaponId = weaponId;
            this.projectileType = projectileType;
            this.startedAt = startedAt;
            this.previousPosition = position;
        }

        private void sample(Vec3 position) {
            Vec3 movement = position.subtract(previousPosition);
            previousPosition = position;
            double speed = movement.length();
            if (!Double.isFinite(speed) || speed < 1.0E-3D || speed > 20.0D) {
                return;
            }
            speeds.add(speed);
            if (previousVerticalSpeed != null) {
                double gravity = previousVerticalSpeed - movement.y;
                if (Double.isFinite(gravity) && gravity >= 0.0D && gravity <= 2.0D) {
                    gravities.add(gravity);
                }
            }
            previousVerticalSpeed = movement.y;
        }

        private boolean ready() {
            return speeds.size() >= MINIMUM_SPEED_SAMPLES && gravities.size() >= MINIMUM_GRAVITY_SAMPLES;
        }

        private ProjectileBallistics finish() {
            if (!ready()) {
                return null;
            }
            return ProjectileBallistics.detected(projectileType, median(speeds), median(gravities));
        }

        private static double median(List<Double> values) {
            List<Double> sorted = new ArrayList<>(values);
            sorted.sort(Double::compareTo);
            int middle = sorted.size() / 2;
            return sorted.size() % 2 == 0
                    ? (sorted.get(middle - 1) + sorted.get(middle)) * 0.5D
                    : sorted.get(middle);
        }

        private ResourceLocation weaponId() {
            return weaponId;
        }

        private long startedAt() {
            return startedAt;
        }

        private int speedCount() {
            return speeds.size();
        }

        private int gravityCount() {
            return gravities.size();
        }
    }
}
