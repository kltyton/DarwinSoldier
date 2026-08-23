package com.kltyton.darwin_soldier.nutrition;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NutritionDrinkScaling {
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();

    private NutritionDrinkScaling() {
    }

    public static int scaleHydration(Player player, int baseHydration) {
        if (!(player instanceof ServerPlayer serverPlayer) || baseHydration <= 0) {
            return baseHydration;
        }
        try {
            PlayerGrowthData data = GrowthSavedData.get(serverPlayer).getOrCreate(serverPlayer.getUUID());
            if (!data.isEnabled() || !DarwinConfig.NUTRITION_ENABLED.get()) {
                return baseHydration;
            }
            int points = data.getNutritionPoints();
            double bonusPerPoint = DarwinConfig.NUTRITION_DRINK_RESTORE_BONUS_PER_POINT.get();
            if (points <= 0 || bonusPerPoint <= 0.0D) {
                return baseHydration;
            }
            NutritionMath.IntegralScaling scaling = data.getNutrition().scaleDrinkRestoration(
                    baseHydration, bonusPerPoint);
            GrowthSavedData.get(serverPlayer).setDirty();
            return scaling.amount();
        } catch (LinkageError | RuntimeException exception) {
            logFailure(exception);
            return baseHydration;
        }
    }

    public static float scaleSaturation(Player player, float baseSaturation) {
        if (!(player instanceof ServerPlayer serverPlayer) || baseSaturation <= 0.0F) {
            return baseSaturation;
        }
        try {
            PlayerGrowthData data = GrowthSavedData.get(serverPlayer).getOrCreate(serverPlayer.getUUID());
            if (!data.isEnabled() || !DarwinConfig.NUTRITION_ENABLED.get()) {
                return baseSaturation;
            }
            int points = data.getNutritionPoints();
            double bonusPerPoint = DarwinConfig.NUTRITION_DRINK_RESTORE_BONUS_PER_POINT.get();
            if (points <= 0 || bonusPerPoint <= 0.0D) {
                return baseSaturation;
            }
            return (float) (baseSaturation * NutritionMath.restorationMultiplier(
                    points, bonusPerPoint));
        } catch (LinkageError | RuntimeException exception) {
            logFailure(exception);
            return baseSaturation;
        }
    }

    public static int scaleQuenched(Player player, int baseQuenched) {
        if (!(player instanceof ServerPlayer serverPlayer) || baseQuenched <= 0) {
            return baseQuenched;
        }
        try {
            PlayerGrowthData data = GrowthSavedData.get(serverPlayer).getOrCreate(serverPlayer.getUUID());
            if (!data.isEnabled() || !DarwinConfig.NUTRITION_ENABLED.get()) {
                return baseQuenched;
            }
            int points = data.getNutritionPoints();
            double bonusPerPoint = DarwinConfig.NUTRITION_DRINK_RESTORE_BONUS_PER_POINT.get();
            if (points <= 0 || bonusPerPoint <= 0.0D) {
                return baseQuenched;
            }
            return Math.max(baseQuenched, (int) Math.floor(baseQuenched
                    * NutritionMath.restorationMultiplier(points, bonusPerPoint)));
        } catch (LinkageError | RuntimeException exception) {
            logFailure(exception);
            return baseQuenched;
        }
    }

    private static void logFailure(Throwable exception) {
        if (FAILURE_LOGGED.compareAndSet(false, true)) {
            Darwin_soldier.LOGGER.error(
                    "Failed to apply the thirst drink bonus; base drinking behavior will be used",
                    exception);
        }
    }
}
