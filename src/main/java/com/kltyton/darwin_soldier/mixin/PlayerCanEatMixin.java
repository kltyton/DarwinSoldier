package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends the vanilla food-level gate, which has no Forge hook for a per-player maximum above 20. */
@Mixin(Player.class)
public abstract class PlayerCanEatMixin {
    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void darwinSoldier$allowEatingToNutritionMaximum(boolean canAlwaysEat,
                                                              CallbackInfoReturnable<Boolean> callback) {
        if (Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }

        Player player = (Player) (Object) this;
        if (player.getFoodData().getFoodLevel() < NutritionFood.maximumFood(player)) {
            callback.setReturnValue(true);
        }
    }
}
