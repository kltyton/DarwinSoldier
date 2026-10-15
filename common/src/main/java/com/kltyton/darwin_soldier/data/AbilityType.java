package com.kltyton.darwin_soldier.data;

import java.util.Locale;

public enum AbilityType {
    DAMAGE_ADAPTATION("damage_adaptation"),
    HUNTING_INSTINCT("hunting_instinct"),
    HUNTING_INTERNAL_IMPACT("hunting_internal_impact"),
    STRESS_EVOLUTION("stress_evolution"),
    SUPER_PERCEPTION("super_perception"),
    BATTLE_INSTINCT("battle_instinct"),
    EFFICIENT_METABOLISM("efficient_metabolism"),
    NUTRITION_FULLNESS("nutrition_fullness");

    private final String id;

    AbilityType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AbilityType byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        if ("adaptation".equals(normalized)) {
            return DAMAGE_ADAPTATION;
        }
        if ("hunt".equals(normalized) || "hunting".equals(normalized)) {
            return HUNTING_INSTINCT;
        }
        if ("impact".equals(normalized) || "internal_impact".equals(normalized)) {
            return HUNTING_INTERNAL_IMPACT;
        }
        if ("stress".equals(normalized)) {
            return STRESS_EVOLUTION;
        }
        if ("perception".equals(normalized) || "super_perception".equals(normalized)) {
            return SUPER_PERCEPTION;
        }
        if ("battle".equals(normalized) || "instinct".equals(normalized) || "combat_instinct".equals(normalized)) {
            return BATTLE_INSTINCT;
        }
        if ("metabolism".equals(normalized) || "efficient".equals(normalized)) {
            return EFFICIENT_METABOLISM;
        }
        if ("nutrition".equals(normalized) || "fullness".equals(normalized)) {
            return NUTRITION_FULLNESS;
        }
        for (AbilityType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ability: " + id);
    }
}
