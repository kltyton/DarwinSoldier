package com.kltyton.darwin_soldier.client.hud.growth;

import com.kltyton.darwin_soldier.growth.notification.GrowthGainReason;
import com.kltyton.darwin_soldier.network.GrowthGainNotificationPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GrowthGainHud {
    private static final int GROWTH_COLOR = 0xF4D35E;
    private static final int REASON_COLOR = 0xFFFFFF;
    private static final GrowthGainAccumulator ACCUMULATOR = new GrowthGainAccumulator();

    private GrowthGainHud() {
    }

    public static void accept(GrowthGainNotificationPacket packet) {
        ACCUMULATOR.accept(packet.amount(), packet.reason(), Util.getMillis());
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        if (minecraft.screen != null) {
            return;
        }
        GrowthGainAccumulator.DisplayState state = ACCUMULATOR.snapshot(Util.getMillis());
        if (!state.visible()) {
            return;
        }

        int x = width / 2;
        int y = height / 2 + 22;
        Component amount = Component.translatable("hud.darwin_soldier.growth_gain",
                GrowthGainFormatter.format(state.amount()));
        graphics.drawCenteredString(minecraft.font, amount, x, y, color(GROWTH_COLOR, state.alpha()));

        if (state.reason().special()) {
            graphics.drawCenteredString(minecraft.font, reasonText(state.reason()), x, y + 11,
                    color(REASON_COLOR, state.alpha()));
        }
    }

    public static void clear() {
        ACCUMULATOR.clear();
    }

    private static Component reasonText(GrowthGainReason reason) {
        String multiplier = GrowthGainFormatter.format(reason.multiplier());
        return switch (reason.type()) {
            case NORMAL -> Component.empty();
            case PET_KILL -> Component.translatable("hud.darwin_soldier.growth_reason.pet_kill", multiplier);
            case SAFE_COMBAT -> Component.translatable("hud.darwin_soldier.growth_reason.safe_combat", multiplier);
            case LOW_HEALTH -> Component.translatable("hud.darwin_soldier.growth_reason.low_health", multiplier);
        };
    }

    private static int color(int rgb, float alpha) {
        int channel = Math.max(4, Math.min(255, Math.round(alpha * 255.0F)));
        return channel << 24 | rgb;
    }
}
