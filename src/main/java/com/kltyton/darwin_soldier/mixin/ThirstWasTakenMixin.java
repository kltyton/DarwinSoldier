package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionDrinkScaling;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import com.kltyton.darwin_soldier.compat.thirst.ThirstDrinkContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scales Thirst Was Taken's capability-level drink operation without requiring the mod at runtime. */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.content.thirst.PlayerThirst", remap = false)
public abstract class ThirstWasTakenMixin {
    @ModifyVariable(
            method = "drink(II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private int darwinSoldier$scaleThirst(int thirst) {
        return NutritionDrinkScaling.scaleHydration(ThirstDrinkContext.player(), thirst);
    }

    @ModifyVariable(
            method = "drink(II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2,
            require = 0
    )
    private int darwinSoldier$scaleQuenched(int quenched) {
        return NutritionDrinkScaling.scaleQuenched(ThirstDrinkContext.player(), quenched);
    }

    @ModifyConstant(
            method = "drink(II)V",
            constant = @Constant(intValue = 20),
            require = 2,
            allow = 2
    )
    private int darwinSoldier$maximumDrinkCapacity(int original) {
        return NutritionFood.maximumFood(ThirstDrinkContext.player());
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "drink(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            require = 1
    )
    private static void darwinSoldier$captureItemDrink(ItemStack stack, Player player, CallbackInfo callback) {
        ThirstDrinkContext.set(player);
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "drink(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("RETURN"),
            require = 1
    )
    private static void darwinSoldier$clearItemDrink(ItemStack stack, Player player, CallbackInfo callback) {
        ThirstDrinkContext.clear();
    }

    @ModifyConstant(
            method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
            constant = @Constant(intValue = 20),
            require = 3,
            allow = 3
    )
    private int darwinSoldier$maximumPassiveCapacity(int original, Player player) {
        return NutritionFood.maximumFood(player);
    }
}
