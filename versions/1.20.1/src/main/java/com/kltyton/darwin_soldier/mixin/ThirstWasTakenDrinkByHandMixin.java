package com.kltyton.darwin_soldier.mixin;

import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.network.message.DrinkByHandMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allows drinking directly from water sources until Darwin's expanded thirst capacity is full. */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.foundation.network.message.DrinkByHandMessage", remap = false)
public abstract class ThirstWasTakenDrinkByHandMixin {
    @ModifyConstant(
            method = "lambda$handle$0(Lnet/minecraft/world/level/Level;Ldev/ghen/thirst/foundation/network/message/DrinkByHandMessage;Lnet/minecraft/world/entity/player/Player;Ldev/ghen/thirst/foundation/common/capability/IThirst;)V",
            constant = @Constant(intValue = 20),
            require = 1,
            allow = 1
    )
    private static int darwinSoldier$maximumHandDrinkingCapacity(int original, Level level,
                                                                  DrinkByHandMessage message, Player player,
                                                                  IThirst thirst) {
        return NutritionFood.maximumFood(player);
    }
}
