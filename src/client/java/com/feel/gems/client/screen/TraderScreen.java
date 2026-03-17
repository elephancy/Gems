package com.feel.gems.client.screen;

import com.feel.gems.GemsMod;
import com.feel.gems.client.ClientDisables;
import com.feel.gems.core.GemId;
import com.feel.gems.screen.TraderScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;




public final class TraderScreen extends AbstractContainerScreen<TraderScreenHandler> {
    private static final Component TITLE_TRADE = Component.translatable("screen." + GemsMod.MOD_ID + ".gem_trader.title");
    private static final Component TITLE_PURCHASE = Component.translatable("screen." + GemsMod.MOD_ID + ".purchase.title");
    private static final Component HINT_TRADE = Component.translatable("screen." + GemsMod.MOD_ID + ".gem_trader.hint");
    private static final Component HINT_PURCHASE = Component.translatable("screen." + GemsMod.MOD_ID + ".purchase.hint");

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int gridStartX;
    private int gridStartY;
    private int titleY;
    private int hintY;
    private java.util.List<net.minecraft.util.FormattedCharSequence> hintLines = java.util.List.of();

    private List<GemId> visibleGems = List.of();

    public TraderScreen(TraderScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, TITLE_TRADE);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int gap = 8;
        int columns = 2;

        visibleGems = computeVisibleGems();
        int rows = (visibleGems.size() + columns - 1) / columns;
        int gridWidth = columns * buttonWidth + (columns - 1) * gap;
        int gridHeight = rows * buttonHeight + (rows - 1) * gap;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.gridStartX = centerX - (gridWidth / 2);

        int maxTextWidth = Math.min(gridWidth + 40, this.width - 32);
        hintLines = this.font.split(hintText(), maxTextWidth);
        int hintHeight = hintLines.size() * this.font.lineHeight;
        titleLabelY = 12;
        hintY = titleLabelY + this.font.lineHeight + 4;
        int textBlockHeight = (hintY - titleLabelY) + hintHeight;

        this.gridStartY = Math.max(titleLabelY + textBlockHeight + 12, centerY - (gridHeight / 2));

        this.panelLeft = Math.max(8, gridStartX - 14);
        this.panelRight = Math.min(this.width - 8, gridStartX + gridWidth + 14);
        this.panelTop = Math.max(6, titleLabelY - 6);
        this.panelBottom = Math.min(this.height - 6, gridStartY + gridHeight + 54);

        for (int i = 0; i < visibleGems.size(); i++) {
            GemId gemId = visibleGems.get(i);
            int row = i / columns;
            int col = i % columns;

            int x = this.gridStartX + col * (buttonWidth + gap);
            int y = this.gridStartY + row * (buttonHeight + gap);

            Component label = Component.translatable("item." + GemsMod.MOD_ID + "." + gemId.name().toLowerCase() + "_gem");
            int buttonId = gemId.ordinal();
            addRenderableWidget(Button.builder(label, btn -> select(buttonId)).bounds(x, y, buttonWidth, buttonHeight).build());
        }

        int cancelWidth = gridWidth;
        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.cancel"), btn -> onClose()).bounds(this.gridStartX, this.gridStartY + gridHeight + 18, cancelWidth, buttonHeight).build());
    }

    private List<GemId> computeVisibleGems() {
        List<GemId> out = new ArrayList<>();
        for (GemId gem : GemId.values()) {
            if (ClientDisables.isInitialized() && ClientDisables.isGemDisabled(gem)) {
                continue;
            }
            out.add(gem);
        }
        return List.copyOf(out);
    }

    private void select(int buttonId) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        onClose();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // No-op: we render our own title/hint.
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xB0000000);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x33FFFFFF);
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x22111111);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.extractTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.centeredText(this.font, getTitle(), centerX, titleLabelY, 0xFFFFFFFF);
        int textY = hintY;
        for (var line : hintLines) {
            context.centeredText(this.font, line, centerX, textY, 0xA0A0A0);
            textY += this.font.lineHeight;
        }
    }

    @Override
    public Component getTitle() {
        return menu.isPurchaseMode() ? TITLE_PURCHASE : TITLE_TRADE;
    }

    private Component hintText() {
        return menu.isPurchaseMode() ? HINT_PURCHASE : HINT_TRADE;
    }
}
