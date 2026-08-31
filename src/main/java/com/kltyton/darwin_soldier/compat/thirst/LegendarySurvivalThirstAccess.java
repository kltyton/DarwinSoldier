package com.kltyton.darwin_soldier.compat.thirst;

import com.kltyton.darwin_soldier.data.NutritionData;
import net.minecraft.server.level.ServerPlayer;
import sfiomn.legendarysurvivaloverhaul.api.thirst.IThirstAttachment;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.common.attachments.thirst.ThirstAttachment;
import sfiomn.legendarysurvivaloverhaul.util.AttachmentUtil;

/**
 * Keeps LSO 2.4.7 attachment access isolated so an absent or incompatible LSO never links common gameplay code.
 */
final class LegendarySurvivalThirstAccess implements ThirstCompat.ThirstAccess {
    @Override
    public ThirstCompat.ThirstSnapshot snapshot(ServerPlayer player, NutritionData nutrition) {
        IThirstAttachment thirst = getAttachment(player);
        if (thirst == null || !ThirstUtil.isThirstActive(player)) {
            return ThirstCompat.ThirstSnapshot.unavailable();
        }

        double maximum = Math.max(1, ThirstAttachment.MAX_HYDRATION);
        double current = Math.max(0.0D, thirst.getHydrationLevel() - nutrition.getMetabolismThirstDebt());
        return new ThirstCompat.ThirstSnapshot(true, current, maximum);
    }

    @Override
    public boolean consume(ServerPlayer player, NutritionData nutrition, double amount) {
        IThirstAttachment thirst = getAttachment(player);
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

    private static IThirstAttachment getAttachment(ServerPlayer player) {
        return AttachmentUtil.getThirstAttachment(player);
    }
}
