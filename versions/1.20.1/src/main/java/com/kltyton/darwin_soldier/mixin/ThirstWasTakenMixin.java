package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionDrinkScaling;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scales Thirst Was Taken's capability-level drink operation without requiring the mod at runtime. */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.content.thirst.PlayerThirst", remap = false)
public abstract class ThirstWasTakenMixin {
    @ModifyVariable(
            method = "drink(Lnet/minecraft/world/entity/player/Player;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2,
            require = 0
    )
    private int darwinSoldier$scaleThirst(int thirst, Player player, int originalThirst, int quenched) {
        return NutritionDrinkScaling.scaleHydration(player, thirst);
    }

    @ModifyVariable(
            method = "drink(Lnet/minecraft/world/entity/player/Player;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3,
            require = 0
    )
    private int darwinSoldier$scaleQuenched(int quenched, Player player, int thirst, int originalQuenched) {
        return NutritionDrinkScaling.scaleQuenched(player, quenched);
    }

    @ModifyConstant(
            method = "drink(Lnet/minecraft/world/entity/player/Player;II)V",
            constant = @Constant(intValue = 20),
            require = 2,
            allow = 2
    )
    private int darwinSoldier$maximumDrinkCapacity(int original, Player player, int thirst, int quenched) {
        return NutritionFood.maximumFood(player);
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
