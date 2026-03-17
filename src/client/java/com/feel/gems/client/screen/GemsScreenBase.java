package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base screen for consistent Gems UI layout.
 */
public abstract class GemsScreenBase extends Screen {
    protected GemsScreenBase(Component title) {
        super(title);
    }

    protected void renderBase(GuiGraphicsExtractor context) {
        context.centeredText(this.font, this.title, this.width / 2, TITLE_Y, COLOR_WHITE);
    }

    protected Button addCloseButton() {
        int x = (this.width - CLOSE_BUTTON_WIDTH) / 2;
        int y = closeButtonY(this.height);
        return addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(x, y, CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    protected void drawPanel(GuiGraphicsExtractor context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, COLOR_PANEL_BG);
        context.fill(left, top, right, top + 1, COLOR_PANEL_BORDER);
        context.fill(left, bottom - 1, right, bottom, COLOR_PANEL_BORDER);
        context.fill(left, top, left + 1, bottom, COLOR_PANEL_BORDER);
        context.fill(right - 1, top, right, bottom, COLOR_PANEL_BORDER);
    }
}
