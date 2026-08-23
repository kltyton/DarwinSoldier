package com.kltyton.darwin_soldier.client.aim;

import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallisticsTracker;
import com.kltyton.darwin_soldier.client.aim.ballistics.WeaponLaunchResolver;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Objects;

public final class AimAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/aim.html";
    private static final int SAVE_DELAY_TICKS = 10;

    private ResourceLocation weaponId;
    private ItemStack weapon = ItemStack.EMPTY;
    private AimWeaponSettings settings = AimWeaponSettings.defaults();
    private int saveDelay;

    public AimAuiScreen(Screen parent) {
        super(TEMPLATE, parent);
    }

    @Override
    protected void onDocumentCreated(Document document) {
        loadCurrentWeapon();
        RuntimeDiagnostics.info("aim_screen_init", "weapon=" + weaponId + " settings=" + settings);
    }

    @Override
    protected void renderDocument(Document document) {
        boolean hasWeapon = weaponId != null;
        className(document, "aim-no-weapon", hasWeapon ? "alert alert-danger hidden" : "alert alert-danger");
        className(document, "aim-content", hasWeapon ? "card card-accent-purple" : "card card-accent-purple hidden");
        if (!hasWeapon) {
            return;
        }
        text(document, "aim-weapon", tr("screen.darwin_soldier.aim.current_weapon", weapon.getHoverName()));
        text(document, "aim-weapon-id", weaponId.toString());
        text(document, "aim-ballistics", ballisticsStatus());
        text(document, "aim-mode", tr(settings.mode() == AimMode.MANUAL
                ? "screen.darwin_soldier.aim.mode_manual" : "screen.darwin_soldier.aim.mode_adaptive"));
        text(document, "aim-trajectory", tr(settings.trajectoryVisible()
                ? "screen.darwin_soldier.aim.trajectory_on" : "screen.darwin_soldier.aim.trajectory_off"));
        html(document, "aim-fields", tuningMarkup());
        refreshTuningState(document);
    }

    @Override
    protected void handleAction(Document document, Element action, String actionName) {
        switch (actionName) {
            case "back" -> onClose();
            case "toggle-mode" -> {
                settings = settings.withMode(settings.mode() == AimMode.MANUAL ? AimMode.ADAPTIVE : AimMode.MANUAL);
                saveSettingsImmediately();
            }
            case "toggle-trajectory" -> {
                settings = settings.withTrajectoryVisible(!settings.trajectoryVisible());
                saveSettingsImmediately();
            }
            case "adjust-tuning" -> adjust(TuningField.valueOf(data(action, "field")),
                    Integer.parseInt(data(action, "direction")) * modifierStep());
            case "reset" -> resetSettings();
            case "recalibrate" -> recalibrate();
            default -> {
                return;
            }
        }
        if (!"back".equals(actionName)) {
            renderNow();
        }
    }

    @Override
    protected void handleInput(Document document, Element input) {
        if (!"tuning".equals(data(input, "input"))) {
            return;
        }
        try {
            TuningField field = TuningField.valueOf(data(input, "field"));
            update(field, Double.parseDouble(input.getValue()));
            text(document, "tuning-" + field.name().toLowerCase(Locale.ROOT) + "-value",
                    format(field.read(settings.activeTuning())));
        } catch (IllegalArgumentException ignored) {
            refreshTuningState(document);
        }
    }

    @Override
    public void tick() {
        ResourceLocation current = currentWeaponId();
        if (!Objects.equals(current, weaponId)) {
            RuntimeDiagnostics.info("aim_screen_weapon_change", "previous=" + weaponId + " current=" + current);
            flushPendingSave();
            loadCurrentWeapon();
            renderNow();
            return;
        }
        if (saveDelay > 0 && --saveDelay == 0) {
            AimWeaponSettingsStore.flush();
        }
    }

    @Override
    public void onClose() {
        flushPendingSave();
        super.onClose();
    }

    @Override
    public void removed() {
        flushPendingSave();
        super.removed();
    }

    private String tuningMarkup() {
        StringBuilder markup = new StringBuilder();
        for (TuningField field : TuningField.values()) {
            String token = field.name().toLowerCase(Locale.ROOT);
            markup.append("<div class=\"darwin-allocation-row\"><div class=\"darwin-allocation-head\"><label for=\"tuning-")
                    .append(token).append("\">").append(escapeHtml(tr(field.translationKey)))
                    .append("</label><div class=\"darwin-stepper\">")
                    .append(stepButton(field, -1, "-"))
                    .append("<span id=\"tuning-").append(token).append("-value\" class=\"darwin-step-value\"></span>")
                    .append(stepButton(field, 1, "+"))
                    .append("</div></div>")
                    .append("<input id=\"tuning-").append(token)
                    .append("\" class=\"darwin-range\" type=\"range\" min=\"").append(field.minimum)
                    .append("\" max=\"").append(field.maximum).append("\" step=\"").append(field.step)
                    .append("\" data-input=\"tuning\" data-field=\"").append(field.name())
                    .append("\"></div>");
        }
        return markup.toString();
    }

    private String stepButton(TuningField field, int direction, String label) {
        return "<button class=\"button button-small darwin-step-button\" type=\"button\" data-action=\"adjust-tuning\" data-field=\""
                + field.name() + "\" data-direction=\"" + direction + "\">" + label + "</button>";
    }

    private void refreshTuningState(Document document) {
        for (TuningField field : TuningField.values()) {
            String token = field.name().toLowerCase(Locale.ROOT);
            double current = field.read(settings.activeTuning());
            value(document, "tuning-" + token, Double.toString(current));
            text(document, "tuning-" + token + "-value", format(current));
        }
    }

    private int modifierStep() {
        if (Screen.hasControlDown()) return 100;
        if (Screen.hasShiftDown()) return 10;
        return 1;
    }

    private void adjust(TuningField field, int stepMultiplier) {
        double delta = field.step * stepMultiplier;
        RuntimeDiagnostics.info("aim_screen_step", "weapon=" + weaponId + " field=" + field + " delta=" + delta);
        update(field, field.read(settings.activeTuning()) + delta);
        AimWeaponSettingsStore.flush();
        saveDelay = 0;
    }

    private void update(TuningField field, double value) {
        AimWeaponTuning tuning = field.write(settings.activeTuning(), Mth.clamp(value, field.minimum, field.maximum)).sanitized();
        settings = settings.withActiveTuning(tuning);
        AimWeaponSettingsStore.put(weaponId, settings);
        saveDelay = SAVE_DELAY_TICKS;
    }

    private void resetSettings() {
        AimWeaponSettingsStore.reset(weaponId);
        AimWeaponSettingsStore.flush();
        settings = AimWeaponSettings.defaults();
        saveDelay = 0;
    }

    private void recalibrate() {
        AimWeaponSettingsStore.clearBallistics(weaponId);
        ProjectileBallisticsTracker.resetLearning(weaponId);
        AimWeaponSettingsStore.flush();
        settings = AimWeaponSettingsStore.get(weaponId);
        saveDelay = 0;
    }

    private void loadCurrentWeapon() {
        if (minecraft == null || minecraft.player == null) {
            weapon = ItemStack.EMPTY;
            weaponId = null;
            settings = AimWeaponSettings.defaults();
            return;
        }
        weapon = minecraft.player.getMainHandItem().copy();
        weaponId = AimWeaponSettingsStore.weaponId(weapon);
        settings = weaponId == null ? AimWeaponSettings.defaults() : AimWeaponSettingsStore.get(weaponId);
    }

    private ResourceLocation currentWeaponId() {
        return minecraft == null || minecraft.player == null
                ? null : AimWeaponSettingsStore.weaponId(minecraft.player.getMainHandItem());
    }

    private void flushPendingSave() {
        if (saveDelay > 0) {
            AimWeaponSettingsStore.flush();
            saveDelay = 0;
        }
    }

    private void saveSettingsImmediately() {
        AimWeaponSettingsStore.put(weaponId, settings);
        AimWeaponSettingsStore.flush();
        saveDelay = 0;
    }

    private Component ballisticsStatus() {
        if (WeaponLaunchResolver.isSupported(weapon)) {
            return Component.translatable("screen.darwin_soldier.aim.ballistics_vanilla_live");
        }
        if (settings.ballistics() == null) {
            return Component.translatable("screen.darwin_soldier.aim.ballistics_pending");
        }
        if (settings.ballistics().hitscan()) {
            return Component.translatable("screen.darwin_soldier.aim.ballistics_hitscan");
        }
        return Component.translatable("screen.darwin_soldier.aim.ballistics_detected",
                settings.ballistics().projectileType(), format(settings.ballistics().speed()),
                format(settings.ballistics().gravity()));
    }

    private enum TuningField {
        LEAD("screen.darwin_soldier.aim.lead", AimWeaponTuning.MIN_LEAD, AimWeaponTuning.MAX_LEAD, 0.05D) {
            @Override double read(AimWeaponTuning tuning) { return tuning.leadMultiplier(); }
            @Override AimWeaponTuning write(AimWeaponTuning tuning, double value) {
                return new AimWeaponTuning(value, tuning.dropMultiplier(), tuning.smooth());
            }
        },
        DROP("screen.darwin_soldier.aim.drop", AimWeaponTuning.MIN_DROP, AimWeaponTuning.MAX_DROP, 0.05D) {
            @Override double read(AimWeaponTuning tuning) { return tuning.dropMultiplier(); }
            @Override AimWeaponTuning write(AimWeaponTuning tuning, double value) {
                return new AimWeaponTuning(tuning.leadMultiplier(), value, tuning.smooth());
            }
        },
        SMOOTH("screen.darwin_soldier.aim.smooth", AimWeaponTuning.MIN_SMOOTH, AimWeaponTuning.MAX_SMOOTH, 0.01D) {
            @Override double read(AimWeaponTuning tuning) { return tuning.smooth(); }
            @Override AimWeaponTuning write(AimWeaponTuning tuning, double value) {
                return new AimWeaponTuning(tuning.leadMultiplier(), tuning.dropMultiplier(), value);
            }
        };

        private final String translationKey;
        private final double minimum;
        private final double maximum;
        private final double step;

        TuningField(String translationKey, double minimum, double maximum, double step) {
            this.translationKey = translationKey;
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
        }

        abstract double read(AimWeaponTuning tuning);
        abstract AimWeaponTuning write(AimWeaponTuning tuning, double value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
