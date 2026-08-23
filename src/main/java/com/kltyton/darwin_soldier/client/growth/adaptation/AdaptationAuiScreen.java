package com.kltyton.darwin_soldier.client.growth.adaptation;

import com.kltyton.darwin_soldier.client.ClientGrowthData;
import com.kltyton.darwin_soldier.client.ui.foundation.InteractiveAuiScreen;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.network.SyncGrowthDataPacket;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
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
        if (entries.isEmpty()) {
            html(document, "adaptation-list", "<div class=\"darwin-empty\">"
                    + escapeHtml(tr("screen.darwin_soldier.no_adaptations")) + "</div>");
        } else {
            html(document, "adaptation-list", table(entries.subList(from, to)));
        }
        text(document, "adaptation-page-summary", tr("screen.darwin_soldier.aui.page_summary",
                page + 1, pageCount, entries.size()));
        disabled(document, "adaptation-previous", page == 0);
        disabled(document, "adaptation-next", page + 1 >= pageCount);
    }

    @Override
    protected void handleAction(Document document, Element action, String actionName) {
        switch (actionName) {
            case "back" -> onClose();
            case "previous-page" -> page = Math.max(0, page - 1);
            case "next-page" -> page++;
            case "toggle-adaptation" -> toggle(data(action, "key"));
            default -> {
                return;
            }
        }
        if (!"back".equals(actionName)) {
            renderNow();
        }
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

    private String table(List<SyncGrowthDataPacket.AdaptationEntry> entries) {
        StringBuilder rows = new StringBuilder("<div class=\"table-wrap\"><table class=\"table darwin-table darwin-adaptation-table\"><thead><tr><th>")
                .append(escapeHtml(tr("screen.darwin_soldier.aui.adaptation"))).append("</th><th>")
                .append(escapeHtml(tr("screen.darwin_soldier.aui.registry_id"))).append("</th><th>")
                .append(escapeHtml(tr("screen.darwin_soldier.aui.level"))).append("</th><th>")
                .append(escapeHtml(tr("screen.darwin_soldier.aui.action"))).append("</th></tr></thead><tbody>");
        for (SyncGrowthDataPacket.AdaptationEntry entry : entries) {
            boolean pending = pendingKeys.contains(entry.key());
            rows.append("<tr><td>").append(escapeHtml(entry.displayName())).append("</td><td><code>")
                    .append(escapeHtml(entry.targetId())).append("</code></td><td>")
                    .append(escapeHtml(tr("screen.darwin_soldier.adaptation_detail",
                            entry.targetId(), entry.level(), entry.reductionPercent())))
                    .append("</td><td><button class=\"button button-small ")
                    .append(entry.enabled() ? "button-primary" : "button-normal")
                    .append("\" type=\"button\" data-action=\"toggle-adaptation\" data-key=\"")
                    .append(escapeHtml(entry.key())).append("\"");
            if (pending) rows.append(" disabled");
            rows.append('>').append(escapeHtml(tr(entry.enabled()
                    ? "screen.darwin_soldier.enabled" : "screen.darwin_soldier.disabled")))
                    .append("</button></td></tr>");
        }
        return rows.append("</tbody></table></div>").toString();
    }
}
