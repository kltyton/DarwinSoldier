package com.kltyton.darwin_soldier.data.generation;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.combat.DarwinDamageTypes;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.Set;

public final class DarwinDataGenerators {
    private DarwinDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        DataGenerator generator = event.getGenerator();
        RegistrySetBuilder registryBuilder = new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, DarwinDamageTypes::bootstrap);
        DatapackBuiltinEntriesProvider damageTypes = generator.addProvider(true,
                new DatapackBuiltinEntriesProvider(generator.getPackOutput(), event.getLookupProvider(),
                        registryBuilder, Set.of(Darwin_soldier.MODID)));
        generator.addProvider(true, new DarwinDamageTypeTagsProvider(generator.getPackOutput(),
                damageTypes.getRegistryProvider(), event.getExistingFileHelper()));
    }
}
