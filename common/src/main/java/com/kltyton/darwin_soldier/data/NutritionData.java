package com.kltyton.darwin_soldier.data;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.nutrition.NutritionMath;
import net.minecraft.nbt.CompoundTag;

public final class NutritionData {
    private int points;
    private int cumulativePoints;
    private boolean efficientMetabolismUnlocked;
    private boolean nutritionFullnessUnlocked;
    private boolean efficientMetabolismEnabled = true;
    private boolean nutritionFullnessEnabled = true;
    private double foodRestorationRemainder;
    private double drinkRestorationRemainder;
    private double metabolismThirstDebt;

    private transient boolean metabolismHeld;
    private transient boolean efficientMetabolismActive;
    private transient boolean nutritionFullnessActive;
    private transient long nextMetabolismGameTime;
    private transient long nextAbsorptionRefillGameTime;
    private transient long lastCombatGameTime = Long.MIN_VALUE;

    public void load(CompoundTag tag) {
        points = tag.getInt("Points");
        cumulativePoints = tag.contains("CumulativePoints") ? tag.getInt("CumulativePoints") : points;
        efficientMetabolismUnlocked = tag.getBoolean("EfficientMetabolismUnlocked");
        nutritionFullnessUnlocked = tag.getBoolean("NutritionFullnessUnlocked");
        efficientMetabolismEnabled = !tag.contains("EfficientMetabolismEnabled")
                || tag.getBoolean("EfficientMetabolismEnabled");
        nutritionFullnessEnabled = !tag.contains("NutritionFullnessEnabled")
                || tag.getBoolean("NutritionFullnessEnabled");
        foodRestorationRemainder = sanitizeNonNegative(tag.getDouble("FoodRestorationRemainder"));
        drinkRestorationRemainder = sanitizeNonNegative(tag.getDouble("DrinkRestorationRemainder"));
        metabolismThirstDebt = sanitizeNonNegative(tag.getDouble("MetabolismThirstDebt"));
        sanitize();
        updateUnlocks();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", points);
        tag.putInt("CumulativePoints", cumulativePoints);
        tag.putBoolean("EfficientMetabolismUnlocked", efficientMetabolismUnlocked);
        tag.putBoolean("NutritionFullnessUnlocked", nutritionFullnessUnlocked);
        tag.putBoolean("EfficientMetabolismEnabled", efficientMetabolismEnabled);
        tag.putBoolean("NutritionFullnessEnabled", nutritionFullnessEnabled);
        tag.putDouble("FoodRestorationRemainder", foodRestorationRemainder);
        tag.putDouble("DrinkRestorationRemainder", drinkRestorationRemainder);
        tag.putDouble("MetabolismThirstDebt", metabolismThirstDebt);
        return tag;
    }

    public boolean sanitize() {
        int previousPoints = points;
        int previousCumulativePoints = cumulativePoints;
        double previousFoodRemainder = foodRestorationRemainder;
        double previousDrinkRemainder = drinkRestorationRemainder;
        double previousThirstDebt = metabolismThirstDebt;
        int maximum = Math.max(0, DarwinConfig.NUTRITION_MAX_POINTS.get());
        points = Math.max(0, Math.min(maximum, points));
        cumulativePoints = Math.max(0, cumulativePoints);
        foodRestorationRemainder = fractionalPart(foodRestorationRemainder);
        drinkRestorationRemainder = fractionalPart(drinkRestorationRemainder);
        metabolismThirstDebt = sanitizeNonNegative(metabolismThirstDebt);
        return points != previousPoints
                || cumulativePoints != previousCumulativePoints
                || Double.compare(foodRestorationRemainder, previousFoodRemainder) != 0
                || Double.compare(drinkRestorationRemainder, previousDrinkRemainder) != 0
                || Double.compare(metabolismThirstDebt, previousThirstDebt) != 0;
    }

    public void updateUnlocks() {
        setEfficientMetabolismUnlocked(points >= DarwinConfig.EFFICIENT_METABOLISM_UNLOCK_POINTS.get());
        setNutritionFullnessUnlocked(points >= DarwinConfig.NUTRITION_FULLNESS_UNLOCK_POINTS.get());
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
        sanitize();
    }

    public void setPointsDebug(int points) {
        this.points = points;
        sanitize();
        cumulativePoints = Math.max(cumulativePoints, this.points);
        updateUnlocks();
    }

    public int getCumulativePoints() {
        return cumulativePoints;
    }

    public void setCumulativePoints(int cumulativePoints) {
        this.cumulativePoints = cumulativePoints;
        sanitize();
        updateUnlocks();
    }

    public void addHistoricalPoints(int amount) {
        if (amount <= 0) {
            return;
        }
        cumulativePoints += amount;
        sanitize();
        updateUnlocks();
    }

