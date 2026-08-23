package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaProjectilePhysicsTest {
    @Test
    void bowLaunchSpeedTracksTheCurrentDrawDuration() {
        assertEquals(0.5625D, VanillaProjectilePhysics.bowLaunchSpeed(5), 1.0E-9D);
        assertEquals(1.25D, VanillaProjectilePhysics.bowLaunchSpeed(10), 1.0E-9D);
        assertEquals(3.0D, VanillaProjectilePhysics.bowLaunchSpeed(20), 1.0E-9D);
        assertEquals(3.0D, VanillaProjectilePhysics.bowLaunchSpeed(60), 1.0E-9D);
    }

    @Test
    void arrowStepMatchesVanillaMoveThenDragThenGravityOrder() {
        VanillaProjectilePhysics.Step first = VanillaProjectilePhysics.advance(
                Vec3.ZERO, new Vec3(0.0D, 0.0D, 3.0D), 0.99D, 0.05D);
        assertEquals(new Vec3(0.0D, 0.0D, 3.0D), first.position());
        assertEquals(2.97D, first.velocity().z, 1.0E-9D);
        assertEquals(-0.05D, first.velocity().y, 1.0E-9D);

        VanillaProjectilePhysics.Step second = VanillaProjectilePhysics.advance(
                first.position(), first.velocity(), 0.99D, 0.05D);
        assertEquals(5.97D, second.position().z, 1.0E-9D);
        assertEquals(-0.05D, second.position().y, 1.0E-9D);
        assertEquals(2.9403D, second.velocity().z, 1.0E-9D);
        assertEquals(-0.0995D, second.velocity().y, 1.0E-9D);
    }

    @Test
    void projectedPositionMatchesRepeatedVanillaSteps() {
        Vec3 origin = new Vec3(1.0D, 4.0D, -2.0D);
        Vec3 initialVelocity = new Vec3(0.2D, 0.4D, 2.8D);
        VanillaProjectilePhysics.Step stepped = new VanillaProjectilePhysics.Step(origin, initialVelocity);
        for (int tick = 0; tick < 25; tick++) {
            stepped = VanillaProjectilePhysics.advance(
                    stepped.position(), stepped.velocity(), VanillaProjectilePhysics.AIR_DRAG, 0.05D);
        }

        Vec3 projected = VanillaProjectilePhysics.project(
                origin, initialVelocity, 25.0D, VanillaProjectilePhysics.AIR_DRAG, 0.05D);
        assertEquals(stepped.position().x, projected.x, 1.0E-9D);
        assertEquals(stepped.position().y, projected.y, 1.0E-9D);
        assertEquals(stepped.position().z, projected.z, 1.0E-9D);
    }
}
