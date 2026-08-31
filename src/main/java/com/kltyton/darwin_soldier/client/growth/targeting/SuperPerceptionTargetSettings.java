package com.kltyton.darwin_soldier.client.growth.targeting;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

final class SuperPerceptionTargetSettings {
    private SuperPerceptionTargetSettings() {
    }

    static void toggle(ModConfigSpec.BooleanValue value) {
        value.set(!value.get());
        value.save();
    }

    static boolean isWhitelisted(ResourceLocation id) {
        return whitelistIds().contains(id.toString().toLowerCase(Locale.ROOT));
    }

    static void setWhitelisted(ResourceLocation id, boolean enabled) {
        TreeSet<String> ids = new TreeSet<>(whitelistIds());
        String normalized = id.toString().toLowerCase(Locale.ROOT);
        if (enabled) {
            ids.add(normalized);
        } else {
            ids.remove(normalized);
        }
        DarwinConfig.SUPER_PERCEPTION_WHITELIST.set(List.copyOf(ids));
        DarwinConfig.SUPER_PERCEPTION_WHITELIST.save();
    }

    private static Set<String> whitelistIds() {
        TreeSet<String> ids = new TreeSet<>();
        for (String value : DarwinConfig.SUPER_PERCEPTION_WHITELIST.get()) {
            if (value != null && !value.isBlank()) {
                ids.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return ids;
    }
}
