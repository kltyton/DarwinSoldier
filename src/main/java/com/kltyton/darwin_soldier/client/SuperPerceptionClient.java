package com.kltyton.darwin_soldier.client;

import com.kltyton.darwin_soldier.client.aim.AimMode;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettings;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettingsStore;
import com.kltyton.darwin_soldier.client.aim.AimWeaponTuning;
import com.kltyton.darwin_soldier.client.aim.ballistics.AimSolution;
import com.kltyton.darwin_soldier.client.aim.ballistics.BallisticAimSolver;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallistics;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileLaunchState;
import com.kltyton.darwin_soldier.client.aim.ballistics.WeaponLaunchResolver;
import com.kltyton.darwin_soldier.client.aim.control.AimDirection;
import com.kltyton.darwin_soldier.client.aim.control.AimInputController;
import com.kltyton.darwin_soldier.client.aim.control.AimRotation;
import com.kltyton.darwin_soldier.client.aim.control.AimRotationSmoother;
import com.kltyton.darwin_soldier.client.aim.control.MouseInputAngleConverter;
import com.kltyton.darwin_soldier.client.aim.control.TargetCandidate;
import com.kltyton.darwin_soldier.client.aim.control.TargetSwitchSelector;
import com.kltyton.darwin_soldier.client.aim.spread.SpreadAimSelection;
import com.kltyton.darwin_soldier.client.aim.spread.SpreadAimSelector;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SuperPerceptionClient {
    private static final int MOTION_SAMPLE_TICKS = 10;
    /** Same 12 degrees-per-tick ceiling as before, expressed per second. */
    private static final double AIM_MAX_DEGREES_PER_SECOND = 240.0D;
    private static final double FIRST_FRAME_DELTA_SECONDS = 1.0D / 60.0D;
    private static final double MAX_RENDER_DELTA_SECONDS = AimRotationSmoother.MAX_FRAME_DELTA_SECONDS;
    private static final double[][] AIM_SAMPLES = {
            {0.50D, 0.88D, 0.50D},
            {0.50D, 0.70D, 0.50D},
            {0.50D, 0.50D, 0.50D},
            {0.50D, 0.30D, 0.50D},
            {0.25D, 0.70D, 0.50D},
            {0.75D, 0.70D, 0.50D},
            {0.50D, 0.70D, 0.25D},
            {0.50D, 0.70D, 0.75D}
    };
    private static final ArrayDeque<Vec3> TARGET_POSITIONS = new ArrayDeque<>();
    private static int targetId = -1;
    private static int motionTargetId = -1;
    private static boolean aimingActive;
    private static final AimInputController AIM_INPUT = new AimInputController();
    private static AimRotationSmoother aimSmoother;
    private static float desiredYaw;
    private static float desiredPitch;
    private static float desiredResponse;
    private static boolean hasDesiredAim;
    private static long lastAimFrameNanos = -1L;

    private SuperPerceptionClient() {
    }

    public static void tick(Minecraft minecraft, boolean held) {
        LocalPlayer player = minecraft.player;
        boolean active = held && player != null && minecraft.level != null
                && ClientGrowthData.isSuperPerceptionUnlocked()
                && ClientGrowthData.isSuperPerceptionEnabled();
        if (active != aimingActive) {
            RuntimeDiagnostics.info("auto_aim_state", "active=" + active + " held=" + held
                    + " playerPresent=" + (player != null) + " levelPresent=" + (minecraft.level != null)
                    + " unlocked=" + ClientGrowthData.isSuperPerceptionUnlocked()
                    + " enabled=" + ClientGrowthData.isSuperPerceptionEnabled());
            if (active) {
                startAimSession(player);
            } else {
                endAimSession();
            }
            aimingActive = active;
        }
        if (!active) {
            clearTarget("aim_inactive");
            return;
        }

        LivingEntity target = getCurrentTarget(minecraft, player);
        if (target == null) {
            target = findTarget(minecraft, player);
            targetId = target == null ? -1 : target.getId();
            if (target == null) {
                RuntimeDiagnostics.infoRateLimited("auto-aim-no-target", 2000L, "auto_aim_no_target",
                        () -> "player=" + player.getName().getString() + " range="
                                + DarwinConfig.SUPER_PERCEPTION_DISTANCE.get() + " angle="
                                + DarwinConfig.SUPER_PERCEPTION_ANGLE.get());
            } else {
                RuntimeDiagnostics.info("auto_aim_target_acquired", "target=" + describeEntity(target)
                        + " distance=" + player.distanceTo(target));
            }
        }
        if (target != null) {
            target = handleManualSwitch(minecraft, player, target);
            if (target == null) {
                return;
            }
            Vec3 visiblePoint = findVisibleAimPoint(player, target);
            if (visiblePoint == null) {
                clearTarget("all_aim_points_occluded");
                return;
            }
            updateMotionHistory(target);
            aimAt(player, target, visiblePoint, AimWeaponSettingsStore.get(player.getMainHandItem()));
        }
    }

    private static LivingEntity getCurrentTarget(Minecraft minecraft, LocalPlayer player) {
        if (targetId < 0) {
            return null;
        }
        if (!(minecraft.level.getEntity(targetId) instanceof LivingEntity target) || !isValidTarget(player, target, false)) {
            clearTarget("target_invalid_or_out_of_range");
            return null;
        }
        return target;
    }

    private static LivingEntity findTarget(Minecraft minecraft, LocalPlayer player) {
        double range = DarwinConfig.SUPER_PERCEPTION_DISTANCE.get();
        AABB area = player.getBoundingBox().inflate(range);
        return minecraft.level.getEntitiesOfClass(LivingEntity.class, area, target -> isValidTarget(player, target, true))
                .stream()
                .min(Comparator
                        .comparing((LivingEntity target) -> !isWhitelisted(target))
                        .thenComparingDouble(target -> aimScore(player, target)))
                .orElse(null);
    }

    private static boolean isValidTarget(LocalPlayer player, LivingEntity target, boolean requireAngle) {
        if (target == player || !target.isAlive() || target.isSpectator()
                || player.distanceTo(target) > DarwinConfig.SUPER_PERCEPTION_DISTANCE.get()
                || findVisibleAimPoint(player, target) == null) {
            return false;
        }
        if (DarwinConfig.SUPER_PERCEPTION_FILTER_PETS.get() && isOwnedPet(target)) {
            return false;
        }
        if (!isWhitelisted(target) && !categoryEnabled(target)) {
            return false;
        }
        return !requireAngle || angleFromCrosshair(player, target) <= DarwinConfig.SUPER_PERCEPTION_ANGLE.get();
    }

    private static boolean categoryEnabled(LivingEntity target) {
        if (target instanceof Player) {
            return DarwinConfig.SUPER_PERCEPTION_TARGET_PLAYERS.get();
        }
        if (target instanceof NeutralMob) {
            return DarwinConfig.SUPER_PERCEPTION_TARGET_NEUTRAL.get();
        }
        if (target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER) {
            return DarwinConfig.SUPER_PERCEPTION_TARGET_MONSTERS.get();
        }
        return DarwinConfig.SUPER_PERCEPTION_TARGET_PASSIVE.get();
    }

    private static boolean isOwnedPet(LivingEntity target) {
        if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return true;
        }
        return target instanceof AbstractHorse horse && horse.isTamed();
    }

    private static boolean isWhitelisted(LivingEntity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id == null) {
            return false;
        }
        String idText = id.toString().toLowerCase(Locale.ROOT);
        return DarwinConfig.SUPER_PERCEPTION_WHITELIST.get().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(idText::equals);
    }

    private static double aimScore(LocalPlayer player, LivingEntity target) {
        return angleFromCrosshair(player, target) * 100.0D + player.distanceTo(target) * 0.01D;
    }

    private static double angleFromCrosshair(LocalPlayer player, LivingEntity target) {
        Vec3 aimPoint = findVisibleAimPoint(player, target);
        if (aimPoint == null) {
            return 180.0D;
        }
        Vec3 direction = aimPoint.subtract(player.getEyePosition()).normalize();
        double dot = Mth.clamp(player.getLookAngle().normalize().dot(direction), -1.0D, 1.0D);
        return Math.toDegrees(Math.acos(dot));
    }

    private static void aimAt(LocalPlayer player, LivingEntity target, Vec3 visiblePoint, AimWeaponSettings settings) {
        AimWeaponTuning tuning = settings.activeTuning();
        List<Vec3> targetPositions = List.copyOf(TARGET_POSITIONS);
        ProjectileLaunchState liveLaunch = WeaponLaunchResolver.resolve(
                player, player.getMainHandItem(), 1.0F);
        ProjectileBallistics learnedBallistics = liveLaunch == null
                && !WeaponLaunchResolver.isSupported(player.getMainHandItem())
                ? settings.ballistics()
                : null;
        Vec3 solutionOrigin = player.getEyePosition();
        AimSolution solution;
        String ballisticsDescription;
        if (settings.mode() == AimMode.ADAPTIVE && liveLaunch != null) {
            boolean inWater = WeaponLaunchResolver.isInWater(player, liveLaunch.origin());
            solutionOrigin = liveLaunch.origin();
            solution = BallisticAimSolver.solveAdaptive(
                    liveLaunch, inWater, visiblePoint, targetPositions, tuning);
            ballisticsDescription = liveLaunch.source() + " launchSpeed=" + liveLaunch.launchSpeed()
                    + " inherited=" + liveLaunch.inheritedVelocity() + " gravity=" + liveLaunch.gravity()
                    + " drag=" + liveLaunch.drag(inWater);
        } else if (settings.mode() == AimMode.ADAPTIVE) {
            solution = BallisticAimSolver.solveAdaptive(
                    solutionOrigin, visiblePoint, targetPositions, learnedBallistics, tuning);
            ballisticsDescription = String.valueOf(learnedBallistics);
        } else {
            solution = BallisticAimSolver.solveManual(
                    solutionOrigin, visiblePoint, targetPositions, tuning);
            ballisticsDescription = "manual";
        }
        Vec3 finalSolutionOrigin = solutionOrigin;
        double spreadDegrees = 0.0D;
        Vec3 aimPoint = solution.aimPoint();
        SpreadAimSelection spreadSelection = null;
        if (settings.mode() == AimMode.ADAPTIVE && liveLaunch != null && liveLaunch.spreadDegrees() > 0.0D) {
            spreadDegrees = liveLaunch.spreadDegrees();
            AABB predictedAABB = target.getBoundingBox().move(solution.predictedTarget().subtract(visiblePoint));
            spreadSelection = SpreadAimSelector.select(solutionOrigin, solution.aimPoint(),
                    solution.predictedTarget(), predictedAABB, spreadDegrees);
            aimPoint = spreadSelection.aimPoint;
        }
        final double loggedSpreadDegrees = spreadDegrees;
        final SpreadAimSelection loggedSelection = spreadSelection;
        Vec3 direction = aimPoint.subtract(solutionOrigin);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float wantedYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D);
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
        float smooth = (float) solution.interpolation();
        storeDesiredSolution(wantedYaw, wantedPitch, smooth);
        RuntimeDiagnostics.infoRateLimited("auto-aim-track-" + target.getId(), 1000L, "auto_aim_track",
                () -> "target=" + describeEntity(target) + " distance="
                        + player.getEyePosition().distanceTo(visiblePoint)
                         + " samples=" + TARGET_POSITIONS.size() + " velocity=" + solution.targetVelocity()
                         + " acceleration=" + solution.targetAcceleration()
                         + " predictedTarget=" + solution.predictedTarget()
                         + " spreadDegrees=" + loggedSpreadDegrees
                         + (loggedSelection == null ? "" : " spreadCandidate=" + loggedSelection.candidate
                                + "/" + loggedSelection.candidateIndex
                                + " hitWeight=" + loggedSelection.hitWeight)
                         + " flightTicks=" + solution.flightTicks() + " mode=" + settings.mode()
                         + " ballistics=" + ballisticsDescription + " tuning=" + tuning
                         + " solutionOrigin=" + finalSolutionOrigin + " visiblePoint=" + visiblePoint
                         + " wantedYawPitch=" + wantedYaw + "/" + wantedPitch
                        + " appliedYawPitch=" + player.getYRot() + "/" + player.getXRot());
    }

    /**
     * Stores the latest desired aim solution without touching player rotation.
     * The actual rotation write happens at frame rate in
     * {@link #renderFrame(Minecraft)}, which is the sole automatic writer while
     * a valid lock and desired solution exist.
     */
    private static void storeDesiredSolution(float yaw, float pitch, float response) {
        desiredYaw = yaw;
        desiredPitch = pitch;
        desiredResponse = response;
        hasDesiredAim = true;
    }

    /**
     * Applies the latest desired rotation once per rendered frame. Invoked from
     * the Forge render tick START event; delta time comes from
     * {@link System#nanoTime()} (first frame 1/60 s, later deltas clamped so a
     * pause cannot snap), so rotation advances smoothly at any frame rate
     * instead of stepping at the 20 Hz client tick rate. This is the sole
     * automatic writer of the player's aim rotation while an aim session is
     * active, a locked target exists, and tick/aimAt has stored a desired
     * solution. Previous-rotation fields are shifted by the same deltas before
     * the current rotations are set, so Entity#getViewYRot(partialTick) keeps
     * lerping between frame-updated values instead of re-quantizing at the
     * 20 Hz tick rate.
     */
    public static void renderFrame(Minecraft minecraft) {
        if (!aimingActive || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (aimSmoother == null || !hasDesiredAim || getLockedTarget(minecraft) == null) {
            return;
        }
        double deltaSeconds = frameDeltaSeconds();
        AimRotation smoothed = aimSmoother.updateFrame(
                desiredYaw, desiredPitch, desiredResponse, AIM_MAX_DEGREES_PER_SECOND, deltaSeconds);
        LocalPlayer player = minecraft.player;
        float yaw = (float) smoothed.yaw();
        float pitch = (float) smoothed.pitch();
        RuntimeDiagnostics.infoRateLimited("auto-aim-frame", 1000L, "auto_aim_frame",
                () -> "deltaSeconds=" + deltaSeconds
                        + " wantedYawPitch=" + desiredYaw + "/" + desiredPitch
                        + " appliedYawPitch=" + yaw + "/" + pitch);
        float deltaYaw = Mth.wrapDegrees(yaw - player.getYRot());
        float deltaPitch = pitch - player.getXRot();
        float deltaHeadYaw = Mth.wrapDegrees(yaw - player.yHeadRot);
        player.yRotO += deltaYaw;
        player.xRotO += deltaPitch;
        player.yHeadRotO += deltaHeadYaw;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yHeadRot = yaw;
    }

    private static double frameDeltaSeconds() {
        long now = System.nanoTime();
        if (lastAimFrameNanos < 0L) {
            lastAimFrameNanos = now;
            return FIRST_FRAME_DELTA_SECONDS;
        }
        double deltaSeconds = (now - lastAimFrameNanos) / 1_000_000_000.0D;
        lastAimFrameNanos = now;
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0D) {
            return FIRST_FRAME_DELTA_SECONDS;
        }
        return Math.min(deltaSeconds, MAX_RENDER_DELTA_SECONDS);
    }

    /**
     * Mouse-only input ownership. Invoked from the MouseHandler turnPlayer
     * mixin at HEAD with the raw accumulated deltas. The cheap lock snapshot is
     * used here because this runs per mouse event; visibility, range, and
     * category are re-validated authoritatively by tick. Returns true only
     * while an aim session is active and a locked living target exists, in
     * which case the caller must zero the accumulated deltas and reset the
     * smooth turn state so vanilla continues with zero input.
     */
    public static boolean captureMouseInput(Minecraft minecraft, double rawDX, double rawDY) {
        if (!aimingActive || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (!(getLockedTarget(minecraft) instanceof LivingEntity)) {
            return false;
        }
        MouseInputAngleConverter.RotationDelta delta = MouseInputAngleConverter.convert(
                rawDX, rawDY, minecraft.options.sensitivity().get(),
                minecraft.options.invertYMouse().get());
        AIM_INPUT.addInput(delta.yawDegrees(), delta.pitchDegrees());
        return true;
    }

    private static void startAimSession(LocalPlayer player) {
        aimSmoother = new AimRotationSmoother(player.getYRot(), player.getXRot());
        AIM_INPUT.reset();
        clearDesiredFrameState();
        RuntimeDiagnostics.info("auto_aim_session", "started yaw=" + player.getYRot()
                + " pitch=" + player.getXRot());
    }

    private static void endAimSession() {
        aimSmoother = null;
        AIM_INPUT.reset();
        clearDesiredFrameState();
        RuntimeDiagnostics.info("auto_aim_session", "ended");
    }

    private static void clearDesiredFrameState() {
        desiredYaw = 0.0F;
        desiredPitch = 0.0F;
        desiredResponse = 0.0F;
        hasDesiredAim = false;
        lastAimFrameNanos = -1L;
    }

    private static LivingEntity handleManualSwitch(Minecraft minecraft, LocalPlayer player,
                                                   LivingEntity current) {
        if (!AIM_INPUT.pollSwitchIntent()) {
            return current;
        }
        Vec3 cursorDirection = Vec3.directionFromRotation(
                (float) (player.getXRot() + AIM_INPUT.pitchOffsetDegrees()),
                (float) (player.getYRot() + AIM_INPUT.yawOffsetDegrees()));
        AimDirection cursor = AimDirection.of(cursorDirection.x, cursorDirection.y, cursorDirection.z);
        List<TargetCandidate> candidates = new ArrayList<>();
        AABB area = player.getBoundingBox().inflate(DarwinConfig.SUPER_PERCEPTION_DISTANCE.get());
        for (LivingEntity candidate : minecraft.level.getEntitiesOfClass(
                LivingEntity.class, area, target -> isValidTarget(player, target, false))) {
            Vec3 point = findVisibleAimPoint(player, candidate);
            if (point == null) {
                continue;
            }
            Vec3 direction = point.subtract(player.getEyePosition());
            if (direction.lengthSqr() <= 1.0E-12D) {
                continue;
            }
            Vec3 normalized = direction.normalize();
            candidates.add(new TargetCandidate(candidate.getId(),
                    AimDirection.of(normalized.x, normalized.y, normalized.z)));
        }
        Optional<TargetCandidate> selection = TargetSwitchSelector.select(
                candidates, current.getId(), cursor, DarwinConfig.SUPER_PERCEPTION_ANGLE.get());
        if (selection.isEmpty() || selection.get().id() == current.getId()) {
            AIM_INPUT.markSwitchFailed();
            RuntimeDiagnostics.infoRateLimited("auto-aim-switch-failed", 1000L, "auto_aim_switch_failed",
                    () -> "cursor=" + cursorDirection + " candidates=" + candidates.size()
                            + " offsetYawPitch=" + AIM_INPUT.yawOffsetDegrees() + "/"
                            + AIM_INPUT.pitchOffsetDegrees());
            return current;
        }
        TargetCandidate chosen = selection.get();
        int previousId = current.getId();
        targetId = chosen.id();
        motionTargetId = -1;
        TARGET_POSITIONS.clear();
        AIM_INPUT.reset();
        RuntimeDiagnostics.info("auto_aim_switch", "from=" + previousId + " to=" + chosen.id()
                + " cursor=" + cursorDirection + " candidates=" + candidates.size());
        Entity switched = minecraft.level.getEntity(chosen.id());
        return switched instanceof LivingEntity living ? living : current;
    }

    /**
     * Exact lock snapshot API used by glow and bracket rendering. Returns true
     * only while aiming, the entity id matches the current target, and the
     * entity is a present living/alive valid client entity.
     */
    public static boolean isLockedTarget(Entity entity) {
        return aimingActive && entity != null && targetId >= 0
                && entity.getId() == targetId && isValidLockedEntity(entity);
    }

    /**
     * Returns the current locked client entity, or null when the session is
     * inactive, the target id is stale, or the entity is no longer valid.
     */
    public static @Nullable Entity getLockedTarget(Minecraft minecraft) {
        if (!aimingActive || targetId < 0 || minecraft == null || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(targetId);
        return isValidLockedEntity(entity) ? entity : null;
    }

    private static boolean isValidLockedEntity(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator();
    }

    private static Vec3 findVisibleAimPoint(LocalPlayer player, LivingEntity target) {
        Vec3 preferred = isHumanoid(target)
                ? new Vec3(target.getX(), target.getEyeY(), target.getZ())
                : target.getBoundingBox().getCenter();
        if (isVisible(player, preferred)) {
            return preferred;
        }

        AABB box = target.getBoundingBox();
        for (int index = 0; index < AIM_SAMPLES.length; index++) {
            double[] sample = AIM_SAMPLES[index];
            Vec3 point = new Vec3(
                    Mth.lerp(sample[0], box.minX, box.maxX),
                    Mth.lerp(sample[1], box.minY, box.maxY),
                    Mth.lerp(sample[2], box.minZ, box.maxZ)
            );
            if (isVisible(player, point)) {
                int sampleIndex = index;
                RuntimeDiagnostics.infoRateLimited("auto-aim-fallback-" + target.getId(), 2000L,
                        "auto_aim_visible_fallback", () -> "target=" + describeEntity(target)
                                + " preferred=" + preferred + " sampleIndex=" + sampleIndex + " point=" + point);
                return point;
            }
        }
        RuntimeDiagnostics.infoRateLimited("auto-aim-occluded-" + target.getId(), 2000L,
                "auto_aim_occluded", () -> "target=" + describeEntity(target)
                        + " preferred=" + preferred + " samples=" + AIM_SAMPLES.length);
        return null;
    }

    private static boolean isVisible(LocalPlayer player, Vec3 point) {
        HitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) <= 0.01D;
    }

    private static void updateMotionHistory(LivingEntity target) {
        if (motionTargetId != target.getId()) {
            TARGET_POSITIONS.clear();
            motionTargetId = target.getId();
        }
        TARGET_POSITIONS.addLast(target.position());
        while (TARGET_POSITIONS.size() > MOTION_SAMPLE_TICKS) {
            TARGET_POSITIONS.removeFirst();
        }
    }

    private static void clearTarget(String reason) {
        if (targetId >= 0) {
            RuntimeDiagnostics.info("auto_aim_target_cleared", "targetId=" + targetId + " reason=" + reason
                    + " motionSamples=" + TARGET_POSITIONS.size());
        }
        targetId = -1;
        motionTargetId = -1;
        TARGET_POSITIONS.clear();
        AIM_INPUT.reset();
        clearDesiredFrameState();
    }

    public static boolean isAimingActive() {
        return aimingActive;
    }

    private static boolean isHumanoid(LivingEntity target) {
        return target instanceof Player
                || target instanceof Zombie
                || target instanceof AbstractSkeleton
                || target instanceof AbstractPiglin
                || target instanceof AbstractIllager
                || target instanceof Villager;
    }

    private static String describeEntity(LivingEntity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        return (id == null ? target.getType().toString() : id.toString()) + "#" + target.getId();
    }
}
