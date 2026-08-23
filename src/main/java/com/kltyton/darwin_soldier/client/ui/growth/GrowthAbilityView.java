package com.kltyton.darwin_soldier.client.ui.growth;

import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class GrowthAbilityView {
    private GrowthAbilityView() {
    }

    static String tabs(Tab selected) {
        StringBuilder markup = new StringBuilder();
        for (Tab tab : Tab.values()) {
            int requirement = Math.max(0, tab.requirement());
            int current = Math.max(0, tab.points());
            int shownCurrent = Math.min(current, requirement);
            int percent = requirement == 0 ? 100 : Math.min(100, (int) Math.round(current * 100.0D / requirement));
            boolean enabled = tab.featureEnabled() && tab.unlocked();
            String classes = "darwin-ability-tab" + (selected == tab ? " active" : "");
            markup.append("<button id=\"ability-tab-").append(tab.name()).append("\" class=\"")
                    .append(classes).append("\" type=\"button\" role=\"tab\" data-action=\"select-ability\" data-tab=\"")
                    .append(tab.name()).append("\"");
            if (!enabled) markup.append(" disabled");
            markup.append("><texture src=\"darwin_soldier:textures/gui/skills/")
                    .append(tab.textureName).append(enabled ? ".png" : "_locked.png").append("\">")
                    .append("<span class=\"darwin-ability-title font-display\">").append(e(tr(tab.titleKey))).append("</span>")
                    .append("<div class=\"progress").append(selected == tab ? " progress-purple" : "").append("\"><div class=\"progress-bar\" style=\"width:")
                    .append(percent).append("%\"></div></div>")
                    .append("<span class=\"darwin-ability-progress-text\">")
                    .append(e(tr(tab.attributeKey))).append(' ').append(shownCurrent).append('/').append(requirement)
                    .append("</span></button>");
        }
        return markup.toString();
    }

    static String detail(Tab tab, Minecraft minecraft) {
        StringBuilder markup = new StringBuilder();
        switch (tab) {
            case DAMAGE_ADAPTATION -> adaptation(markup);
            case HUNTING_INSTINCT -> hunting(markup, minecraft);
            case STRESS_EVOLUTION -> stress(markup, minecraft);
            case SUPER_PERCEPTION -> perception(markup, minecraft);
            case BATTLE_INSTINCT -> battle(markup);
            case EFFICIENT_METABOLISM -> metabolism(markup);
            case NUTRITION_FULLNESS -> fullness(markup, minecraft);
        }
        return markup.toString();
    }

    private static void adaptation(StringBuilder out) {
        line(out, tr("screen.darwin_soldier.unlocked", yesNo(ClientGrowthData.isDamageAdaptationUnlocked())));
        line(out, tr("screen.darwin_soldier.ability_enabled", yesNo(ClientGrowthData.isDamageAdaptationEnabled())));
        muted(out, tr("screen.darwin_soldier.adaptations.summary", ClientGrowthData.getAdaptationEntries().size()));
        muted(out, tr("screen.darwin_soldier.adaptations.rules"));
        if (ClientGrowthData.isAdaptationFeatureEnabled() && ClientGrowthData.isDamageAdaptationUnlocked()) {
            actions(out, toggleAbility("DAMAGE_ADAPTATION", ClientGrowthData.isDamageAdaptationEnabled())
                    + button("button button-secondary", "open-adaptations",
                    tr("screen.darwin_soldier.adaptations.open", ClientGrowthData.getAdaptationEntries().size()), ""));
        }
    }

    private static void hunting(StringBuilder out, Minecraft minecraft) {
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
            String buttons = toggleAbility("HUNTING_INSTINCT", ClientGrowthData.isHuntingInstinctEnabled());
            if (ClientGrowthData.isHuntingInternalImpactFeatureEnabled()) {
                buttons += button("button button-secondary", "toggle-hunting-impact",
                        tr(ClientGrowthData.isHuntingInternalImpactEnabled()
                                ? "screen.darwin_soldier.internal_impact_on"
                                : "screen.darwin_soldier.internal_impact_off"), "");
            }
            actions(out, buttons);
        }
    }

    private static void stress(StringBuilder out, Minecraft minecraft) {
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

    private static void perception(StringBuilder out, Minecraft minecraft) {
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
            actions(out, toggleAbility("SUPER_PERCEPTION", ClientGrowthData.isSuperPerceptionEnabled())
                    + button("button button-secondary", "open-aim", tr("screen.darwin_soldier.aim_settings"), "")
                    + button("button button-tertiary", "open-targets", tr("screen.darwin_soldier.targets.open"), ""));
        }
    }

    private static void battle(StringBuilder out) {
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
            actions(out, toggleAbility("BATTLE_INSTINCT", ClientGrowthData.isBattleInstinctEnabled())
                    + button("button button-secondary", "toggle-battle-mode",
                    tr(ClientGrowthData.isBattleInstinctCounterEnabled()
                            ? "screen.darwin_soldier.battle_mode_normal"
                            : "screen.darwin_soldier.battle_mode_dodge_only"), ""));
        }
    }

    private static void metabolism(StringBuilder out) {
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

    private static void fullness(StringBuilder out, Minecraft minecraft) {
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

    private static String toggleAbility(String ability, boolean enabled) {
        return button("button", "toggle-ability",
                tr(enabled ? "screen.darwin_soldier.disable" : "screen.darwin_soldier.enable"),
                " data-ability=\"" + ability + "\"");
    }

    private static String button(String classes, String action, String label, String extra) {
        return "<button class=\"" + classes + "\" type=\"button\" data-action=\"" + action + "\""
                + extra + ">" + e(label) + "</button>";
    }

    private static void actions(StringBuilder out, String buttons) {
        out.append("<div class=\"darwin-action-row\">").append(buttons).append("</div>");
    }

    private static void line(StringBuilder out, String value) {
        out.append("<div>").append(e(value)).append("</div>");
    }

    private static void muted(StringBuilder out, String value) {
        out.append("<div class=\"text-muted\">").append(e(value)).append("</div>");
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

    private static String e(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
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
