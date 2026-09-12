package com.kltyton.darwin_soldier.client.growth.targeting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.sighs.apricityui.init.Document;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class TargetingAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/targets.html";
    private static final int PAGE_SIZE = 24;

    private List<RegistryEntityCatalog.Entry> catalog = List.of();
    private String query = "";
    private int page;

    public TargetingAuiScreen(Screen parent) {
        super(TEMPLATE, parent);
    }

    @Override
    protected void onDocumentCreated(Document document) {
        catalog = RegistryEntityCatalog.all();
    }

    @Override
    protected void renderDocument(Document document) {
        JsonObject state = new JsonObject();
        state.addProperty("title", tr("screen.darwin_soldier.targets.title"));
        state.addProperty("closeLabel", tr("gui.done"));
        state.addProperty("closeAction", "back");
        state.addProperty("filtersTitle", tr("screen.darwin_soldier.aui.target_filters"));
        state.addProperty("listTitle", tr("screen.darwin_soldier.aui.entity_whitelist"));
        state.addProperty("searchLabel", tr("screen.darwin_soldier.targets.search"));
        state.addProperty("query", query);
        state.addProperty("searchHint", tr("screen.darwin_soldier.targets.search_hint"));
        state.addProperty("emptyText", tr("screen.darwin_soldier.targets.no_results"));
        state.addProperty("entityLabel", tr("screen.darwin_soldier.aui.entity"));
        state.addProperty("idLabel", tr("screen.darwin_soldier.aui.registry_id"));
        state.addProperty("actionLabel", tr("screen.darwin_soldier.aui.action"));
        state.addProperty("previousLabel", tr("screen.darwin_soldier.aui.previous"));
        state.addProperty("nextLabel", tr("screen.darwin_soldier.aui.next"));
        JsonArray filters = new JsonArray();
        for (Filter filter : Filter.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", filter.name());
            entry.addProperty("enabled", filter.value.get());
            entry.addProperty("label", tr(filter.translationKey));
            filters.add(entry);
        }
        state.add("filters", filters);
        populateList(state);
        publishState(document, state);
    }

    @Override
    protected void handleAction(Document document, JsonObject action, String actionName) {
        switch (actionName) {
            case "back" -> onClose();
            case "toggle-filter" -> SuperPerceptionTargetSettings.toggle(
                    Filter.valueOf(data(action, "filter")).value);
            case "toggle-whitelist" -> {
                ResourceLocation id = ResourceLocation.tryParse(data(action, "id"));
                if (id != null) {
                    SuperPerceptionTargetSettings.setWhitelisted(id,
                            !SuperPerceptionTargetSettings.isWhitelisted(id));
                }
            }
            case "previous-page" -> page = Math.max(0, page - 1);
            case "next-page" -> page++;
            default -> {
                return;
            }
        }
        if (!"back".equals(actionName)) {
            renderNow();
        }
    }

    @Override
    protected void handleInput(Document document, JsonObject input) {
        if (!"target-search".equals(data(input, "input"))) {
            return;
        }
        query = data(input, "value");
        page = 0;
        renderNow();
    }

    private void populateList(JsonObject state) {
        List<RegistryEntityCatalog.Entry> matches = catalog.stream()
                .filter(entry -> RegistryEntityCatalog.matches(entry, query))
                .toList();
        int pageCount = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pageCount - 1);
        int from = Math.min(matches.size(), page * PAGE_SIZE);
        int to = Math.min(matches.size(), from + PAGE_SIZE);

        JsonArray entries = new JsonArray();
        for (RegistryEntityCatalog.Entry entry : matches.subList(from, to)) {
            boolean selected = SuperPerceptionTargetSettings.isWhitelisted(entry.id());
            JsonObject row = new JsonObject();
            row.addProperty("id", entry.id().toString());
            row.addProperty("name", entry.type().getDescription().getString());
            row.addProperty("selected", selected);
            row.addProperty("actionLabel", tr(selected
                    ? "screen.darwin_soldier.targets.remove" : "screen.darwin_soldier.targets.add"));
            entries.add(row);
        }
        state.add("entries", entries);
        state.addProperty("pageSummary", tr("screen.darwin_soldier.aui.page_summary", page + 1, pageCount, matches.size()));
        state.addProperty("previousDisabled", page == 0);
        state.addProperty("nextDisabled", page + 1 >= pageCount);
    }

    private enum Filter {
        HOSTILE("screen.darwin_soldier.targets.hostile", DarwinConfig.SUPER_PERCEPTION_TARGET_MONSTERS),
        NEUTRAL("screen.darwin_soldier.targets.neutral", DarwinConfig.SUPER_PERCEPTION_TARGET_NEUTRAL),
        PASSIVE("screen.darwin_soldier.targets.passive", DarwinConfig.SUPER_PERCEPTION_TARGET_PASSIVE),
        PLAYERS("screen.darwin_soldier.targets.players", DarwinConfig.SUPER_PERCEPTION_TARGET_PLAYERS),
        PETS("screen.darwin_soldier.targets.filter_pets", DarwinConfig.SUPER_PERCEPTION_FILTER_PETS);

        private final String translationKey;
        private final ModConfigSpec.BooleanValue value;

        Filter(String translationKey, ModConfigSpec.BooleanValue value) {
            this.translationKey = translationKey;
            this.value = value;
        }
    }
}
