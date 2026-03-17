package com.feel.gems.client.hud;

import com.feel.gems.GemsMod;
import com.feel.gems.client.ClientAbilityOrder;
import com.feel.gems.client.ClientAbilitySelection;
import com.feel.gems.client.ClientBonusState;
import com.feel.gems.client.ClientChaosState;
import com.feel.gems.client.ClientCooldowns;
import com.feel.gems.client.ClientExtraState;
import com.feel.gems.client.ClientGemState;
import com.feel.gems.client.ClientHudLayout;
import com.feel.gems.client.ClientPrismState;
import com.feel.gems.client.ClientRivalryState;
import com.feel.gems.client.GemsKeybinds;
import com.feel.gems.client.screen.GemsUiTheme;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemEnergyTier;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;




public final class GemsHud {
    private static final int PANEL = 0xB80B1722;
    private static final int MUTED = 0xFFC0CCD2;
    private static final int READY = 0xFF80DAB3;
    private static final int WAITING = 0xFFFFD080;
    private static final int COMPACT_WIDTH = 194;
    private static boolean registered = false;

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private GemsHud() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gems_hud"),
                (ctx, tickCounter) -> render(ctx)
        );
    }

    private static void render(GuiGraphicsExtractor ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.gui.hud.isHidden() || client.gui.screen() != null) {
            return;
        }

        Font tr = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        GemLoadout.HudLayout layout = ClientHudLayout.get();
        int lineHeight = tr.lineHeight + (layout.compactMode() ? 0 : 2);
        int margin = 8;
        int maxLines = Math.max(1, (screenHeight - (margin * 2)) / Math.max(1, lineHeight));
        boolean showCooldowns = layout.showCooldowns();
        boolean showEnergy = layout.showEnergy();
        boolean compactMode = layout.compactMode();
        List<HudLine> lines = new java.util.ArrayList<>();

        if (!ClientGemState.isInitialized()) {
            addLine(lines, maxLines, line("Gem: syncing...", opaque(0xAAAAAA)));
            drawLines(ctx, tr, lines, layout.position(), margin, lineHeight, screenWidth, screenHeight, MUTED);
            return;
        }

        GemId gem = ClientGemState.activeGem();
        int energy = ClientGemState.energy();
        GemEnergyTier tier = new GemEnergyState(energy).tier();

        if (compactMode) {
            renderCompact(ctx, tr, layout, margin, screenWidth, screenHeight,
                    gem, energy, tier, maxLines);
            return;
        }

        addLine(lines, maxLines, line("Gem: " + title(gem.name()), opaque(0xFFFFFF)));
        if (showEnergy) {
            addLine(lines, maxLines, line("Energy: " + tierLabel(tier) + " (" + energy + "/10)", tierColor(tier)));
        }

        if (gem == GemId.FLUX) {
            int charge = ClientExtraState.fluxChargePercent();
            int color = charge >= 200 ? opaque(0xFF4444) : (charge >= 100 ? opaque(0x55FFFF) : opaque(0xFFFFFF));
            addLine(lines, maxLines, line("Flux charge: " + charge + "%", color));
        }
        if (gem == GemId.ASTRA) {
            if (ClientExtraState.hasSoul()) {
                String type = prettyEntityId(ClientExtraState.soulTypeId());
                addLine(lines, maxLines, line("Soul: captured (" + type + ")", opaque(0x55FFFF)));
            } else {
                addLine(lines, maxLines, line("Soul: none", opaque(0xAAAAAA)));
            }
        }

        // Rivalry target display
        if (ClientRivalryState.hasRivalTarget()) {
            addLine(lines, maxLines, line("Rival: " + ClientRivalryState.getRivalTarget(), opaque(0xFF6600)));
        }

        String modifier = GemsKeybinds.modifierLabel();
        if (!modifier.isEmpty() && !compactMode) {
            addLine(lines, maxLines, line("Abilities: hold " + modifier + " + [1..]", opaque(0xAAAAAA)));
        }

        List<Identifier> abilities = orderedAbilities(gem);
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        int selectedSlot = ClientAbilitySelection.slotNumber(gem);

        if (gem == GemId.FLUX) {
            // Flux has a special key layout:
            // - Slot 1: ability 1
            // - Slot 2: Flux Charge (pseudo-ability, not part of unlock/cooldown)
            // - Slot 3+: remaining abilities
            for (int i = 0; i < abilities.size(); i++) {
                if (lines.size() >= maxLines) {
                    break;
                }
                Identifier id = abilities.get(i);
                GemAbility ability = ModAbilities.get(id);
                String name = ability != null ? ability.name() : id.toString();

                boolean isUnlocked = i < unlocked;
                int remaining = ClientCooldowns.remainingTicks(gem, id);
                int cooldownCostTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                int lastCooldown = ClientCooldowns.lastCooldownTicks(gem, id);
                if (lastCooldown > 0) {
                    cooldownCostTicks = lastCooldown;
                }

                int slotNumber = i == 0 ? 1 : (i + 2);
                String key = GemsKeybinds.chordSlotLabel(slotNumber);
                String stateSuffix;
                int stateColor;
                if (!isUnlocked) {
                    stateSuffix = " (locked)";
                    stateColor = opaque(0x777777);
                } else if (remaining > 0) {
                    stateSuffix = showCooldowns ? " (" + seconds(remaining) + "s)" : "";
                    stateColor = opaque(0xFFCC33);
                } else {
                    stateSuffix = "";
                    stateColor = opaque(0xFFFFFF);
                }

                boolean selected = selectedSlot == slotNumber;
                boolean lastUsed = ClientCooldowns.isLastUsed(gem, id);
                String prefix = selected ? "> " : (lastUsed && remaining > 0 ? "* " : "");
                int lineColor = stateColor;
                if (selected && isUnlocked) {
                    lineColor = remaining > 0 ? opaque(0xFF5555) : opaque(0x55FF55);
                } else if (lastUsed && remaining > 0 && isUnlocked) {
                    lineColor = opaque(0x55FFFF); // Cyan for last used on cooldown
                }

                String base = prefix + key + " " + name;
                java.util.List<HudSegment> segments = new java.util.ArrayList<>();
                segments.add(new HudSegment(base, lineColor));
                if (showCooldowns && cooldownCostTicks > 0) {
                    segments.add(new HudSegment(" [" + seconds(cooldownCostTicks) + "s]", abilityAccentColor(id)));
                }
                if (!stateSuffix.isEmpty()) {
                    int suffixColor = remaining > 0 && showCooldowns ? abilityAccentColor(id) : stateColor;
                    segments.add(new HudSegment(stateSuffix, suffixColor));
                }
                addLine(lines, maxLines, new HudLine(segments));

                if (i == 0) {
                    boolean selectedCharge = selectedSlot == 2;
                    addLine(lines, maxLines, line(
                            (selectedCharge ? "> " : "") + GemsKeybinds.chordSlotLabel(2) + " Flux Charge",
                            selectedCharge ? opaque(0x55FF55) : opaque(0x55FFFF)
                    ));
                }
            }
        } else if (gem == GemId.CHAOS) {
            // Chaos has independent ability slots
            for (int i = 0; i < ClientChaosState.slotCount(); i++) {
                if (lines.size() >= maxLines) {
                    break;
                }
                ClientChaosState.SlotState slot = ClientChaosState.getSlot(i);
                String key = GemsKeybinds.chordSlotLabel(i + 1);
                boolean selected = selectedSlot == (i + 1);
                
                if (slot.isActive()) {
                    String abilityName = slot.abilityName();
                    int remaining = slot.remainingSeconds();
                    int cooldown = slot.cooldownSeconds();
                    
                    String prefix = selected ? "> " : "";
                    int lineColor;
                    String stateSuffix;
                    
                    if (cooldown > 0) {
                        stateSuffix = showCooldowns ? " (CD " + cooldown + "s)" : "";
                        lineColor = selected ? opaque(0xFF5555) : opaque(0xFFCC33);
                    } else {
                        int mins = remaining / 60;
                        int secs = remaining % 60;
                        stateSuffix = showCooldowns ? " [" + mins + ":" + String.format("%02d", secs) + "]" : "";
                        lineColor = selected ? opaque(0x55FF55) : opaque(0xFFFFFF);
                    }
                    
                    // Line 1: Ability
                    String base = prefix + key + " " + abilityName;
                    addLine(lines, maxLines, line(base + stateSuffix, lineColor));
                } else {
                    // Inactive slot - can be activated
                    String prefix = selected ? "> " : "";
                    addLine(lines, maxLines, line(prefix + key + " [Press to roll]", selected ? opaque(0xAA55FF) : opaque(0x777777)));
                }
            }
        } else if (gem == GemId.PRISM) {
            // Prism uses selected abilities from player's choices
            List<ClientPrismState.PrismAbilityEntry> prismAbilities = ClientPrismState.getAbilities();
            if (prismAbilities.isEmpty()) {
                addLine(lines, maxLines, line("No abilities selected", opaque(0x777777)));
                if (!compactMode) {
                    addLine(lines, maxLines, line("Press " + GemsKeybinds.bonusScreenLabel() + " to open selection", opaque(0xAAAAAA)));
                }
            } else {
                int prismUnlocked = new GemEnergyState(energy).unlockedAbilityCount(prismAbilities.size());
                for (int i = 0; i < prismAbilities.size(); i++) {
                    if (lines.size() >= maxLines) {
                        break;
                    }
                    ClientPrismState.PrismAbilityEntry entry = prismAbilities.get(i);
                    String name = entry.name();
                    Identifier id = entry.id();
                    
                    boolean isUnlocked = i < prismUnlocked;
                    int remaining = ClientPrismState.remainingTicks(id);
                    GemAbility ability = ModAbilities.get(id);
                    int cooldownCostTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                    int lastCooldown = ClientPrismState.lastCooldownTicks(id);
                    if (lastCooldown > 0) {
                        cooldownCostTicks = lastCooldown;
                    }
                    
                    String key = GemsKeybinds.chordSlotLabel(i + 1);
                    String stateSuffix;
                    int stateColor;
                    if (!isUnlocked) {
                        stateSuffix = " (locked)";
                        stateColor = opaque(0x777777);
                    } else if (remaining > 0) {
                        stateSuffix = showCooldowns ? " (" + seconds(remaining) + "s)" : "";
                        stateColor = opaque(0xFFCC33);
                    } else {
                        stateSuffix = "";
                        stateColor = opaque(0xFFFFFF);
                    }
                    
                    boolean selected = selectedSlot == (i + 1);
                    boolean lastUsed = ClientPrismState.isLastUsed(id);
                    String prefix = selected ? "> " : (lastUsed && remaining > 0 ? "* " : "");
                    int lineColor = stateColor;
                    if (selected && isUnlocked) {
                        lineColor = remaining > 0 ? opaque(0xFF5555) : opaque(0x55FF55);
                    } else if (lastUsed && remaining > 0 && isUnlocked) {
                        lineColor = opaque(0x55FFFF);
                    }
                    
                    String base = prefix + key + " " + name;
                    java.util.List<HudSegment> segments = new java.util.ArrayList<>();
                    segments.add(new HudSegment(base, lineColor));
                    if (showCooldowns && cooldownCostTicks > 0) {
                        segments.add(new HudSegment(" [" + seconds(cooldownCostTicks) + "s]", opaque(0xAA55FF)));
                    }
                    if (!stateSuffix.isEmpty()) {
                        int suffixColor = remaining > 0 && showCooldowns ? opaque(0xAA55FF) : stateColor;
                        segments.add(new HudSegment(stateSuffix, suffixColor));
                    }
                    addLine(lines, maxLines, new HudLine(segments));
                }
            }
        } else {
            for (int i = 0; i < abilities.size(); i++) {
                if (lines.size() >= maxLines) {
                    break;
                }
                Identifier id = abilities.get(i);
                GemAbility ability = ModAbilities.get(id);
                String name = ability != null ? ability.name() : id.toString();

                boolean isUnlocked = i < unlocked;
                int remaining = ClientCooldowns.remainingTicks(gem, id);
                int cooldownCostTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                int lastCooldown = ClientCooldowns.lastCooldownTicks(gem, id);
                if (lastCooldown > 0) {
                    cooldownCostTicks = lastCooldown;
                }

                String key = GemsKeybinds.chordSlotLabel(i + 1);
                String stateSuffix;
                int stateColor;
                if (!isUnlocked) {
                    stateSuffix = " (locked)";
                    stateColor = opaque(0x777777);
                } else if (remaining > 0) {
                    stateSuffix = showCooldowns ? " (" + seconds(remaining) + "s)" : "";
                    stateColor = opaque(0xFFCC33);
                } else {
                    stateSuffix = "";
                    stateColor = opaque(0xFFFFFF);
                }

                boolean selected = selectedSlot == (i + 1);
                boolean lastUsed = ClientCooldowns.isLastUsed(gem, id);
                String prefix = selected ? "> " : (lastUsed && remaining > 0 ? "* " : "");
                int lineColor = stateColor;
                if (selected && isUnlocked) {
                    lineColor = remaining > 0 ? opaque(0xFF5555) : opaque(0x55FF55);
                } else if (lastUsed && remaining > 0 && isUnlocked) {
                    lineColor = opaque(0x55FFFF); // Cyan for last used on cooldown
                }

                String base = prefix + key + " " + name;
                java.util.List<HudSegment> segments = new java.util.ArrayList<>();
                segments.add(new HudSegment(base, lineColor));
                if (showCooldowns && cooldownCostTicks > 0) {
                    segments.add(new HudSegment(" [" + seconds(cooldownCostTicks) + "s]", abilityAccentColor(id)));
                }
                if (!stateSuffix.isEmpty()) {
                    int suffixColor = remaining > 0 && showCooldowns ? abilityAccentColor(id) : stateColor;
                    segments.add(new HudSegment(stateSuffix, suffixColor));
                }
                addLine(lines, maxLines, new HudLine(segments));
            }
        }

        if (gem == GemId.ASTRA) {
            int soulSlot = abilities.size() + 1;
            if (soulSlot <= 10) {
                boolean selected = selectedSlot == soulSlot;
                addLine(lines, maxLines, line(
                        (selected ? "> " : "") + GemsKeybinds.chordSlotLabel(soulSlot) + " Soul Release",
                        selected ? opaque(0x55FF55) : opaque(0x55FFFF)
                ));
            }
        }
        if (gem == GemId.SUMMONER) {
            int customizeSlot = abilities.size() + 1;
            if (customizeSlot <= 10) {
                boolean selected = selectedSlot == customizeSlot;
                addLine(lines, maxLines, line(
                        (selected ? "> " : "") + GemsKeybinds.chordSlotLabel(customizeSlot) + " Customize",
                        selected ? opaque(0x55FF55) : opaque(0x55FFFF)
                ));
            }
        }

        // Bonus abilities (only shown at energy 10)
        if (energy >= 10) {
            List<ClientBonusState.BonusAbilityEntry> bonusAbilities = ClientBonusState.getAbilities();
            if (!bonusAbilities.isEmpty()) {
                if (!compactMode) {
                    addLine(lines, maxLines, line("--- Bonus ---", opaque(0xFFD700)));
                }
                for (int i = 0; i < bonusAbilities.size() && i < 2; i++) {
                    if (lines.size() >= maxLines) {
                        break;
                    }
                    ClientBonusState.BonusAbilityEntry entry = bonusAbilities.get(i);
                    int remaining = ClientBonusState.remainingTicks(entry.id());
                    boolean lastUsed = ClientBonusState.isLastUsed(entry.id());
                    GemAbility ability = ModAbilities.get(entry.id());
                    int cooldownCostTicks = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
                    int lastCooldown = ClientBonusState.lastCooldownTicks(entry.id());
                    if (lastCooldown > 0) {
                        cooldownCostTicks = lastCooldown;
                    }

                    String key = GemsKeybinds.bonusAbilityLabel(i);
                    String prefix = lastUsed && remaining > 0 ? "* " : "";
                    int lineColor;
                    String stateSuffix;
                    
                    if (remaining > 0) {
                        stateSuffix = showCooldowns ? " (" + seconds(remaining) + "s)" : "";
                        lineColor = lastUsed ? opaque(0x55FFFF) : opaque(0xFFCC33);
                    } else {
                        stateSuffix = "";
                        lineColor = opaque(0xFFD700);
                    }
                    
                    String base = prefix + key + " " + entry.name();
                    java.util.List<HudSegment> segments = new java.util.ArrayList<>();
                    segments.add(new HudSegment(base, lineColor));
                    if (showCooldowns && cooldownCostTicks > 0) {
                        segments.add(new HudSegment(" [" + seconds(cooldownCostTicks) + "s]", abilityAccentColor(entry.id())));
                    }
                    if (!stateSuffix.isEmpty()) {
                        int suffixColor = remaining > 0 && showCooldowns ? abilityAccentColor(entry.id()) : lineColor;
                        segments.add(new HudSegment(stateSuffix, suffixColor));
                    }
                    addLine(lines, maxLines, new HudLine(segments));
                }
            }
        }

        drawLines(ctx, tr, lines, layout.position(), margin, lineHeight, screenWidth, screenHeight, tierColor(tier));
    }

    private static void renderCompact(
            GuiGraphicsExtractor ctx, Font font, GemLoadout.HudLayout layout,
            int margin, int screenWidth, int screenHeight,
            GemId gem, int energy, GemEnergyTier tier, int maxLines
    ) {
        List<HudLine> lines = new java.util.ArrayList<>(4);
        String heading = title(gem.name());
        addLine(lines, maxLines, line(heading, tierColor(tier)));

        List<Identifier> abilities = orderedAbilities(gem);
        int selected = ClientAbilitySelection.slotNumber(gem);
        if (gem == GemId.CHAOS) {
            int slotNumber = selected >= 1 && selected <= ClientChaosState.slotCount() ? selected : 1;
            ClientChaosState.SlotState slot = ClientChaosState.getSlot(slotNumber - 1);
            String label = GemsKeybinds.chordSlotLabel(slotNumber);
            if (!slot.isActive()) {
                addLine(lines, maxLines, line(label + "  Roll a power", READY));
            } else if (slot.cooldownSeconds() > 0) {
                addLine(lines, maxLines, compactAbilityLine(font, label, slot.abilityName(),
                        layout.showCooldowns() ? slot.cooldownSeconds() + "s" : "Recharging",
                        screenWidth, margin, WAITING));
            } else {
                addLine(lines, maxLines, compactAbilityLine(font, label, slot.abilityName(),
                        "Ready", screenWidth, margin, READY));
            }
        } else if (gem == GemId.FLUX && selected == 2) {
            addLine(lines, maxLines, line(GemsKeybinds.chordSlotLabel(2) + "  Add charge item", READY));
        } else if (gem == GemId.ASTRA && selected == abilities.size() + 1 && selected <= 10) {
            addLine(lines, maxLines, line(GemsKeybinds.chordSlotLabel(selected) + "  Soul Release  "
                    + (ClientExtraState.hasSoul() ? "Ready" : "No soul"),
                    ClientExtraState.hasSoul() ? READY : MUTED));
        } else if (gem == GemId.SUMMONER && selected == abilities.size() + 1 && selected <= 10) {
            addLine(lines, maxLines, line(GemsKeybinds.chordSlotLabel(selected) + "  Customize summons", READY));
        } else if (gem == GemId.PRISM && ClientPrismState.getAbilities().isEmpty()) {
            addLine(lines, maxLines, line("Choose powers with " + GemsKeybinds.bonusScreenLabel(), MUTED));
        } else {
            List<ClientPrismState.PrismAbilityEntry> prism = gem == GemId.PRISM
                    ? ClientPrismState.getAbilities() : List.of();
            int abilityCount = gem == GemId.PRISM ? prism.size() : abilities.size();
            int available = new GemEnergyState(energy).unlockedAbilityCount(abilityCount);
            if (available == 0) {
                addLine(lines, maxLines, line("Powers unlock at 2 energy", MUTED));
            } else {
                int slot = selected >= 1 && selected <= (gem == GemId.FLUX ? abilityCount + 1 : abilityCount)
                        ? selected : 1;
                int index = gem == GemId.FLUX && slot >= 3 ? slot - 2 : slot - 1;
                Identifier id = gem == GemId.PRISM ? prism.get(index).id() : abilities.get(index);
                String name = gem == GemId.PRISM ? prism.get(index).name() : abilityName(id);
                int remaining = gem == GemId.PRISM
                        ? ClientPrismState.remainingTicks(id) : ClientCooldowns.remainingTicks(gem, id);
                boolean locked = index >= available;
                String status = locked ? "Locked" : remaining > 0
                        ? (layout.showCooldowns() ? seconds(remaining) + "s" : "Recharging") : "Ready";
                addLine(lines, maxLines, compactAbilityLine(font, GemsKeybinds.chordSlotLabel(slot),
                        name, status, screenWidth, margin,
                        locked ? MUTED : remaining > 0 ? WAITING : READY));
            }
        }

        if (gem == GemId.FLUX) {
            int charge = ClientExtraState.fluxChargePercent();
            addLine(lines, maxLines, line("Charge " + charge + "%", charge >= 100 ? READY : MUTED));
        } else if (gem == GemId.ASTRA) {
            addLine(lines, maxLines, line(ClientExtraState.hasSoul()
                    ? "Soul held: " + prettyEntityId(ClientExtraState.soulTypeId()) : "No soul held", MUTED));
        }
        if (ClientRivalryState.hasRivalTarget()) {
            addLine(lines, maxLines, line("Rival: " + ClientRivalryState.getRivalTarget(), WAITING));
        }
        drawCompact(ctx, font, lines, layout.position(), margin,
                screenWidth, screenHeight, tier, energy, layout.showEnergy());
    }

    private static void drawCompact(GuiGraphicsExtractor ctx, Font font, List<HudLine> lines,
                                    GemLoadout.HudPosition position, int margin,
                                    int screenWidth, int screenHeight,
                                    GemEnergyTier tier, int energy, boolean showEnergy) {
        boolean right = position == GemLoadout.HudPosition.TOP_RIGHT || position == GemLoadout.HudPosition.BOTTOM_RIGHT;
        boolean bottom = position == GemLoadout.HudPosition.BOTTOM_LEFT || position == GemLoadout.HudPosition.BOTTOM_RIGHT;
        int panelWidth = Math.min(COMPACT_WIDTH, screenWidth - margin * 2);
        int rowCount = Math.max(0, lines.size() - 1);
        int headerHeight = showEnergy ? 23 : 17;
        int panelHeight = headerHeight + rowCount * 15 + 2;
        int x = right ? screenWidth - margin - panelWidth : margin;
        // Leave the vanilla hearts, food, and hotbar clear when the HUD is placed at the bottom.
        int y = bottom ? Math.max(margin, screenHeight - 55 - panelHeight) : margin;
        ctx.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        ctx.fill(x, y, x + 2, y + panelHeight, tierColor(tier));
        ctx.fill(x + 2, y, x + panelWidth, y + 1, GemsUiTheme.RULE);
        String name = lines.isEmpty() ? "Gem" : lines.getFirst().segments().getFirst().text();
        String tierText = showEnergy ? (panelWidth >= 170 ? tierLabel(tier) + " " : "") + energy + "/10" : "";
        int titleWidth = panelWidth - 16 - (showEnergy ? font.width(tierText) + 5 : 0);
        ctx.text(font, font.plainSubstrByWidth(name, Math.max(20, titleWidth)), x + 7, y + 4,
                tierColor(tier));
        if (showEnergy) {
            ctx.text(font, tierText, x + panelWidth - 6 - font.width(tierText), y + 4, GemsUiTheme.PALE);
            GemsUiTheme.segments(ctx, x + 7, y + 17, 10, energy, tierColor(tier));
        }
        ctx.fill(x + 6, y + headerHeight - 1, x + panelWidth - 6, y + headerHeight, GemsUiTheme.RULE);
        for (int i = 1; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            String content = line.segments().getFirst().text();
            int color = line.segments().getFirst().color();
            String state = "";
            if (content.endsWith(" Ready")) state = "READY";
            else if (content.endsWith(" Locked")) state = "LOCKED";
            else if (content.endsWith(" Recharging")) state = "WAIT";
            else {
                int lastSpace = content.lastIndexOf(' ');
                if (lastSpace > 0 && content.endsWith("s")
                        && Character.isDigit(content.charAt(content.length() - 2))) {
                    state = content.substring(lastSpace + 1);
                }
            }
            if (!state.isEmpty()) content = content.substring(0, content.lastIndexOf(' '));
            int rowY = y + headerHeight + (i - 1) * 15;
            int stateWidth = state.isEmpty() ? 0 : font.width(state) + 7;
            ctx.text(font, font.plainSubstrByWidth(content, Math.max(20, panelWidth - 16 - stateWidth)),
                    x + 7, rowY + 3, GemsUiTheme.PALE);
            if (!state.isEmpty()) {
                ctx.text(font, state, x + panelWidth - 7 - font.width(state), rowY + 3, color);
            }
        }
    }

    private static String abilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability != null ? ability.name() : id.getPath().replace('_', ' ');
    }

    private static HudLine compactAbilityLine(Font font, String key, String name, String status,
                                               int screenWidth, int margin, int color) {
        int availableWidth = Math.max(70, Math.min(COMPACT_WIDTH, screenWidth - margin * 2) - 16);
        String fittedName = name;
        String value = key + "  " + fittedName + "  " + status;
        if (font.width(value) > availableWidth) {
            while (fittedName.length() > 1
                    && font.width(key + "  " + fittedName + "...  " + status) > availableWidth) {
                fittedName = fittedName.substring(0, fittedName.length() - 1);
            }
            value = key + "  " + fittedName + "...  " + status;
        }
        return line(value, color);
    }

    private static int seconds(int remainingTicks) {
        return Math.max(1, (remainingTicks + 19) / 20);
    }

    private static List<Identifier> orderedAbilities(GemId gem) {
        List<Identifier> order = ClientAbilityOrder.getOrder(gem);
        if (!order.isEmpty()) {
            return order;
        }
        GemDefinition def = GemRegistry.definition(gem);
        return def.abilities();
    }

    private static boolean addLine(List<HudLine> lines, int maxLines, HudLine line) {
        if (lines.size() >= maxLines) {
            return false;
        }
        lines.add(line);
        return true;
    }

    private static HudLine line(String text, int color) {
        return new HudLine(java.util.List.of(new HudSegment(text, color)));
    }

    private static void drawLines(
            GuiGraphicsExtractor ctx,
            Font tr,
            List<HudLine> lines,
            GemLoadout.HudPosition position,
            int margin,
            int lineHeight,
            int screenWidth,
            int screenHeight,
            int accent
    ) {
        boolean right = position == GemLoadout.HudPosition.TOP_RIGHT || position == GemLoadout.HudPosition.BOTTOM_RIGHT;
        boolean bottom = position == GemLoadout.HudPosition.BOTTOM_LEFT || position == GemLoadout.HudPosition.BOTTOM_RIGHT;
        int totalHeight = lines.size() * lineHeight;
        int startY = bottom ? (screenHeight - margin - totalHeight) : margin;
        if (startY < margin) {
            startY = margin;
        }
        int panelWidth = 0;
        for (HudLine line : lines) {
            int lineWidth = 0;
            for (HudSegment segment : line.segments()) {
                lineWidth += tr.width(segment.text());
            }
            panelWidth = Math.max(panelWidth, lineWidth);
        }
        panelWidth = Math.min(panelWidth + 15, screenWidth - margin * 2);
        int panelX = right ? screenWidth - margin - panelWidth : margin;
        int panelY = Math.max(2, startY - 4);
        int panelBottom = Math.min(screenHeight - 2, startY + totalHeight + 3);
        ctx.fill(panelX, panelY, panelX + panelWidth, panelBottom, PANEL);
        ctx.fill(right ? panelX + panelWidth - 2 : panelX, panelY,
                right ? panelX + panelWidth : panelX + 2, panelBottom, accent);
        int y = startY;
        for (HudLine line : lines) {
            int width = 0;
            for (HudSegment segment : line.segments()) {
                width += tr.width(segment.text());
            }
            int x = right ? (panelX + panelWidth - 7 - width) : panelX + 7;
            for (HudSegment segment : line.segments()) {
                ctx.text(tr, segment.text(), x, y, segment.color());
                x += tr.width(segment.text());
            }
            y += lineHeight;
        }
    }

    private static int tierColor(GemEnergyTier tier) {
        return switch (tier) {
            case BROKEN -> opaque(0xFF4444);
            case COMMON -> opaque(0xAAAAAA);
            case RARE -> opaque(0x55AAFF);
            case ELITE -> opaque(0x55FF55);
            case MYTHICAL -> opaque(0xCC66FF);
            case LEGENDARY, LEGENDARY_PLUS_1, LEGENDARY_PLUS_2, LEGENDARY_PLUS_3, LEGENDARY_PLUS_4, LEGENDARY_PLUS_5 -> opaque(0xFFD700);
        };
    }

    private static String tierLabel(GemEnergyTier tier) {
        return switch (tier) {
            case BROKEN -> "Broken";
            case COMMON -> "Common";
            case RARE -> "Rare";
            case ELITE -> "Elite";
            case MYTHICAL -> "Mythical";
            case LEGENDARY -> "Legendary";
            case LEGENDARY_PLUS_1 -> "Legendary +1";
            case LEGENDARY_PLUS_2 -> "Legendary +2";
            case LEGENDARY_PLUS_3 -> "Legendary +3";
            case LEGENDARY_PLUS_4 -> "Legendary +4";
            case LEGENDARY_PLUS_5 -> "Legendary +5";
        };
    }

    private static String title(String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        String lower = raw.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String prettyEntityId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "?";
        }
        int idx = raw.indexOf(':');
        String path = idx >= 0 ? raw.substring(idx + 1) : raw;
        return path.replace('_', ' ');
    }

    private static int abilityAccentColor(Identifier id) {
        int hash = id.toString().hashCode();
        float hue = (hash & 0xFFFF) / (float) 0x10000; // [0,1)
        int rgb = hsvToRgb(hue, 0.75f, 1.0f);
        return 0xFF000000 | rgb;
    }

    private static int hsvToRgb(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6.0f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));

        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ri = Math.max(0, Math.min(255, (int) (r * 255.0f)));
        int gi = Math.max(0, Math.min(255, (int) (g * 255.0f)));
        int bi = Math.max(0, Math.min(255, (int) (b * 255.0f)));
        return (ri << 16) | (gi << 8) | bi;
    }

    private record HudSegment(String text, int color) {
    }

    private record HudLine(List<HudSegment> segments) {
    }
}
