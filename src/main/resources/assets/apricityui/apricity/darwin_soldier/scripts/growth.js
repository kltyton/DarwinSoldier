(function () {
    "use strict";
    var ui = DarwinUi, h = ui.h, Mc = McUIVue;

    function detail(s) {
        var d = s.detail;
        var children = d.lines.map(function (line, i) {
            return h("p", { key: i, class: line.muted ? "darwin-ability-line text-muted" : "darwin-ability-line" }, line.text);
        });
        children.push(h("div", { class: "darwin-action-row" }, d.actions.map(function (a, i) {
            if (typeof a.enabled === "boolean") {
                var change = function (value) {
                    if (!a.disabled) ui.action(a.action, Object.assign({}, a, { enabled: value }));
                };
                return h("div", { key: i, class: "darwin-ability-switch" }, [
                    h("span", null, a.label),
                    h(Mc.McSwitch, {
                        modelValue: a.enabled, disabled: a.disabled, tabindex: a.disabled ? -1 : 0,
                        "aria-label": a.label, "data-action": a.action,
                        "onUpdate:modelValue": change,
                        onKeydown: function (event) {
                            if (event.key === " " || event.key === "Enter") {
                                event.preventDefault();
                                change(!a.enabled);
                            }
                        }
                    })
                ]);
            }
            return ui.button(a.label, a.action, a, { key: i, disabled: a.disabled });
        })));
        return h("section", { class: "darwin-ability-detail-shell", role: "tabpanel", "aria-label": d.title }, [
            h("div", { class: "darwin-ability-copy" }, children)
        ]);
    }

    function intel(items) {
        var split = Math.ceil(items.length / 2);
        return [items.slice(0, split), items.slice(split)].map(function (column, index) {
            return h("div", { key: index, class: "darwin-intel-column" }, column.map(function (value, i) {
                return h("p", { key: i, class: "darwin-intel-item" }, value);
            }));
        });
    }

    function tab(item) {
        return h(Mc.McButton, {
            key: item.id, class: "darwin-ability-tab", variant: item.selected ? "primary" : "normal",
            role: "tab", "aria-selected": String(item.selected), disabled: !item.available,
            onClick: function () { ui.action("select-ability", { tab: item.id }); }
        }, { default: function () { return [
            h("texture", { src: item.texture, class: "darwin-skill-icon", alt: "" }),
            h("span", { class: "darwin-ability-title" }, item.title),
            h(Mc.McProgress, { value: item.percent, max: 100, showValue: false }),
            h("span", { class: "darwin-ability-progress-text" }, item.attribute + " " + item.current + "/" + item.requirement)
        ]; } });
    }

    function allocation(item) {
        return h("div", { key: item.id, class: "darwin-allocation-row" }, [
            h("div", { class: "darwin-allocation-head" }, [
                h("span", { id: "allocation-label-" + item.id }, item.label),
                h("div", { class: "darwin-stepper" }, [
                    ui.button("−", "adjust-allocation", { target: item.id, direction: -1 }, { size: "small", "aria-label": item.label + " −" }),
                    h("span", { class: "darwin-step-value" }, String(item.value)),
                    ui.button("+", "adjust-allocation", { target: item.id, direction: 1 }, { size: "small", "aria-label": item.label + " +" })
                ])
            ]),
            h(Mc.McSlider, {
                modelValue: item.value, min: 0, max: item.max, step: 1, showSegments: false, class: "darwin-range",
                "aria-label": item.label, "aria-labelledby": "allocation-label-" + item.id,
                "onUpdate:modelValue": function (value) { ui.input("allocation", { target: item.id, value: String(Math.round(value)) }); }
            })
        ]);
    }

    ui.mount(function (s) {
        if (!s.enabled) return [h("p", { class: "darwin-empty" }, s.lockedText)];
        return [
            s.saveNotice ? h("p", { class: "darwin-save-notice", role: "status" }, s.saveNotice) : null,
            h("div", { class: "darwin-status-strip" }, s.metrics.map(function (m, i) {
                return h("div", { key: i, class: "darwin-status-cell " + m.className }, [
                    h("span", { class: "darwin-status-label" }, m.label), h("strong", { class: "darwin-status-value" }, m.value)
                ]);
            })),
            h("div", { class: "darwin-growth-workspace" }, [
                ui.panel(s.allocationTitle, [
                    h("div", { class: "darwin-fields" }, s.allocation.map(allocation))
                ], { class: "darwin-growth-left" }, [
                    h("span", { class: "text-muted" }, s.remainingLabel),
                    ui.button(s.confirmLabel, "confirm-allocation", null, { size: "small", variant: "primary" })
                ]),
                h("section", { class: "darwin-ability-workspace" }, [
                    ui.panel(s.abilityTitle, [
                        h("div", { class: "darwin-skill-grid", role: "tablist", "aria-label": s.abilityTitle }, s.abilities.map(tab)), detail(s)
                    ], { class: "darwin-ability-card" }),
                    ui.panel(s.intelTitle, intel(s.intel), { class: "darwin-intel-card" })
                ])
            ])
        ];
    });
})();
