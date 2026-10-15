package com.kltyton.darwin_soldier.mixin.client;

import com.kltyton.darwin_soldier.client.battle.BattleInstinctClientVisuals;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void darwinSoldier$showBattleInstinctBlockPose(
            AbstractClientPlayer player,
            InteractionHand hand,
            CallbackInfoReturnable<HumanoidModel.ArmPose> callback
    ) {
        if (hand == InteractionHand.MAIN_HAND && BattleInstinctClientVisuals.isBlocking(player)) {
            callback.setReturnValue(HumanoidModel.ArmPose.BLOCK);
        }
    }
}
