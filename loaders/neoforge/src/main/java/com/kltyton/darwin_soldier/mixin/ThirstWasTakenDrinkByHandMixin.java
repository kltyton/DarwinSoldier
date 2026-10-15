package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.compat.thirst.ThirstDrinkContext;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import dev.ghen.thirst.foundation.network.message.DrinkByHandMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Allows drinking directly from water sources until Darwin's expanded thirst capacity is full. */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.foundation.network.message.DrinkByHandMessage", remap = false)
public abstract class ThirstWasTakenDrinkByHandMixin {
    @ModifyConstant(
            method = "lambda$serverHandle$0(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Ldev/ghen/thirst/foundation/network/message/DrinkByHandMessage;)V",
            constant = @Constant(intValue = 20),
            require = 1,
            allow = 1
    )
    private static int darwinSoldier$maximumHandDrinkingCapacity(int original, IPayloadContext context,
                                                                  DrinkByHandMessage message) {
        return NutritionFood.maximumFood(context.player());
    }

    @Inject(
            method = "lambda$serverHandle$0(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Ldev/ghen/thirst/foundation/network/message/DrinkByHandMessage;)V",
            at = @At("HEAD"),
            require = 1
    )
    private static void darwinSoldier$captureHandDrink(IPayloadContext context, DrinkByHandMessage message,
                                                        CallbackInfo callback) {
        ThirstDrinkContext.set(context.player());
    }

    @Inject(
            method = "lambda$serverHandle$0(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Ldev/ghen/thirst/foundation/network/message/DrinkByHandMessage;)V",
            at = @At("RETURN"),
            require = 1
    )
    private static void darwinSoldier$clearHandDrink(IPayloadContext context, DrinkByHandMessage message,
                                                      CallbackInfo callback) {
        ThirstDrinkContext.clear();
    }
}
