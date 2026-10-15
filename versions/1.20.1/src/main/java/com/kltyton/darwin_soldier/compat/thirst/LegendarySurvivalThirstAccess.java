package com.kltyton.darwin_soldier.compat.thirst;

import com.kltyton.darwin_soldier.data.NutritionData;
import net.minecraft.server.level.ServerPlayer;
import sfiomn.legendarysurvivaloverhaul.api.thirst.IThirstCapability;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstCapability;
import sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstProvider;

/**
 * LSO 2.4.6 exposes the thirst value interface publicly, but not capability lookup or its configured maximum.
 * Those two internal references stay isolated here so an absent or incompatible LSO never links common gameplay code.
 */
final class LegendarySurvivalThirstAccess implements ThirstCompat.ThirstAccess {
    @Override
    public ThirstCompat.ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition) {
        IThirstCapability thirst = getCapability(player);
        if (thirst == null || !ThirstUtil.isThirstActive(player)) {
            return ThirstCompat.ThirstSnapshot.unavailable();
        }

        double maximum = Math.max(1, ThirstCapability.MAX_HYDRATION);
        double current = Math.max(0.0D, thirst.getHydrationLevel() - nutrition.getMetabolismThirstDebt());
        return new ThirstCompat.ThirstSnapshot(true, current, maximum);
    }

    @Override
    public boolean consume(ServerPlayer player, NutritionData nutrition, double amount) {
        IThirstCapability thirst = getCapability(player);
        if (thirst == null || !ThirstUtil.isThirstActive(player)) {
            return false;
        }

        int wholeCost = nutrition.accumulateThirstCost(amount);
        if (wholeCost > 0) {
            thirst.setHydrationLevel(Math.max(0, thirst.getHydrationLevel() - wholeCost));
            thirst.setDirty();
        }
        return true;
    }

    private static IThirstCapability getCapability(ServerPlayer player) {
        if (ThirstProvider.THIRST_CAPABILITY == null) {
            return null;
        }
        return player.getCapability(ThirstProvider.THIRST_CAPABILITY).resolve().orElse(null);
    }
}
