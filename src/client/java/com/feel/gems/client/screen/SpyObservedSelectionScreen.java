package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.SpyObservedScreenPayload;
import com.feel.gems.net.SpyObservedSelectPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client UI for selecting observed Spy abilities for Echo/Steal.
 */
public final class SpyObservedSelectionScreen extends GemsScreenBase {
    private final List<SpyObservedScreenPayload.ObservedEntry> entries;
    private final List<SpyObservedScreenPayload.StolenEntry> stolen;
    private final Identifier selectedEchoId;
    private final Identifier selectedStealId;
    private final Identifier selectedStolenCastId;
    private int page = 0;
    private Tab tab = Tab.ECHO;

    public SpyObservedSelectionScreen(SpyObservedScreenPayload payload) {
        super(Component.translatable("gems.screen.spy_observed.title"));
        this.entries = new ArrayList<>(payload.observed());
        this.stolen = new ArrayList<>(payload.stolen());
        this.selectedEchoId = payload.selectedEchoId();
        this.selectedStealId = payload.selectedStealId();
        this.selectedStolenCastId = payload.selectedStolenCastId();
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

        List<EntryView> visible = entriesForTab();
        int totalPg = totalPages(visible.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(visible.size(), start + ENTRIES_PER_PAGE);
        int listY = CONTENT_START_Y + BUTTON_HEIGHT + SPACING;

        addTabButtons(centerX, entryX, panelW);

        if (visible.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.spy_observed.none").withStyle(ChatFormatting.GRAY), btn -> {})
                    .bounds(entryX, listY + 8, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                EntryView view = visible.get(i);
                boolean isSelected = view.selected;
                String labelText = view.label;

                ChatFormatting color = view.color;
                if (isSelected) {
                    color = ChatFormatting.GOLD;
                }
                Component label = Component.literal(labelText).withStyle(color);

                Button button = addRenderableWidget(Button.builder(label, btn -> select(view.id))
                        .bounds(entryX, listY + (i - start) * (BUTTON_HEIGHT + SPACING), panelW, BUTTON_HEIGHT)
                        .build());
                button.active = view.clickable;
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
        next.active = page < totalPg - 1;
        addRenderableWidget(prev);
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void addTabButtons(int centerX, int entryX, int panelW) {
        int tabW = (panelW - (SPACING * 2)) / 3;
        int tabY = CONTENT_START_Y;

        addRenderableWidget(Button.builder(tabLabel(Tab.ECHO), btn -> switchTab(Tab.ECHO))
                .bounds(entryX, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.ECHO;

        addRenderableWidget(Button.builder(tabLabel(Tab.STEAL), btn -> switchTab(Tab.STEAL))
                .bounds(entryX + tabW + SPACING, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.STEAL;

        addRenderableWidget(Button.builder(tabLabel(Tab.STOLEN_CAST), btn -> switchTab(Tab.STOLEN_CAST))
                .bounds(entryX + (tabW + SPACING) * 2, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.STOLEN_CAST;
    }

    private Component tabLabel(Tab target) {
        return switch (target) {
            case ECHO -> Component.translatable("gems.screen.spy_observed.tab.echo");
            case STEAL -> Component.translatable("gems.screen.spy_observed.tab.steal");
            case STOLEN_CAST -> Component.translatable("gems.screen.spy_observed.tab.stolen_cast");
        };
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        page = 0;
        rebuild();
    }

    private List<EntryView> entriesForTab() {
        List<EntryView> out = new ArrayList<>();
        if (tab == Tab.STOLEN_CAST) {
            for (SpyObservedScreenPayload.StolenEntry entry : stolen) {
                boolean selected = entry.id().equals(selectedStolenCastId);
                out.add(new EntryView(entry.id(), entry.name(), ChatFormatting.AQUA, true, selected));
            }
            return out;
        }

        for (SpyObservedScreenPayload.ObservedEntry entry : entries) {
            if (tab == Tab.ECHO && !entry.canEcho()) {
                continue;
            }
            if (tab == Tab.STEAL && !entry.canSteal()) {
                continue;
            }
            boolean selected = tab == Tab.ECHO
                    ? entry.id().equals(selectedEchoId)
                    : entry.id().equals(selectedStealId);
            String label = entry.name() + " x" + entry.count();
            ChatFormatting color = tab == Tab.STEAL ? ChatFormatting.GREEN : ChatFormatting.AQUA;
            out.add(new EntryView(entry.id(), label, color, true, selected));
        }
        return out;
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (entriesForTab().size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(Identifier id) {
        int tabId = switch (tab) {
            case ECHO -> SpyObservedSelectPayload.TAB_ECHO;
            case STEAL -> SpyObservedSelectPayload.TAB_STEAL;
            case STOLEN_CAST -> SpyObservedSelectPayload.TAB_STOLEN_CAST;
        };
        ClientPlayNetworking.send(new SpyObservedSelectPayload(tabId, id));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        renderBase(context);
        context.centeredText(this.font,
                Component.translatable("gems.screen.spy_observed.subtitle"),
                centerX, SUBTITLE_Y, COLOR_GRAY);
    }

    private enum Tab {
        ECHO,
        STEAL,
        STOLEN_CAST
    }

    private record EntryView(Identifier id, String label, ChatFormatting color, boolean clickable, boolean selected) {
    }
}
