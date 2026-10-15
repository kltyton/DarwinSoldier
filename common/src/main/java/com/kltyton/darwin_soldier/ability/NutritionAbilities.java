package com.kltyton.darwin_soldier.ability;

import com.kltyton.darwin_soldier.compat.thirst.ThirstCompat;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.NutritionData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import com.kltyton.darwin_soldier.nutrition.NutritionMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class NutritionAbilities {
    private static final int FULLNESS_HASTE_REFRESH_TICKS = 3;

    private NutritionAbilities() {
    }

    public static TickResult tick(ServerPlayer player, PlayerGrowthData growth) {
        NutritionData nutrition = growth.getNutrition();
        boolean nutritionSanitized = nutrition.sanitize();
        ThirstCompat.ThirstSnapshot thirst = growth.isEnabled() && DarwinConfig.NUTRITION_ENABLED.get()
                ? ThirstCompat.snapshot(player, nutrition)
                : ThirstCompat.ThirstSnapshot.unavailable();
        boolean dataChanged = nutritionSanitized;
        dataChanged |= NutritionFood.clampToMaximum(player, growth);
        boolean syncRequired = nutritionSanitized;

        TickResult metabolism = tickEfficientMetabolism(player, growth, nutrition, thirst);
        dataChanged |= metabolism.dataChanged();
        syncRequired |= metabolism.syncRequired();

        TickResult fullness = tickNutritionFullness(player, growth, nutrition, thirst);
        dataChanged |= fullness.dataChanged();
        syncRequired |= fullness.syncRequired();
        return new TickResult(dataChanged, syncRequired);
    }

    public static void setMetabolismHeld(ServerPlayer player, boolean held) {
        GrowthSavedData savedData = GrowthSavedData.get(player);
        PlayerGrowthData growth = savedData.getOrCreate(player.getUUID());
        NutritionData nutrition = growth.getNutrition();
        boolean wasActive = nutrition.isEfficientMetabolismActive();
        boolean accepted = held
                && growth.isEnabled()
                && DarwinConfig.NUTRITION_ENABLED.get()
                && nutrition.isEfficientMetabolismUnlocked()
                && nutrition.isEfficientMetabolismEnabled();
        nutrition.setMetabolismHeld(accepted);
        if (wasActive != nutrition.isEfficientMetabolismActive()) {
            ModNetwork.syncTo(player, growth);
        }
    }

    public static void clearRuntime(PlayerGrowthData growth) {
        growth.getNutrition().clearRuntime();
    }

    public static void markCombat(ServerPlayer player) {
        PlayerGrowthData growth = GrowthSavedData.get(player).getOrCreate(player.getUUID());
        growth.getNutrition().markCombat(player.serverLevel().getGameTime());
    }

    public static float absorptionCap(ServerPlayer player) {
        return NutritionMath.absorptionCap(
                DarwinConfig.NUTRITION_FULLNESS_ABSORPTION_BASE.get(),
                DarwinConfig.NUTRITION_FULLNESS_ABSORPTION_MAX_HEALTH_PERCENT.get(),
                player.getMaxHealth());
    }

    public static ThirstCompat.ThirstSnapshot thirstSnapshot(ServerPlayer player, PlayerGrowthData growth) {
        return ThirstCompat.snapshot(player, growth.getNutrition());
    }

    private static TickResult tickEfficientMetabolism(ServerPlayer player, PlayerGrowthData growth,
                                                       NutritionData nutrition,
                                                       ThirstCompat.ThirstSnapshot thirst) {
        boolean wasActive = nutrition.isEfficientMetabolismActive();
        if (!canContinueMetabolism(player, growth, nutrition, thirst)) {
            if (nutrition.isMetabolismHeld() || wasActive) {
                nutrition.stopEfficientMetabolism();
            }
            return new TickResult(false, wasActive);
        }

        long now = player.serverLevel().getGameTime();
        if (!wasActive) {
            nutrition.setEfficientMetabolismActive(true);
            nutrition.setNextMetabolismGameTime(now + DarwinConfig.EFFICIENT_METABOLISM_INTERVAL_TICKS.get());
            return new TickResult(false, true);
        }
        if (now < nutrition.getNextMetabolismGameTime()) {
            return TickResult.NONE;
        }

        int foodCost = DarwinConfig.EFFICIENT_METABOLISM_FOOD_COST.get();
        player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - foodCost));
        double thirstCost = DarwinConfig.EFFICIENT_METABOLISM_THIRST_COST.get();
        boolean thirstDataChanged = thirst.linked() && thirstCost > 0.0D
                && ThirstCompat.consume(player, nutrition, thirstCost);

        float healing = (float) (player.getMaxHealth()
                * DarwinConfig.EFFICIENT_METABOLISM_HEAL_MAX_HEALTH_PERCENT.get());
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + Math.max(0.0F, healing)));
        nutrition.setNextMetabolismGameTime(now + DarwinConfig.EFFICIENT_METABOLISM_INTERVAL_TICKS.get());

        ThirstCompat.ThirstSnapshot updatedThirst = thirst.linked()
                ? ThirstCompat.snapshot(player, nutrition)
                : thirst;
        boolean shouldStop = player.getHealth() >= player.getMaxHealth()
                || player.getFoodData().getFoodLevel() <= DarwinConfig.EFFICIENT_METABOLISM_MIN_FOOD.get()
                || updatedThirst.linked() && updatedThirst.ratio() <= DarwinConfig.EFFICIENT_METABOLISM_MIN_THIRST_PERCENT.get();
        if (shouldStop) {
            nutrition.stopEfficientMetabolism();
        }
        return new TickResult(thirstDataChanged, shouldStop);
    }

    private static boolean canContinueMetabolism(ServerPlayer player, PlayerGrowthData growth,
                                                  NutritionData nutrition,
                                                  ThirstCompat.ThirstSnapshot thirst) {
        return growth.isEnabled()
                && DarwinConfig.NUTRITION_ENABLED.get()
                && nutrition.isEfficientMetabolismUnlocked()
                && nutrition.isEfficientMetabolismEnabled()
                && nutrition.isMetabolismHeld()
                && player.isAlive()
                && player.getHealth() < player.getMaxHealth()
                && player.getFoodData().getFoodLevel() > DarwinConfig.EFFICIENT_METABOLISM_MIN_FOOD.get()
                && (!thirst.linked()
                || thirst.ratio() > DarwinConfig.EFFICIENT_METABOLISM_MIN_THIRST_PERCENT.get());
    }

    private static TickResult tickNutritionFullness(ServerPlayer player, PlayerGrowthData growth,
                                                     NutritionData nutrition,
                                                     ThirstCompat.ThirstSnapshot thirst) {
        int maximumFood = growth.isEnabled() && DarwinConfig.NUTRITION_ENABLED.get()
                ? NutritionFood.maximumFood(nutrition.getPoints())
                : 20;
        boolean active = growth.isEnabled()
                && DarwinConfig.NUTRITION_ENABLED.get()
                && nutrition.isNutritionFullnessUnlocked()
                && nutrition.isNutritionFullnessEnabled()
                && player.isAlive()
                && NutritionMath.meetsThreshold(player.getFoodData().getFoodLevel(), maximumFood,
                DarwinConfig.NUTRITION_FULLNESS_THRESHOLD.get())
                && (!thirst.linked() || NutritionMath.meetsThreshold(thirst.current(), thirst.maximum(),
                DarwinConfig.NUTRITION_FULLNESS_THRESHOLD.get()));

        boolean changed = active != nutrition.isNutritionFullnessActive();
        nutrition.setNutritionFullnessActive(active);
        if (!active) {
            return new TickResult(false, changed);
        }

        applyHaste(player);
        long now = player.serverLevel().getGameTime();
        long combatDelay = Math.max(0L, DarwinConfig.NUTRITION_FULLNESS_COMBAT_DELAY_SECONDS.get()) * 20L;
        if (!nutrition.canRefillAbsorptionAfterCombat(now, combatDelay)) {
            return new TickResult(false, changed);
        }
        if (nutrition.getNextAbsorptionRefillGameTime() > now) {
            return new TickResult(false, changed);
        }

        float cap = absorptionCap(player);
        if (player.getAbsorptionAmount() < cap) {
            player.setAbsorptionAmount(cap);
        }
        long interval = Math.max(1L, DarwinConfig.NUTRITION_FULLNESS_ABSORPTION_REFILL_SECONDS.get()) * 20L;
        nutrition.setNextAbsorptionRefillGameTime(now + interval);
        return new TickResult(false, changed);
    }

    private static void applyHaste(ServerPlayer player) {
        int level = DarwinConfig.NUTRITION_FULLNESS_HASTE_LEVEL.get();
        if (level <= 0) {
            return;
        }

        int amplifier = level - 1;
        MobEffectInstance current = player.getEffect(MobEffects.DIG_SPEED);
        if (current == null
                || current.getAmplifier() < amplifier
                || current.getAmplifier() == amplifier && current.getDuration() <= 1) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED,
                    FULLNESS_HASTE_REFRESH_TICKS,
                    amplifier,
                    true,
                    false,
                    true));
        }
    }

    public record TickResult(boolean dataChanged, boolean syncRequired) {
        private static final TickResult NONE = new TickResult(false, false);
    }
}
