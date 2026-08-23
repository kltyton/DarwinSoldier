package com.kltyton.darwin_soldier.client.aim.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class TrajectoryQuadRenderer {
    private static final double PATH_MARKER_RADIUS = 0.10D;
    private static final double IMPACT_MARKER_RADIUS = 0.20D;
    private static final double PATH_MARKER_SPACING = 1.50D;

    private TrajectoryQuadRenderer() {
    }

    static int emitSegment(
            VertexConsumer consumer,
            Matrix4f pose,
            Vec3 start,
            Vec3 end,
            boolean impact,
            double radiusScale
    ) {
        double length = start.distanceTo(end);
        int samples = Math.max(1, (int) Math.ceil(length / PATH_MARKER_SPACING));
        int pathSamples = impact ? samples - 1 : samples;
        for (int sample = 1; sample <= pathSamples; sample++) {
            Vec3 point = start.lerp(end, sample / (double) samples);
            emitCube(consumer, pose, point, PATH_MARKER_RADIUS * radiusScale, 255, 255, 255, 72);
        }
        if (impact) {
            emitCube(consumer, pose, end, IMPACT_MARKER_RADIUS * radiusScale, 255, 48, 48, 145);
        }
        return pathSamples + (impact ? 1 : 0);
    }

    private static void emitCube(
            VertexConsumer consumer,
            Matrix4f pose,
            Vec3 center,
            double radius,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float minX = (float) (center.x - radius);
        float minY = (float) (center.y - radius);
        float minZ = (float) (center.z - radius);
        float maxX = (float) (center.x + radius);
        float maxY = (float) (center.y + radius);
        float maxZ = (float) (center.z + radius);

        quad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        quad(consumer, pose, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
                red, green, blue, alpha);
        quad(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                red, green, blue, alpha);
        quad(consumer, pose, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                red, green, blue, alpha);
        quad(consumer, pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                red, green, blue, alpha);
        quad(consumer, pose, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
    }

    private static void quad(
            VertexConsumer consumer,
            Matrix4f pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int red, int green, int blue, int alpha
    ) {
        consumer.vertex(pose, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, x3, y3, z3).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose, x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }
}
