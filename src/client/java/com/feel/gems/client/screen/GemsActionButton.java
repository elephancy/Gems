package com.feel.gems.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A pixel-native, narrated action used by the field guide's navigation. */
public final class GemsActionButton extends Button {
    private static final int REST = 0xFF20343D;
    private static final int HOVER = 0xFF30525A;
    private static final int SELECTED = 0xFF344C4C;

    private final boolean selected;
    private Component detail = Component.empty();

    public GemsActionButton(int x, int y, int width, int height, Component label,
                            OnPress onPress, boolean selected) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.selected = selected;
    }

    public GemsActionButton withDetail(Component detail) {
        this.detail = detail;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX,
                                   int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int right = getRight();
        int bottom = getBottom();
        int edge = selected ? GemsUiTheme.GOLD : GemsUiTheme.RULE;
        int fill = selected ? SELECTED : isHoveredOrFocused() && active ? HOVER : REST;
        graphics.fill(x, y, right, bottom, fill);
        graphics.fill(x, y, x + 3, bottom, edge);
        graphics.fill(x + 3, y, right, y + 1, edge);
        graphics.fill(x + 3, bottom - 1, right, bottom, edge);
        if (isHoveredOrFocused() && active) {
            graphics.fill(x + 4, y + 2, right - 2, y + 3, GemsUiTheme.READY);
        }
        var font = Minecraft.getInstance().font;
        if (detail.getString().isEmpty()) {
            graphics.centeredText(font, getMessage(), x + getWidth() / 2,
                    y + Math.max(1, (getHeight() - font.lineHeight) / 2),
                    active ? GemsUiTheme.PALE : GemsUiTheme.MUTED);
        } else {
            graphics.text(font, font.plainSubstrByWidth(getMessage().getString(), getWidth() - 17),
                    x + 10, y + 4, active ? GemsUiTheme.PALE : GemsUiTheme.MUTED);
            graphics.text(font, font.plainSubstrByWidth(detail.getString(), getWidth() - 17),
                    x + 10, y + 17, GemsUiTheme.MUTED);
        }
    }
}
