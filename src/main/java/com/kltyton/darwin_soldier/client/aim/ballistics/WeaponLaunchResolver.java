package com.kltyton.darwin_soldier.client.aim.ballistics;

import com.kltyton.darwin_soldier.client.aim.compat.tacz.TaczBallisticsResolver;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * 弹道发射状态门面：先尝试原版解析，原版不支持且 TaCZ 已加载时再走 TaCZ 兼容解析。
 * 本类不直接引用任何 TaCZ 类型，TaCZ 缺失时不会触及其类加载。
 */
public final class WeaponLaunchResolver {
    private static final String TACZ_MOD_ID = "tacz";

    private WeaponLaunchResolver() {
    }

    public static ProjectileLaunchState resolve(LocalPlayer player, ItemStack stack, float partialTick) {
        ProjectileLaunchState state = VanillaProjectileLaunchResolver.resolve(player, stack, partialTick);
        if (state == null && isTaczLoaded()) {
            state = TaczBallisticsResolver.resolve(player, stack, partialTick);
        }
        return state;
    }

    public static boolean isSupported(ItemStack stack) {
        return VanillaProjectileLaunchResolver.isSupported(stack)
                || (isTaczLoaded() && TaczBallisticsResolver.isSupported(stack));
    }

    public static boolean isInWater(LocalPlayer player, Vec3 position) {
        return VanillaProjectileLaunchResolver.isInWater(player, position);
    }

    private static boolean isTaczLoaded() {
        return ModList.get().isLoaded(TACZ_MOD_ID);
    }
}
