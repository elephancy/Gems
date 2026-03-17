package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.BountyBoardPayload;
import com.feel.gems.net.BountyPlacePayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class BountyBoardScreen extends GemsScreenBase {
    private enum Tab {
        ACTIVE,
        PLACE
    }

    private final List<BountyBoardPayload.Entry> bounties;
    private final List<BountyBoardPayload.PlayerEntry> players;
    private final UUID viewerId;
    private final int maxAdditionalHearts;
    private final int maxAdditionalEnergy;

    private Tab tab = Tab.ACTIVE;
    private int page = 0;
    private int playerPage = 0;
    private UUID selectedTarget;
    private int selectedHearts;
    private int selectedEnergy;

    public BountyBoardScreen(BountyBoardPayload payload) {
        super(Component.translatable("gems.screen.bounty.title"));
        this.viewerId = payload.viewerId();
        this.maxAdditionalHearts = payload.maxAdditionalHearts();
        this.maxAdditionalEnergy = payload.maxAdditionalEnergy();
        this.bounties = new ArrayList<>(payload.bounties());
        this.players = new ArrayList<>(payload.players());
        this.bounties.sort(Comparator.comparing((BountyBoardPayload.Entry e) -> e.targetName().toLowerCase(Locale.ROOT))
                .thenComparing(e -> e.placerName().toLowerCase(Locale.ROOT)));
        this.players.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        addTabs();

        if (tab == Tab.ACTIVE) {
            buildActiveTab();
        } else {
            buildPlaceTab();
        }

        addCloseButton();
    }

    private void addTabs() {
        int centerX = width / 2;
        int tabY = SUBTITLE_Y + 2;
        Button active = Button.builder(Component.translatable("gems.screen.bounty.tab.active"), btn -> {
            tab = Tab.ACTIVE;
            rebuild();
        }).bounds(centerX - TAB_WIDTH - TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        Button place = Button.builder(Component.translatable("gems.screen.bounty.tab.place"), btn -> {
            tab = Tab.PLACE;
            rebuild();
        }).bounds(centerX + TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        active.active = tab != Tab.ACTIVE;
        place.active = tab != Tab.PLACE;
        addRenderableWidget(active);
        addRenderableWidget(place);
    }

    private void buildActiveTab() {
        int panelW = panelWidth(width);
        int entryX = (width - panelW) / 2;
        int totalPg = totalPages(bounties.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(bounties.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (bounties.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.bounty.empty").withStyle(ChatFormatting.GRAY), btn -> {
            }).bounds(entryX, y, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BountyBoardPayload.Entry entry = bounties.get(i);
                String label = entry.targetName() + " | " + entry.placerName()
                        + " | " + entry.hearts() + "H " + entry.energy() + "E";
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                }).bounds(entryX, buttonY, panelW, BUTTON_HEIGHT).build()).active = false;
            }
        }

        addNavButtons(bounties.size(), page, delta -> {
            page = clampPage(page + delta, totalPg);
            rebuild();
        });
    }

    private void buildPlaceTab() {
        int panelW = panelWidth(width);
        int entryX = (width - panelW) / 2;
        int totalPg = totalPages(players.size(), ENTRIES_PER_PAGE);
        playerPage = clampPage(playerPage, totalPg);

        int start = playerPage * ENTRIES_PER_PAGE;
        int end = Math.min(players.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (players.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.bounty.no_players").withStyle(ChatFormatting.GRAY), btn -> {
            }).bounds(entryX, y, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BountyBoardPayload.PlayerEntry entry = players.get(i);
                boolean selected = entry.uuid().equals(selectedTarget);
                Component label = Component.literal(entry.name()).withStyle(selected ? ChatFormatting.GOLD : ChatFormatting.WHITE);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                addRenderableWidget(Button.builder(label, btn -> selectTarget(entry.uuid()))
                        .bounds(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
            }
        }

        int controlsY = CONTENT_START_Y + ENTRIES_PER_PAGE * (BUTTON_HEIGHT + SPACING) + SPACING;
        addCounterRow(entryX, controlsY, Component.translatable("gems.screen.bounty.hearts"), true);
        addCounterRow(entryX, controlsY + BUTTON_HEIGHT + SPACING, Component.translatable("gems.screen.bounty.energy"), false);

        int placeY = controlsY + (BUTTON_HEIGHT + SPACING) * 2;
        Button placeButton = Button.builder(Component.translatable("gems.screen.bounty.place"), btn -> placeBounty())
                .bounds(entryX, placeY, panelW, BUTTON_HEIGHT)
                .build();
        placeButton.active = selectedTarget != null && (selectedHearts > 0 || selectedEnergy > 0);
        addRenderableWidget(placeButton);

        addNavButtons(players.size(), playerPage, delta -> {
            playerPage = clampPage(playerPage + delta, totalPg);
            rebuild();
        });
    }

    private void addNavButtons(int count, int currentPage, java.util.function.IntConsumer changePage) {
        int centerX = width / 2;
        int navY = navButtonY(height);
        Button prev = Button.builder(Component.translatable("gems.screen.button.prev"), btn -> changePage.accept(-1))
                .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        Button next = Button.builder(Component.translatable("gems.screen.button.next"), btn -> changePage.accept(1))
                .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = currentPage > 0;
        next.active = (currentPage + 1) * ENTRIES_PER_PAGE < count;
        addRenderableWidget(prev);
        addRenderableWidget(next);
    }

    private void addCounterRow(int x, int y, Component label, boolean hearts) {
        int maxTotal = hearts ? maxHeartsTotal() : maxEnergyTotal();
        int minTotal = hearts ? baselineHearts() : baselineEnergy();
        int current = hearts ? selectedHearts : selectedEnergy;

        Component line = Component.literal(label.getString() + ": " + current + " / " + maxTotal)
                .withStyle(ChatFormatting.GRAY);
        addRenderableWidget(Button.builder(line, btn -> {
        }).bounds(x, y, panelWidth(width), BUTTON_HEIGHT).build()).active = false;

        int btnX = x + panelWidth(width) - (BUTTON_HEIGHT * 2 + SPACING);
        Button minus = Button.builder(Component.literal("-"), btn -> adjust(hearts, -1))
                .bounds(btnX, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                .build();
        Button plus = Button.builder(Component.literal("+"), btn -> adjust(hearts, 1))
                .bounds(btnX + BUTTON_HEIGHT + SPACING, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                .build();
        minus.active = current > minTotal;
        plus.active = current < maxTotal;
        addRenderableWidget(minus);
        addRenderableWidget(plus);
    }

    private void selectTarget(UUID uuid) {
        selectedTarget = uuid;
        selectedHearts = baselineHearts();
        selectedEnergy = baselineEnergy();
        rebuild();
    }

    private void adjust(boolean hearts, int delta) {
        int min = hearts ? baselineHearts() : baselineEnergy();
        int max = hearts ? maxHeartsTotal() : maxEnergyTotal();
        if (hearts) {
            selectedHearts = Math.max(min, Math.min(max, selectedHearts + delta));
        } else {
            selectedEnergy = Math.max(min, Math.min(max, selectedEnergy + delta));
        }
        rebuild();
    }

    private int baselineHearts() {
        BountyBoardPayload.Entry entry = existingBounty();
        return entry == null ? 0 : entry.hearts();
    }

    private int baselineEnergy() {
        BountyBoardPayload.Entry entry = existingBounty();
        return entry == null ? 0 : entry.energy();
    }

    private int maxHeartsTotal() {
        return baselineHearts() + maxAdditionalHearts;
    }

    private int maxEnergyTotal() {
        return baselineEnergy() + maxAdditionalEnergy;
    }

    private BountyBoardPayload.Entry existingBounty() {
        if (selectedTarget == null) {
            return null;
        }
        for (BountyBoardPayload.Entry entry : bounties) {
            if (entry.targetId().equals(selectedTarget) && entry.placerId().equals(viewerId)) {
                return entry;
            }
        }
        return null;
    }

    private void placeBounty() {
        if (selectedTarget == null) {
            return;
        }
        ClientPlayNetworking.send(new BountyPlacePayload(selectedTarget, selectedHearts, selectedEnergy));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderBase(context);
        if (tab == Tab.ACTIVE && !bounties.isEmpty()) {
            int maxPage = totalPages(bounties.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.centeredText(this.font, pageText, this.width / 2, SUBTITLE_Y + 22, COLOR_GRAY);
        }
        if (tab == Tab.PLACE && !players.isEmpty()) {
            int maxPage = totalPages(players.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (playerPage + 1) + " / " + maxPage;
            context.centeredText(this.font, pageText, this.width / 2, SUBTITLE_Y + 22, COLOR_GRAY);
        }
    }
}
