package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.BonusSelectionClaimPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.BonusSelectionScreenPayload.BonusEntry;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Client UI for selecting bonus abilities and passives at energy 10.
 */
public final class BonusSelectionScreen extends GemsScreenBase {
    private static boolean lastShowingAbilities = true;
    private static int lastPageAbilities = 0;
    private static int lastPagePassives = 0;

    private final List<BonusEntry> abilities;
    private final List<BonusEntry> passives;
    private final int maxAbilities;
    private final int maxPassives;
    
    private boolean showingAbilities = true;
    private int page = 0;
    
    private Button tabAbilities;
    private Button tabPassives;
    
    public BonusSelectionScreen(BonusSelectionScreenPayload payload) {
        super(Component.translatable("gems.screen.bonus_selection.title"));
        this.abilities = new ArrayList<>(payload.abilities());
        this.passives = new ArrayList<>(payload.passives());
        this.maxAbilities = payload.maxAbilities();
        this.maxPassives = payload.maxPassives();
        this.showingAbilities = lastShowingAbilities;
        this.page = showingAbilities ? lastPageAbilities : lastPagePassives;
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
        
        // Tab buttons
        int tabY = 30;
        tabAbilities = addRenderableWidget(Button.builder(
                        Component.translatable("gems.screen.bonus_selection.tab_abilities").withStyle(showingAbilities ? ChatFormatting.UNDERLINE : ChatFormatting.RESET),
                        btn -> switchTab(true))
                .bounds(centerX - TAB_WIDTH - TAB_GAP / 2, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        tabPassives = addRenderableWidget(Button.builder(
                        Component.translatable("gems.screen.bonus_selection.tab_passives").withStyle(!showingAbilities ? ChatFormatting.UNDERLINE : ChatFormatting.RESET),
                        btn -> switchTab(false))
                .bounds(centerX + TAB_GAP / 2, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        
        int totalPg = totalPages(currentList.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);
        cachePage();
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        int y = tabY + TAB_HEIGHT + 26;
        
        if (currentList.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.bonus_selection.no_bonuses").withStyle(ChatFormatting.GRAY), btn -> {})
                    .bounds(entryX, y, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BonusEntry entry = currentList.get(i);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                
                ChatFormatting formatting;
                String suffix;
                String cooldownSuffix = "";
                if (entry.claimed()) {
                    formatting = ChatFormatting.GREEN;
                    suffix = " [Claimed]";
                } else if (entry.available()) {
                    formatting = ChatFormatting.WHITE;
                    suffix = "";
                } else {
                    formatting = ChatFormatting.DARK_GRAY;
                    suffix = " [Taken]";
                }

                if (showingAbilities) {
                    GemAbility ability = ModAbilities.get(entry.id());
                    int cooldownTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                    if (cooldownTicks > 0) {
                        cooldownSuffix = " [" + seconds(cooldownTicks) + "s]";
                    }
                }
                
                Component label = Component.literal(entry.name() + cooldownSuffix + suffix).withStyle(formatting);
                final BonusEntry finalEntry = entry;
                
                Button button = addRenderableWidget(Button.builder(label, btn -> toggleClaim(finalEntry))
                        .bounds(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
                
                // Disable if taken by someone else
                if (!entry.available() && !entry.claimed()) {
                    button.active = false;
                }
            }
        }
        
        // Pagination
        int navY = navButtonY(height);
        Button prev = Button.builder(Component.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        Button next = Button.builder(Component.translatable("gems.screen.button.next"), btn -> changePage(1))
                .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        
        int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE);
        prev.active = page > 0;
        next.active = page < maxPage;
        addRenderableWidget(prev);
        addRenderableWidget(next);
        
        // Close button
        addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
    
    private void switchTab(boolean toAbilities) {
        if (showingAbilities != toAbilities) {
            showingAbilities = toAbilities;
            page = showingAbilities ? lastPageAbilities : lastPagePassives;
            lastShowingAbilities = showingAbilities;
            rebuild();
        }
    }
    
    private void changePage(int delta) {
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        cachePage();
        rebuild();
    }

    private void cachePage() {
        lastShowingAbilities = showingAbilities;
        if (showingAbilities) {
            lastPageAbilities = page;
        } else {
            lastPagePassives = page;
        }
    }
    
    private void toggleClaim(BonusEntry entry) {
        boolean isAbility = showingAbilities;
        boolean currentlyClaimed = entry.claimed();
        
        // Check if trying to claim but at limit
        if (!currentlyClaimed) {
            int currentClaims = countClaims(isAbility);
            int max = isAbility ? maxAbilities : maxPassives;
            if (currentClaims >= max) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(
                            Component.translatable(isAbility ? "gems.screen.bonus_selection.max_abilities" : "gems.screen.bonus_selection.max_passives", max)
                    );
                }
                return;
            }
        }
        
        ClientPlayNetworking.send(new BonusSelectionClaimPayload(
                entry.id(),
                isAbility,
                !currentlyClaimed
        ));
    }
    
    private int countClaims(boolean abilities) {
        List<BonusEntry> list = abilities ? this.abilities : this.passives;
        int count = 0;
        for (BonusEntry entry : list) {
            if (entry.claimed()) count++;
        }
        return count;
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        
        renderBase(context);
        
        // Claimed count
        int claimedAbilities = countClaims(true);
        int claimedPassives = countClaims(false);
        String statusText = String.format("Abilities: %d/%d | Passives: %d/%d", 
                claimedAbilities, maxAbilities, claimedPassives, maxPassives);
        context.centeredText(this.font, statusText, centerX, 56, COLOR_GRAY);
        
        // Page info
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        if (!currentList.isEmpty()) {
            int maxPage = totalPages(currentList.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.centeredText(this.font, pageText, centerX, this.height - 80, COLOR_DARK_GRAY);
        }
        
        // Tooltip for hovered entry
        renderTooltip(context, mouseX, mouseY);
    }
    
    private void renderTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int centerX = this.width / 2;
        int panelW = panelWidth(width);
        int topY = 30 + TAB_HEIGHT + 26;
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        
        for (int i = start; i < end; i++) {
            int buttonY = topY + (i - start) * (BUTTON_HEIGHT + SPACING);
            int buttonX = centerX - panelW / 2;
            
            if (mouseX >= buttonX && mouseX <= buttonX + panelW &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                BonusEntry entry = currentList.get(i);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(entry.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
                
                // Wrap description
                String desc = entry.description();
                if (desc != null && !desc.isEmpty()) {
                    List<String> lines = wrapText(desc, 40);
                    for (String line : lines) {
                        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                    }
                }

                if (showingAbilities) {
                    GemAbility ability = ModAbilities.get(entry.id());
                    int cooldownTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                    if (cooldownTicks > 0) {
                        tooltip.add(Component.translatable("gems.screen.bonus_selection.cooldown", seconds(cooldownTicks))
                                .withStyle(ChatFormatting.DARK_AQUA));
                    }
                }
                
                if (entry.claimed()) {
                    tooltip.add(Component.translatable("gems.screen.bonus_selection.click_release").withStyle(ChatFormatting.YELLOW));
                } else if (entry.available()) {
                    tooltip.add(Component.translatable("gems.screen.bonus_selection.click_claim").withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(Component.translatable("gems.screen.bonus_selection.claimed_by_other").withStyle(ChatFormatting.RED));
                }
                
                context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
    
    private List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
            }
            currentLine.append(word).append(" ");
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines;
    }

    private static int seconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return Math.max(1, (ticks + 19) / 20);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
