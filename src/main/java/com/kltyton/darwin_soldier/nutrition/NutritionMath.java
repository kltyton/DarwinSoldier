package com.kltyton.darwin_soldier.nutrition;

public final class NutritionMath {
    private NutritionMath() {
    }

    public static int maximumFood(int baseMaximum, int perPoint, int nutritionPoints) {
        return maximumCapacity(baseMaximum, perPoint, nutritionPoints);
    }

    public static int maximumCapacity(int baseMaximum, int perPoint, int nutritionPoints) {
        long maximum = Math.max(1L, baseMaximum)
                + (long) Math.max(0, perPoint) * Math.max(0, nutritionPoints);
        return (int) Math.min(Integer.MAX_VALUE, maximum);
    }

    public static double restorationMultiplier(int nutritionPoints, double bonusPerPoint) {
        return 1.0D + Math.max(0, nutritionPoints) * Math.max(0.0D, bonusPerPoint);
    }

    public static IntegralScaling scaleIntegral(int baseAmount, int nutritionPoints,
                                                double bonusPerPoint, double carriedFraction) {
        if (baseAmount <= 0) {
            return new IntegralScaling(baseAmount, sanitizeFraction(carriedFraction));
        }

        double exact = baseAmount * restorationMultiplier(nutritionPoints, bonusPerPoint)
                + sanitizeFraction(carriedFraction);
        if (exact >= Integer.MAX_VALUE) {
            return new IntegralScaling(Integer.MAX_VALUE, 0.0D);
        }

        int whole = (int) Math.floor(exact + 1.0E-9D);
        return new IntegralScaling(whole, sanitizeFraction(exact - whole));
    }

    public static float absorptionCap(double baseAmount, double maxHealthRatio, float maxHealth) {
        double cap = Math.max(0.0D, baseAmount)
                + Math.max(0.0D, maxHealthRatio) * Math.max(0.0F, maxHealth);
        return (float) Math.min(Float.MAX_VALUE, cap);
    }

    public static boolean meetsThreshold(double current, double maximum, double threshold) {
        return maximum > 0.0D
                && current >= maximum * Math.max(0.0D, Math.min(1.0D, threshold));
    }

    private static double sanitizeFraction(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return value - Math.floor(value);
    }

    public record IntegralScaling(int amount, double remainder) {
    }
}
