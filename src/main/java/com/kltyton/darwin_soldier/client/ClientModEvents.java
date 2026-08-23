package com.kltyton.darwin_soldier.client;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.client.ui.config.DarwinConfigAuiScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Darwin_soldier.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping OPEN_GROWTH_SCREEN = new KeyMapping(
            "key.darwin_soldier.open_growth_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.darwin_soldier"
    );
    public static final KeyMapping ACTIVATE_HUNTING_INSTINCT = new KeyMapping(
            "key.darwin_soldier.activate_hunting_instinct",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.darwin_soldier"
    );
    public static final KeyMapping SUPER_PERCEPTION = new KeyMapping(
            "key.darwin_soldier.super_perception",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.darwin_soldier"
    );
    public static final KeyMapping OPEN_AIM_SETTINGS = new KeyMapping(
            "key.darwin_soldier.open_aim_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.darwin_soldier"
    );
    public static final KeyMapping ACTIVATE_STRESS_EVOLUTION = new KeyMapping(
            "key.darwin_soldier.activate_stress_evolution",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.darwin_soldier"
    );
    public static final KeyMapping EFFICIENT_METABOLISM = new KeyMapping(
            "key.darwin_soldier.efficient_metabolism",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.darwin_soldier"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new DarwinConfigAuiScreen(parent)));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GROWTH_SCREEN);
        event.register(ACTIVATE_HUNTING_INSTINCT);
        event.register(SUPER_PERCEPTION);
        event.register(OPEN_AIM_SETTINGS);
        event.register(ACTIVATE_STRESS_EVOLUTION);
        event.register(EFFICIENT_METABOLISM);
    }
}
