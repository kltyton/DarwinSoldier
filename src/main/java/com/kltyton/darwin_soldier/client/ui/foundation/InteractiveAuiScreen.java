package com.kltyton.darwin_soldier.client.ui.foundation;

import com.sighs.apricityui.event.Event;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-thread host shared by Darwin Soldier's interactive AUI pages. */
public abstract class InteractiveAuiScreen extends ApricityScreen {
    private final Screen parent;

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
        if (document == null) {
            return;
        }
        document.addEventListener("click", event -> dispatchAction(document, event));
        document.addEventListener("input", event -> dispatchInput(document, event));
        document.addEventListener("DOMContentLoaded", event -> renderIfCurrent(document));
        onDocumentCreated(document);
        renderIfCurrent(document);
    }

    protected void onDocumentCreated(Document document) {
    }

    protected abstract void renderDocument(Document document);

    protected abstract void handleAction(Document document, Element action, String actionName);

    protected void handleInput(Document document, Element input) {
    }

    protected final void renderNow() {
        Document document = getLinkedDocument();
        if (document != null) {
            renderDocument(document);
        }
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
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    protected static String data(Element element, String name) {
        String value = element.getAttribute("data-" + name);
        return value == null ? "" : value;
    }

    protected static void text(Document document, String id, Component value) {
        text(document, id, value.getString());
    }

    protected static void text(Document document, String id, String value) {
        Element element = document.getElementById(id);
        if (element != null) {
            element.setTextContent(value == null ? "" : value);
        }
    }

    protected static void html(Document document, String id, String value) {
        Element element = document.getElementById(id);
        if (element != null) {
            element.setInnerHTML(value == null ? "" : value);
        }
    }

    protected static void value(Document document, String id, String value) {
        Element element = document.getElementById(id);
        if (element != null) {
            element.setValue(value == null ? "" : value);
        }
    }

    protected static void attribute(Document document, String id, String name, String value) {
        Element element = document.getElementById(id);
        if (element == null) {
            return;
        }
        if (value == null) {
            element.removeAttribute(name);
        } else {
            element.setAttribute(name, value);
        }
    }

    protected static void disabled(Document document, String id, boolean disabled) {
        Element element = document.getElementById(id);
        if (element != null) {
            element.setDisabled(disabled);
        }
    }

    protected static void className(Document document, String id, String className) {
        Element element = document.getElementById(id);
        if (element != null) {
            element.setClassName(className);
        }
    }

    protected static String tr(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }

    protected static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void dispatchAction(Document document, Event event) {
        Element action = findActionElement(event.target);
        if (action == null || action.isDisabled()) {
            return;
        }
        String actionName = data(action, "action");
        if (!actionName.isBlank()) {
            handleAction(document, action, actionName);
        }
    }

    private void dispatchInput(Document document, Event event) {
        if (event.target instanceof Element input && !input.isDisabled()) {
            handleInput(document, input);
        }
    }

    private void renderIfCurrent(Document document) {
        if (document == getLinkedDocument()) {
            clearBrowserSelection();
            renderDocument(document);
        }
    }

    private void clearBrowserSelection() {
        Document document = getLinkedDocument();
        if (document == null) {
            return;
        }
        document.clearAllTextSelections();
        document.clearDocumentSelection();
        document.clearRichTextSelection();
    }

    private static Element findActionElement(Object target) {
        if (!(target instanceof Element element)) {
            return null;
        }
        Element current = element;
        while (current != null) {
            if (current.hasAttribute("data-action")) {
                return current;
            }
            current = current.parentElement;
        }
        return null;
    }
}
