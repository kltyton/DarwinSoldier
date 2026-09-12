(function () {
    "use strict";
    var ui = DarwinUi, h = ui.h;
    function filter(entry) {
        function toggle() { ui.action("toggle-filter", { filter: entry.id }); }
        return h("div", { class: "darwin-filter-row", key: entry.id }, [
            h("span", null, entry.label),
            h(McUIVue.McSwitch, {
                modelValue: entry.enabled, tabindex: 0, "aria-label": entry.label,
                "onUpdate:modelValue": toggle,
                onKeydown: function (event) {
                    if (event.key === " " || event.key === "Enter") { event.preventDefault(); toggle(); }
                }
            })
        ]);
    }
    function table(s) {
        if (!s.entries.length) return h("p", { class: "darwin-empty" }, s.emptyText);
        return h("table", { class: "darwin-data-table" }, [
            h("thead", null, [h("tr", null, [s.entityLabel, s.idLabel, s.actionLabel].map(function (v) { return h("th", { scope: "col" }, v); }))]),
            h("tbody", null, s.entries.map(function (entry) {
                return h("tr", { key: entry.id }, [
                    h("td", null, entry.name), h("td", null, [h("code", null, entry.id)]),
                    h("td", null, [ui.button(entry.actionLabel, "toggle-whitelist", { id: entry.id }, { size: "small", variant: entry.selected ? "error" : "primary" })])
                ]);
            }))
        ]);
    }
    ui.mount(function (s) {
        return [h("div", { class: "darwin-target-layout" }, [
            ui.panel(s.filtersTitle, s.filters.map(filter), { class: "target-filters" }),
            ui.panel(s.listTitle, [
                h("div", { id: "target-search", class: "darwin-search" }, [
                    h("span", null, s.searchLabel),
                    h(McUIVue.McTextField, {
                        modelValue: s.query, type: "all", hint: s.searchLabel, "aria-label": s.searchLabel,
                        "onUpdate:modelValue": function (value) { ui.input("target-search", { value: String(value) }); }
                    }), h("p", { class: "darwin-field-help" }, s.searchHint)
                ]), table(s)
            ], { class: "target-list" })
        ])];
    }, function (s) { return [
        ui.button(s.previousLabel, "previous-page", null, { disabled: s.previousDisabled }),
        h("span", { class: "darwin-pagination-summary" }, s.pageSummary),
        ui.button(s.nextLabel, "next-page", null, { disabled: s.nextDisabled })
    ]; });
})();
