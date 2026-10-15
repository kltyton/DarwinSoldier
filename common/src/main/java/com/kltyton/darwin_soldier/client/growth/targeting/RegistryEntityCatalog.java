package com.kltyton.darwin_soldier.client.growth.targeting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Registry enumeration follows ReEntityOutliner's MIT-licensed 1.20.1 entity selector:
 * https://github.com/SioGabx/ReEntityOutliner/tree/1130f002b7460015d1b20304c04658df7de66f22
 */
final class RegistryEntityCatalog {
    private static final Set<String> NON_TARGET_PATHS = Set.of(
            "experience_orb",
            "area_effect_cloud",
            "interaction",
            "marker",
            "lightning_bolt",
            "ominous_item_spawner",
            "lingering_potion"
    );

    private RegistryEntityCatalog() {
    }

    static List<Entry> all() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .map(type -> new Entry(type, BuiltInRegistries.ENTITY_TYPE.getKey(type)))
                .filter(entry -> entry.id() != null && !NON_TARGET_PATHS.contains(entry.id().getPath()))
                .sorted(Comparator.comparing((Entry entry) -> entry.type().getDescription().getString(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
    }

    static boolean matches(Entry entry, String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        String name = entry.type().getDescription().getString().toLowerCase(Locale.ROOT);
        String id = entry.id().toString().toLowerCase(Locale.ROOT);
        return name.contains(normalized) || id.contains(normalized);
    }

    record Entry(EntityType<?> type, ResourceLocation id) {
    }
}
