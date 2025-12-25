package com.technobecet.minerscompass.client.gui;

import com.technobecet.minerscompass.MinersCompassMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.Collections;
import java.util.List;

public class ButtonElementListWidget extends ElementListWidget<ButtonElementListWidget.ButtonElement> {
    public ButtonElementListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Override
    public void addEntryToTop(ButtonElement buttonElement) {
        super.addEntryToTop(buttonElement);
    }

    @Override
    public int addEntry(ButtonElement buttonElement) {
        return super.addEntry(buttonElement);
    }

    public static class ButtonElement extends ElementListWidget.Entry<ButtonElement> {
        private final int buttonWidth = 120;
        private final int buttonHeight = 20;
        private final int spacing = 10;
        public final List<ButtonWidget> buttons;

        public ButtonElement(List<ButtonWidget> buttons) {
            this.buttons = buttons;
            for (var button : this.buttons) {
                button.setHeight(buttonHeight);
                button.setWidth(buttonWidth);
            }
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            var buttonCount = buttons.size();
            if (buttonCount == 0) return;

            //int x0 = x - (buttonWidth * buttonCount + spacing * (buttonCount - 1)) / 2;

            for (int i = 0; i < buttonCount; i++) {

                int xi = x + buttonWidth / 2 + (i - 1) * (buttonWidth + spacing);
                buttons.get(i).setPosition(xi, y);
                buttons.get(i).render(context, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return buttons;
        }

        @Override
        public List<? extends Element> children() {
            return buttons;
        }
    }
}
