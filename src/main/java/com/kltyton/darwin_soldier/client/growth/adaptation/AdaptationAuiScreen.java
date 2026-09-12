package com.kltyton.darwin_soldier.client.growth.adaptation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.network.SyncGrowthDataPacket;
import com.sighs.apricityui.init.Document;
import net.minecraft.client.gui.screens.Screen;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AdaptationAuiScreen extends InteractiveAuiScreen {
    private static final String TEMPLATE = "darwin_soldier/screens/adaptations.html";
    private static final int PAGE_SIZE = 20;

    private final Set<String> pendingKeys = new HashSet<>();
    private long seenRevision = -1L;
    private int page;

    public AdaptationAuiScreen(Screen parent) {
        super(TEMPLATE, parent);
    }

    @Override
    protected void onDocumentCreated(Document document) {
        seenRevision = ClientGrowthData.getRevision();
    }

    @Override
    protected void renderDocument(Document document) {
        List<SyncGrowthDataPacket.AdaptationEntry> entries = sortedEntries();
        int pageCount = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pageCount - 1);
        int from = Math.min(entries.size(), page * PAGE_SIZE);
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        JsonObject state = new JsonObject();
        state.addProperty("title", tr("screen.darwin_soldier.adaptations.title"));
        state.addProperty("closeLabel", tr("gui.done"));
        state.addProperty("closeAction", "back");
        state.addProperty("emptyText", tr("screen.darwin_soldier.no_adaptations"));
        state.addProperty("nameLabel", tr("screen.darwin_soldier.aui.adaptation"));
        state.addProperty("idLabel", tr("screen.darwin_soldier.aui.registry_id"));
        state.addProperty("levelLabel", tr("screen.darwin_soldier.aui.level"));
        state.addProperty("actionLabel", tr("screen.darwin_soldier.aui.action"));
        state.addProperty("previousLabel", tr("screen.darwin_soldier.aui.previous"));
        state.addProperty("nextLabel", tr("screen.darwin_soldier.aui.next"));
        state.addProperty("pageSummary", tr("screen.darwin_soldier.aui.page_summary", page + 1, pageCount, entries.size()));
        state.addProperty("previousDisabled", page == 0);
        state.addProperty("nextDisabled", page + 1 >= pageCount);
        JsonArray rows = new JsonArray();
        for (SyncGrowthDataPacket.AdaptationEntry entry : entries.subList(from, to)) {
            JsonObject row = new JsonObject();
            row.addProperty("key", entry.key());
            row.addProperty("name", entry.displayName());
            row.addProperty("id", entry.targetId());
            row.addProperty("detail", tr("screen.darwin_soldier.adaptation_detail",
                    entry.targetId(), entry.level(), entry.reductionPercent()));
            row.addProperty("enabled", entry.enabled());
            row.addProperty("pending", pendingKeys.contains(entry.key()));
            row.addProperty("actionLabel", tr(entry.enabled()
                    ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled"));
            rows.add(row);
        }
        state.add("entries", rows);
        publishState(document, state);
    }

    @Override
    protected void handleAction(Document document, JsonObject action, String actionName) {
        switch (actionName) {
            case "back" -> onClose();
            case "previous-page" -> page = Math.max(0, page - 1);
            case "next-page" -> page++;
            case "toggle-adaptation" -> toggle(data(action, "key"));
            default -> { return; }
        }
        if (!"back".equals(actionName)) renderNow();
    }

    @Override
    public void tick() {
        if (seenRevision != ClientGrowthData.getRevision()) {
            seenRevision = ClientGrowthData.getRevision();
            pendingKeys.clear();
            renderNow();
        }
    }

    private void toggle(String key) {
        if (pendingKeys.contains(key)) return;
        sortedEntries().stream()
                .filter(entry -> entry.key().equals(key))
                .findFirst()
                .ifPresent(entry -> {
                    pendingKeys.add(key);
                    ModNetwork.sendAdaptationToggle(entry.key(), !entry.enabled());
                });
    }

    private List<SyncGrowthDataPacket.AdaptationEntry> sortedEntries() {
        return ClientGrowthData.getAdaptationEntries().stream()
                .sorted(Comparator.comparing(SyncGrowthDataPacket.AdaptationEntry::displayName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SyncGrowthDataPacket.AdaptationEntry::targetId))
                .toList();
    }
}
