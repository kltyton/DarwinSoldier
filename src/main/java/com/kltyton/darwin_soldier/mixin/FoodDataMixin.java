package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reapplies every FoodData restoration source using the owning player's nutrition after vanilla logic runs. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Unique
    private Player darwinSoldier$nutritionPlayer;
    @Unique
    private boolean darwinSoldier$scaleFood;
    @Unique
    private int darwinSoldier$baseFood;
    @Unique
    private float darwinSoldier$baseSaturationModifier;
    @Unique
    private int darwinSoldier$foodBeforeEating;
    @Unique
    private float darwinSoldier$saturationBeforeEating;

    @Inject(method = "tick", at = @At("HEAD"))
    private void darwinSoldier$captureFoodOwner(Player player, CallbackInfo callback) {
        darwinSoldier$nutritionPlayer = player;
    }

    @Inject(
            method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void darwinSoldier$captureEatingPlayer(Item item, ItemStack stack, LivingEntity entity,
                                                    CallbackInfo callback) {
        if (entity instanceof Player player) {
            darwinSoldier$nutritionPlayer = player;
        }
    }

    @Inject(method = "eat(IF)V", at = @At("HEAD"))
    private void darwinSoldier$captureFoodBeforeEating(int food, float saturationModifier,
                                                        CallbackInfo callback) {
        darwinSoldier$clearFoodContext();
        if (food <= 0 || !Float.isFinite(saturationModifier) || saturationModifier < 0.0F
                || darwinSoldier$nutritionPlayer == null
                || !NutritionFood.shouldScale(darwinSoldier$nutritionPlayer)) {
            return;
        }

        FoodData foodData = (FoodData) (Object) this;
        darwinSoldier$scaleFood = true;
        darwinSoldier$baseFood = food;
        darwinSoldier$baseSaturationModifier = saturationModifier;
        darwinSoldier$foodBeforeEating = foodData.getFoodLevel();
        darwinSoldier$saturationBeforeEating = foodData.getSaturationLevel();
    }

    @Inject(method = "eat(IF)V", at = @At("RETURN"))
    private void darwinSoldier$applyNutritionAfterEating(int food, float saturationModifier,
                                                          CallbackInfo callback) {
        if (!darwinSoldier$scaleFood || darwinSoldier$nutritionPlayer == null) {
            return;
        }

        Player player = darwinSoldier$nutritionPlayer;
        int baseFood = darwinSoldier$baseFood;
        float baseSaturationModifier = darwinSoldier$baseSaturationModifier;
        FoodData foodData = (FoodData) (Object) this;
        int vanillaFood = foodData.getFoodLevel();
        float vanillaSaturation = foodData.getSaturationLevel();
        foodData.setFoodLevel(darwinSoldier$foodBeforeEating);
        foodData.setSaturation(darwinSoldier$saturationBeforeEating);
        darwinSoldier$clearFoodContext();
        if (!NutritionFood.applyFood(player, foodData, baseFood, baseSaturationModifier)) {
            foodData.setFoodLevel(vanillaFood);
            foodData.setSaturation(vanillaSaturation);
        }
    }

    @Unique
    private void darwinSoldier$clearFoodContext() {
        darwinSoldier$scaleFood = false;
        darwinSoldier$baseFood = 0;
        darwinSoldier$baseSaturationModifier = 0.0F;
        darwinSoldier$foodBeforeEating = 0;
        darwinSoldier$saturationBeforeEating = 0.0F;
    }
}
