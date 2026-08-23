package com.kltyton.darwin_soldier.nutrition;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.NutritionData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class NutritionFood {
    private NutritionFood() {
    }

    public static int maximumFood(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!DarwinConfig.NUTRITION_ENABLED.get()) {
                return 20;
            }
            PlayerGrowthData data = GrowthSavedData.get(serverPlayer).getOrCreate(serverPlayer.getUUID());
            return data.isEnabled() ? maximumFood(data.getNutritionPoints()) : 20;
        }
        return NutritionClientState.getMaximumFood();
    }

    public static int maximumFood(int nutritionPoints) {
        return NutritionMath.maximumFood(
                DarwinConfig.NUTRITION_BASE_MAX_FOOD.get(),
                DarwinConfig.NUTRITION_MAX_FOOD_PER_POINT.get(),
                nutritionPoints);
    }

    public static boolean applyFood(Player player, FoodData foodData, int baseFood,
                                    float baseSaturationModifier) {
        if (player instanceof ServerPlayer serverPlayer) {
            return applyServerFood(serverPlayer, foodData, baseFood, baseSaturationModifier);
        }
        return applyClientFood(foodData, baseFood, baseSaturationModifier);
    }

    public static boolean shouldScale(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!DarwinConfig.NUTRITION_ENABLED.get()) {
                return false;
            }
            PlayerGrowthData data = GrowthSavedData.get(serverPlayer).getOrCreate(serverPlayer.getUUID());
            return data.isEnabled()
                    && (data.getNutritionPoints() > 0 || maximumFood(data.getNutritionPoints()) != 20);
        }
        return NutritionClientState.isActive()
                && (NutritionClientState.getNutritionPoints() > 0
                || NutritionClientState.getMaximumFood() != 20);
    }

    public static boolean clampToMaximum(ServerPlayer player, PlayerGrowthData data) {
        int maximum = data.isEnabled() && DarwinConfig.NUTRITION_ENABLED.get()
                ? maximumFood(data.getNutritionPoints())
                : 20;
        FoodData foodData = player.getFoodData();
        boolean changed = false;
        if (foodData.getFoodLevel() > maximum) {
            foodData.setFoodLevel(maximum);
            changed = true;
        }
        if (foodData.getSaturationLevel() > foodData.getFoodLevel()) {
            foodData.setSaturation(foodData.getFoodLevel());
            changed = true;
        }
        return changed;
    }

    private static boolean applyServerFood(ServerPlayer player, FoodData foodData, int baseFood,
                                           float baseSaturationModifier) {
        if (!DarwinConfig.NUTRITION_ENABLED.get()) {
            return false;
        }

        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData growth = savedData.getOrCreate(player.getUUID());
        if (!growth.isEnabled()) {
            return false;
        }

        NutritionData nutrition = growth.getNutrition();
        int points = nutrition.getPoints();
        int maximum = maximumFood(points);
        if (points <= 0 && maximum == 20) {
            return false;
        }

        NutritionMath.IntegralScaling foodScaling = nutrition.scaleFoodRestoration(
                baseFood, DarwinConfig.NUTRITION_FOOD_RESTORE_BONUS_PER_POINT.get());
        applyScaledFood(foodData, maximum, foodScaling.amount(), baseFood, baseSaturationModifier, points,
                DarwinConfig.NUTRITION_SATURATION_RESTORE_BONUS_PER_POINT.get());
        savedData.setDirty();
        return true;
    }

    private static boolean applyClientFood(FoodData foodData, int baseFood, float baseSaturationModifier) {
        if (!NutritionClientState.isActive()) {
            return false;
        }

        int points = NutritionClientState.getNutritionPoints();
        int maximum = NutritionClientState.getMaximumFood();
        if (points <= 0 && maximum == 20) {
            return false;
        }

        int scaledFood = NutritionMath.scaleIntegral(baseFood, points,
                NutritionClientState.getFoodBonusPerPoint(), 0.0D).amount();
        applyScaledFood(foodData, maximum, scaledFood, baseFood, baseSaturationModifier, points,
                NutritionClientState.getSaturationBonusPerPoint());
        return true;
    }

    private static void applyScaledFood(FoodData foodData, int maximum, int scaledFood,
                                        int baseFood, float baseSaturationModifier, int nutritionPoints,
                                        double saturationBonusPerPoint) {
        long increasedFood = (long) foodData.getFoodLevel() + Math.max(0, scaledFood);
        int finalFood = (int) Math.max(0L,
                Math.min(maximum, Math.min(Integer.MAX_VALUE, increasedFood)));
        double baseSaturation = (double) baseFood * baseSaturationModifier * 2.0D;
        double saturationMultiplier = NutritionMath.restorationMultiplier(nutritionPoints, saturationBonusPerPoint);
        float finalSaturation = (float) Math.min(finalFood,
                foodData.getSaturationLevel() + Math.max(0.0D, baseSaturation * saturationMultiplier));
        foodData.setFoodLevel(finalFood);
        foodData.setSaturation(finalSaturation);
    }
}
