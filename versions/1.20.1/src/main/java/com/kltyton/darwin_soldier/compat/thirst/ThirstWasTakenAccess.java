package com.kltyton.darwin_soldier.compat.thirst;

import com.kltyton.darwin_soldier.data.NutritionData;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;

/** Keeps every direct Thirst Was Taken 1.20.1 API reference behind the optional-mod boundary. */
final class ThirstWasTakenAccess implements ThirstCompat.ThirstAccess {
    @Override
    public ThirstCompat.ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition) {
        IThirst thirst = getCapability(player);
        if (thirst == null) {
            return ThirstCompat.ThirstSnapshot.unavailable();
        }
        double current = Math.max(0.0D, thirst.getThirst() - nutrition.getMetabolismThirstDebt());
        return new ThirstCompat.ThirstSnapshot(true, current, NutritionFood.maximumFood(player));
    }

    @Override
    public boolean consume(ServerPlayer player, NutritionData nutrition, double amount) {
        IThirst thirst = getCapability(player);
        if (thirst == null) {
            return false;
        }

        int wholeCost = nutrition.accumulateThirstCost(amount);
        if (wholeCost > 0) {
            thirst.setThirst(Math.max(0, thirst.getThirst() - wholeCost));
            thirst.setQuenched(Math.min(thirst.getQuenched(), thirst.getThirst()));
            thirst.updateThirstData(player);
        }
        return true;
    }

    private static IThirst getCapability(ServerPlayer player) {
        if (ModCapabilities.PLAYER_THIRST == null) {
            return null;
        }
        return player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElse(null);
    }
}
