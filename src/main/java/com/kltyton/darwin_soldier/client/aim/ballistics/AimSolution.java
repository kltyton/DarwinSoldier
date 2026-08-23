package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.phys.Vec3;

public record AimSolution(
        Vec3 aimPoint,
        Vec3 targetVelocity,
        double flightTicks,
        double interpolation,
        Vec3 targetAcceleration,
        Vec3 predictedTarget
) {
    public AimSolution(Vec3 aimPoint, Vec3 targetVelocity, double flightTicks, double interpolation) {
        this(aimPoint, targetVelocity, flightTicks, interpolation, Vec3.ZERO, aimPoint);
    }
}