    public boolean isEfficientMetabolismUnlocked() {
        return efficientMetabolismUnlocked;
    }

    public void setEfficientMetabolismUnlocked(boolean unlocked) {
        efficientMetabolismUnlocked = unlocked;
        if (!unlocked) {
            stopEfficientMetabolism();
        }
    }

    public boolean isNutritionFullnessUnlocked() {
        return nutritionFullnessUnlocked;
    }

    public void setNutritionFullnessUnlocked(boolean unlocked) {
        nutritionFullnessUnlocked = unlocked;
        if (!unlocked) {
            nutritionFullnessActive = false;
        }
    }

    public boolean isEfficientMetabolismEnabled() {
        return efficientMetabolismEnabled;
    }

    public void setEfficientMetabolismEnabled(boolean enabled) {
        efficientMetabolismEnabled = enabled;
        if (!enabled) {
            stopEfficientMetabolism();
        }
    }

    public boolean isNutritionFullnessEnabled() {
        return nutritionFullnessEnabled;
    }

    public void setNutritionFullnessEnabled(boolean enabled) {
        nutritionFullnessEnabled = enabled;
        if (!enabled) {
            nutritionFullnessActive = false;
        }
    }

    public NutritionMath.IntegralScaling scaleFoodRestoration(int baseAmount, double bonusPerPoint) {
        NutritionMath.IntegralScaling scaling = NutritionMath.scaleIntegral(
                baseAmount, points, bonusPerPoint, foodRestorationRemainder);
        foodRestorationRemainder = scaling.remainder();
        return scaling;
    }

    public NutritionMath.IntegralScaling scaleDrinkRestoration(int baseAmount, double bonusPerPoint) {
        NutritionMath.IntegralScaling scaling = NutritionMath.scaleIntegral(
                baseAmount, points, bonusPerPoint, drinkRestorationRemainder);
        drinkRestorationRemainder = scaling.remainder();
        return scaling;
    }

    public int accumulateThirstCost(double amount) {
        metabolismThirstDebt += Math.max(0.0D, amount);
        if (!Double.isFinite(metabolismThirstDebt) || metabolismThirstDebt >= Integer.MAX_VALUE) {
            metabolismThirstDebt = 0.0D;
            return Integer.MAX_VALUE;
        }
        int whole = (int) Math.floor(metabolismThirstDebt + 1.0E-9D);
        metabolismThirstDebt -= whole;
        return whole;
    }

    public double getMetabolismThirstDebt() {
        return metabolismThirstDebt;
    }

    public boolean isMetabolismHeld() {
        return metabolismHeld;
    }

    public void setMetabolismHeld(boolean held) {
        metabolismHeld = held;
        if (!held) {
            efficientMetabolismActive = false;
            nextMetabolismGameTime = 0L;
        }
    }

    public boolean isEfficientMetabolismActive() {
        return efficientMetabolismActive;
    }

    public void setEfficientMetabolismActive(boolean active) {
        efficientMetabolismActive = active;
        if (!active) {
            nextMetabolismGameTime = 0L;
        }
    }

    public boolean isNutritionFullnessActive() {
        return nutritionFullnessActive;
    }

    public void setNutritionFullnessActive(boolean active) {
        nutritionFullnessActive = active;
        if (!active) {
            nextAbsorptionRefillGameTime = 0L;
        }
    }

    public long getNextMetabolismGameTime() {
        return nextMetabolismGameTime;
    }

    public void setNextMetabolismGameTime(long gameTime) {
        nextMetabolismGameTime = Math.max(0L, gameTime);
    }

    public long getNextAbsorptionRefillGameTime() {
        return nextAbsorptionRefillGameTime;
    }

    public void setNextAbsorptionRefillGameTime(long gameTime) {
        nextAbsorptionRefillGameTime = Math.max(0L, gameTime);
    }

    public void markCombat(long gameTime) {
        lastCombatGameTime = Math.max(lastCombatGameTime, gameTime);
    }

    public boolean canRefillAbsorptionAfterCombat(long gameTime, long delayTicks) {
        return lastCombatGameTime == Long.MIN_VALUE
                || gameTime - lastCombatGameTime >= Math.max(0L, delayTicks);
    }

    public void stopEfficientMetabolism() {
        metabolismHeld = false;
        efficientMetabolismActive = false;
        nextMetabolismGameTime = 0L;
    }

    public void clearRuntime() {
        stopEfficientMetabolism();
        nutritionFullnessActive = false;
        nextAbsorptionRefillGameTime = 0L;
        lastCombatGameTime = Long.MIN_VALUE;
    }

    private static double sanitizeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double fractionalPart(double value) {
        double sanitized = sanitizeNonNegative(value);
        return sanitized - Math.floor(sanitized);
    }
}
