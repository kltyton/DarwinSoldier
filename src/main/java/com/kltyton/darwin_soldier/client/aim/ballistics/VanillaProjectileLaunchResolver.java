package com.kltyton.darwin_soldier.client.aim.ballistics;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class VanillaProjectileLaunchResolver {
    private VanillaProjectileLaunchResolver() {
    }

    public static ProjectileLaunchState resolve(LocalPlayer player, ItemStack stack, float partialTick) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        Vec3 arrowOrigin = interpolatedOrigin(player, partialTick, 0.1D);

        if (item instanceof BowItem) {
            if (!isUsingMainHand(player, stack)) {
                return null;
            }
            int chargeTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
            double speed = VanillaProjectilePhysics.bowLaunchSpeed(chargeTicks);
            if (speed < 0.1D) {
                return null;
            }
            Vec3 inherited = inheritedVelocity(player, true);
            return arrowLike("vanilla_bow", arrowOrigin,
                    shootFromRotation(player, 0.0F, speed, inherited), inherited);
        }

        if (item instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            boolean firework = CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET);
            double speed = firework ? 1.6D : 3.15D;
            Vec3 origin = interpolatedOrigin(player, partialTick, firework ? 0.15D : 0.1D);
            return new ProjectileLaunchState(firework ? "vanilla_crossbow_firework" : "vanilla_crossbow_arrow",
                    origin, direction(player, 0.0F).scale(speed), Vec3.ZERO, firework ? 0.0D : 0.05D,
                    firework ? 1.0D : VanillaProjectilePhysics.AIR_DRAG,
                    firework ? 1.0D : VanillaProjectilePhysics.ARROW_WATER_DRAG, false, 1.0D);
        }

        if (item instanceof TridentItem && isUsingMainHand(player, stack)) {
            int chargeTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
            int riptide = EnchantmentHelper.getRiptide(stack);
            if (chargeTicks < 10 || riptide != 0) {
                return null;
            }
            Vec3 inherited = inheritedVelocity(player, true);
            return arrowLike("vanilla_trident", arrowOrigin,
                    shootFromRotation(player, 0.0F, 2.5D, inherited), inherited);
        }

        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            Vec3 inherited = inheritedVelocity(player, true);
            return throwable("vanilla_throwable", arrowOrigin,
                    shootFromRotation(player, 0.0F, 1.5D, inherited), inherited, 0.03D);
        }
        if (item instanceof ExperienceBottleItem) {
            Vec3 inherited = inheritedVelocity(player, true);
            return throwable("vanilla_experience_bottle", arrowOrigin,
                    shootFromRotation(player, -20.0F, 0.7D, inherited), inherited, 0.07D);
        }
        if (item instanceof ThrowablePotionItem) {
            Vec3 inherited = inheritedVelocity(player, true);
            return throwable("vanilla_potion", arrowOrigin,
                    shootFromRotation(player, -20.0F, 0.5D, inherited), inherited, 0.05D);
        }
        return null;
    }

    public static boolean isSupported(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem
                || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
    }

    public static boolean isInWater(LocalPlayer player, Vec3 position) {
        return player.level().getFluidState(net.minecraft.core.BlockPos.containing(position)).is(FluidTags.WATER);
    }

    private static ProjectileLaunchState arrowLike(String source, Vec3 origin, Vec3 velocity, Vec3 inheritedVelocity) {
        return new ProjectileLaunchState(source, origin, velocity, inheritedVelocity, 0.05D,
                VanillaProjectilePhysics.AIR_DRAG, VanillaProjectilePhysics.ARROW_WATER_DRAG, false, 1.0D);
    }

    private static ProjectileLaunchState throwable(
            String source,
            Vec3 origin,
            Vec3 velocity,
            Vec3 inheritedVelocity,
            double gravity
    ) {
        return new ProjectileLaunchState(source, origin, velocity, inheritedVelocity, gravity,
                VanillaProjectilePhysics.AIR_DRAG, VanillaProjectilePhysics.THROWABLE_WATER_DRAG, false, 1.0D);
    }

    private static boolean isUsingMainHand(LocalPlayer player, ItemStack stack) {
        return player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem().getItem() == stack.getItem();
    }

    private static Vec3 interpolatedOrigin(LocalPlayer player, float partialTick, double eyeOffset) {
        return player.getPosition(partialTick).add(0.0D, player.getEyeHeight() - eyeOffset, 0.0D);
    }

    private static Vec3 inheritedVelocity(LocalPlayer player, boolean inheritShooterVelocity) {
        if (!inheritShooterVelocity) {
            return Vec3.ZERO;
        }
        Vec3 shooterVelocity = player.getDeltaMovement();
        return new Vec3(shooterVelocity.x, player.onGround() ? 0.0D : shooterVelocity.y, shooterVelocity.z);
    }

    private static Vec3 shootFromRotation(LocalPlayer player, float pitchOffset, double speed,
                                           Vec3 inheritedVelocity) {
        return direction(player, pitchOffset).scale(speed).add(inheritedVelocity);
    }

    private static Vec3 direction(LocalPlayer player, float pitchOffset) {
        float pitch = (player.getXRot() + pitchOffset) * Mth.DEG_TO_RAD;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch),
                Mth.cos(yaw) * Mth.cos(pitch)).normalize();
    }
}
