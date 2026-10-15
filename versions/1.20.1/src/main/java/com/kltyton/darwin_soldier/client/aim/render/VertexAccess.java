package com.kltyton.darwin_soldier.client.aim.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public final class VertexAccess {
    private VertexAccess() {}
    public static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                              int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
