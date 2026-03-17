package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.InscriptionRemovePayload;
import com.feel.gems.net.InscriptionScreenPayload;
import com.feel.gems.net.InscriptionScreenPayload.Entry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Client UI for viewing/removing legendary inscriptions.
 */
public final class InscriptionScreen extends GemsScreenBase {
    private static final int ROW_HEIGHT = 28;
    private static final int REMOVE_BUTTON_WIDTH = 56;

    private final InscriptionScreenPayload payload;
    private final List<Button> removeButtons = new ArrayList<>();

    private int page = 0;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public InscriptionScreen(InscriptionScreenPayload payload) {
        super(Component.translatable("gems.screen.inscription.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        removeButtons.clear();

        int centerX = width / 2;
        int panelW = panelWidth(width);
        panelLeft = centerX - panelW / 2;
        panelRight = centerX + panelW / 2;
        panelTop = 18;
        panelBottom = height - 36;

        int totalRows = Math.max(1, Math.max(payload.maxSlots(), payload.inscriptions().size()));
        int totalPages = totalPages(totalRows, ENTRIES_PER_PAGE);
        page = clampPage(page, totalPages);

        int startIndex = page * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalRows);

        int entryX = panelLeft + 8;
        int entryY = CONTENT_START_Y;
        int removeX = panelRight - REMOVE_BUTTON_WIDTH - 8;

        for (int i = startIndex; i < endIndex; i++) {
            boolean hasInscription = i < payload.inscriptions().size();
            if (hasInscription) {
                int index = i;
                Button remove = Button.builder(Component.translatable("gems.screen.augment.remove"), b -> removeInscription(index))
                        .bounds(removeX, entryY - 2, REMOVE_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();
                removeButtons.add(remove);
                addRenderableWidget(remove);
            }
            entryY += ROW_HEIGHT;
        }

        int navY = navButtonY(height);
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.prev"), b -> changePage(-1))
                    .bounds(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
        if (page < totalPages - 1) {
            addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.next"), b -> changePage(1))
                    .bounds(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        addCloseButton();
    }

    private void changePage(int delta) {
        page += delta;
        rebuild();
    }

    private void removeInscription(int index) {
        ClientPlayNetworking.send(new InscriptionRemovePayload(payload.mainHand(), index));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawPanel(context, panelLeft, panelTop, panelRight, panelBottom);
        renderBase(context);

        String itemName = resolveItemName(payload.itemKey());
        String handLabel = payload.mainHand()
                ? Component.translatable("gems.screen.augment.hand.main").getString()
                : Component.translatable("gems.screen.augment.hand.off").getString();
        String itemLabel = Component.translatable("gems.screen.inscription.item", itemName + " (" + handLabel + ")").getString();
        context.centeredText(font, itemLabel, width / 2, SUBTITLE_Y, COLOR_GRAY);

        int slotsUsed = payload.inscriptions().size();
        String slotLabel = Component.translatable("gems.screen.augment.slots", slotsUsed, payload.maxSlots()).getString();
        context.centeredText(font, slotLabel, width / 2, SUBTITLE_Y + 12, COLOR_DARK_GRAY);

        int totalRows = Math.max(1, Math.max(payload.maxSlots(), payload.inscriptions().size()));
        int startIndex = page * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalRows);

        int entryX = panelLeft + 8;
        int entryY = CONTENT_START_Y;
        int textWidth = panelRight - panelLeft - REMOVE_BUTTON_WIDTH - 26;

        for (int i = startIndex; i < endIndex; i++) {
            if (i < payload.inscriptions().size()) {
                Entry entry = payload.inscriptions().get(i);
                MutableComponent title = Component.literal(entry.name());
                ChatFormatting rarity = rarityColor(entry.rarity());
                title = title.withStyle(rarity);

                String mag = formatMagnitude(entry.magnitude());
                if (!mag.isEmpty()) {
                    title.append(Component.literal(" " + mag).withStyle(ChatFormatting.DARK_GRAY));
                }

                context.text(font, title, entryX, entryY, COLOR_WHITE);
                String desc = entry.description();
                if (desc != null && !desc.isBlank()) {
                    context.text(font, trimText(desc, textWidth), entryX, entryY + 12, COLOR_GRAY);
                }
            } else {
                context.text(font, Component.translatable("gems.screen.augment.empty"), entryX, entryY, COLOR_DARK_GRAY);
            }
            entryY += ROW_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ChatFormatting rarityColor(String rarity) {
        if (rarity == null) {
            return ChatFormatting.WHITE;
        }
        return switch (rarity.toUpperCase()) {
            case "RARE" -> ChatFormatting.AQUA;
            case "EPIC" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }

    private String resolveItemName(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return "?";
        }
        return Component.translatable(itemKey).getString();
    }

    private String trimText(String text, int width) {
        return this.font.plainSubstrByWidth(text, width);
    }

    private static String formatMagnitude(float magnitude) {
        if (magnitude <= 0.0001f) {
            return "";
        }
        return String.format("x%.2f", magnitude);
    }
}
