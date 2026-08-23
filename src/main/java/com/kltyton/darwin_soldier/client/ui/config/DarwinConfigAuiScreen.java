package com.kltyton.darwin_soldier.client.ui.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DarwinConfigAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/config.html";

    private final Map<String, List<Field>> fieldsByCategory = new LinkedHashMap<>();
    private final Map<String, Field> fieldsByKey = new LinkedHashMap<>();
    private final Map<String, String> rawValues = new LinkedHashMap<>();
    private final Set<String> invalidKeys = new LinkedHashSet<>();
    private String activeCategory;
    private String errorMessage = "";

    public DarwinConfigAuiScreen(Screen parent) {
        super(TEMPLATE, parent);
        loadCatalog();
    }

    @Override
    protected void renderDocument(Document document) {
        if (activeCategory == null) {
            showError(document, tr("screen.darwin_soldier.aui.config_empty"));
            return;
        }
        html(document, "config-categories", categoryMarkup());
        text(document, "config-category-title", tr("config.darwin_soldier.category." + activeCategory));
        html(document, "config-fields", fieldsMarkup(fieldsByCategory.getOrDefault(activeCategory, List.of())));
        showError(document, errorMessage);
    }

    @Override
    protected void handleAction(Document document, Element action, String actionName) {
        switch (actionName) {
            case "cancel" -> onClose();
            case "select-category" -> {
                activeCategory = data(action, "category");
                errorMessage = "";
            }
            case "toggle-config" -> toggleBoolean(data(action, "key"));
            case "reset-category" -> resetCategory();
            case "save" -> {
                if (save()) {
                    onClose();
                    return;
                }
            }
            default -> {
                return;
            }
        }
        if (!"cancel".equals(actionName)) {
            renderNow();
        }
    }

    @Override
    protected void handleInput(Document document, Element input) {
        if (!"config".equals(data(input, "input"))) {
            return;
        }
        String key = data(input, "key");
        Field field = fieldsByKey.get(key);
        if (field == null) {
            return;
        }
        rawValues.put(key, input.getValue());
        boolean valid = validate(field, input.getValue()) != null;
        if (valid) invalidKeys.remove(key); else invalidKeys.add(key);
        input.setClassName(inputClass(field, valid));
    }

    private void loadCatalog() {
        fieldsByCategory.clear();
        fieldsByKey.clear();
        rawValues.clear();
        collect("", DarwinConfig.SPEC.getValues(), DarwinConfig.SPEC.getSpec());
        activeCategory = fieldsByCategory.keySet().stream().findFirst().orElse(null);
    }

    private void collect(String category, UnmodifiableConfig values, UnmodifiableConfig specs) {
        for (Map.Entry<String, Object> entry : values.valueMap().entrySet()) {
            Object rawValue = entry.getValue();
            Object rawSpec = specs.getRaw(entry.getKey());
            String nextCategory = category.isEmpty() ? entry.getKey() : category;
            if (rawValue instanceof UnmodifiableConfig nestedValues
                    && rawSpec instanceof UnmodifiableConfig nestedSpecs) {
                collect(nextCategory, nestedValues, nestedSpecs);
                continue;
            }
            if (!(rawValue instanceof ForgeConfigSpec.ConfigValue<?> configValue)
                    || !(rawSpec instanceof ForgeConfigSpec.ValueSpec valueSpec)) {
                continue;
            }
            if (!isVisibleCategory(nextCategory)) {
                continue;
            }
            String key = String.join(".", configValue.getPath());
            Field field = new Field(nextCategory, entry.getKey(), key, configValue, valueSpec);
            fieldsByCategory.computeIfAbsent(nextCategory, ignored -> new ArrayList<>()).add(field);
            fieldsByKey.put(key, field);
            rawValues.put(key, serialize(configValue.get()));
        }
    }

    private String categoryMarkup() {
        StringBuilder markup = new StringBuilder();
        for (String category : fieldsByCategory.keySet()) {
            markup.append("<button class=\"list-group-item")
                    .append(category.equals(activeCategory) ? " active" : "")
                    .append("\" type=\"button\" data-action=\"select-category\" data-category=\"")
                    .append(escapeHtml(category)).append("\">")
                    .append(escapeHtml(tr("config.darwin_soldier.category." + category)))
                    .append("</button>");
        }
        return markup.toString();
    }

    private String fieldsMarkup(List<Field> fields) {
        StringBuilder markup = new StringBuilder();
        for (Field field : fields) {
            boolean valid = !invalidKeys.contains(field.key);
            String raw = rawValues.getOrDefault(field.key, "");
            markup.append("<div class=\"darwin-config-field\"><label class=\"form-label\" for=\"")
                    .append(field.elementId()).append("\">")
                    .append(escapeHtml(tr(optionTranslationKey(field.id)))).append("</label>");
            switch (field.kind()) {
                case BOOLEAN -> markup.append(booleanButton(field, Boolean.parseBoolean(raw)));
                case INTEGER, DOUBLE, STRING -> markup.append("<input id=\"").append(field.elementId())
                        .append("\" class=\"").append(inputClass(field, valid))
                        .append("\" type=\"").append(field.kind() == Kind.STRING ? "text" : "number")
                        .append("\" value=\"").append(escapeHtml(raw)).append("\" data-input=\"config\" data-key=\"")
                        .append(escapeHtml(field.key)).append("\"").append(rangeAttributes(field)).append(">");
                case STRING_LIST -> markup.append("<textarea id=\"").append(field.elementId())
                        .append("\" class=\"").append(inputClass(field, valid))
                        .append("\" data-input=\"config\" data-key=\"").append(escapeHtml(field.key))
                        .append("\">").append(escapeHtml(raw)).append("</textarea>");
            }
            markup.append("<div class=\"form-help\">")
                    .append(escapeHtml(tr("screen.darwin_soldier.aui.default_value",
                            serialize(field.value.getDefault())))).append("</div>");
            if (!valid) {
                markup.append("<div class=\"form-help text-danger\">")
                        .append(escapeHtml(tr("screen.darwin_soldier.aui.invalid_value"))).append("</div>");
            }
            markup.append("</div>");
        }
        return markup.toString();
    }

    private String booleanButton(Field field, boolean enabled) {
        String label = tr("screen.darwin_soldier.targets.filter_state",
                tr(optionTranslationKey(field.id)),
                tr(enabled ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled"));
        return "<button id=\"" + field.elementId() + "\" class=\"button "
                + (enabled ? "button-primary" : "button-normal")
                + "\" type=\"button\" data-action=\"toggle-config\" data-key=\""
                + escapeHtml(field.key) + "\">" + escapeHtml(label) + "</button>";
    }

    private String rangeAttributes(Field field) {
        ForgeConfigSpec.Range<?> range = field.spec.getRange();
        if (range == null) {
            return "";
        }
        String step = numberStep(field.value.getDefault());
        return " min=\"" + escapeHtml(String.valueOf(range.getMin())) + "\" max=\""
                + escapeHtml(String.valueOf(range.getMax())) + "\" step=\"" + step + "\"";
    }

    static String numberStep(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            return "1";
        }
        double number = ((Number) defaultValue).doubleValue();
        int scale = Math.max(1, BigDecimal.valueOf(number).stripTrailingZeros().scale());
        return BigDecimal.ONE.movePointLeft(scale).toPlainString();
    }

    static boolean isVisibleCategory(String category) {
        return !"migration".equals(category);
    }

    static String optionTranslationKey(String id) {
        String normalized = id.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        return "config.darwin_soldier.option." + normalized;
    }

    private String inputClass(Field field, boolean valid) {
        String base = field.kind() == Kind.STRING_LIST ? "form-textarea" : "form-input";
        return valid ? base : base + " is-invalid";
    }

    private void toggleBoolean(String key) {
        Field field = fieldsByKey.get(key);
        if (field == null || field.kind() != Kind.BOOLEAN) {
            return;
        }
        rawValues.put(key, Boolean.toString(!Boolean.parseBoolean(rawValues.getOrDefault(key, "false"))));
        invalidKeys.remove(key);
    }

    private void resetCategory() {
        for (Field field : fieldsByCategory.getOrDefault(activeCategory, List.of())) {
            rawValues.put(field.key, serialize(field.value.getDefault()));
            invalidKeys.remove(field.key);
        }
        errorMessage = "";
    }

    private boolean save() {
        Map<Field, Object> parsed = new LinkedHashMap<>();
        invalidKeys.clear();
        for (Field field : fieldsByKey.values()) {
            Object value = validate(field, rawValues.getOrDefault(field.key, ""));
            if (value == null) {
                invalidKeys.add(field.key);
            } else {
                parsed.put(field, value);
            }
        }
        if (!invalidKeys.isEmpty()) {
            activeCategory = fieldsByKey.get(invalidKeys.iterator().next()).category;
            errorMessage = tr("screen.darwin_soldier.aui.config_invalid_count", invalidKeys.size());
            return false;
        }
        parsed.forEach((field, value) -> setConfigValue(field.value, value));
        DarwinConfig.SPEC.save();
        return true;
    }

    private Object validate(Field field, String raw) {
        try {
            Object parsed = switch (field.kind()) {
                case BOOLEAN -> Boolean.parseBoolean(raw);
                case INTEGER -> Integer.parseInt(raw.trim());
                case DOUBLE -> Double.parseDouble(raw.trim());
                case STRING -> raw;
                case STRING_LIST -> List.of(raw.split("[\\r\\n,]+")).stream()
                        .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toList());
            };
            return field.spec.test(parsed) ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void showError(Document document, String message) {
        errorMessage = message == null ? "" : message;
        className(document, "config-error", errorMessage.isBlank()
                ? "alert alert-danger darwin-config-error hidden"
                : "alert alert-danger darwin-config-error");
        text(document, "config-error", errorMessage);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setConfigValue(ForgeConfigSpec.ConfigValue<?> value, Object parsed) {
        ((ForgeConfigSpec.ConfigValue) value).set(parsed);
    }

    private static String serialize(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining("\n"));
        }
        return String.valueOf(value);
    }

    private enum Kind {
        BOOLEAN, INTEGER, DOUBLE, STRING, STRING_LIST
    }

    private record Field(String category, String id, String key,
                         ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec spec) {
        private Kind kind() {
            Object sample = value.getDefault();
            if (sample instanceof Boolean) return Kind.BOOLEAN;
            if (sample instanceof Integer) return Kind.INTEGER;
            if (sample instanceof Number) return Kind.DOUBLE;
            if (sample instanceof List<?>) return Kind.STRING_LIST;
            return Kind.STRING;
        }

        private String elementId() {
            return "config-" + key.toLowerCase(Locale.ROOT).replace('.', '-').replace('_', '-');
        }
    }
}
