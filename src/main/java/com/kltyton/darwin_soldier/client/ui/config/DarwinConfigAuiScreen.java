package com.kltyton.darwin_soldier.client.ui.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.sighs.apricityui.init.Document;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.common.ModConfigSpec;

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
        JsonObject state = new JsonObject();
        state.addProperty("title", tr("config.darwin_soldier.title"));
        state.addProperty("closeLabel", tr("gui.cancel"));
        state.addProperty("closeAction", "cancel");
        state.addProperty("activeCategory", activeCategory == null ? "" : activeCategory);
        state.addProperty("activeCategoryLabel", activeCategory == null ? ""
                : tr("config.darwin_soldier.category." + activeCategory));
        state.addProperty("categoriesTitle", tr("screen.darwin_soldier.aui.categories"));
        state.addProperty("error", activeCategory == null ? tr("screen.darwin_soldier.aui.config_empty") : errorMessage);
        state.addProperty("resetLabel", tr("screen.darwin_soldier.aui.reset_category"));
        state.addProperty("cancelLabel", tr("gui.cancel"));
        state.addProperty("saveLabel", tr("gui.done"));
        state.addProperty("invalidLabel", tr("screen.darwin_soldier.aui.invalid_value"));
        JsonArray categories = new JsonArray();
        for (String category : fieldsByCategory.keySet()) {
            JsonObject item = new JsonObject();
            item.addProperty("key", category);
            item.addProperty("label", tr("config.darwin_soldier.category." + category));
            categories.add(item);
        }
        state.add("categories", categories);
        JsonArray fields = new JsonArray();
        for (Field field : fieldsByCategory.getOrDefault(activeCategory, List.of())) {
            JsonObject item = new JsonObject();
            item.addProperty("key", field.key);
            item.addProperty("label", tr(optionTranslationKey(field.id)));
            item.addProperty("kind", field.kind().name().toLowerCase(Locale.ROOT));
            item.addProperty("value", rawValues.getOrDefault(field.key, ""));
            item.addProperty("defaultValue", tr("screen.darwin_soldier.aui.default_value", serialize(field.value.getDefault())));
            item.addProperty("invalid", invalidKeys.contains(field.key));
            if (field.kind() == Kind.INTEGER || field.kind() == Kind.DOUBLE) {
                item.addProperty("step", numberStep(field.value.getDefault()));
                if (field.spec.getRange() != null) {
                    item.addProperty("min", String.valueOf(field.spec.getRange().getMin()));
                    item.addProperty("max", String.valueOf(field.spec.getRange().getMax()));
                }
            }
            fields.add(item);
        }
        state.add("fields", fields);
        publishState(document, state);
    }

    @Override
    protected void handleAction(Document document, JsonObject action, String actionName) {
        switch (actionName) {
            case "cancel" -> onClose();
            case "select-category" -> {
                String category = data(action, "category");
                if (!fieldsByCategory.containsKey(category)) return;
                activeCategory = category;
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
    protected void handleInput(Document document, JsonObject input) {
        if (!"config".equals(data(input, "input"))) {
            return;
        }
        String key = data(input, "key");
        Field field = fieldsByKey.get(key);
        if (field == null) {
            return;
        }
        String value = data(input, "value");
        rawValues.put(key, value);
        boolean valid = validate(field, value) != null;
        if (valid) invalidKeys.remove(key); else invalidKeys.add(key);
        renderNow();
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
            if (!(rawValue instanceof ModConfigSpec.ConfigValue<?> configValue)
                    || !(rawSpec instanceof ModConfigSpec.ValueSpec valueSpec)) {
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setConfigValue(ModConfigSpec.ConfigValue<?> value, Object parsed) {
        ((ModConfigSpec.ConfigValue) value).set(parsed);
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
                         ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {
        private Kind kind() {
            Object sample = value.getDefault();
            if (sample instanceof Boolean) return Kind.BOOLEAN;
            if (sample instanceof Integer) return Kind.INTEGER;
            if (sample instanceof Number) return Kind.DOUBLE;
            if (sample instanceof List<?>) return Kind.STRING_LIST;
            return Kind.STRING;
        }

    }
}
