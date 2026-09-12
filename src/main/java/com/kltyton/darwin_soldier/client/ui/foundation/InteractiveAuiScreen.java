package com.kltyton.darwin_soldier.client.ui.foundation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import com.sighs.apricityui.event.Event;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.screen.ApricityScreen;
import java.math.BigDecimal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-thread host for Vue pages backed by game state and explicit UI events. */
public abstract class InteractiveAuiScreen extends ApricityScreen {
    private final Screen parent;
    private String publishedState = "";

    protected InteractiveAuiScreen(String templatePath, Screen parent) {
        super(templatePath);
        this.parent = parent;
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected final void init() {
        super.init();
        Document document = getLinkedDocument();
        if (document == null) return;
        publishedState = "";
        document.addEventListener("darwin-action", event -> receive(document, event, false));
        document.addEventListener("darwin-input", event -> receive(document, event, true));
        document.addEventListener("darwin-ready", event -> renderFresh(document));
        document.addEventListener("DOMContentLoaded", event -> renderFresh(document));
        onDocumentCreated(document);
        renderFresh(document);
    }

    protected void onDocumentCreated(Document document) {
    }

    protected abstract void renderDocument(Document document);

    protected abstract void handleAction(Document document, JsonObject action, String actionName);

    protected void handleInput(Document document, JsonObject input) {
    }

    protected final void publishState(Document document, JsonObject state) {
        String json = state.toString();
        if (!json.equals(publishedState)) {
            publishedState = json;
            document.dispatchEvent(new Event.CustomEvent("darwin-state", json, false));
        }
    }

    protected final void renderNow() {
        Document document = getLinkedDocument();
        if (document != null) renderDocument(document);
    }

    protected final Screen parent() {
        return parent;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        clearBrowserSelection();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (parent != null && minecraft != null) minecraft.setScreen(parent);
    }

    protected static String data(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Expected a scalar UI field: " + name);
        return value.getAsString();
    }

    protected static String tr(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }

    protected static int integerData(JsonObject object, String name) {
        try {
            return new BigDecimal(data(object, name)).intValueExact();
        } catch (ArithmeticException | NumberFormatException invalidValue) {
            throw new IllegalArgumentException("Expected an integer UI field: " + name, invalidValue);
        }
    }

    private void receive(Document document, Event event, boolean input) {
        try {
            JsonElement payload = JsonParser.parseString(String.valueOf(event.detail));
            if (!payload.isJsonObject()) throw new IllegalArgumentException("Expected a UI event object");
            JsonObject data = payload.getAsJsonObject();
            if (input) handleInput(document, data);
            else handleAction(document, data, data(data, "action"));
        } catch (JsonParseException | IllegalArgumentException invalidInput) {
            RuntimeDiagnostics.warn("aui_invalid_event", getClass().getSimpleName() + ": " + invalidInput.getMessage());
            renderFresh(document);
        }
    }

    private void renderFresh(Document document) {
        if (document == getLinkedDocument()) {
            publishedState = "";
            clearBrowserSelection();
            renderDocument(document);
        }
    }

    private void clearBrowserSelection() {
        Document document = getLinkedDocument();
        if (document == null) return;
        document.clearAllTextSelectionsExcept(document.getFocusedElement());
        document.clearDocumentSelection();
        document.clearRichTextSelection();
    }
}
