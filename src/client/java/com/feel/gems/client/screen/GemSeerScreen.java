package com.feel.gems.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static com.feel.gems.client.screen.GemsScreenConstants.BUTTON_HEIGHT;
import static com.feel.gems.client.screen.GemsScreenConstants.CLOSE_BUTTON_WIDTH;
import static com.feel.gems.client.screen.GemsScreenConstants.COLOR_GRAY;
import static com.feel.gems.client.screen.GemsScreenConstants.COLOR_WHITE;
import static com.feel.gems.client.screen.GemsScreenConstants.CONTENT_START_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.ENTRIES_PER_PAGE;
import static com.feel.gems.client.screen.GemsScreenConstants.NAV_BUTTON_WIDTH;
import static com.feel.gems.client.screen.GemsScreenConstants.SPACING;
import static com.feel.gems.client.screen.GemsScreenConstants.SUBTITLE_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.TITLE_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.closeButtonY;
import static com.feel.gems.client.screen.GemsScreenConstants.navButtonY;
import static com.feel.gems.client.screen.GemsScreenConstants.panelWidth;
import com.feel.gems.screen.GemSeerScreenHandler;

/**
 * Client screen for the Gem Seer item.
 * Shows a list of online players with their active and owned gems.
 */
public final class GemSeerScreen extends AbstractContainerScreen<GemSeerScreenHandler> {
    private static final Component TITLE = Component.translatable("gems.screen.gem_seer.title").withStyle(ChatFormatting.GOLD);
    private static final Component HINT = Component.translatable("gems.screen.gem_seer.hint");
    private static final int ROW_HEIGHT = BUTTON_HEIGHT + SPACING;

    private final List<Row> visibleRows = new ArrayList<>();
    private int page = 0;
    private int entriesPerPage = ENTRIES_PER_PAGE;

    public GemSeerScreen(GemSeerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, TITLE);
    }

    private record Row(GemSeerScreenHandler.PlayerInfo info, Button button) {
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    public void refreshPlayers() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        visibleRows.clear();

        int centerX = this.width / 2;
        int panelW = panelWidth(this.width);
        int entryX = centerX - panelW / 2;
        int listY = CONTENT_START_Y;
        entriesPerPage = ENTRIES_PER_PAGE;

        List<GemSeerScreenHandler.PlayerInfo> players = menu.getPlayerInfos();
        int maxPage = players.isEmpty() ? 0 : Math.max(0, (players.size() - 1) / entriesPerPage);
        page = Math.max(0, Math.min(maxPage, page));

        if (players.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.gem_seer.no_players").withStyle(ChatFormatting.GRAY), btn -> {
            }).bounds(entryX, listY, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            int start = page * entriesPerPage;
            int end = Math.min(players.size(), start + entriesPerPage);

            int y = listY;
            for (int i = start; i < end; i++) {
                GemSeerScreenHandler.PlayerInfo info = players.get(i);

                Component label = Component.literal(info.name()).withStyle(info.online() ? ChatFormatting.GREEN : ChatFormatting.GRAY);

                int buttonId = i;
                Button button = Button.builder(label, btn -> select(buttonId))
                        .bounds(entryX, y, panelW, BUTTON_HEIGHT)
                        .build();
                addRenderableWidget(button);
                visibleRows.add(new Row(info, button));

                y += ROW_HEIGHT;
            }
        }

        int navY = navButtonY(this.height);
        Button prev = Button.builder(Component.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        Button next = Button.builder(Component.translatable("gems.screen.button.next"), btn -> changePage(1))
                .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = page > 0;
        next.active = (page + 1) * entriesPerPage < players.size();
        addRenderableWidget(prev);
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(this.height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void select(int buttonId) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void changePage(int delta) {
        List<GemSeerScreenHandler.PlayerInfo> players = menu.getPlayerInfos();
        if (players.isEmpty()) {
            return;
        }
        int maxPage = Math.max(0, (players.size() - 1) / entriesPerPage);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // No-op: we render our own title/hint.
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // No custom panel background; keep the same transparent look as Tracker Compass.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.extractTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.centeredText(this.font, TITLE, centerX, TITLE_Y, COLOR_WHITE);

        List<GemSeerScreenHandler.PlayerInfo> players = menu.getPlayerInfos();
        if (players.isEmpty()) {
            return;
        }

        context.centeredText(this.font, HINT, centerX, SUBTITLE_Y, COLOR_GRAY);

        Row hovered = getHoveredRow(mouseX, mouseY);
        if (hovered != null) {
            context.setComponentTooltipForNextFrame(this.font, tooltipFor(hovered.info), mouseX, mouseY);
        }
    }

    private Row getHoveredRow(int mouseX, int mouseY) {
        for (Row row : visibleRows) {
            if (row.button.isMouseOver(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private List<Component> tooltipFor(GemSeerScreenHandler.PlayerInfo info) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(info.name()).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("gems.screen.gem_seer.active_gem").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(GemSeerScreenHandler.formatGemName(info.activeGem()))
                        .withStyle(GemSeerScreenHandler.gemColor(info.activeGem()))));
        tooltip.add(Component.translatable("gems.screen.gem_seer.energy").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + info.energy() + "/10]").withStyle(GemSeerScreenHandler.energyColor(info.energy()))));
        tooltip.add(Component.translatable("gems.screen.gem_seer.owned_gems").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formatOwnedGems(info)).withStyle(ChatFormatting.WHITE)));
        return tooltip;
    }

    private static String formatOwnedGems(GemSeerScreenHandler.PlayerInfo info) {
        if (info.ownedGems() == null || info.ownedGems().isEmpty()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var gem : info.ownedGems()) {
            if (!first) sb.append(", ");
            sb.append(GemSeerScreenHandler.formatGemName(gem.gem()));
            if (gem.count() > 1) {
                sb.append(" x").append(gem.count());
            }
            first = false;
        }
        return sb.toString();
    }

}
