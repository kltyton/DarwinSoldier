package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.phys.Vec3;

public record ProjectileLaunchState(
        String source,
        Vec3 origin,
        Vec3 velocity,
        Vec3 inheritedVelocity,
        double gravity,
        double airDrag,
        double waterDrag,
        boolean hitscan,
        double spreadDegrees
) {
    public ProjectileLaunchState {
        gravity = sanitize(gravity);
        airDrag = sanitize(airDrag);
        waterDrag = sanitize(waterDrag);
        spreadDegrees = sanitize(spreadDegrees);
    }

    /**
     * 旧 8 参数兼容构造器，散布角度默认为 0（hitscan/未知弹道）。
     */
    public ProjectileLaunchState(
            String source,
            Vec3 origin,
            Vec3 velocity,
            Vec3 inheritedVelocity,
            double gravity,
            double airDrag,
            double waterDrag,
            boolean hitscan
    ) {
        this(source, origin, velocity, inheritedVelocity, gravity, airDrag, waterDrag, hitscan, 0.0D);
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) && value >= 0.0D ? value : 0.0D;
    }

    public double launchSpeed() {
        return velocity.subtract(inheritedVelocity).length();
    }

    public double drag(boolean inWater) {
        return inWater ? waterDrag : airDrag;
    }
}
