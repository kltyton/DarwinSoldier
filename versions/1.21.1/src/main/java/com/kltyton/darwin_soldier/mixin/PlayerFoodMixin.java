package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies expanded food capacity at the 1.21 player-owned FoodData boundary. */
@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @WrapOperation(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
            )
    )
    private void darwinSoldier$applyExpandedFoodCapacity(FoodData foodData, FoodProperties properties,
                                                          Operation<Void> original) {
        Player player = (Player) (Object) this;
        if (!NutritionFood.applyFood(player, foodData, properties.nutrition(), properties.saturation())) {
            original.call(foodData, properties);
        }
    }
}
