package com.kltyton.darwin_soldier.client.aim.render.lock;

import com.kltyton.darwin_soldier.client.SuperPerceptionClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders a camera-facing two-layer sci-fi bracket around exactly the entity
 * reported by {@link SuperPerceptionClient#getLockedTarget}. The bracket keeps
 * the four-corner/eight-strip layout, is centered on the interpolated
 * bounding-box center of the target (never the entity feet), and disappears
 * automatically as soon as the locked target becomes null. The visual design
 * replaces the old red wireframe with three constant-geometry layers, all in
 * pure position-color quads: a thicker near-black navy translucent backing, a
 * thinner amber-gold foreground, and a subtle pale-gold accent node on each
 * outer corner. A pure sine pulse breathes the gap around its base of
 * 0.25 blocks by +/-0.025 blocks over about 1.6 seconds; there is no flashing,
 * alpha strobe, rotation, or view obstruction.
 *
 * <p>The three layers are submitted as three ordered translucent batches so
 * backing renders before foreground before accents, and each layer is offset
 * slightly along the camera forward axis (backing away from the camera, accent
 * toward it) so the LEQUAL depth test deterministically keeps the layers
 * stacked correctly. Geometry stays constant: 8 backing + 8 foreground + 4
 * accent quads per frame, and exactly one {@link System#nanoTime()} sample is
 * taken per render.
 */
public final class TargetLockRenderer {
    /** Minimum half-extent so tiny entities keep a readable bracket. */
    private static final double MIN_HALF_WIDTH = 0.4D;
    private static final double MIN_HALF_HEIGHT = 0.6D;
    /** Backing layer: near-black navy (#0A0E1A), 43% alpha. */
    private static final int BACKING_RED = 10;
    private static final int BACKING_GREEN = 14;
    private static final int BACKING_BLUE = 26;
    private static final int BACKING_ALPHA = 110;
    /** Foreground layer: amber-gold (#F4AC28), 92% alpha. */
    private static final int FOREGROUND_RED = 244;
    private static final int FOREGROUND_GREEN = 172;
    private static final int FOREGROUND_BLUE = 40;
    private static final int FOREGROUND_ALPHA = 235;
    /** Accent layer: pale gold (#FFE696), 82% alpha. */
    private static final int ACCENT_RED = 255;
    private static final int ACCENT_GREEN = 230;
    private static final int ACCENT_BLUE = 150;
    private static final int ACCENT_ALPHA = 210;
    /** View-axis offsets in blocks: backing farthest, accent nearest. */
    private static final double BACKING_VIEW_OFFSET = 0.02D;
    private static final double ACCENT_VIEW_OFFSET = -0.02D;

    private TargetLockRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        Entity target = SuperPerceptionClient.getLockedTarget(minecraft);
        if (target == null || !target.isAlive()) {
            return;
        }

        float partialTick = event.getPartialTick();
        AABB box = target.getBoundingBox()
                .move(target.getPosition(partialTick).subtract(target.position()));
        Vec3 center = box.getCenter();
        double halfWidth = Math.max(box.getXsize() * 0.5D, MIN_HALF_WIDTH);
        double halfHeight = Math.max(box.getYsize() * 0.5D, MIN_HALF_HEIGHT);

        long nowNanos = System.nanoTime();
        double gap = LockBracketMath.pulseGap(
                LockBracketMath.DEFAULT_GAP, nowNanos / 1.0E9D);

        List<LockBracketMath.Segment> backing = LockBracketMath.bracket(
                halfWidth, halfHeight, gap, LockBracketMath.DEFAULT_ARM_LENGTH,
                LockBracketMath.BACKING_STRIP_THICKNESS);
        List<LockBracketMath.Segment> foreground = LockBracketMath.bracket(
                halfWidth, halfHeight, gap, LockBracketMath.DEFAULT_ARM_LENGTH,
                LockBracketMath.FOREGROUND_STRIP_THICKNESS);
        List<LockBracketMath.Segment> accents = LockBracketMath.cornerAccents(
                halfWidth, halfHeight, gap, LockBracketMath.DEFAULT_ACCENT_SIZE);

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f pose = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        emitLayerBatch(buffers, pose, camera, center, backing, BACKING_VIEW_OFFSET,
                BACKING_RED, BACKING_GREEN, BACKING_BLUE, BACKING_ALPHA);
        emitLayerBatch(buffers, pose, camera, center, foreground, 0.0D,
                FOREGROUND_RED, FOREGROUND_GREEN, FOREGROUND_BLUE, FOREGROUND_ALPHA);
        emitLayerBatch(buffers, pose, camera, center, accents, ACCENT_VIEW_OFFSET,
                ACCENT_RED, ACCENT_GREEN, ACCENT_BLUE, ACCENT_ALPHA);
        poseStack.popPose();
    }

    private static void emitLayerBatch(MultiBufferSource.BufferSource buffers, Matrix4f pose,
                                       Camera camera, Vec3 center,
                                       List<LockBracketMath.Segment> segments,
                                       double viewOffset, int red, int green, int blue, int alpha) {
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        Vector3f left = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        Vector3f lookVector = camera.getLookVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 up = new Vec3(upVector.x(), upVector.y(), upVector.z());
        Vec3 forward = new Vec3(lookVector.x(), lookVector.y(), lookVector.z());
        for (LockBracketMath.Segment segment : segments) {
            Vec3 cornerA = corner(center, right, up, forward, viewOffset, segment.minX(), segment.minY());
            Vec3 cornerB = corner(center, right, up, forward, viewOffset, segment.minX(), segment.maxY());
            Vec3 cornerC = corner(center, right, up, forward, viewOffset, segment.maxX(), segment.maxY());
            Vec3 cornerD = corner(center, right, up, forward, viewOffset, segment.maxX(), segment.minY());
            quad(quads, pose, cornerA, cornerB, cornerC, cornerD, red, green, blue, alpha);
        }
        buffers.endBatch(RenderType.debugQuads());
    }

    private static Vec3 corner(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                               double viewOffset, double planeX, double planeY) {
        return center.add(right.scale(planeX))
                .add(up.scale(planeY))
                .add(forward.scale(viewOffset));
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                             Vec3 cornerA, Vec3 cornerB, Vec3 cornerC, Vec3 cornerD,
                             int red, int green, int blue, int alpha) {
        consumer.vertex(pose, (float) cornerA.x, (float) cornerA.y, (float) cornerA.z)
                .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) cornerB.x, (float) cornerB.y, (float) cornerB.z)
                .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) cornerC.x, (float) cornerC.y, (float) cornerC.z)
                .color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, (float) cornerD.x, (float) cornerD.y, (float) cornerD.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
