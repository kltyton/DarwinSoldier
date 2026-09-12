var DarwinUi = (function () {
    "use strict";
    var h = Vue.h;

    function send(type, key, name, data) {
        var payload = Object.assign({}, data || {});
        payload[key] = name;
        document.dispatchEvent(new CustomEvent(type, { detail: JSON.stringify(payload) }));
    }
    function action(name, data) { send("darwin-action", "action", name, data); }
    function input(name, data) { send("darwin-input", "input", name, data); }
    function button(label, name, data, props) {
        return h(McUIVue.McButton, Object.assign({ "data-action": name }, props || {}, {
            onClick: function () { action(name, data); }
        }), { default: function () { return label; } });
    }
    function panel(title, children, props, footer) {
        var slots = { default: function () { return children; } };
        if (footer) slots.footer = function () { return footer; };
        return h(McUIVue.McPanel, Object.assign({ title: title }, props || {}), slots);
    }
    function mount(renderBody, renderFooter) {
        var state = Vue.shallowRef(null);
        document.addEventListener("darwin-state", function (event) {
            state.value = JSON.parse(String(event.detail));
        });
        var app = Vue.createApp({
            render: function () {
                var s = state.value;
                if (!s) return h("div");
                var children = [
                    h(McUIVue.McHeader, { title: s.title, class: "darwin-header" }, {
                        title: function () { return h("div", { class: "darwin-brand-lockup" }, [
                            s.headerKicker ? h("span", { class: "darwin-brand-kicker" }, s.headerKicker) : null,
                            h("span", { class: "darwin-brand-title" }, s.title)
                        ]); },
                        right: function () {
                            return [s.headerStatus ? h("span", { class: "darwin-header-status" }, s.headerStatus) : null,
                                button(s.closeLabel, s.closeAction, null, { id: "screen-close", size: "small" })];
                        }
                    }),
                    h("main", { class: "darwin-main", id: "screen-content" }, renderBody(s))
                ];
                if (renderFooter) children.push(h("footer", { class: "darwin-footer" }, renderFooter(s)));
                return h("div", { class: "darwin-page" }, children);
            }
        });
        app.use(McUIVue.default);
        app.mount("#darwin-app");
        document.dispatchEvent(new CustomEvent("darwin-ready"));
    }
    return { h: h, action: action, input: input, button: button, panel: panel, mount: mount };
})();
