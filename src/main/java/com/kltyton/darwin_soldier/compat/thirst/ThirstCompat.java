package com.kltyton.darwin_soldier.compat.thirst;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.data.NutritionData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class ThirstCompat {
    public static final String THIRST_WAS_TAKEN_MOD_ID = "thirst";
    public static final String LEGENDARY_SURVIVAL_MOD_ID = "legendarysurvivaloverhaul";

    private static final ThirstAccess ACCESS = createAccess();
    private static volatile boolean disabledByFailure;

    private ThirstCompat() {
    }

    public static ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition) {
        if (disabledByFailure) {
            return ThirstSnapshot.unavailable();
        }
        try {
            return ACCESS.snapshot(player, nutrition);
        } catch (LinkageError | RuntimeException exception) {
            disable(exception);
            return ThirstSnapshot.unavailable();
        }
    }

    public static boolean consume(ServerPlayer player, NutritionData nutrition, double amount) {
        if (disabledByFailure) {
            return false;
        }
        try {
            return ACCESS.consume(player, nutrition, amount);
        } catch (LinkageError | RuntimeException exception) {
            disable(exception);
            return false;
        }
    }

    private static ThirstAccess createAccess() {
        if (ModList.get().isLoaded(THIRST_WAS_TAKEN_MOD_ID)) {
            try {
                Darwin_soldier.LOGGER.info("Thirst Was Taken compatibility enabled");
                return new ThirstWasTakenAccess();
            } catch (LinkageError | RuntimeException exception) {
                disable(exception);
                return ThirstAccess.UNAVAILABLE;
            }
        }
        if (ModList.get().isLoaded(LEGENDARY_SURVIVAL_MOD_ID)) {
            try {
                Darwin_soldier.LOGGER.info("Legendary Survival Overhaul compatibility enabled");
                return new LegendarySurvivalThirstAccess();
            } catch (LinkageError | RuntimeException exception) {
                disable(exception);
                return ThirstAccess.UNAVAILABLE;
            }
        }
        return ThirstAccess.UNAVAILABLE;
    }

    private static void disable(Throwable exception) {
        if (!disabledByFailure) {
            disabledByFailure = true;
            Darwin_soldier.LOGGER.error(
                    "Thirst compatibility was disabled after an API failure; Darwin Soldier will continue without thirst integration",
                    exception);
        }
    }

    interface ThirstAccess {
        ThirstAccess UNAVAILABLE = new ThirstAccess() {
            @Override
            public ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition) {
                return ThirstSnapshot.unavailable();
            }

            @Override
            public boolean consume(ServerPlayer player, NutritionData nutrition, double amount) {
                return false;
            }
        };

        ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition);

        boolean consume(ServerPlayer player, NutritionData nutrition, double amount);
    }

    public record ThirstSnapshot(boolean linked, double current, double maximum) {
        public static ThirstSnapshot unavailable() {
            return new ThirstSnapshot(false, 0.0D, 0.0D);
        }

        public double ratio() {
            return maximum > 0.0D ? Math.max(0.0D, current) / maximum : 0.0D;
        }
    }
}
