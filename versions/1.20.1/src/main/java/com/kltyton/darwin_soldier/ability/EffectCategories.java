package com.kltyton.darwin_soldier.ability;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;

public final class EffectCategories {
    private EffectCategories() {}
    public static boolean harmful(MobEffectInstance instance) {
        return instance.getEffect().getCategory() == MobEffectCategory.HARMFUL;
    }
}
