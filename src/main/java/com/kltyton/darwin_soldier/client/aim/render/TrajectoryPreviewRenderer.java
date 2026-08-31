package com.kltyton.darwin_soldier.client.aim.render;

import com.kltyton.darwin_soldier.client.SuperPerceptionClient;
import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettings;
import com.kltyton.darwin_soldier.client.aim.AimWeaponSettingsStore;
import com.kltyton.darwin_soldier.client.aim.TrajectoryFeaturePolicy;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileLaunchState;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallistics;
import com.kltyton.darwin_soldier.client.aim.ballistics.WeaponLaunchResolver;
import com.kltyton.darwin_soldier.client.aim.ballistics.VanillaProjectilePhysics;
import com.kltyton.darwin_soldier.client.aim.render.trajectory.TrajectoryShrinkState;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class TrajectoryPreviewRenderer {
    private static final int MAX_SEGMENTS = 80;
    private static final double MAX_DISTANCE_SQR = 96.0D * 96.0D;
    private static final TrajectoryShrinkState TRAJECTORY_SHRINK = new TrajectoryShrinkState();

    private TrajectoryPreviewRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        long nowNanos = System.nanoTime();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            TRAJECTORY_SHRINK.update(null, false, nowNanos);
            return;
        }
        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        if (!TrajectoryFeaturePolicy.isEligibleWeapon(mainHandItem.has(DataComponents.FOOD))) {
            TRAJECTORY_SHRINK.update(null, false, nowNanos);
            return;
        }
        AimWeaponSettings settings = AimWeaponSettingsStore.get(mainHandItem);
        if (!TrajectoryFeaturePolicy.isAvailable(ClientGrowthData.isEnabled(), settings.trajectoryVisible())) {
            TRAJECTORY_SHRINK.update(null, false, nowNanos);
            return;
        }
        boolean liveSupported = WeaponLaunchResolver.isSupported(mainHandItem);
        ProjectileLaunchState liveLaunch = WeaponLaunchResolver.resolve(
                minecraft.player, mainHandItem, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        boolean mainHandUse = minecraft.player.isUsingItem()
                && minecraft.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        boolean supportedWeapon = liveLaunch != null || !liveSupported
                && settings.ballistics() != null && !settings.ballistics().hitscan();
        boolean weaponTrigger = TrajectoryFeaturePolicy.shouldTrigger(
                mainHandUse,
                minecraft.player.isUsingItem(),
                minecraft.options.keyUse.isDown(),
                minecraft.options.keyAttack.isDown(),
                SuperPerceptionClient.isAimingActive());
        if (!supportedWeapon || !weaponTrigger) {
            TRAJECTORY_SHRINK.update(null, false, nowNanos);
            return;
        }

        ProjectileLaunchState launch = liveLaunch == null
                ? learnedFallback(minecraft, settings, event.getPartialTick().getGameTimeDeltaPartialTick(false))
                : liveLaunch;
        Vec3 origin = launch.origin();
        Vec3 velocity = launch.velocity();

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f pose = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());

        Vec3 previous = origin;
        boolean collided = false;
        int renderedSegments = 0;
        int renderedMarkers = 0;
        ResourceLocation weaponId = AimWeaponSettingsStore.weaponId(mainHandItem);
        double radiusScale = TRAJECTORY_SHRINK.update(
                weaponId == null ? null : weaponId.toString(), true, nowNanos);
        for (int segment = 1; segment <= MAX_SEGMENTS; segment++) {
            double drag = WeaponLaunchResolver.isInWater(minecraft.player, previous)
                    ? launch.waterDrag() : launch.airDrag();
            VanillaProjectilePhysics.Step step = VanillaProjectilePhysics.advance(
                    previous, velocity, drag, launch.gravity());
            Vec3 next = step.position();
            if (next.distanceToSqr(origin) > MAX_DISTANCE_SQR) {
                break;
            }
            BlockHitResult hit = minecraft.level.clip(new ClipContext(
                    previous, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
            boolean blockCollision = hit.getType() != HitResult.Type.MISS;
            Vec3 blockEnd = blockCollision ? hit.getLocation() : next;
            EntityHitResult entityHit = findEntityHit(minecraft, previous, blockEnd);
            collided = blockCollision || entityHit != null;
            Vec3 end = entityHit == null ? blockEnd : entityHit.getLocation();
            renderedMarkers += TrajectoryQuadRenderer.emitSegment(quads, pose, previous, end, collided, radiusScale);
            renderedSegments++;
            previous = end;
            if (collided) {
                break;
            }
            velocity = step.velocity();
        }

        buffers.endBatch(RenderType.debugQuads());
        poseStack.popPose();
        boolean finalCollision = collided;
        int finalSegments = renderedSegments;
        int finalMarkers = renderedMarkers;
        RuntimeDiagnostics.infoRateLimited("trajectory-preview-" + launch.source(), 1000L,
                "trajectory_preview_render", () -> "weapon="
                        + weaponId
                        + " source=" + launch.source() + " speed=" + launch.velocity().length()
                        + " gravity=" + launch.gravity() + " drag=" + launch.airDrag()
                        + " spread=" + launch.spreadDegrees()
                        + " renderer=translucent_quads segments=" + finalSegments
                        + " radiusScale=" + radiusScale
                        + " markers=" + finalMarkers + " collision=" + finalCollision);
    }

    private static ProjectileLaunchState learnedFallback(
            Minecraft minecraft,
            AimWeaponSettings settings,
            float partialTick
    ) {
        ProjectileBallistics learned = settings.ballistics();
        boolean hitscan = learned == null || learned.hitscan();
        double speed = hitscan ? 4.0D : learned.speed();
        double gravity = hitscan ? 0.0D : learned.gravity();
        Vec3 direction = minecraft.player.getViewVector(partialTick).normalize();
        Vec3 origin = minecraft.player.getPosition(partialTick)
                .add(0.0D, minecraft.player.getEyeHeight() - 0.1D, 0.0D);
        Vec3 shooterVelocity = minecraft.player.getDeltaMovement();
        Vec3 inheritedVelocity = new Vec3(shooterVelocity.x,
                minecraft.player.onGround() ? 0.0D : shooterVelocity.y, shooterVelocity.z);
        Vec3 velocity = direction.scale(speed).add(inheritedVelocity);
        return new ProjectileLaunchState(hitscan ? "fallback_hitscan" : "learned_" + learned.projectileType(),
                origin, velocity, inheritedVelocity, gravity, hitscan ? 1.0D : VanillaProjectilePhysics.AIR_DRAG,
                hitscan ? 1.0D : VanillaProjectilePhysics.THROWABLE_WATER_DRAG, hitscan);
    }

    private static EntityHitResult findEntityHit(Minecraft minecraft, Vec3 start, Vec3 end) {
        AABB sweptArea = new AABB(start, end).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                minecraft.level,
                minecraft.player,
                start,
                end,
                sweptArea,
                TrajectoryPreviewRenderer::canHit,
                0.3F
        );
    }

    private static boolean canHit(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return entity != minecraft.player && !entity.isSpectator() && entity.isPickable();
    }
}
