package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.core.GemId;
import com.feel.gems.net.TitleSelectionScreenPayload;
import com.feel.gems.net.TitleSelectionSelectPayload;
import com.feel.gems.screen.GemSeerScreenHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TitleSelectionScreen extends GemsScreenBase {
    private final List<TitleSelectionScreenPayload.Entry> entries;
    private Tab tab = Tab.UNLOCKED;
    private int page = 0;

    public TitleSelectionScreen(TitleSelectionScreenPayload payload) {
        super(Component.translatable("gems.screen.title_selection.title"));
        this.entries = new ArrayList<>(payload.entries());
        this.entries.sort(Comparator.comparingInt(TitleSelectionScreen::sortKey));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int centerX = this.width / 2;
        int panelW = panelWidth(width);
        int entryX = centerX - panelW / 2;
        int tabY = SUBTITLE_Y + 12;

        Button unlockedTab = Button.builder(Component.translatable("gems.screen.title_selection.tab.unlocked"), btn -> switchTab(Tab.UNLOCKED))
                .bounds(centerX - TAB_WIDTH - TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build();
        Button allTab = Button.builder(Component.translatable("gems.screen.title_selection.tab.all"), btn -> switchTab(Tab.ALL))
                .bounds(centerX + TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build();
        unlockedTab.active = tab != Tab.UNLOCKED;
        allTab.active = tab != Tab.ALL;
        addRenderableWidget(unlockedTab);
        addRenderableWidget(allTab);

        List<TitleSelectionScreenPayload.Entry> visible = visibleEntries();
        int totalPg = totalPages(visible.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(visible.size(), start + ENTRIES_PER_PAGE);
        int y = tabY + TAB_HEIGHT + SPACING * 2;

        if (visible.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.title_selection.none").withStyle(ChatFormatting.GRAY), btn -> {})
                    .bounds(entryX, y, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                TitleSelectionScreenPayload.Entry entry = visible.get(i);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                Component label = labelFor(entry);
                Button button = addRenderableWidget(Button.builder(label, btn -> select(entry.id()))
                        .bounds(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
                if (entry.gemOrdinal() < 0) {
                    button.active = entry.unlocked();
                } else if (!entry.unlocked() && !entry.selected()) {
                    button.active = false;
                }
            }
        }

        int navY = navButtonY(height);
        Button prev = Button.builder(Component.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        Button next = Button.builder(Component.translatable("gems.screen.button.next"), btn -> changePage(1))
                .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = page > 0;
        next.active = (page + 1) * ENTRIES_PER_PAGE < visible.size();
        addRenderableWidget(prev);
        addRenderableWidget(next);

        int closeY = closeButtonY(height);
        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.clear"), btn -> clearSelection())
                .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(centerX + SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private Component labelFor(TitleSelectionScreenPayload.Entry entry) {
        if (entry.gemOrdinal() < 0) {
            MutableComponent label = Component.translatable("gems.screen.title_selection.general").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(entry.displayKey()).withStyle(ChatFormatting.GOLD));
            if (entry.selected()) {
                label.append(Component.translatable("gems.screen.title_selection.selected").withStyle(ChatFormatting.GOLD));
            } else if (entry.unlocked()) {
                label.append(Component.translatable("gems.screen.title_selection.leader").withStyle(ChatFormatting.GOLD));
            } else {
                label.append(Component.translatable("gems.screen.title_selection.locked").withStyle(ChatFormatting.DARK_GRAY));
            }
            return label;
        }
        GemId gem = safeGem(entry.gemOrdinal());
        String gemName = GemSeerScreenHandler.formatGemName(gem);

        MutableComponent label = Component.literal(gemName + " - ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(entry.displayKey()).withStyle(GemSeerScreenHandler.gemColor(gem)))
                .append(Component.literal(" [" + entry.usage() + "/" + entry.threshold() + "]").withStyle(ChatFormatting.DARK_GRAY));

        if (entry.selected()) {
            label.append(Component.translatable("gems.screen.title_selection.selected").withStyle(ChatFormatting.GOLD));
        } else if (!entry.unlocked()) {
            label.append(Component.translatable("gems.screen.title_selection.locked").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (entry.forcedSelected()) {
            label.append(Component.translatable("gems.screen.title_selection.admin").withStyle(ChatFormatting.DARK_GRAY));
        }

        return label;
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (visibleEntries().size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(String id) {
        ClientPlayNetworking.send(new TitleSelectionSelectPayload(id));
    }

    private void clearSelection() {
        ClientPlayNetworking.send(new TitleSelectionSelectPayload(""));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderBase(context);
        context.centeredText(this.font, Component.translatable("gems.screen.title_selection.subtitle"),
                this.width / 2, SUBTITLE_Y, COLOR_GRAY);

        if (!entries.isEmpty()) {
            int maxPage = totalPages(visibleEntries().size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            int pageY = SUBTITLE_Y + 12 + TAB_HEIGHT + 2;
            context.centeredText(this.font, pageText, this.width / 2, pageY, COLOR_GRAY);
        }
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        page = 0;
        rebuild();
    }

    private List<TitleSelectionScreenPayload.Entry> visibleEntries() {
        if (tab == Tab.ALL) {
            return entries;
        }
        List<TitleSelectionScreenPayload.Entry> filtered = new ArrayList<>();
        for (TitleSelectionScreenPayload.Entry entry : entries) {
            if (entry.unlocked() || entry.selected() || entry.forcedSelected()) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private static int sortKey(TitleSelectionScreenPayload.Entry entry) {
        int gem = Math.max(0, entry.gemOrdinal());
        int threshold = Math.max(0, entry.threshold());
        if (entry.gemOrdinal() < 0) {
            return -10000 + threshold;
        }
        return gem * 10000 + threshold;
    }

    private static GemId safeGem(int ordinal) {
        GemId[] values = GemId.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GemId.ASTRA;
        }
        return values[ordinal];
    }

    private enum Tab {
        UNLOCKED,
        ALL
    }
}
