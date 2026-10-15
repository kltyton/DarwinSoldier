(function () {
    "use strict";
    var ui = DarwinUi, h = ui.h;
    ui.mount(function (s) {
        var content = s.entries.length ? h("table", { class: "darwin-data-table darwin-adaptation-table" }, [
            h("thead", null, [h("tr", null, [s.nameLabel, s.idLabel, s.levelLabel, s.actionLabel].map(function (v) { return h("th", { scope: "col" }, v); }))]),
            h("tbody", null, s.entries.map(function (entry) {
                return h("tr", { key: entry.key }, [
                    h("td", null, entry.name), h("td", null, [h("code", null, entry.id)]), h("td", null, entry.detail),
                    h("td", null, [ui.button(entry.actionLabel, "toggle-adaptation", { key: entry.key }, {
                        size: "small", variant: entry.enabled ? "primary" : "normal", disabled: entry.pending,
                        "aria-pressed": String(entry.enabled), "aria-busy": String(entry.pending)
                    })])
                ]);
            }))
        ]) : h("p", { class: "darwin-empty" }, s.emptyText);
        return [ui.panel(s.title, [content])];
    }, function (s) { return [
        ui.button(s.previousLabel, "previous-page", null, { disabled: s.previousDisabled }),
        h("span", { class: "darwin-pagination-summary" }, s.pageSummary),
        ui.button(s.nextLabel, "next-page", null, { disabled: s.nextDisabled })
    ]; });
})();
