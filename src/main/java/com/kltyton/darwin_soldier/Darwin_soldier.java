package com.kltyton.darwin_soldier;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.generation.DarwinDataGenerators;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.event.GrowthEvents;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.registry.ModCreativeTabs;
import com.kltyton.darwin_soldier.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Darwin_soldier.MODID)
public class Darwin_soldier {
    public static final String MODID = "darwin_soldier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Darwin_soldier() {
        RuntimeDiagnostics.info("mod_construct", "modId=" + MODID + " java=" + System.getProperty("java.version"));
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(DarwinDataGenerators::gatherData);
        modEventBus.addListener(ModCreativeTabs::addCreativeTabItems);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DarwinConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(GrowthEvents.class);
        RuntimeDiagnostics.info("mod_construct_complete", "commonConfigRegistered=true forgeEventsRegistered=true");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        RuntimeDiagnostics.info("common_setup", "enqueueNetworkRegistration=true");
        event.enqueueWork(ModNetwork::register);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == DarwinConfig.SPEC && DarwinConfig.migrateLegacyDefaults()) {
            LOGGER.info("Migrated Darwin Soldier legacy ability defaults to config version 1");
        }
    }
}
