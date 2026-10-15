package com.kltyton.darwin_soldier.mixin.client;

import com.kltyton.darwin_soldier.client.SuperPerceptionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only mouse ownership hook. When Super Perception owns the mouse
 * input, the raw accumulated deltas are handed to
 * {@link SuperPerceptionClient#captureMouseInput} and the vanilla accumulators
 * plus smooth-turn state are zeroed so {@code MouseHandler#turnPlayer}
 * continues with zero deltas and cannot fight the aim controller.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void darwinSoldier$captureAimMouseInput(CallbackInfo callback) {
        if (SuperPerceptionClient.captureMouseInput(
                Minecraft.getInstance(), this.accumulatedDX, this.accumulatedDY)) {
            this.accumulatedDX = 0.0D;
            this.accumulatedDY = 0.0D;
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
        }
    }
}
