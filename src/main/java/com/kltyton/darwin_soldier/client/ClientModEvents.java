package com.kltyton.darwin_soldier.client;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.client.ui.config.DarwinConfigAuiScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = Darwin_soldier.MODID, dist = Dist.CLIENT)
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

    public ClientModEvents(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new DarwinConfigAuiScreen(parent));
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
        NeoForge.EVENT_BUS.register(ClientNeoForgeEvents.class);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GROWTH_SCREEN);
        event.register(ACTIVATE_HUNTING_INSTINCT);
        event.register(SUPER_PERCEPTION);
        event.register(OPEN_AIM_SETTINGS);
        event.register(ACTIVATE_STRESS_EVOLUTION);
        event.register(EFFICIENT_METABOLISM);
    }
}
