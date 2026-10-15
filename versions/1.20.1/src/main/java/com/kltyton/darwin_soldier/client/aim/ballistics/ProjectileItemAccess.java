package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;


public final class ProjectileItemAccess {
    private ProjectileItemAccess() {}
    public static boolean isFood(ItemStack stack) { return stack.isEdible(); }
    public static int useDuration(ItemStack stack, LivingEntity user) { return stack.getUseDuration(); }
    public static boolean chargedFirework(ItemStack stack) {
        return CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET);
    }
    public static boolean hasRiptide(ItemStack stack, LivingEntity user) {
        return EnchantmentHelper.getRiptide(stack) != 0;
    }
}
