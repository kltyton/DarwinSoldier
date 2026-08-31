package com.kltyton.darwin_soldier.client.growth.targeting;

import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
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
        html(document, "target-filters", filterMarkup());
        value(document, "target-search", query);
        renderList(document);
    }

    @Override
    protected void handleAction(Document document, Element action, String actionName) {
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
    protected void handleInput(Document document, Element input) {
        if (!"target-search".equals(data(input, "input"))) {
            return;
        }
        query = input.getValue();
        page = 0;
        renderList(document);
    }

    private String filterMarkup() {
        StringBuilder markup = new StringBuilder();
        for (Filter filter : Filter.values()) {
            boolean enabled = filter.value.get();
            String label = tr("screen.darwin_soldier.targets.filter_state", tr(filter.translationKey),
                    tr(enabled ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled"));
            markup.append("<button class=\"button ")
                    .append(enabled ? "button-primary" : "button-normal")
                    .append("\" type=\"button\" data-action=\"toggle-filter\" data-filter=\"")
                    .append(filter.name()).append("\">").append(escapeHtml(label)).append("</button>");
        }
        return markup.toString();
    }

    private void renderList(Document document) {
        List<RegistryEntityCatalog.Entry> matches = catalog.stream()
                .filter(entry -> RegistryEntityCatalog.matches(entry, query))
                .toList();
        int pageCount = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pageCount - 1);
        int from = Math.min(matches.size(), page * PAGE_SIZE);
        int to = Math.min(matches.size(), from + PAGE_SIZE);

        if (matches.isEmpty()) {
            html(document, "target-list", "<div class=\"darwin-empty\">"
                    + escapeHtml(tr("screen.darwin_soldier.targets.no_results")) + "</div>");
        } else {
            StringBuilder rows = new StringBuilder("<div class=\"table-wrap\"><table class=\"table darwin-table\"><thead><tr><th>")
                    .append(escapeHtml(tr("screen.darwin_soldier.aui.entity"))).append("</th><th>")
                    .append(escapeHtml(tr("screen.darwin_soldier.aui.registry_id"))).append("</th><th>")
                    .append(escapeHtml(tr("screen.darwin_soldier.aui.action"))).append("</th></tr></thead><tbody>");
            for (RegistryEntityCatalog.Entry entry : matches.subList(from, to)) {
                boolean selected = SuperPerceptionTargetSettings.isWhitelisted(entry.id());
                rows.append("<tr><td>").append(escapeHtml(entry.type().getDescription().getString()))
                        .append("</td><td><code>").append(escapeHtml(entry.id().toString())).append("</code></td><td>")
                        .append("<button class=\"button button-small ")
                        .append(selected ? "button-danger" : "button-primary")
                        .append("\" type=\"button\" data-action=\"toggle-whitelist\" data-id=\"")
                        .append(escapeHtml(entry.id().toString())).append("\">")
                        .append(escapeHtml(tr(selected
                                ? "screen.darwin_soldier.targets.remove" : "screen.darwin_soldier.targets.add")))
                        .append("</button></td></tr>");
            }
            rows.append("</tbody></table></div>");
            html(document, "target-list", rows.toString());
        }
        text(document, "target-page-summary", tr("screen.darwin_soldier.aui.page_summary",
                page + 1, pageCount, matches.size()));
        disabled(document, "target-previous", page == 0);
        disabled(document, "target-next", page + 1 >= pageCount);
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
