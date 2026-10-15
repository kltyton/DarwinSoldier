(function () {
    "use strict";
    var ui = DarwinUi, h = ui.h;
    function tuning(field) {
        return h("div", { class: "darwin-allocation-row", key: field.field }, [
            h("div", { class: "darwin-allocation-head" }, [
                h("span", null, field.label),
                h("div", { class: "darwin-stepper" }, [
                    ui.button("−", "adjust-tuning", { field: field.field, direction: -1 }, { size: "small", "aria-label": field.label + " −" }),
                    h("span", { class: "darwin-step-value" }, field.formatted),
                    ui.button("+", "adjust-tuning", { field: field.field, direction: 1 }, { size: "small", "aria-label": field.label + " +" })
                ])
            ]),
            h(McUIVue.McSlider, {
                modelValue: field.value, min: field.min, max: field.max, step: field.step, class: "darwin-range", "aria-label": field.label,
                "onUpdate:modelValue": function (value) { ui.input("tuning", { field: field.field, value: String(value) }); }
            })
        ]);
    }
    ui.mount(function (s) {
        if (!s.hasWeapon) return [h("p", { class: "darwin-empty" }, s.noWeapon)];
        return [ui.panel(s.weapon, [
            h("p", { class: "darwin-aim-weapon-id" }, s.weaponId),
            h("p", { class: "darwin-aim-ballistics" }, s.ballistics),
            h("div", { class: "darwin-aim-controls" }, [
                ui.button(s.mode, "toggle-mode"),
                ui.button(s.trajectory, "toggle-trajectory", null, { "aria-pressed": String(s.trajectoryVisible) })
            ]),
            h("div", { class: "darwin-fields" }, s.tuning.map(tuning)),
            h("div", { class: "darwin-action-row" }, [
                ui.button(s.resetLabel, "reset", null, { variant: "error" }),
                ui.button(s.recalibrateLabel, "recalibrate"), ui.button(s.closeLabel, "back")
            ])
        ], { class: "darwin-aim-content" })];
    });
})();
