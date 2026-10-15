package com.kltyton.darwin_soldier.client.ui.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveKuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import io.github.kltyton.kltytonui.init.Document;
import net.minecraft.client.gui.screens.Screen;
import com.kltyton.darwin_soldier.config.ConfigValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DarwinConfigKuiScreen extends InteractiveKuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/config.html";

    private final Map<String, List<Field>> fieldsByCategory = new LinkedHashMap<>();
    private final Map<String, Field> fieldsByKey = new LinkedHashMap<>();
    private final Map<String, String> rawValues = new LinkedHashMap<>();
    private final Set<String> invalidKeys = new LinkedHashSet<>();
    private String activeCategory;
    private String errorMessage = "";

    public DarwinConfigKuiScreen(Screen parent) {
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
        state.addProperty("categoriesTitle", tr("screen.darwin_soldier.kui.categories"));
        state.addProperty("error", activeCategory == null ? tr("screen.darwin_soldier.kui.config_empty") : errorMessage);
        state.addProperty("resetLabel", tr("screen.darwin_soldier.kui.reset_category"));
        state.addProperty("cancelLabel", tr("gui.cancel"));
        state.addProperty("saveLabel", tr("gui.done"));
        state.addProperty("invalidLabel", tr("screen.darwin_soldier.kui.invalid_value"));
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
            item.addProperty("defaultValue", tr("screen.darwin_soldier.kui.default_value", serialize(field.value.getDefault())));
            item.addProperty("invalid", invalidKeys.contains(field.key));
            if (field.kind() == Kind.INTEGER || field.kind() == Kind.DOUBLE) {
                item.addProperty("step", numberStep(field.value.getDefault()));
                if (field.value.minimum() != null) {
                    item.addProperty("min", String.valueOf(field.value.minimum()));
                    item.addProperty("max", String.valueOf(field.value.maximum()));
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
        for (ConfigValue<?> value : DarwinConfig.SPEC.values()) {
            List<String> path = value.getPath();
            String category = path.get(0);
            if (!isVisibleCategory(category)) continue;
            String key = String.join(".", path);
            Field field = new Field(category, path.get(path.size() - 1), key, value);
            fieldsByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(field);
            fieldsByKey.put(key, field);
            rawValues.put(key, serialize(value.get()));
        }
        activeCategory = fieldsByCategory.keySet().stream().findFirst().orElse(null);
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
        List<ConfigValue.Update<?>> parsed = new ArrayList<>();
        invalidKeys.clear();
        for (Field field : fieldsByKey.values()) {
            ConfigValue.Update<?> value = validate(field, rawValues.getOrDefault(field.key, ""));
            if (value == null) {
                invalidKeys.add(field.key);
            } else {
                parsed.add(value);
            }
        }
        if (!invalidKeys.isEmpty()) {
            activeCategory = fieldsByKey.get(invalidKeys.iterator().next()).category;
            errorMessage = tr("screen.darwin_soldier.kui.config_invalid_count", invalidKeys.size());
            return false;
        }
        parsed.forEach(ConfigValue.Update::apply);
        DarwinConfig.SPEC.save();
        return true;
    }

    private ConfigValue.Update<?> validate(Field field, String raw) {
        try {
            return field.value.parse(raw);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
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
                         ConfigValue<?> value) {
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
