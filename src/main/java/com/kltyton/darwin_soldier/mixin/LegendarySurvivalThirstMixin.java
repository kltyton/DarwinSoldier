package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionDrinkScaling;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Scales the public LSO 2.4.6 drinking facade without making LSO a required runtime dependency. */
@Pseudo
@Mixin(targets = "sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil", remap = false)
public abstract class LegendarySurvivalThirstMixin {
    @Unique
    private static final ThreadLocal<Player> DARWIN_SOLDIER$DRINKING_PLAYER = new ThreadLocal<>();

    @Inject(method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V", at = @At("HEAD"), require = 0)
    private static void darwinSoldier$captureSimpleDrink(Player player, int hydration, float saturation,
                                                          CallbackInfo callback) {
        DARWIN_SOLDIER$DRINKING_PLAYER.set(player);
    }

    @ModifyArg(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lsfiomn/legendarysurvivaloverhaul/api/thirst/IThirstUtil;takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V",
                    remap = false
            ), index = 1,
            require = 0
    )
    private static int darwinSoldier$scaleSimpleHydration(int hydration) {
        return NutritionDrinkScaling.scaleHydration(DARWIN_SOLDIER$DRINKING_PLAYER.get(), hydration);
    }

    @ModifyArg(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lsfiomn/legendarysurvivaloverhaul/api/thirst/IThirstUtil;takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V",
                    remap = false
            ), index = 2,
            require = 0
    )
    private static float darwinSoldier$scaleSimpleSaturation(float saturation) {
        return NutritionDrinkScaling.scaleSaturation(DARWIN_SOLDIER$DRINKING_PLAYER.get(), saturation);
    }

    @Inject(method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IF)V", at = @At("RETURN"), require = 0)
    private static void darwinSoldier$clearSimpleDrink(Player player, int hydration, float saturation,
                                                        CallbackInfo callback) {
        DARWIN_SOLDIER$DRINKING_PLAYER.remove();
    }

    @Inject(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
            at = @At("HEAD"),
            require = 0
    )
    private static void darwinSoldier$captureDrinkWithEffects(Player player, int hydration, float saturation,
                                                               List<?> effects, CallbackInfo callback) {
        DARWIN_SOLDIER$DRINKING_PLAYER.set(player);
    }

    @ModifyArg(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lsfiomn/legendarysurvivaloverhaul/api/thirst/IThirstUtil;takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
                    remap = false
            ), index = 1,
            require = 0
    )
    private static int darwinSoldier$scaleHydrationWithEffects(int hydration) {
        return NutritionDrinkScaling.scaleHydration(DARWIN_SOLDIER$DRINKING_PLAYER.get(), hydration);
    }

    @ModifyArg(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lsfiomn/legendarysurvivaloverhaul/api/thirst/IThirstUtil;takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
                    remap = false
            ), index = 2,
            require = 0
    )
    private static float darwinSoldier$scaleSaturationWithEffects(float saturation) {
        return NutritionDrinkScaling.scaleSaturation(DARWIN_SOLDIER$DRINKING_PLAYER.get(), saturation);
    }

    @Inject(
            method = "takeDrink(Lnet/minecraft/world/entity/player/Player;IFLjava/util/List;)V",
            at = @At("RETURN"),
            require = 0
    )
    private static void darwinSoldier$clearDrinkWithEffects(Player player, int hydration, float saturation,
                                                             List<?> effects, CallbackInfo callback) {
        DARWIN_SOLDIER$DRINKING_PLAYER.remove();
    }
}
