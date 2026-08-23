package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.phys.Vec3;

public final class VanillaProjectilePhysics {
    public static final double AIR_DRAG = 0.99D;
    public static final double ARROW_WATER_DRAG = 0.6D;
    public static final double THROWABLE_WATER_DRAG = 0.8D;

    private VanillaProjectilePhysics() {
    }

    public static double bowLaunchSpeed(int chargeTicks) {
        double power = Math.max(0, chargeTicks) / 20.0D;
        power = (power * power + power * 2.0D) / 3.0D;
        return Math.min(1.0D, power) * 3.0D;
    }

    public static Step advance(Vec3 position, Vec3 velocity, double drag, double gravity) {
        Vec3 nextPosition = position.add(velocity);
        Vec3 nextVelocity = velocity.scale(drag).add(0.0D, -gravity, 0.0D);
        return new Step(nextPosition, nextVelocity);
    }

    public static double flightTicks(double distance, double initialSpeed, double drag, double maximumTicks) {
        if (!Double.isFinite(distance) || distance <= 0.0D
                || !Double.isFinite(initialSpeed) || initialSpeed <= 0.0D) {
            return 0.0D;
        }
        double travelled = 0.0D;
        double speed = initialSpeed;
        int wholeTicks = 0;
        int limit = Math.max(1, (int) Math.ceil(maximumTicks));
        while (wholeTicks < limit && travelled + speed < distance && speed > 1.0E-6D) {
            travelled += speed;
            speed *= drag;
            wholeTicks++;
        }
        double fraction = speed <= 1.0E-6D ? 0.0D : Math.min(1.0D, (distance - travelled) / speed);
        return Math.min(maximumTicks, wholeTicks + Math.max(0.0D, fraction));
    }

    public static double gravityDrop(double flightTicks, double gravity, double drag) {
        if (!Double.isFinite(flightTicks) || flightTicks <= 0.0D
                || !Double.isFinite(gravity) || gravity <= 0.0D) {
            return 0.0D;
        }
        int wholeTicks = (int) Math.floor(flightTicks);
        double fraction = flightTicks - wholeTicks;
        if (Math.abs(1.0D - drag) < 1.0E-9D) {
            return gravity * (wholeTicks * (wholeTicks - 1.0D) * 0.5D + wholeTicks * fraction);
        }
        double dragPower = Math.pow(drag, wholeTicks);
        double distanceFactor = (1.0D - dragPower) / (1.0D - drag);
        return gravity / (1.0D - drag)
                * (wholeTicks - distanceFactor + (1.0D - dragPower) * fraction);
    }

    public static double dragDistanceFactor(double flightTicks, double drag) {
        if (!Double.isFinite(flightTicks) || flightTicks <= 0.0D || !Double.isFinite(drag) || drag < 0.0D) {
            return 0.0D;
        }
        int wholeTicks = (int) Math.floor(flightTicks);
        double fraction = flightTicks - wholeTicks;
        if (Math.abs(1.0D - drag) < 1.0E-9D) {
            return wholeTicks + fraction;
        }
        double dragPower = Math.pow(drag, wholeTicks);
        return (1.0D - dragPower) / (1.0D - drag) + dragPower * fraction;
    }

    public static Vec3 project(
            Vec3 origin,
            Vec3 initialVelocity,
            double flightTicks,
            double drag,
            double gravity
    ) {
        double distanceFactor = dragDistanceFactor(flightTicks, drag);
        double drop = gravityDrop(flightTicks, gravity, drag);
        return origin.add(initialVelocity.scale(distanceFactor)).add(0.0D, -drop, 0.0D);
    }

    public record Step(Vec3 position, Vec3 velocity) {
    }
}
