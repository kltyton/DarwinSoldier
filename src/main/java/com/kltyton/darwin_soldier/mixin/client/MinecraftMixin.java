package com.kltyton.darwin_soldier.mixin.client;

import com.kltyton.darwin_soldier.client.SuperPerceptionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only glow: makes the currently locked target appear glowing without
 * touching the entity's glow flag. Vanilla results are preserved; only a
 * false result for the locked target is overridden to true.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void darwinSoldier$glowLockedTarget(
            Entity entity,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!callback.getReturnValueZ() && SuperPerceptionClient.isLockedTarget(entity)) {
            callback.setReturnValue(true);
        }
    }
}
