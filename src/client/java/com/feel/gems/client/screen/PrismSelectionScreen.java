package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.PrismSelectionClaimPayload;
import com.feel.gems.net.PrismSelectionScreenPayload;
import com.feel.gems.net.PrismSelectionScreenPayload.PowerEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Client UI for Prism gem ability/passive selection at energy 10.
 */
public final class PrismSelectionScreen extends GemsScreenBase {
    private static int lastTab = 0;
    private static final int[] lastPages = new int[4];

    private final List<PowerEntry> gemAbilities;
    private final List<PowerEntry> bonusAbilities;
    private final List<PowerEntry> gemPassives;
    private final List<PowerEntry> bonusPassives;
    private final List<Identifier> selectedGemAbilities;
    private final List<Identifier> selectedBonusAbilities;
    private final List<Identifier> selectedGemPassives;
    private final List<Identifier> selectedBonusPassives;
    private final int maxGemAbilities;
    private final int maxBonusAbilities;
    private final int maxGemPassives;
    private final int maxBonusPassives;

    private int currentTab = 0; // 0=gem abilities, 1=bonus abilities, 2=gem passives, 3=bonus passives
    private int page = 0;

    private Button[] tabButtons;

    public PrismSelectionScreen(PrismSelectionScreenPayload payload) {
        super(Component.translatable("gems.prism.screen.title"));
        this.gemAbilities = payload.gemAbilities();
        this.bonusAbilities = payload.bonusAbilities();
        this.gemPassives = payload.gemPassives();
        this.bonusPassives = payload.bonusPassives();
        this.selectedGemAbilities = new ArrayList<>(payload.selectedGemAbilities());
        this.selectedBonusAbilities = new ArrayList<>(payload.selectedBonusAbilities());
        this.selectedGemPassives = new ArrayList<>(payload.selectedGemPassives());
        this.selectedBonusPassives = new ArrayList<>(payload.selectedBonusPassives());
        this.maxGemAbilities = payload.maxGemAbilities();
        this.maxBonusAbilities = payload.maxBonusAbilities();
        this.maxGemPassives = payload.maxGemPassives();
        this.maxBonusPassives = payload.maxBonusPassives();
        if (lastTab < 0 || lastTab > 3) {
            lastTab = 0;
        }
        this.currentTab = lastTab;
        this.page = lastPages[currentTab];
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int centerX = width / 2;
        int startY = 30;
        int panelW = panelWidth(width);

        // Tab buttons - 4 tabs
        tabButtons = new Button[4];
        int tabX = centerX - (TAB_WIDTH * 4 + TAB_GAP * 3) / 2;

        for (int i = 0; i < 4; i++) {
            final int tabIdx = i;
            Component label = switch (i) {
                case 0 -> Component.translatable("gems.screen.prism_selection.tab_gem_abilities");
                case 1 -> Component.translatable("gems.screen.prism_selection.tab_bonus_abilities");
                case 2 -> Component.translatable("gems.screen.prism_selection.tab_gem_passives");
                case 3 -> Component.translatable("gems.screen.prism_selection.tab_bonus_passives");
                default -> Component.empty();
            };
            String suffix = switch (i) {
                case 0 -> " (" + selectedGemAbilities.size() + "/" + maxGemAbilities + ")";
                case 1 -> " (" + selectedBonusAbilities.size() + "/" + maxBonusAbilities + ")";
                case 2 -> " (" + selectedGemPassives.size() + "/" + maxGemPassives + ")";
                case 3 -> " (" + selectedBonusPassives.size() + "/" + maxBonusPassives + ")";
                default -> "";
            };

            tabButtons[i] = Button.builder(label.copy().append(suffix), b -> switchTab(tabIdx))
                    .bounds(tabX + i * (TAB_WIDTH + TAB_GAP), startY, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            addRenderableWidget(tabButtons[i]);
        }

        // Update tab button appearance
        for (int i = 0; i < 4; i++) {
            tabButtons[i].active = (i != currentTab);
        }

        // Get current list
        List<PowerEntry> entries = getCurrentEntries();
        List<Identifier> selected = getCurrentSelected();

        int totalPg = totalPages(entries.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);
        cachePage();

        int entryY = startY + TAB_HEIGHT + 20;
        int entryX = centerX - panelW / 2;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            PowerEntry entry = entries.get(i);
            boolean isSelected = selected.contains(entry.id());
            boolean isAvailable = entry.available() || isSelected;
            String buttonText = entry.name() + " (" + entry.sourceName() + ")";

            final Identifier entryId = entry.id();
            Button entryButton = Button.builder(
                    Component.literal(buttonText).withStyle(!isAvailable ? ChatFormatting.RED : (isSelected ? ChatFormatting.GREEN : ChatFormatting.WHITE)),
                    b -> toggleEntry(entryId)
            ).bounds(entryX, entryY, panelW, ENTRY_HEIGHT).build();
            entryButton.active = isAvailable;

            addRenderableWidget(entryButton);
            entryY += ENTRY_HEIGHT + SPACING;
        }

        // Pagination
        int navY = navButtonY(height);
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.prev"), b -> changePage(-1))
                    .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
        if (page < totalPg - 1) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.next"), b -> changePage(1))
                    .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        // Close button
        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), b -> onClose())
                .bounds(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private List<PowerEntry> getCurrentEntries() {
        return switch (currentTab) {
            case 0 -> gemAbilities;
            case 1 -> bonusAbilities;
            case 2 -> gemPassives;
            case 3 -> bonusPassives;
            default -> List.of();
        };
    }

    private List<Identifier> getCurrentSelected() {
        return switch (currentTab) {
            case 0 -> selectedGemAbilities;
            case 1 -> selectedBonusAbilities;
            case 2 -> selectedGemPassives;
            case 3 -> selectedBonusPassives;
            default -> List.of();
        };
    }

    private int getMaxForCurrentTab() {
        return switch (currentTab) {
            case 0 -> maxGemAbilities;
            case 1 -> maxBonusAbilities;
            case 2 -> maxGemPassives;
            case 3 -> maxBonusPassives;
            default -> 0;
        };
    }

    private void switchTab(int tab) {
        currentTab = tab;
        page = lastPages[currentTab];
        lastTab = currentTab;
        rebuild();
    }

    private void changePage(int delta) {
        page += delta;
        cachePage();
        rebuild();
    }

    private void cachePage() {
        if (currentTab < 0 || currentTab > 3) {
            return;
        }
        lastTab = currentTab;
        lastPages[currentTab] = page;
    }

    private void toggleEntry(Identifier entryId) {
        List<Identifier> selected = getCurrentSelected();
        boolean isSelected = selected.contains(entryId);
        boolean isAbility = currentTab < 2;
        boolean isBonus = currentTab == 1 || currentTab == 3;

        if (isBonus && !isSelected) {
            for (PowerEntry entry : getCurrentEntries()) {
                if (entry.id().equals(entryId) && !entry.available()) {
                    return;
                }
            }
        }

        if (isSelected) {
            // Release
            selected.remove(entryId);
            ClientPlayNetworking.send(new PrismSelectionClaimPayload(entryId, isAbility, isBonus, false));
        } else {
            // Claim - check limit
            if (selected.size() >= getMaxForCurrentTab()) {
                return;
            }
            selected.add(entryId);
            ClientPlayNetworking.send(new PrismSelectionClaimPayload(entryId, isAbility, isBonus, true));
        }
        rebuild();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        renderBase(context);

        // Page indicator
        List<PowerEntry> entries = getCurrentEntries();
        int totalPg = totalPages(entries.size(), ENTRIES_PER_PAGE);
        context.centeredText(font,
                Component.literal("Page " + (page + 1) + "/" + totalPg),
                width / 2, height - 65, COLOR_GRAY);

        // Hover tooltip
        int centerX = width / 2;
        int panelW = panelWidth(width);
        int entryX = centerX - panelW / 2;
        int startY = 30 + TAB_HEIGHT + 20;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            int y = startY + (i - start) * (ENTRY_HEIGHT + SPACING);
            if (mouseX >= entryX && mouseX < entryX + panelW && mouseY >= y && mouseY < y + ENTRY_HEIGHT) {
                PowerEntry entry = entries.get(i);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(entry.name()).withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal(entry.description()).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Source: " + entry.sourceName()).withStyle(ChatFormatting.AQUA));
                if ((currentTab == 1 || currentTab == 3) && !entry.available() && !getCurrentSelected().contains(entry.id())) {
                    tooltip.add(Component.translatable("gems.screen.bonus_selection.claimed_by_other").withStyle(ChatFormatting.RED));
                }
                context.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
