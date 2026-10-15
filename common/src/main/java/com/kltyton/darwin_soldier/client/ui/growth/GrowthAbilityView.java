package com.kltyton.darwin_soldier.client.ui.growth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class GrowthAbilityView {
    private GrowthAbilityView() {
    }

    static JsonArray tabs(Tab selected) {
        JsonArray tabs = new JsonArray();
        for (Tab tab : Tab.values()) {
            int requirement = Math.max(0, tab.requirement());
            int current = Math.max(0, tab.points());
            boolean available = tab.featureEnabled() && tab.unlocked();
            JsonObject entry = new JsonObject();
            entry.addProperty("id", tab.name());
            entry.addProperty("title", tr(tab.titleKey));
            entry.addProperty("attribute", tr(tab.attributeKey));
            entry.addProperty("texture", "darwin_soldier:textures/gui/skills/"
                    + tab.textureName + (available ? ".png" : "_locked.png"));
            entry.addProperty("current", Math.min(current, requirement));
            entry.addProperty("requirement", requirement);
            entry.addProperty("percent", requirement == 0 ? 100
                    : Math.min(100, (int) Math.round(current * 100.0D / requirement)));
            entry.addProperty("selected", selected == tab);
            entry.addProperty("available", available);
            tabs.add(entry);
        }
        return tabs;
    }

    static JsonObject detail(Tab tab, Minecraft minecraft) {
        JsonObject detail = new JsonObject();
        detail.addProperty("id", tab.name());
        detail.addProperty("title", tr(tab.titleKey));
        detail.add("lines", new JsonArray());
        detail.add("actions", new JsonArray());
        switch (tab) {
            case DAMAGE_ADAPTATION -> adaptation(detail);
            case HUNTING_INSTINCT -> hunting(detail, minecraft);
            case STRESS_EVOLUTION -> stress(detail, minecraft);
            case SUPER_PERCEPTION -> perception(detail, minecraft);
            case BATTLE_INSTINCT -> battle(detail);
            case EFFICIENT_METABOLISM -> metabolism(detail);
            case NUTRITION_FULLNESS -> fullness(detail, minecraft);
        }
        return detail;
    }

    private static void adaptation(JsonObject out) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isDamageAdaptationUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isDamageAdaptationEnabled())));
        muted(out, tr("screen.darwin_soldier.adaptations.summary", ClientGrowthData.getAdaptationEntries().size()));
        muted(out, tr("screen.darwin_soldier.adaptations.rules"));
        if (ClientGrowthData.isAdaptationFeatureEnabled() && ClientGrowthData.isDamageAdaptationUnlocked()) {
            actions(out, toggleAbility("DAMAGE_ADAPTATION", ClientGrowthData.isDamageAdaptationEnabled()),
                    button("open-adaptations",
                    tr("screen.darwin_soldier.adaptations.open", ClientGrowthData.getAdaptationEntries().size())));
        }
    }

    private static void hunting(JsonObject out, Minecraft minecraft) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isHuntingInstinctUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isHuntingInstinctEnabled())));
        line(out, tr("screen.darwin_soldier.hunting_state", huntingStatus(minecraft)));
        line(out, tr("screen.darwin_soldier.internal_impact_status",
                yesNo(ClientGrowthData.isHuntingInternalImpactFeatureEnabled()
                        && ClientGrowthData.isHuntingInternalImpactEnabled())));
        muted(out, tr("screen.darwin_soldier.hunting_desc_1"));
        muted(out, tr("screen.darwin_soldier.hunting_desc_2"));
        muted(out, tr("screen.darwin_soldier.hunting_params"));
        muted(out, tr("screen.darwin_soldier.hunting_internal_desc"));
        if (ClientGrowthData.isHuntingInstinctFeatureEnabled() && ClientGrowthData.isHuntingInstinctUnlocked()) {
            actions(out, toggleAbility("HUNTING_INSTINCT", ClientGrowthData.isHuntingInstinctEnabled()));
            if (ClientGrowthData.isHuntingInternalImpactFeatureEnabled()) {
                JsonObject impact = button("toggle-hunting-impact",
                        tr(ClientGrowthData.isHuntingInternalImpactEnabled()
                                ? "screen.darwin_soldier.internal_impact_on"
                                : "screen.darwin_soldier.internal_impact_off"));
                impact.addProperty("enabled", ClientGrowthData.isHuntingInternalImpactEnabled());
                actions(out, impact);
            }
        }
    }

    private static void stress(JsonObject out, Minecraft minecraft) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isStressEvolutionUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isStressEvolutionEnabled())));
        line(out, tr("screen.darwin_soldier.cooldown",
                cooldown(minecraft, ClientGrowthData.getStressEvolutionCooldownUntil())));
        muted(out, tr("screen.darwin_soldier.stress_desc_1"));
        muted(out, tr("screen.darwin_soldier.stress_desc_2"));
        muted(out, tr("screen.darwin_soldier.stress_params"));
        if (ClientGrowthData.isStressEvolutionFeatureEnabled() && ClientGrowthData.isStressEvolutionUnlocked()) {
            actions(out, toggleAbility("STRESS_EVOLUTION", ClientGrowthData.isStressEvolutionEnabled()));
        }
    }

    private static void perception(JsonObject out, Minecraft minecraft) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isSuperPerceptionUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isSuperPerceptionEnabled())));
        Component weapon = minecraft != null && minecraft.player != null
                ? minecraft.player.getMainHandItem().getHoverName()
                : Component.translatable("screen.darwin_soldier.none");
        line(out, tr("screen.darwin_soldier.current_weapon", weapon));
        muted(out, tr("screen.darwin_soldier.super_perception_desc_1"));
        muted(out, tr("screen.darwin_soldier.super_perception_desc_2"));
        muted(out, tr("screen.darwin_soldier.targets.summary", DarwinConfig.SUPER_PERCEPTION_WHITELIST.get().size()));
        if (ClientGrowthData.isSuperPerceptionUnlocked()) {
            actions(out, toggleAbility("SUPER_PERCEPTION", ClientGrowthData.isSuperPerceptionEnabled()),
                    button("open-aim", tr("screen.darwin_soldier.aim_settings")),
                    button("open-targets", tr("screen.darwin_soldier.targets.open")));
        }
    }

    private static void battle(JsonObject out) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isBattleInstinctUnlocked())));
        line(out, tr("screen.darwin_soldier.battle_instinct_status",
                tr(ClientGrowthData.isBattleInstinctEnabled()
                        ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled")));
        line(out, tr("screen.darwin_soldier.battle_instinct_reserves",
                ClientGrowthData.getBattleInstinctReserves(), ClientGrowthData.getBattleInstinctMaxReserves()));
        line(out, tr("screen.darwin_soldier.battle_instinct_mode",
                tr(ClientGrowthData.isBattleInstinctCounterEnabled()
                        ? "screen.darwin_soldier.battle_mode_normal"
                        : "screen.darwin_soldier.battle_mode_dodge_only")));
        muted(out, tr("screen.darwin_soldier.battle_instinct_mode_desc"));
        if (ClientGrowthData.isBattleInstinctFeatureEnabled() && ClientGrowthData.isBattleInstinctUnlocked()) {
            actions(out, toggleAbility("BATTLE_INSTINCT", ClientGrowthData.isBattleInstinctEnabled()),
                    button("toggle-battle-mode",
                    tr(ClientGrowthData.isBattleInstinctCounterEnabled()
                            ? "screen.darwin_soldier.battle_mode_normal"
                            : "screen.darwin_soldier.battle_mode_dodge_only")));
        }
    }

    private static void metabolism(JsonObject out) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isEfficientMetabolismUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isEfficientMetabolismEnabled())));
        muted(out, tr("screen.darwin_soldier.metabolism_description"));
        muted(out, tr("screen.darwin_soldier.metabolism_healing",
                format(ClientGrowthData.getEfficientMetabolismHealingPercent() * 100.0D)));
        muted(out, tr("screen.darwin_soldier.metabolism_food_cost", ClientGrowthData.getEfficientMetabolismFoodCost()));
        muted(out, tr("screen.darwin_soldier.thirst_link", tr(ClientGrowthData.isThirstLinked()
                ? "screen.darwin_soldier.linked" : "screen.darwin_soldier.not_linked")));
        if (ClientGrowthData.isNutritionFeatureEnabled() && ClientGrowthData.isEfficientMetabolismUnlocked()) {
            actions(out, toggleAbility("EFFICIENT_METABOLISM", ClientGrowthData.isEfficientMetabolismEnabled()));
        }
    }

    private static void fullness(JsonObject out, Minecraft minecraft) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isNutritionFullnessUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isNutritionFullnessEnabled())));
        line(out, tr("screen.darwin_soldier.current_status", tr(ClientGrowthData.isNutritionFullnessActive()
                ? "screen.darwin_soldier.active" : "screen.darwin_soldier.inactive")));
        muted(out, tr("screen.darwin_soldier.fullness_threshold",
                format(ClientGrowthData.getNutritionFullnessThreshold() * 100.0D)));
        muted(out, tr("screen.darwin_soldier.fullness_haste",
                effectLevel(ClientGrowthData.getNutritionFullnessHasteLevel())));
        float absorption = minecraft != null && minecraft.player != null ? minecraft.player.getAbsorptionAmount() : 0.0F;
        muted(out, tr("screen.darwin_soldier.fullness_absorption", format(absorption),
                format(ClientGrowthData.getNutritionFullnessAbsorptionCap())));
        muted(out, tr("screen.darwin_soldier.thirst_link", tr(ClientGrowthData.isThirstLinked()
                ? "screen.darwin_soldier.linked" : "screen.darwin_soldier.not_linked")));
        muted(out, tr("screen.darwin_soldier.fullness_combat_refill"));
        if (ClientGrowthData.isNutritionFeatureEnabled() && ClientGrowthData.isNutritionFullnessUnlocked()) {
            actions(out, toggleAbility("NUTRITION_FULLNESS", ClientGrowthData.isNutritionFullnessEnabled()));
        }
    }

    private static JsonObject toggleAbility(String ability, boolean enabled) {
        JsonObject action = button("toggle-ability",
                tr("screen.darwin_soldier.enable"));
        action.addProperty("ability", ability);
        action.addProperty("enabled", enabled);
        return action;
    }

    private static JsonObject button(String action, String label) {
        JsonObject result = new JsonObject();
        result.addProperty("action", action);
        result.addProperty("label", label);
        return result;
    }

    private static void actions(JsonObject detail, JsonObject... actions) {
        for (JsonObject action : actions) detail.getAsJsonArray("actions").add(action);
    }

    private static void line(JsonObject detail, String value) {
        addLine(detail, value, false);
    }

    private static void muted(JsonObject detail, String value) {
        addLine(detail, value, true);
    }

    private static void addLine(JsonObject detail, String text, boolean muted) {
        JsonObject line = new JsonObject();
        line.addProperty("text", text);
        line.addProperty("muted", muted);
        detail.getAsJsonArray("lines").add(line);
    }

    private static String cooldown(Minecraft minecraft, long cooldownUntil) {
        if (minecraft == null || minecraft.level == null) return tr("screen.darwin_soldier.none");
        long ticks = Math.max(0L, cooldownUntil - minecraft.level.getGameTime());
        return ticks <= 0L ? tr("hud.darwin_soldier.ready") : ((ticks + 19L) / 20L) + "s";
    }

    private static String huntingStatus(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null) return tr("screen.darwin_soldier.none");
        long ticks = ClientGrowthData.getHuntingInstinctActiveUntil() - minecraft.level.getGameTime();
        return ticks > 0L ? tr("hud.darwin_soldier.active_seconds", (ticks + 19L) / 20L)
                : cooldown(minecraft, ClientGrowthData.getHuntingInstinctCooldownUntil());
    }

    private static String yesNo(boolean value) {
        return tr(value ? "screen.darwin_soldier.yes" : "screen.darwin_soldier.no");
    }

    private static String effectLevel(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String tr(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }

    enum Tab {
        DAMAGE_ADAPTATION("damage_adaptation", "screen.darwin_soldier.tab_adaptation", "screen.darwin_soldier.attribute_defense"),
        HUNTING_INSTINCT("hunting_instinct", "screen.darwin_soldier.tab_hunting", "screen.darwin_soldier.attribute_attack"),
        STRESS_EVOLUTION("stress_evolution", "screen.darwin_soldier.tab_stress", "screen.darwin_soldier.attribute_health"),
        SUPER_PERCEPTION("super_perception", "screen.darwin_soldier.tab_super_perception", "screen.darwin_soldier.attribute_perception"),
        BATTLE_INSTINCT("battle_instinct", "screen.darwin_soldier.tab_battle", "screen.darwin_soldier.attribute_perception"),
        EFFICIENT_METABOLISM("efficient_metabolism", "screen.darwin_soldier.efficient_metabolism", "screen.darwin_soldier.attribute_nutrition"),
        NUTRITION_FULLNESS("nutrition_fullness", "screen.darwin_soldier.nutrition_fullness", "screen.darwin_soldier.attribute_nutrition");

        private final String textureName;
        private final String titleKey;
        private final String attributeKey;

        Tab(String textureName, String titleKey, String attributeKey) {
            this.textureName = textureName;
            this.titleKey = titleKey;
            this.attributeKey = attributeKey;
        }

        int points() {
            return switch (this) {
                case DAMAGE_ADAPTATION -> ClientGrowthData.getDefensePoints();
                case HUNTING_INSTINCT -> ClientGrowthData.getAttackPoints();
                case STRESS_EVOLUTION -> ClientGrowthData.getHealthPoints();
                case SUPER_PERCEPTION, BATTLE_INSTINCT -> ClientGrowthData.getPerceptionPoints();
                case EFFICIENT_METABOLISM, NUTRITION_FULLNESS -> ClientGrowthData.getNutritionPoints();
            };
        }

        int requirement() {
            return switch (this) {
                case DAMAGE_ADAPTATION -> ClientGrowthData.getDamageAdaptationUnlockPoints();
                case HUNTING_INSTINCT -> ClientGrowthData.getHuntingInstinctUnlockPoints();
                case STRESS_EVOLUTION -> ClientGrowthData.getStressEvolutionUnlockPoints();
                case SUPER_PERCEPTION -> ClientGrowthData.getSuperPerceptionUnlockPoints();
                case BATTLE_INSTINCT -> ClientGrowthData.getBattleInstinctUnlockPoints();
                case EFFICIENT_METABOLISM -> ClientGrowthData.getEfficientMetabolismUnlockPoints();
                case NUTRITION_FULLNESS -> ClientGrowthData.getNutritionFullnessUnlockPoints();
            };
        }

        boolean unlocked() {
            return switch (this) {
                case DAMAGE_ADAPTATION -> ClientGrowthData.isDamageAdaptationUnlocked();
                case HUNTING_INSTINCT -> ClientGrowthData.isHuntingInstinctUnlocked();
                case STRESS_EVOLUTION -> ClientGrowthData.isStressEvolutionUnlocked();
                case SUPER_PERCEPTION -> ClientGrowthData.isSuperPerceptionUnlocked();
                case BATTLE_INSTINCT -> ClientGrowthData.isBattleInstinctUnlocked();
                case EFFICIENT_METABOLISM -> ClientGrowthData.isEfficientMetabolismUnlocked();
                case NUTRITION_FULLNESS -> ClientGrowthData.isNutritionFullnessUnlocked();
            };
        }

        boolean featureEnabled() {
            return switch (this) {
                case DAMAGE_ADAPTATION -> ClientGrowthData.isAdaptationFeatureEnabled();
                case HUNTING_INSTINCT -> ClientGrowthData.isHuntingInstinctFeatureEnabled();
                case STRESS_EVOLUTION -> ClientGrowthData.isStressEvolutionFeatureEnabled();
                case SUPER_PERCEPTION -> true;
                case BATTLE_INSTINCT -> ClientGrowthData.isBattleInstinctFeatureEnabled();
                case EFFICIENT_METABOLISM, NUTRITION_FULLNESS -> ClientGrowthData.isNutritionFeatureEnabled();
            };
        }
    }
}
