package com.kltyton.darwin_soldier.client.ui.growth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.Locale;

public final class GrowthAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/growth.html";

    private int stagedHealth;
    private int stagedAttack;
    private int stagedDefense;
    private int stagedPerception;
    private int stagedNutrition;
    private long seenRevision = -1;
    private int dynamicRefreshTicks;
    private String saveNotice = "";
    private GrowthAbilityView.Tab activeTab = GrowthAbilityView.Tab.DAMAGE_ADAPTATION;

    public GrowthAuiScreen() {
        super(TEMPLATE, null);
    }

    @Override
    protected void onDocumentCreated(Document document) {
        if (seenRevision != ClientGrowthData.getRevision()) syncFromClientData();
    }

    @Override
    protected void renderDocument(Document document) {
        JsonObject state = new JsonObject();
        state.addProperty("title", tr("screen.darwin_soldier.growth"));
        state.addProperty("headerKicker", tr("screen.darwin_soldier.aui.command_console"));
        state.addProperty("closeLabel", tr("gui.done"));
        state.addProperty("closeAction", "close");
        state.addProperty("enabled", ClientGrowthData.isEnabled());
        state.addProperty("lockedText", tr("screen.darwin_soldier.locked"));
        state.addProperty("headerStatus", tr("screen.darwin_soldier.total_points", ClientGrowthData.getTotalPoints()));
        if (ClientGrowthData.isEnabled()) {
            state.addProperty("allocationTitle", tr("screen.darwin_soldier.aui.allocation"));
            state.addProperty("abilityTitle", tr("screen.darwin_soldier.ability_panel"));
            state.addProperty("intelTitle", tr("screen.darwin_soldier.aui.combat_intel"));
            state.addProperty("confirmLabel", tr("screen.darwin_soldier.confirm"));
            state.addProperty("remainingLabel", tr("screen.darwin_soldier.remaining_points", remaining()));
            state.addProperty("saveNotice", saveNotice);
            state.add("metrics", metrics());
            state.add("allocation", allocation());
            state.add("abilities", GrowthAbilityView.tabs(activeTab));
            state.add("detail", GrowthAbilityView.detail(activeTab, minecraft));
            state.add("intel", intel());
        }
        publishState(document, state);
    }

    @Override
    protected void handleAction(Document document, JsonObject action, String name) {
        switch (name) {
            case "close" -> onClose();
            case "adjust-allocation" -> adjustPoints(AllocationTarget.valueOf(data(action, "target")),
                    integerData(action, "direction") * modifierStep());
            case "confirm-allocation" -> confirmAllocation();
            case "select-ability" -> activeTab = GrowthAbilityView.Tab.valueOf(data(action, "tab"));
            case "toggle-ability" -> ModNetwork.sendAbilityToggle(AbilityType.valueOf(data(action, "ability")),
                    Boolean.parseBoolean(data(action, "enabled")));
            case "toggle-hunting-impact" -> ModNetwork.sendAbilityToggle(AbilityType.HUNTING_INTERNAL_IMPACT,
                    Boolean.parseBoolean(data(action, "enabled")));
            case "toggle-battle-mode" -> ModNetwork.sendBattleInstinctMode(!ClientGrowthData.isBattleInstinctCounterEnabled());
            case "open-aim" -> minecraft.setScreen(new AimAuiScreen(this));
            case "open-targets" -> minecraft.setScreen(new TargetingAuiScreen(this));
            case "open-adaptations" -> minecraft.setScreen(new AdaptationAuiScreen(this));
            default -> { return; }
        }
        if (!name.startsWith("open-") && !name.equals("close")) renderNow();
    }

    @Override
    protected void handleInput(Document document, JsonObject input) {
        if (!"allocation".equals(data(input, "input"))) return;
        AllocationTarget target = AllocationTarget.valueOf(data(input, "target"));
        setPoints(target, Mth.clamp(integerData(input, "value"), 0, getMaxForTarget(target)));
        renderNow();
    }

    @Override
    public void tick() {
        if (seenRevision != ClientGrowthData.getRevision()) {
            syncFromClientData();
            renderNow();
            return;
        }
        if (ClientGrowthData.isEnabled() && ++dynamicRefreshTicks >= 10) {
            dynamicRefreshTicks = 0;
            renderNow();
        }
    }

    private JsonArray metrics() {
        JsonArray rows = new JsonArray();
        metric(rows, tr("screen.darwin_soldier.aui.current_growth_label"), fmt(ClientGrowthData.getCurrentGrowth()), "status-gold");
        metric(rows, tr("screen.darwin_soldier.aui.total_growth_label"), fmt(ClientGrowthData.getTotalGrowth()), "");
        metric(rows, tr("screen.darwin_soldier.aui.total_points_label"), Integer.toString(ClientGrowthData.getTotalPoints()), "status-purple");
        metric(rows, tr("screen.darwin_soldier.aui.used_points_label"), Integer.toString(used()), "");
        metric(rows, tr("screen.darwin_soldier.aui.remaining_points_label"), Integer.toString(remaining()), "status-gold");
        return rows;
    }

    private static void metric(JsonArray rows, String label, String value, String className) {
        JsonObject row = new JsonObject();
        row.addProperty("label", label);
        row.addProperty("value", value);
        row.addProperty("className", className);
        rows.add(row);
    }

    private JsonArray allocation() {
        JsonArray rows = new JsonArray();
        for (AllocationTarget target : AllocationTarget.values()) {
            JsonObject row = new JsonObject();
            row.addProperty("id", target.name());
            row.addProperty("label", tr(target.translationKey));
            row.addProperty("value", points(target));
            row.addProperty("max", getMaxForTarget(target));
            rows.add(row);
        }
        return rows;
    }

    private JsonArray intel() {
        String[] lines = {
                tr("screen.darwin_soldier.health_line", stagedHealth, fmt(stagedHealth * ClientGrowthData.getHealthPerPoint())),
                tr("screen.darwin_soldier.attack_line", stagedAttack, fmt(stagedAttack * ClientGrowthData.getAttackPerPoint())),
                tr("screen.darwin_soldier.defense_line", stagedDefense, fmt(stagedDefense * ClientGrowthData.getDefensePerPoint())),
                tr("screen.darwin_soldier.perception_line", stagedPerception),
                tr("screen.darwin_soldier.nutrition_line", stagedNutrition, ClientGrowthData.getNutritionMaximumPoints()),
                tr("screen.darwin_soldier.aui.historical_health", ClientGrowthData.getCumulativeHealthPoints()),
                tr("screen.darwin_soldier.aui.historical_attack", ClientGrowthData.getCumulativeAttackPoints()),
                tr("screen.darwin_soldier.aui.historical_defense", ClientGrowthData.getCumulativeDefensePoints()),
                tr("screen.darwin_soldier.aui.historical_perception", ClientGrowthData.getCumulativePerceptionPoints()),
                tr("screen.darwin_soldier.aui.historical_nutrition", ClientGrowthData.getCumulativeNutritionPoints()),
                tr("screen.darwin_soldier.aui.critical_chance", fmt(Mth.clamp(DarwinConfig.BASE_CRITICAL_CHANCE.get()
                        + stagedPerception * DarwinConfig.PERCEPTION_CRITICAL_CHANCE_PER_POINT.get(), 0, 1) * 100)),
                tr("screen.darwin_soldier.aui.critical_damage", fmt(Math.max(1, DarwinConfig.BASE_CRITICAL_DAMAGE.get()
                        + stagedPerception * DarwinConfig.PERCEPTION_CRITICAL_DAMAGE_PER_POINT.get()) * 100)),
                tr("screen.darwin_soldier.aui.healing_bonus", fmt(healingAmplificationBonus() * 100)),
                tr("screen.darwin_soldier.aui.armor_pierce", fmt(armorPierceBonus())),
                tr("screen.darwin_soldier.aui.armor_toughness", fmt(armorToughnessBonus())),
                tr("screen.darwin_soldier.minimum_damage", fmt(ClientGrowthData.getMinimumEffectiveDamage(stagedAttack))),
                tr("screen.darwin_soldier.minimum_reduction", fmt(stagedDefense / 10.0)),
                tr("screen.darwin_soldier.super_perception_status", tr(ClientGrowthData.isSuperPerceptionUnlocked()
                        ? "screen.darwin_soldier.yes" : "screen.darwin_soldier.no"))
        };
        JsonArray rows = new JsonArray();
        for (String line : lines) rows.add(line);
        return rows;
    }

    private void confirmAllocation() {
        RuntimeDiagnostics.info("growth_allocation_confirm", "staged=" + stagedHealth + "/" + stagedAttack + "/"
                + stagedDefense + "/" + stagedPerception + "/" + stagedNutrition + " used=" + used() + " remaining=" + remaining());
        ModNetwork.sendAllocation(stagedHealth, stagedAttack, stagedDefense, stagedPerception, stagedNutrition);
        saveNotice = tr("screen.darwin_soldier.saved");
    }

    private void syncFromClientData() {
        stagedHealth = ClientGrowthData.getHealthPoints();
        stagedAttack = ClientGrowthData.getAttackPoints();
        stagedDefense = ClientGrowthData.getDefensePoints();
        stagedPerception = ClientGrowthData.getPerceptionPoints();
        stagedNutrition = ClientGrowthData.getNutritionPoints();
        seenRevision = ClientGrowthData.getRevision();
    }

    private int used() { return stagedHealth + stagedAttack + stagedDefense + stagedPerception + stagedNutrition; }
    private int remaining() { return Math.max(0, ClientGrowthData.getTotalPoints() - used()); }

    private int points(AllocationTarget target) {
        return switch (target) {
            case HEALTH -> stagedHealth;
            case ATTACK -> stagedAttack;
            case DEFENSE -> stagedDefense;
            case PERCEPTION -> stagedPerception;
            case NUTRITION -> stagedNutrition;
        };
    }

    private void setPoints(AllocationTarget target, int points) {
        saveNotice = "";
        switch (target) {
            case HEALTH -> stagedHealth = points;
            case ATTACK -> stagedAttack = points;
            case DEFENSE -> stagedDefense = points;
            case PERCEPTION -> stagedPerception = points;
            case NUTRITION -> stagedNutrition = points;
        }
    }

    private int getMaxForTarget(AllocationTarget target) {
        int maximum = points(target) + remaining();
        return switch (target) {
            case PERCEPTION -> Math.min(maximum, DarwinConfig.PERCEPTION_MAX_POINTS.get());
            case NUTRITION -> Math.min(maximum, ClientGrowthData.getNutritionMaximumPoints());
            default -> maximum;
        };
    }

    private void adjustPoints(AllocationTarget target, int delta) {
        int before = points(target);
        setPoints(target, Mth.clamp(before + delta, 0, getMaxForTarget(target)));
        RuntimeDiagnostics.info("growth_allocation_step", "target=" + target + " before=" + before
                + " delta=" + delta + " after=" + points(target) + " remaining=" + remaining());
    }

    private int modifierStep() {
        return Screen.hasControlDown() ? 100 : Screen.hasShiftDown() ? 10 : 1;
    }

    private double healingAmplificationBonus() {
        return ClientGrowthData.isDerivedAttributesEnabled() ? stagedHealth / (double) Math.max(1,
                ClientGrowthData.getHealingHealthPoints()) * ClientGrowthData.getHealingPerStep() : 0;
    }

    private double armorPierceBonus() {
        return ClientGrowthData.isDerivedAttributesEnabled() ? stagedAttack / (double) Math.max(1,
                ClientGrowthData.getArmorPierceAttackPoints()) * ClientGrowthData.getArmorPiercePerStep() : 0;
    }

    private double armorToughnessBonus() {
        return ClientGrowthData.isDerivedAttributesEnabled() ? stagedDefense / (double) Math.max(1,
                ClientGrowthData.getToughnessDefensePoints()) * ClientGrowthData.getToughnessPerStep() : 0;
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) return String.format(Locale.ROOT, "%.0f", value);
        if (Math.abs(value * 10 - Math.rint(value * 10)) < 1e-9) return String.format(Locale.ROOT, "%.1f", value);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private enum AllocationTarget {
        HEALTH("screen.darwin_soldier.health_points"),
        ATTACK("screen.darwin_soldier.attack_points"),
        DEFENSE("screen.darwin_soldier.defense_points"),
        PERCEPTION("screen.darwin_soldier.perception_points"),
        NUTRITION("screen.darwin_soldier.nutrition_points");

        private final String translationKey;

        AllocationTarget(String translationKey) { this.translationKey = translationKey; }
    }
}
