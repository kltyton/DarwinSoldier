package com.kltyton.darwin_soldier;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.generation.DarwinDataGenerators;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.event.GrowthEvents;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.registry.ModCreativeTabs;
import com.kltyton.darwin_soldier.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Darwin_soldier.MODID)
public class Darwin_soldier {
    public static final String MODID = "darwin_soldier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Darwin_soldier(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeDiagnostics.info("mod_construct", "modId=" + MODID + " java=" + System.getProperty("java.version"));

        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(DarwinDataGenerators::gatherData);
        modEventBus.addListener(ModCreativeTabs::addCreativeTabItems);

        modContainer.registerConfig(ModConfig.Type.COMMON, DarwinConfig.SPEC);

        NeoForge.EVENT_BUS.register(GrowthEvents.class);
        RuntimeDiagnostics.info("mod_construct_complete", "commonConfigRegistered=true gameEventsRegistered=true");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        RuntimeDiagnostics.info("common_setup", "networkPayloadsRegistered=true");
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == DarwinConfig.SPEC && DarwinConfig.migrateLegacyDefaults()) {
            LOGGER.info("Migrated Darwin Soldier legacy ability defaults to config version 1");
        }
    }
}
