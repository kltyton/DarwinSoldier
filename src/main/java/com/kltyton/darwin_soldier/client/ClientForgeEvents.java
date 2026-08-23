package com.kltyton.darwin_soldier.client;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.client.battle.BattleInstinctClientVisuals;
import com.kltyton.darwin_soldier.client.aim.AimAuiScreen;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallisticsTracker;
import com.kltyton.darwin_soldier.client.aim.render.TrajectoryPreviewRenderer;
import com.kltyton.darwin_soldier.client.aim.render.lock.TargetLockRenderer;
import com.kltyton.darwin_soldier.client.hud.growth.GrowthGainHud;
import com.kltyton.darwin_soldier.client.ui.growth.GrowthAuiScreen;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Darwin_soldier.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static boolean efficientMetabolismDown;


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ProjectileBallisticsTracker.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            GrowthGainHud.clear();
            BattleInstinctClientVisuals.tick(minecraft);
            return;
        }

        BattleInstinctClientVisuals.tick(minecraft);

        while (ClientModEvents.OPEN_GROWTH_SCREEN.consumeClick()) {
            RuntimeDiagnostics.info("growth_screen_open_key", "requestingServerSync=true currentRevision="
                    + ClientGrowthData.getRevision() + " previousScreen="
                    + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getName()));
            ModNetwork.requestSyncFromServer();
            minecraft.setScreen(new GrowthAuiScreen());
        }

        while (ClientModEvents.ACTIVATE_HUNTING_INSTINCT.consumeClick()) {
            if (canActivateHunting(minecraft)) {
                RuntimeDiagnostics.info("hunting_key", "acceptedClientSide=true now="
                        + minecraft.level.getGameTime());
                ModNetwork.activateHuntingInstinct();
            } else {
                RuntimeDiagnostics.info("hunting_key", "acceptedClientSide=false now="
                        + minecraft.level.getGameTime() + " feature="
                        + ClientGrowthData.isHuntingInstinctFeatureEnabled() + " unlocked="
                        + ClientGrowthData.isHuntingInstinctUnlocked() + " enabled="
                        + ClientGrowthData.isHuntingInstinctEnabled() + " activeUntil="
                        + ClientGrowthData.getHuntingInstinctActiveUntil() + " cooldownUntil="
                        + ClientGrowthData.getHuntingInstinctCooldownUntil());
            }
        }

        while (ClientModEvents.OPEN_AIM_SETTINGS.consumeClick()) {
            if (ClientGrowthData.isSuperPerceptionUnlocked()) {
                RuntimeDiagnostics.info("aim_screen_open_key", "accepted=true mainHand="
                        + minecraft.player.getMainHandItem().getItem());
                minecraft.setScreen(new AimAuiScreen(null));
            } else {
                RuntimeDiagnostics.info("aim_screen_open_key", "accepted=false reason=locked perception="
                        + ClientGrowthData.getPerceptionPoints() + " requirement="
                        + ClientGrowthData.getSuperPerceptionUnlockPoints());
            }
        }

        SuperPerceptionClient.tick(minecraft, ClientModEvents.SUPER_PERCEPTION.isDown());

        while (ClientModEvents.ACTIVATE_STRESS_EVOLUTION.consumeClick()) {
            ModNetwork.activateStressEvolution();
        }

        boolean metabolismDown = ClientModEvents.EFFICIENT_METABOLISM.isDown()
                && ClientGrowthData.isEfficientMetabolismUnlocked()
                && ClientGrowthData.isEfficientMetabolismEnabled();
        if (metabolismDown != efficientMetabolismDown) {
            efficientMetabolismDown = metabolismDown;
            ModNetwork.sendEfficientMetabolismHeld(metabolismDown);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        // Sole automatic aim-rotation writer: advances the smoother at frame
        // rate instead of the 20 Hz client tick rate.
        SuperPerceptionClient.renderFrame(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) {
            return;
        }
        if (event.isUseItem() && event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ItemStack weapon = event.isUseItem()
                ? minecraft.player.getItemInHand(event.getHand())
                : minecraft.player.getMainHandItem();
        ProjectileBallisticsTracker.noteTrigger(minecraft, weapon);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        TrajectoryPreviewRenderer.render(event);
        TargetLockRenderer.render(event);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientGrowthData.isEnabled()
                || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        GrowthGainHud.render(graphics, minecraft,
                event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        int x = event.getWindow().getGuiScaledWidth() / 2 + 96;
        int y = event.getWindow().getGuiScaledHeight() - 42;

        if (ClientGrowthData.isNutritionFeatureEnabled() && ClientGrowthData.isNutritionFullnessActive()) {
            drawStatusLine(graphics, minecraft, Component.translatable("hud.darwin_soldier.nutrition_fullness"),
                    x, y, 0xF4D35E);
            y -= 14;
        }
        if (ClientGrowthData.isBattleInstinctFeatureEnabled() && ClientGrowthData.isBattleInstinctUnlocked()) {
            drawStatusLine(graphics, minecraft, Component.translatable("hud.darwin_soldier.battle_instinct",
                    Component.translatable(ClientGrowthData.isBattleInstinctEnabled() ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled"),
                    ClientGrowthData.getBattleInstinctReserves(),
                    ClientGrowthData.getBattleInstinctMaxReserves()), x, y, 0xB8F5A4);
            y -= 14;
        }
        if (ClientGrowthData.isHuntingInstinctFeatureEnabled()
                && ClientGrowthData.isHuntingInstinctUnlocked() && ClientGrowthData.isHuntingInstinctEnabled()) {
            drawCooldownLine(graphics, minecraft, MobEffects.DAMAGE_BOOST, Component.translatable("hud.darwin_soldier.hunting",
                    formatHuntingStatus()), x, y, 0xFFD55A);
            y -= 20;
        }
        if (ClientGrowthData.isStressEvolutionFeatureEnabled()
                && ClientGrowthData.isStressEvolutionUnlocked() && ClientGrowthData.isStressEvolutionEnabled()) {
            drawCooldownLine(graphics, minecraft, MobEffects.DAMAGE_RESISTANCE, Component.translatable("hud.darwin_soldier.stress",
                    formatCooldown(ClientGrowthData.getStressEvolutionCooldownUntil())), x, y, 0x8FD7FF);
        }
    }

    private static void drawCooldownLine(GuiGraphics graphics, Minecraft minecraft, MobEffect effect, Component text, int x, int y, int color) {
        graphics.blit(x, y - 5, 0, 18, 18, minecraft.getMobEffectTextures().get(effect));
        graphics.drawString(minecraft.font, text, x + 22, y, color);
    }

    private static void drawStatusLine(GuiGraphics graphics, Minecraft minecraft, Component text, int x, int y, int color) {
        graphics.drawString(minecraft.font, text, x, y, color);
    }

    private static String formatCooldown(long cooldownUntil) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return "";
        }

        long remainingTicks = Math.max(0L, cooldownUntil - minecraft.level.getGameTime());
        if (remainingTicks <= 0L) {
            return Component.translatable("hud.darwin_soldier.ready").getString();
        }
        return ((remainingTicks + 19L) / 20L) + "s";
    }

    private static boolean canActivateHunting(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }
        long now = minecraft.level.getGameTime();
        return ClientGrowthData.isHuntingInstinctFeatureEnabled()
                && ClientGrowthData.isHuntingInstinctUnlocked()
                && ClientGrowthData.isHuntingInstinctEnabled()
                && ClientGrowthData.getHuntingInstinctCooldownUntil() <= now
                && ClientGrowthData.getHuntingInstinctActiveUntil() <= now;
    }

    private static String formatHuntingStatus() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return "";
        }
        long now = minecraft.level.getGameTime();
        long activeTicks = ClientGrowthData.getHuntingInstinctActiveUntil() - now;
        if (activeTicks > 0L) {
            return Component.translatable("hud.darwin_soldier.active_seconds", (activeTicks + 19L) / 20L).getString();
        }
        return formatCooldown(ClientGrowthData.getHuntingInstinctCooldownUntil());
    }
}
