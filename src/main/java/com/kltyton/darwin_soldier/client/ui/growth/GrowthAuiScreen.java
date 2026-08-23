package com.kltyton.darwin_soldier.client.ui.growth;

import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.aim.AimAuiScreen;
import com.kltyton.darwin_soldier.client.growth.adaptation.AdaptationAuiScreen;
import com.kltyton.darwin_soldier.client.growth.targeting.TargetingAuiScreen;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.data.AbilityType;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GrowthAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/growth.html";

    private int stagedHealth;
    private int stagedAttack;
    private int stagedDefense;
    private int stagedPerception;
    private int stagedNutrition;
    private long seenRevision = -1L;
    private int dynamicRefreshTicks;
    private GrowthAbilityView.Tab activeTab = GrowthAbilityView.Tab.DAMAGE_ADAPTATION;

    public GrowthAuiScreen() {
        super(TEMPLATE, null);
    }

    @Override
    protected void onDocumentCreated(Document document) {
        if (seenRevision != ClientGrowthData.getRevision()) {
            syncFromClientData();
        }
    }

    @Override
    protected void renderDocument(Document document) {
        boolean enabled = ClientGrowthData.isEnabled();
        className(document, "growth-locked", enabled ? "alert alert-danger hidden" : "alert alert-danger");
        className(document, "growth-content", enabled ? "darwin-growth-workspace" : "darwin-growth-workspace hidden");
        if (!enabled) {
            return;
        }
        renderSummary(document);
        html(document, "allocation-fields", allocationMarkup());
        refreshAllocationState(document);
        html(document, "ability-tabs", GrowthAbilityView.tabs(activeTab));
        html(document, "ability-detail", GrowthAbilityView.detail(activeTab, minecraft));
    }

    @Override
    protected void handleAction(Document document, Element action, String actionName) {
        switch (actionName) {
            case "close" -> onClose();
            case "adjust-allocation" -> adjustPoints(
                    AllocationTarget.valueOf(data(action, "target")),
                    Integer.parseInt(data(action, "direction")) * modifierStep());
            case "confirm-allocation" -> confirmAllocation(document);
            case "select-ability" -> {
                activeTab = GrowthAbilityView.Tab.valueOf(data(action, "tab"));
                renderNow();
            }
            case "toggle-ability" -> toggleAbility(data(action, "ability"));
            case "toggle-hunting-impact" -> ModNetwork.sendAbilityToggle(
                    AbilityType.HUNTING_INTERNAL_IMPACT,
                    !ClientGrowthData.isHuntingInternalImpactEnabled());
            case "toggle-battle-mode" -> ModNetwork.sendBattleInstinctMode(
                    !ClientGrowthData.isBattleInstinctCounterEnabled());
            case "open-aim" -> minecraft.setScreen(new AimAuiScreen(this));
            case "open-targets" -> minecraft.setScreen(new TargetingAuiScreen(this));
            case "open-adaptations" -> minecraft.setScreen(new AdaptationAuiScreen(this));
            default -> {
                return;
            }
        }
        if (!actionName.startsWith("open-") && !"close".equals(actionName)) {
            renderNow();
        }
    }

    @Override
    protected void handleInput(Document document, Element input) {
        if (!"allocation".equals(data(input, "input"))) {
            return;
        }
        try {
            AllocationTarget target = AllocationTarget.valueOf(data(input, "target"));
            setPoints(target, Mth.clamp(Integer.parseInt(input.getValue()), 0, getMaxForTarget(target)));
            renderSummary(document);
            refreshAllocationState(document);
        } catch (IllegalArgumentException ignored) {
            refreshAllocationState(document);
        }
    }

    @Override
    public void tick() {
        if (seenRevision != ClientGrowthData.getRevision()) {
            syncFromClientData();
            renderNow();
            return;
        }
        if (++dynamicRefreshTicks >= 10) {
            dynamicRefreshTicks = 0;
            Document document = getLinkedDocument();
            if (document != null && ClientGrowthData.isEnabled()) {
                renderSummary(document);
                html(document, "ability-detail", GrowthAbilityView.detail(activeTab, minecraft));
            }
        }
    }

    private void confirmAllocation(Document document) {
        RuntimeDiagnostics.info("growth_allocation_confirm", "staged=" + stagedHealth + "/" + stagedAttack
                + "/" + stagedDefense + "/" + stagedPerception + "/" + stagedNutrition
                + " used=" + getStagedUsed() + " remaining=" + getStagedRemaining());
        ModNetwork.sendAllocation(stagedHealth, stagedAttack, stagedDefense, stagedPerception, stagedNutrition);
        text(document, "allocation-confirm", tr("screen.darwin_soldier.saved"));
    }

    private void toggleAbility(String abilityName) {
        AbilityType ability = AbilityType.valueOf(abilityName);
        boolean enabled = switch (ability) {
            case DAMAGE_ADAPTATION -> ClientGrowthData.isDamageAdaptationEnabled();
            case HUNTING_INSTINCT -> ClientGrowthData.isHuntingInstinctEnabled();
            case STRESS_EVOLUTION -> ClientGrowthData.isStressEvolutionEnabled();
            case SUPER_PERCEPTION -> ClientGrowthData.isSuperPerceptionEnabled();
            case BATTLE_INSTINCT -> ClientGrowthData.isBattleInstinctEnabled();
            case EFFICIENT_METABOLISM -> ClientGrowthData.isEfficientMetabolismEnabled();
            case NUTRITION_FULLNESS -> ClientGrowthData.isNutritionFullnessEnabled();
            case HUNTING_INTERNAL_IMPACT -> ClientGrowthData.isHuntingInternalImpactEnabled();
        };
        ModNetwork.sendAbilityToggle(ability, !enabled);
    }

    private void syncFromClientData() {
        stagedHealth = ClientGrowthData.getHealthPoints();
        stagedAttack = ClientGrowthData.getAttackPoints();
        stagedDefense = ClientGrowthData.getDefensePoints();
        stagedPerception = ClientGrowthData.getPerceptionPoints();
        stagedNutrition = ClientGrowthData.getNutritionPoints();
        seenRevision = ClientGrowthData.getRevision();
    }

    private void renderSummary(Document document) {
        text(document, "growth-header-status", tr("screen.darwin_soldier.total_points", ClientGrowthData.getTotalPoints()));
        StringBuilder metrics = new StringBuilder();
        appendMetric(metrics, "screen.darwin_soldier.aui.current_growth_label",
                format(ClientGrowthData.getCurrentGrowth()), "status-gold");
        appendMetric(metrics, "screen.darwin_soldier.aui.total_growth_label",
                format(ClientGrowthData.getTotalGrowth()), "");
        appendMetric(metrics, "screen.darwin_soldier.aui.total_points_label",
                Integer.toString(ClientGrowthData.getTotalPoints()), "status-purple");
        appendMetric(metrics, "screen.darwin_soldier.aui.used_points_label",
                Integer.toString(getStagedUsed()), "");
        appendMetric(metrics, "screen.darwin_soldier.aui.remaining_points_label",
                Integer.toString(getStagedRemaining()), "status-gold");
        html(document, "growth-metrics", metrics.toString());

        List<String> rows = new ArrayList<>();
        appendRow(rows, tr("screen.darwin_soldier.health_line", stagedHealth,
                format(stagedHealth * ClientGrowthData.getHealthPerPoint())));
        appendRow(rows, tr("screen.darwin_soldier.attack_line", stagedAttack,
                format(stagedAttack * ClientGrowthData.getAttackPerPoint())));
        appendRow(rows, tr("screen.darwin_soldier.defense_line", stagedDefense,
                format(stagedDefense * ClientGrowthData.getDefensePerPoint())));
        appendRow(rows, tr("screen.darwin_soldier.perception_line", stagedPerception));
        appendRow(rows, tr("screen.darwin_soldier.nutrition_line", stagedNutrition,
                ClientGrowthData.getNutritionMaximumPoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.historical_health",
                ClientGrowthData.getCumulativeHealthPoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.historical_attack",
                ClientGrowthData.getCumulativeAttackPoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.historical_defense",
                ClientGrowthData.getCumulativeDefensePoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.historical_perception",
                ClientGrowthData.getCumulativePerceptionPoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.historical_nutrition",
                ClientGrowthData.getCumulativeNutritionPoints()));
        appendRow(rows, tr("screen.darwin_soldier.aui.critical_chance",
                format(Mth.clamp(DarwinConfig.BASE_CRITICAL_CHANCE.get()
                        + stagedPerception * DarwinConfig.PERCEPTION_CRITICAL_CHANCE_PER_POINT.get(), 0.0D, 1.0D) * 100.0D)));
        appendRow(rows, tr("screen.darwin_soldier.aui.critical_damage",
                format(Math.max(1.0D, DarwinConfig.BASE_CRITICAL_DAMAGE.get()
                        + stagedPerception * DarwinConfig.PERCEPTION_CRITICAL_DAMAGE_PER_POINT.get()) * 100.0D)));
        appendRow(rows, tr("screen.darwin_soldier.aui.healing_bonus",
                format(healingAmplificationBonus() * 100.0D)));
        appendRow(rows, tr("screen.darwin_soldier.aui.armor_pierce", format(armorPierceBonus())));
        appendRow(rows, tr("screen.darwin_soldier.aui.armor_toughness", format(armorToughnessBonus())));
        appendRow(rows, tr("screen.darwin_soldier.minimum_damage",
                format(ClientGrowthData.getMinimumEffectiveDamage(stagedAttack))));
        appendRow(rows, tr("screen.darwin_soldier.minimum_reduction", format(stagedDefense / 10.0D)));
        appendRow(rows, tr("screen.darwin_soldier.super_perception_status",
                tr(ClientGrowthData.isSuperPerceptionUnlocked()
                        ? "screen.darwin_soldier.yes" : "screen.darwin_soldier.no")));
        html(document, "growth-summary", intelColumns(rows));
    }

    private String allocationMarkup() {
        StringBuilder markup = new StringBuilder();
        for (AllocationTarget target : AllocationTarget.values()) {
            String token = target.name().toLowerCase(Locale.ROOT);
            markup.append("<div class=\"darwin-allocation-row\"><div class=\"darwin-allocation-head\"><label for=\"allocation-")
                    .append(token).append("\">").append(escapeHtml(tr(target.translationKey)))
                    .append("</label><div class=\"darwin-stepper\">")
                    .append(stepButton(target, -1, "-"))
                    .append("<span id=\"allocation-").append(token).append("-value\" class=\"darwin-step-value\"></span>")
                    .append(stepButton(target, 1, "+"))
                    .append("</div></div>")
                    .append("<input id=\"allocation-").append(token)
                    .append("\" class=\"darwin-range\" type=\"range\" min=\"0\" data-input=\"allocation\" data-target=\"")
                    .append(target.name()).append("\"></div>");
        }
        return markup.toString();
    }

    private String stepButton(AllocationTarget target, int direction, String label) {
        return "<button class=\"button button-small darwin-step-button\" type=\"button\" data-action=\"adjust-allocation\" data-target=\""
                + target.name() + "\" data-direction=\"" + direction + "\">" + label + "</button>";
    }

    private void refreshAllocationState(Document document) {
        for (AllocationTarget target : AllocationTarget.values()) {
            String token = target.name().toLowerCase(Locale.ROOT);
            int points = getPoints(target);
            value(document, "allocation-" + token, Integer.toString(points));
            attribute(document, "allocation-" + token, "max", Integer.toString(getMaxForTarget(target)));
            text(document, "allocation-" + token + "-value", Integer.toString(points));
        }
        text(document, "allocation-remaining", tr("screen.darwin_soldier.remaining_points", getStagedRemaining()));
    }

    private int modifierStep() {
        if (Screen.hasControlDown()) {
            return 100;
        }
        if (Screen.hasShiftDown()) {
            return 10;
        }
        return 1;
    }

    private void adjustPoints(AllocationTarget target, int delta) {
        int before = getPoints(target);
        int max = getMaxForTarget(target);
        setPoints(target, Mth.clamp(before + delta, 0, max));
        RuntimeDiagnostics.info("growth_allocation_step", "target=" + target + " delta=" + delta
                + " before=" + before + " after=" + getPoints(target) + " max=" + max
                + " remaining=" + getStagedRemaining());
    }

    private int getStagedUsed() {
        return stagedHealth + stagedAttack + stagedDefense + stagedPerception + stagedNutrition;
    }

    private int getStagedRemaining() {
        return Math.max(0, ClientGrowthData.getTotalPoints() - getStagedUsed());
    }

    private int getPoints(AllocationTarget target) {
        return switch (target) {
            case HEALTH -> stagedHealth;
            case ATTACK -> stagedAttack;
            case DEFENSE -> stagedDefense;
            case PERCEPTION -> stagedPerception;
            case NUTRITION -> stagedNutrition;
        };
    }

    private void setPoints(AllocationTarget target, int points) {
        switch (target) {
            case HEALTH -> stagedHealth = points;
            case ATTACK -> stagedAttack = points;
            case DEFENSE -> stagedDefense = points;
            case PERCEPTION -> stagedPerception = points;
            case NUTRITION -> stagedNutrition = points;
        }
    }

    private int getMaxForTarget(AllocationTarget target) {
        int max = getPoints(target) + getStagedRemaining();
        return switch (target) {
            case PERCEPTION -> Math.min(max, DarwinConfig.PERCEPTION_MAX_POINTS.get());
            case NUTRITION -> Math.min(max, ClientGrowthData.getNutritionMaximumPoints());
            default -> max;
        };
    }

    private double healingAmplificationBonus() {
        if (!ClientGrowthData.isDerivedAttributesEnabled()) return 0.0D;
        return stagedHealth / Math.max(1, ClientGrowthData.getHealingHealthPoints())
                * ClientGrowthData.getHealingPerStep();
    }

    private double armorPierceBonus() {
        if (!ClientGrowthData.isDerivedAttributesEnabled()) return 0.0D;
        return stagedAttack / Math.max(1, ClientGrowthData.getArmorPierceAttackPoints())
                * ClientGrowthData.getArmorPiercePerStep();
    }

    private double armorToughnessBonus() {
        if (!ClientGrowthData.isDerivedAttributesEnabled()) return 0.0D;
        return stagedDefense / Math.max(1, ClientGrowthData.getToughnessDefensePoints())
                * ClientGrowthData.getToughnessPerStep();
    }

    private static void appendRow(List<String> rows, String value) {
        rows.add(value);
    }

    private static String intelColumns(List<String> rows) {
        int split = (rows.size() + 1) / 2;
        StringBuilder markup = new StringBuilder("<div class=\"darwin-intel-column\">");
        appendIntelRows(markup, rows, 0, split);
        markup.append("</div><div class=\"darwin-intel-column\">");
        appendIntelRows(markup, rows, split, rows.size());
        return markup.append("</div>").toString();
    }

    private static void appendIntelRows(StringBuilder markup, List<String> rows, int from, int to) {
        for (int index = from; index < to; index++) {
            markup.append("<div class=\"darwin-intel-item\">")
                    .append(escapeHtml(rows.get(index))).append("</div>");
        }
    }

    private static void appendMetric(StringBuilder metrics, String labelKey, String value, String extraClass) {
        metrics.append("<div class=\"darwin-status-cell ").append(extraClass).append("\"><span class=\"darwin-status-label\">")
                .append(escapeHtml(tr(labelKey))).append("</span><span class=\"darwin-status-value\">")
                .append(escapeHtml(value)).append("</span></div>");
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9D) return String.format(Locale.ROOT, "%.0f", value);
        if (Math.abs(value * 10.0D - Math.rint(value * 10.0D)) < 1.0E-9D) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private enum AllocationTarget {
        HEALTH("screen.darwin_soldier.health_points"),
        ATTACK("screen.darwin_soldier.attack_points"),
        DEFENSE("screen.darwin_soldier.defense_points"),
        PERCEPTION("screen.darwin_soldier.perception_points"),
        NUTRITION("screen.darwin_soldier.nutrition_points");

        private final String translationKey;

        AllocationTarget(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
