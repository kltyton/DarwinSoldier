(function () {
    "use strict";
    var ui = DarwinUi, h = ui.h;
    function fieldView(field) {
        function change(value) { ui.input("config", { key: field.key, value: String(value) }); }
        if (field.kind === "boolean") {
            return h(McUIVue.McSwitch, {
                modelValue: field.value === "true", tabindex: 0, "aria-label": field.label,
                "onUpdate:modelValue": change,
                onKeydown: function (event) {
                    if (event.key === " " || event.key === "Enter") {
                        event.preventDefault(); change(field.value !== "true");
                    }
                }
            });
        }
        return h(McUIVue.McTextField, {
            modelValue: field.value, type: "all", singleLine: field.kind !== "string_list", hint: field.label,
            class: field.invalid ? "darwin-config-input is-invalid" : "darwin-config-input",
            "aria-label": field.label, "aria-invalid": String(field.invalid), "onUpdate:modelValue": change,
            onKeydown: function (event) {
                if (!field.step || (event.key !== "ArrowUp" && event.key !== "ArrowDown")) return;
                var value = Number(field.value);
                if (!isFinite(value)) return;
                event.preventDefault();
                value += Number(field.step) * (event.key === "ArrowUp" ? 1 : -1);
                if (field.min != null) value = Math.max(Number(field.min), value);
                if (field.max != null) value = Math.min(Number(field.max), value);
                change(value.toFixed((field.step.split(".")[1] || "").length));
            }
        });
    }
    ui.mount(function (s) {
        var body = [];
        if (s.error) body.push(h("p", { class: "darwin-config-error", role: "alert" }, s.error));
        body.push(h("div", { class: "darwin-config-layout" }, [
            ui.panel(s.categoriesTitle, s.categories.map(function (category) {
                return ui.button(category.label, "select-category", { category: category.key }, {
                    key: category.key, class: "darwin-category", variant: category.key === s.activeCategory ? "primary" : "normal",
                    "aria-current": category.key === s.activeCategory ? "true" : "false"
                });
            }), { class: "darwin-category-list" }),
            ui.panel(s.activeCategoryLabel, s.fields.map(function (field) {
                return h("div", { class: "darwin-form-row", key: field.key }, [
                    h("span", { class: "darwin-field-label" }, field.label), fieldView(field),
                    h("p", { class: "darwin-field-help" }, field.defaultValue),
                    field.invalid ? h("p", { class: "darwin-field-error", role: "alert" }, s.invalidLabel) : null
                ]);
            }), { class: "darwin-config-content" })
        ]));
        return body;
    }, function (s) { return [
        ui.button(s.resetLabel, "reset-category", null, { variant: "error" }),
        h("div", { class: "darwin-action-row" }, [ui.button(s.cancelLabel, "cancel"), ui.button(s.saveLabel, "save", null, { variant: "primary" })])
    ]; });
})();
