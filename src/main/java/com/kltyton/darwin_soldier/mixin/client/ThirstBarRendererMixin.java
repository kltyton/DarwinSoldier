package com.kltyton.darwin_soldier.mixin.client;

import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import dev.ghen.thirst.foundation.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapts Thirst Was Taken 2.1.5's thirst-bar renderer with dynamic capacity rows.
 */
@Pseudo
@Mixin(targets = "dev.ghen.thirst.foundation.gui.ThirstBarRenderer", remap = false)
public abstract class ThirstBarRendererMixin {
    private static final int ICONS_PER_ROW = 10;
    private static final int MAX_RENDERED_ICONS = 100;

    @Shadow
    public static IThirst PLAYER_THIRST;

    @Shadow
    public static ResourceLocation THIRST_ICONS;

    @Shadow
    private static Minecraft minecraft;

    @Shadow
    @Final
    protected static RandomSource random;

    @Inject(
            method = "render(IILnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void darwinSoldier$renderExpandedCapacity(int width, int height,
                                                              GuiGraphics guiGraphics, CallbackInfo callback) {
        if (minecraft.player == null) {
            return;
        }

        int maximum = NutritionFood.maximumFood(minecraft.player);
        if (maximum == 20) {
            return;
        }

        minecraft.getProfiler().push("thirst");
        if (PLAYER_THIRST == null || minecraft.player.tickCount % 40 == 0) {
            PLAYER_THIRST = minecraft.player.getData(ModAttachment.PLAYER_THIRST);
        }
        if (PLAYER_THIRST == null) {
            minecraft.getProfiler().pop();
            callback.cancel();
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, THIRST_ICONS);
        int left = width / 2 + 91 + ClientConfig.THIRST_BAR_X_OFFSET.get();
        int top = height - minecraft.gui.rightHeight + ClientConfig.THIRST_BAR_Y_OFFSET.get();
        int iconCount = Math.min(MAX_RENDERED_ICONS, 1 + (maximum - 1) / 2);
        int rowCount = 1 + (iconCount - 1) / ICONS_PER_ROW;
        minecraft.gui.rightHeight += rowCount * 10;

        int level = Math.max(0, Math.min(maximum, PLAYER_THIRST.getThirst()));
        long rawJitterPeriod = (long) level * 3L + 1L;
        int jitterPeriod = (int) Math.min(Integer.MAX_VALUE, rawJitterPeriod);
        for (int i = 0; i < iconCount; i++) {
            int row = i / ICONS_PER_ROW;
            int column = i % ICONS_PER_ROW;
            int idx = i * 2 + 1;
            int x = left - column * 8 - 9;
            int y = top - row * 10;
            if (PLAYER_THIRST.getQuenched() <= 0 && minecraft.gui.getGuiTicks() % jitterPeriod == 0) {
                y += random.nextInt(3) - 1;
            }

            guiGraphics.blit(THIRST_ICONS, x, y, 0.0F, 0.0F, 9, 9, 25, 9);
            if (idx < level) {
                guiGraphics.blit(THIRST_ICONS, x, y, 16.0F, 0.0F, 9, 9, 25, 9);
            } else if (idx == level) {
                guiGraphics.blit(THIRST_ICONS, x, y, 8.0F, 0.0F, 9, 9, 25, 9);
            }
        }

        RenderSystem.disableBlend();
        minecraft.getProfiler().pop();
        callback.cancel();
    }
}
