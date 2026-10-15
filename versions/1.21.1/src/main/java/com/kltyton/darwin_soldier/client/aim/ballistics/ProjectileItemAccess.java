package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ChargedProjectiles;

public final class ProjectileItemAccess {
    private ProjectileItemAccess() {}
    public static boolean isFood(ItemStack stack) { return stack.has(DataComponents.FOOD); }
    public static int useDuration(ItemStack stack, LivingEntity user) { return stack.getUseDuration(user); }
    public static boolean chargedFirework(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).contains(Items.FIREWORK_ROCKET);
    }
    public static boolean hasRiptide(ItemStack stack, LivingEntity user) {
        return EnchantmentHelper.getTridentSpinAttackStrength(stack, user) > 0.0F;
    }
}
