package com.kltyton.darwin_soldier.data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Pure math for the protected-health compensation modifier.
 *
 * <p>The floor is exactly the max-health contribution earned from Darwin health
 * points ({@code healthPoints * healthPerPoint}), not base health plus the
 * contribution. Foreign/base modifiers are never rewritten: the computation
 * only returns how much ADDITION compensation is needed on top of them.</p>
 */
public final class ProtectedHealthMath {
    private ProtectedHealthMath() {
    }

    /**
     * @param baseValue          the attribute base value (typically 20.0 for max health)
     * @param modifiers          the currently applied modifiers, including our own compensation
     * @param excludedModifierId our compensation modifier ID, excluded from the deficit
     * @param contribution       the protected floor (Darwin max-health contribution)
     * @return the ADDITION amount needed so effective MAX_HEALTH is at least the contribution,
     * or 0.0 when no deficit remains or no positive ADDITION scaling exists to compensate with
     */
    public static double compensationNeeded(double baseValue, Collection<AttributeModifier> modifiers,
                                            ResourceLocation excludedModifierId, double contribution) {
        if (!Double.isFinite(contribution) || contribution <= 0.0D) {
            return 0.0D;
        }
        double additionSum = 0.0D;
        double multiplyBaseSum = 0.0D;
        double multiplyTotalProduct = 1.0D;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.id().equals(excludedModifierId)) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> additionSum += modifier.amount();
                case ADD_MULTIPLIED_BASE -> multiplyBaseSum += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> multiplyTotalProduct *= 1.0D + modifier.amount();
            }
        }
        double baseFactor = 1.0D + multiplyBaseSum;
        if (baseFactor <= 0.0D || multiplyTotalProduct <= 0.0D) {
            // Degenerate scaling (for example a foreign -100% total multiplier): an
            // ADDITION compensation cannot lift the value, and we never delete such a
            // foreign modifier.
            return 0.0D;
        }
        double scale = baseFactor * multiplyTotalProduct;
        if (!Double.isFinite(scale)) {
            return 0.0D;
        }
        double preSanitized = (baseValue + additionSum) * scale;
        if (!Double.isFinite(preSanitized) || preSanitized >= contribution) {
            return 0.0D;
        }
        double needed = (contribution - preSanitized) / scale;
        return Double.isFinite(needed) && needed > 0.0D ? needed : 0.0D;
    }
}
