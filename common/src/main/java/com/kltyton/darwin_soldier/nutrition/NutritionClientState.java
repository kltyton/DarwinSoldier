package com.kltyton.darwin_soldier.nutrition;

public final class NutritionClientState {
    private static volatile boolean growthEnabled;
    private static volatile boolean nutritionEnabled;
    private static volatile int nutritionPoints;
    private static volatile int maximumFood = 20;
    private static volatile double foodBonusPerPoint = 0.20D;
    private static volatile double saturationBonusPerPoint = 0.20D;

    private NutritionClientState() {
    }

    public static void update(boolean growthEnabled, boolean nutritionEnabled, int nutritionPoints,
                              int maximumFood, double foodBonusPerPoint, double saturationBonusPerPoint) {
        NutritionClientState.growthEnabled = growthEnabled;
        NutritionClientState.nutritionEnabled = nutritionEnabled;
        NutritionClientState.nutritionPoints = Math.max(0, nutritionPoints);
        NutritionClientState.maximumFood = Math.max(20, maximumFood);
        NutritionClientState.foodBonusPerPoint = Math.max(0.0D, foodBonusPerPoint);
        NutritionClientState.saturationBonusPerPoint = Math.max(0.0D, saturationBonusPerPoint);
    }

    public static boolean isActive() {
        return growthEnabled && nutritionEnabled;
    }

    public static int getNutritionPoints() {
        return nutritionPoints;
    }

    public static int getMaximumFood() {
        return isActive() ? maximumFood : 20;
    }

    public static double getFoodBonusPerPoint() {
        return foodBonusPerPoint;
    }

    public static double getSaturationBonusPerPoint() {
        return saturationBonusPerPoint;
    }
}
