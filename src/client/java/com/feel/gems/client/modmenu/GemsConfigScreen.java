package com.feel.gems.client.modmenu;

import com.feel.gems.client.GemsClientConfig;
import com.feel.gems.client.GemsClientConfigManager;
import com.feel.gems.client.ClientGemState;
import com.feel.gems.client.screen.GemsUiTheme;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsBalanceConfig;
import com.feel.gems.config.GemsConfigManager;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.config.GemsDisablesConfig;
import com.feel.gems.config.GemsDisablesConfigManager;
import com.feel.gems.net.ClientPassiveTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class GemsConfigScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int MARGIN = 18;
    private static final int FOOTER_GAP = 6;
    private static final int FOOTER_ROW_H = 24;
    private static final int STATUS_RESERVE = 22;

    private final Screen parent;

    private GemsBalanceConfig cfg;
    private GemsBalanceConfig loadedCfg;
    private String loadError = null;
    private boolean localWorld = false;
    private boolean canEdit = false;
    private GemsClientConfig clientCfg;
    private boolean clientDirty = false;
    private boolean canSaveClient = true;
    private String clientError = null;
    private GemsDisablesConfig disablesCfg;
    private GemsDisablesConfig loadedDisablesCfg;
    private String disablesLoadError = null;
    private boolean disablesDirty = false;

    private Category category = Category.GENERAL;
    private Section section = Section.VISUAL;
    private boolean dirty = false;
    private final ValidationTracker validation = new ValidationTracker();

    private Button saveButton;
    private Button saveReloadButton;
    private boolean advancedActions = false;
    private String toolStatus;
    private boolean toolSuccess;

    private int scrollPx = 0;
    private int maxScrollPx = 0;

    private int sidebarScrollPx = 0;
    private int sidebarMaxScrollPx = 0;
    private int sidebarX = 0;
    private int sidebarY = 0;
    private int sidebarW = 0;
    private int sidebarViewH = 0;
    private int fieldsTop = 0;
    private int contentEndY = 0;
    private int footerTop = 0;
    private boolean stackedRows = false;
    private final List<LabelSpec> labels = new ArrayList<>();

    private record LabelSpec(String label, String range, int x, int y, int width) {}

    private boolean compactLayout() {
        return width < 430;
    }

    private boolean showCurrentGemShortcut() {
        return localWorld && width >= 320 && currentGemSection() != null;
    }

    private static Section currentGemSection() {
        if (!ClientGemState.isInitialized()) return null;
        for (Section candidate : Section.values()) {
            if (candidate.name().equals(ClientGemState.activeGem().name())) return candidate;
        }
        return null;
    }

    private int categoryTabWidth() {
        int tabs = Category.values().length;
        int reserved = showCurrentGemShortcut() ? 72 + 4 : 0;
        return Math.max(40, Math.min(80,
                (width - MARGIN * 2 - (tabs - 1) * 4 - reserved) / tabs));
    }

    public GemsConfigScreen(Screen parent) {
        super(Component.literal("Gems Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        load();
        rebuild();
    }

    private void load() {
        boolean editableWorld = canEditConfig();
        this.localWorld = editableWorld;
        var result = editableWorld ? GemsConfigManager.loadOrCreateForUi() : null;
        this.cfg = normalize(result != null && result.config() != null
                ? result.config() : new GemsBalanceConfig());
        this.loadedCfg = GemsConfigManager.copyForUi(this.cfg);
        this.loadError = result == null ? null : result.error();
        this.dirty = false;
        this.scrollPx = 0;
        this.sidebarScrollPx = 0;
        this.advancedActions = false;
        this.toolStatus = null;
        this.clientCfg = GemsClientConfigManager.copyForUi(GemsClientConfigManager.loadOrCreate());
        this.clientDirty = false;
        this.clientError = GemsClientConfigManager.loadError();
        this.canSaveClient = clientError == null;
        var disablesResult = editableWorld ? GemsDisablesConfigManager.loadOrCreateForUi() : null;
        this.disablesCfg = disablesResult != null && disablesResult.config() != null
                ? disablesResult.config() : new GemsDisablesConfig();
        this.loadedDisablesCfg = GemsDisablesConfigManager.copyForUi(this.disablesCfg);
        this.disablesLoadError = disablesResult == null ? null : disablesResult.error();
        this.disablesDirty = false;
        this.canEdit = editableWorld && loadError == null && disablesLoadError == null;
        if (!editableWorld) {
            this.category = Category.GENERAL;
            this.section = Section.CLIENT;
        }
        this.validation.reset();
    }

    private void rebuild() {
        clearWidgets();
        labels.clear();

        // Category tabs at top
        int tabX = MARGIN;
        int tabY = 26;
        int tabW = categoryTabWidth();
        int tabH = 18;
        int tabGap = 4;

        for (int i = 0; i < Category.values().length; i++) {
            Category cat = Category.values()[i];
            if (!localWorld && cat != Category.GENERAL) continue;
            int x = tabX + i * (tabW + tabGap);
            Button tabBtn = addRenderableWidget(Button.builder(Component.literal(cat.label), btn -> {
                category = cat;
                // Switch to first section in this category
                Section[] catSections = visibleSections(cat);
                if (catSections.length > 0) {
                    section = catSections[0];
                }
                sidebarScrollPx = 0;
                scrollPx = 0;
                rebuild();
            }).bounds(x, tabY, tabW, tabH).build());
            tabBtn.active = cat != category;
        }
        if (showCurrentGemShortcut()) {
            Section target = currentGemSection();
            Button shortcut = addRenderableWidget(Button.builder(Component.literal("My gem"), btn -> {
                category = target.category;
                Section[] siblings = Section.forCategory(category);
                sidebarScrollPx = java.util.Arrays.asList(siblings).indexOf(target) * 24;
                selectSection(target);
            }).bounds(tabX + Category.values().length * (tabW + tabGap), tabY, 72, tabH).build());
            shortcut.setTooltip(Tooltip.create(Component.literal("Open "
                    + target.label + " Gem world settings")));
        }

        boolean compact = compactLayout();
        int sidebarX = MARGIN;
        int sidebarY = compact ? 65 : 74;
        int sidebarW = compact ? width - MARGIN * 2 : this.width <= 500 ? 96 : 130;
        int sidebarH = 20;
        int sidebarGap = 4;

        // Three buttons still fit at the common 427px scaled client width.
        // Keeping the primary actions on one row leaves room for two settings.
        int footerCols = this.width < 380 ? 2 : 3;
        int footerRows = advancedActions ? (footerCols == 2 ? 4 : 3) : (footerCols == 2 ? 2 : 1);
        int footerTop = this.height - footerRows * FOOTER_ROW_H - 8;
        this.footerTop = footerTop;
        int sidebarViewH = Math.max(0, footerTop - sidebarY - 6);

        // Only show sections for current category
        Section[] categorySections = visibleSections(category);
        int sidebarContentH = categorySections.length * (sidebarH + sidebarGap) - sidebarGap;
        sidebarMaxScrollPx = compact ? 0 : Math.max(0, sidebarContentH - sidebarViewH);
        int prevSidebarScroll = sidebarScrollPx;
        sidebarScrollPx = clampInt(sidebarScrollPx, 0, sidebarMaxScrollPx);
        if (prevSidebarScroll != sidebarScrollPx) {
            rebuild();
            return;
        }
        this.sidebarX = sidebarX;
        this.sidebarY = sidebarY;
        this.sidebarW = sidebarW;
        this.sidebarViewH = compact ? sidebarH : sidebarViewH;

        if (compact) {
            int current = java.util.Arrays.asList(categorySections).indexOf(section);
            if (current < 0) current = 0;
            int index = current;
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                selectSection(categorySections[index - 1]);
            }).bounds(sidebarX, sidebarY, 24, sidebarH).build());
            previous.active = current > 0;
            Button selected = addRenderableWidget(Button.builder(Component.literal(section.label), btn -> {
                selectSection(categorySections[(index + 1) % categorySections.length]);
            }).bounds(sidebarX + 28, sidebarY, sidebarW - 56, sidebarH).build());
            selected.setTooltip(Tooltip.create(Component.literal("Next section in " + category.label)));
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                selectSection(categorySections[index + 1]);
            }).bounds(sidebarX + sidebarW - 24, sidebarY, 24, sidebarH).build());
            next.active = current + 1 < categorySections.length;
        } else {
            for (int i = 0; i < categorySections.length; i++) {
                Section s = categorySections[i];
                int y = sidebarY + i * (sidebarH + sidebarGap) - sidebarScrollPx;
                if (y < sidebarY || y + sidebarH > sidebarY + sidebarViewH) {
                    continue;
                }
                Button b = addRenderableWidget(Button.builder(Component.literal(s.label), btn -> selectSection(s))
                        .bounds(sidebarX, y, sidebarW, sidebarH).build());
                b.active = s != section;
            }
        }

        int contentX = compact ? MARGIN : sidebarX + sidebarW + (this.width <= 500 ? 12 : 20);
        int fieldsTop = compact ? 98 : 74;
        this.fieldsTop = fieldsTop;

        int labelX = contentX;
        int available = Math.max(80, this.width - contentX - MARGIN);
        int fieldW = Math.max(80, Math.min(150, available / 3));
        int labelW = Math.max(80, Math.min(250, available - fieldW - 8));
        int fieldX = contentX + labelW + 8;
        stackedRows = compact;
        if (stackedRows) {
            labelW = available;
            fieldX = contentX;
            fieldW = available;
        }

        int y = fieldsTop - scrollPx;
        int logicalY = fieldsTop;

        validation.reset();

        switch (section) {
            case CLIENT -> {
                y = addClientBoolRow("Enable gem passives", y, labelX, labelW, fieldX, fieldW, () -> clientCfg.passivesEnabled, v -> clientCfg.passivesEnabled = v);
                y = addClientControlModeRow("Control mode", y, labelX, labelW, fieldX, fieldW, () -> clientCfg.controlMode, v -> clientCfg.controlMode = v);
            }
            case VISUAL -> {
                y = addBoolRow("Particles", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.enableParticles, v -> cfg.visual.enableParticles = v);
                y = addBoolRow("Sounds", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.enableSounds, v -> cfg.visual.enableSounds = v);
                y = addIntRow("Particle amount (%)", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.particleScalePercent, v -> cfg.visual.particleScalePercent = v, 0, 200);
            }
            case SYSTEMS -> {
                y = addIntRow("Min max hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.minMaxHearts, v -> cfg.systems.minMaxHearts = v, 1, 20);
                y = addIntRow("Assassin trigger hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinTriggerHearts, v -> cfg.systems.assassinTriggerHearts = v, 1, 20);
                y = addStringRow("Mob blacklist (comma-separated ids)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(cfg.mobBlacklist),
                        v -> cfg.mobBlacklist = parseList(v));
            }
            case BONUS_POOL -> {
                y = addIntRow("Max bonus abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.maxBonusAbilities, v -> cfg.bonusPool.maxBonusAbilities = v, 0, 10);
                y = addIntRow("Max bonus passives", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.maxBonusPassives, v -> cfg.bonusPool.maxBonusPassives = v, 0, 10);
                y = addFloatRow("Ability cooldown multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bonusAbilityCooldownMultiplier, v -> cfg.bonusPool.bonusAbilityCooldownMultiplier = v, 0.1F, 10.0F);
                y = addFloatRow("Ability damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bonusAbilityDamageMultiplier, v -> cfg.bonusPool.bonusAbilityDamageMultiplier = v, 0.1F, 10.0F);
            }
            case ASTRA -> {
                y = addIntRow("Shadow Anchor window (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.shadowAnchorWindowSeconds, v -> cfg.astra.shadowAnchorWindowSeconds = v, 0, 60);
                y = addIntRow("Void cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.dimensionalVoidCooldownSeconds, v -> cfg.astra.dimensionalVoidCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Astral Daggers damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersDamage, v -> cfg.astra.astralDaggersDamage = v, 0.0F, 40.0F);
            }
            case FIRE -> {
                y = addIntRow("Campfire wait (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireCooldownSeconds, v -> cfg.fire.cosyCampfireCooldownSeconds = v, 0, 3600);
                y = addIntRow("Campfire reach (blocks)", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireRadiusBlocks, v -> cfg.fire.cosyCampfireRadiusBlocks = v, 0, 32);
                y = addIntRow("Fireball charge (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.fireballChargeUpSeconds, v -> cfg.fire.fireballChargeUpSeconds = v, 0, 20);
            }
            case FLUX -> {
                y = addIntRow("Flux Beam cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamCooldownSeconds, v -> cfg.flux.fluxBeamCooldownSeconds = v, 0, 3600);
                y = addIntRow("Flux Beam range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamRangeBlocks, v -> cfg.flux.fluxBeamRangeBlocks = v, 1, 256);
                y = addFloatRow("Flux Beam max damage at 100%", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamMaxDamageAt100, v -> cfg.flux.fluxBeamMaxDamageAt100 = v, 0.0F, 80.0F);
            }
            case LIFE -> {
                y = addIntRow("Vitality Vortex cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexCooldownSeconds, v -> cfg.life.vitalityVortexCooldownSeconds = v, 0, 3600);
                y = addIntRow("Vitality Vortex radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexRadiusBlocks, v -> cfg.life.vitalityVortexRadiusBlocks = v, 0, 32);
                y = addFloatRow("Vitality Vortex ally heal", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexAllyHeal, v -> cfg.life.vitalityVortexAllyHeal = v, 0.0F, 40.0F);
            }
            case PUFF -> {
                y = addIntRow("Double Jump cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.doubleJumpCooldownSeconds, v -> cfg.puff.doubleJumpCooldownSeconds = v, 0, 3600);
                y = addIntRow("Dash cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashCooldownSeconds, v -> cfg.puff.dashCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Dash damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashDamage, v -> cfg.puff.dashDamage = v, 0.0F, 40.0F);
            }
            case SPEED -> {
                y = addFloatRow("Momentum max multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.momentumMaxMultiplier, v -> cfg.speed.momentumMaxMultiplier = v, 0.0F, 3.0F);
                y = addIntRow("Arc Shot cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotCooldownSeconds, v -> cfg.speed.arcShotCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Arc Shot damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotDamage, v -> cfg.speed.arcShotDamage = v, 0.0F, 40.0F);
            }
            case STRENGTH -> {
                y = addIntRow("Nullify cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.nullifyCooldownSeconds, v -> cfg.strength.nullifyCooldownSeconds = v, 0, 3600);
                y = addIntRow("Nullify radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.nullifyRadiusBlocks, v -> cfg.strength.nullifyRadiusBlocks = v, 0, 32);
                y = addFloatRow("Chad Strength bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.chadBonusDamage, v -> cfg.strength.chadBonusDamage = v, 0.0F, 80.0F);
            }
            case WEALTH -> {
                y = addIntRow("Fumble cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.fumbleCooldownSeconds, v -> cfg.wealth.fumbleCooldownSeconds = v, 0, 3600);
                y = addIntRow("Item Lock cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.hotbarLockCooldownSeconds, v -> cfg.wealth.hotbarLockCooldownSeconds = v, 0, 3600);
                y = addIntRow("Rich Rush cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushCooldownSeconds, v -> cfg.wealth.richRushCooldownSeconds = v, 0, 24 * 3600);
            }
            case TERROR -> {
                y = addIntRow("Dread Aura radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.dreadAuraRadiusBlocks, v -> cfg.terror.dreadAuraRadiusBlocks = v, 0, 64);
                y = addIntRow("Terror Trade cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeCooldownSeconds, v -> cfg.terror.terrorTradeCooldownSeconds = v, 0, 3600);
                y = addIntRow("Terror Trade hearts cost", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeHeartsCost, v -> cfg.terror.terrorTradeHeartsCost = v, 0, 20);
            }
            case SUMMONER -> {
                y = addIntRow("Summon point cap", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.maxPoints, v -> cfg.summoner.maxPoints = v, 0, 200);
                y = addIntRow("Max active summons", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.maxActiveSummons, v -> cfg.summoner.maxActiveSummons = v, 0, 200);
                y = addIntRow("Summon cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSlotCooldownSeconds, v -> cfg.summoner.summonSlotCooldownSeconds = v, 0, 3600);
            }
            case SPACE -> {
                y = addIntRow("Orbital Laser cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserCooldownSeconds, v -> cfg.space.orbitalLaserCooldownSeconds = v, 0, 3600);
                y = addIntRow("Orbital Laser radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserRadiusBlocks, v -> cfg.space.orbitalLaserRadiusBlocks = v, 0, 32);
                y = addFloatRow("Orbital Laser damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserDamage, v -> cfg.space.orbitalLaserDamage = v, 0.0F, 80.0F);
            }
            case REAPER -> {
                y = addIntRow("Grave Steed cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.graveSteedCooldownSeconds, v -> cfg.reaper.graveSteedCooldownSeconds = v, 0, 3600);
                y = addIntRow("Withering Strikes cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.witheringStrikesCooldownSeconds, v -> cfg.reaper.witheringStrikesCooldownSeconds = v, 0, 3600);
                y = addIntRow("Death Oath cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathCooldownSeconds, v -> cfg.reaper.deathOathCooldownSeconds = v, 0, 3600);
            }
            case PILLAGER -> {
                y = addIntRow("Fangs cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsCooldownSeconds, v -> cfg.pillager.fangsCooldownSeconds = v, 0, 3600);
                y = addIntRow("Fangs range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsRangeBlocks, v -> cfg.pillager.fangsRangeBlocks = v, 0, 128);
                y = addIntRow("Ravage cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.ravageCooldownSeconds, v -> cfg.pillager.ravageCooldownSeconds = v, 0, 3600);
            }
            case SPY -> {
                y = addIntRow("Backstep cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstepCooldownSeconds, v -> cfg.spy.backstepCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Backstab bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstabBonusDamage, v -> cfg.spy.backstabBonusDamage = v, 0.0F, 40.0F);
                y = addIntRow("Max stolen abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.maxStolenAbilities, v -> cfg.spy.maxStolenAbilities = v, 1, 10);
            }
            case BEACON -> {
                y = addIntRow("Core radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.coreRadiusBlocks, v -> cfg.beacon.coreRadiusBlocks = v, 0, 64);
                y = addIntRow("Rally absorption hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.rallyAbsorptionHearts, v -> cfg.beacon.rallyAbsorptionHearts = v, 0, 40);
                y = addIntRow("Aura cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraCooldownSeconds, v -> cfg.beacon.auraCooldownSeconds = v, 0, 3600);
            }
            case VOID -> {
                y = addBoolRow("Block all status effects", y, labelX, labelW, fieldX, fieldW, () -> cfg.voidGem.blockAllStatusEffects, v -> cfg.voidGem.blockAllStatusEffects = v);
            }
            case CHAOS -> {
                y = addIntRow("Slot count", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotCount, v -> cfg.chaos.slotCount = v, 1, 9);
                y = addIntRow("Slot duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotDurationSeconds, v -> cfg.chaos.slotDurationSeconds = v, 1, 3600);
                y = addIntRow("Slot ability cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotAbilityCooldownSeconds, v -> cfg.chaos.slotAbilityCooldownSeconds = v, 0, 3600);
            }
            case PRISM -> {
                y = addIntRow("Max selected gem abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.prism.maxGemAbilities, v -> cfg.prism.maxGemAbilities = v, 0, 10);
                y = addIntRow("Max selected gem passives", y, labelX, labelW, fieldX, fieldW, () -> cfg.prism.maxGemPassives, v -> cfg.prism.maxGemPassives = v, 0, 10);
            }
            case AIR -> {
                y = addIntRow("Wind Jump cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windJumpCooldownSeconds, v -> cfg.air.windJumpCooldownSeconds = v, 0, 3600);
                y = addIntRow("Gale Slam cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamCooldownSeconds, v -> cfg.air.galeSlamCooldownSeconds = v, 0, 3600);
                y = addIntRow("Gale Slam radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamRadiusBlocks, v -> cfg.air.galeSlamRadiusBlocks = v, 0, 32);
            }
            case DUELIST -> {
                y = addIntRow("Lunge cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.lungeCooldownSeconds, v -> cfg.duelist.lungeCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Lunge damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.lungeDamage, v -> cfg.duelist.lungeDamage = v, 0.0F, 40.0F);
                y = addIntRow("Parry cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.parryCooldownSeconds, v -> cfg.duelist.parryCooldownSeconds = v, 0, 3600);
            }
            case HUNTER -> {
                y = addIntRow("Hunting Trap cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.huntingTrapCooldownSeconds, v -> cfg.hunter.huntingTrapCooldownSeconds = v, 0, 3600);
                y = addIntRow("Pounce cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.pounceCooldownSeconds, v -> cfg.hunter.pounceCooldownSeconds = v, 0, 3600);
                y = addFloatRow("Pounce damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.pounceDamage, v -> cfg.hunter.pounceDamage = v, 0.0F, 200.0F);
            }
            case SENTINEL -> {
                y = addFloatRow("Guardian Aura damage reduction", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.guardianAuraDamageReduction, v -> cfg.sentinel.guardianAuraDamageReduction = v, 0.0F, 1.0F);
                y = addIntRow("Shield Wall cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.shieldWallCooldownSeconds, v -> cfg.sentinel.shieldWallCooldownSeconds = v, 0, 3600);
                y = addIntRow("Taunt cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.tauntCooldownSeconds, v -> cfg.sentinel.tauntCooldownSeconds = v, 0, 3600);
            }
            case TRICKSTER -> {
                y = addIntRow("Shadow Swap cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.shadowSwapCooldownSeconds, v -> cfg.trickster.shadowSwapCooldownSeconds = v, 0, 3600);
                y = addIntRow("Mirage cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mirageCooldownSeconds, v -> cfg.trickster.mirageCooldownSeconds = v, 0, 3600);
                y = addIntRow("Glitch Step cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.glitchStepCooldownSeconds, v -> cfg.trickster.glitchStepCooldownSeconds = v, 0, 3600);
            }
            case LEGENDARY -> {
                y = addIntRow("Legendary craft seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.craftSeconds, v -> cfg.legendary.craftSeconds = v, 0, 36000);
                y = addIntRow("Legendary craft max per item (0 = unlimited)", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.craftMaxPerItem, v -> cfg.legendary.craftMaxPerItem = v, 0, 100);
                y = addIntRow("Tracker refresh seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.trackerRefreshSeconds, v -> cfg.legendary.trackerRefreshSeconds = v, 0, 60);
                y = addIntRow("Recall cooldown (s)", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.recallCooldownSeconds, v -> cfg.legendary.recallCooldownSeconds = v, 0, 3600);
                y = addIntRow("Hypno max controlled", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoMaxControlled, v -> cfg.legendary.hypnoMaxControlled = v, 0, 50);
            }
            case MASTERY -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.mastery.enabled, v -> cfg.mastery.enabled = v);
                y = addBoolRow("Show aura particles", y, labelX, labelW, fieldX, fieldW, () -> cfg.mastery.showAuraParticles, v -> cfg.mastery.showAuraParticles = v);
            }
            case RIVALRY -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.enabled, v -> cfg.rivalry.enabled = v);
                y = addDoubleRow("Damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.damageMultiplier, v -> cfg.rivalry.damageMultiplier = v, 0.0D, 10.0D);
                y = addBoolRow("Show in HUD", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.showInHud, v -> cfg.rivalry.showInHud = v);
            }
            case LOADOUTS -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.enabled, v -> cfg.loadouts.enabled = v);
                y = addIntRow("Unlock energy", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.unlockEnergy, v -> cfg.loadouts.unlockEnergy = v, 0, 10);
                y = addIntRow("Max presets per gem", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.maxPresetsPerGem, v -> cfg.loadouts.maxPresetsPerGem = v, 0, 20);
            }
            case SYNERGIES -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.enabled, v -> cfg.synergies.enabled = v);
                y = addIntRow("Window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.windowSeconds, v -> cfg.synergies.windowSeconds = v, 0, 60);
                y = addIntRow("Cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.cooldownSeconds, v -> cfg.synergies.cooldownSeconds = v, 0, 3600);
            }
            case AUGMENTS -> {
                y = addIntRow("Gem max slots", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.gemMaxSlots, v -> cfg.augments.gemMaxSlots = v, 0, 16);
                y = addIntRow("Legendary max slots", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.legendaryMaxSlots, v -> cfg.augments.legendaryMaxSlots = v, 0, 16);
            }
            case DISABLES -> {
                y = addDisablesStringRow("Disabled gems (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledGems),
                        v -> disablesCfg.disabledGems = parseList(v));
                y = addDisablesStringRow("Disabled abilities (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledAbilities),
                        v -> disablesCfg.disabledAbilities = parseList(v));
                y = addDisablesStringRow("Disabled passives (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledPassives),
                        v -> disablesCfg.disabledPassives = parseList(v));
            }
        }

        logicalY = y + scrollPx;
        contentEndY = logicalY;
        int viewHeight = footerTop - fieldsTop - STATUS_RESERVE - 6;
        if (compact) {
            int rowCount = Math.max(0, (logicalY - fieldsTop) / rowHeight());
            int visibleRows = Math.max(1,
                    (footerTop - STATUS_RESERVE - fieldsTop) / rowHeight());
            maxScrollPx = Math.max(0, (rowCount - visibleRows) * rowHeight());
        } else {
            maxScrollPx = Math.max(0, (logicalY - fieldsTop) - viewHeight);
        }
        int prevScroll = scrollPx;
        scrollPx = clampInt(scrollPx, 0, maxScrollPx);
        if (prevScroll != scrollPx) {
            rebuild();
            return;
        }

        int footerW = Math.min(420, this.width - MARGIN * 2);
        int footerX = (this.width - footerW) / 2;
        int footerButtonW = (footerW - (footerCols - 1) * FOOTER_GAP) / footerCols;
        int[] footerXs = new int[8];
        int[] footerYs = new int[8];
        for (int i = 0; i < footerXs.length; i++) {
            footerXs[i] = footerX + (i % footerCols) * (footerButtonW + FOOTER_GAP);
            footerYs[i] = footerTop + (i / footerCols) * FOOTER_ROW_H;
        }

        if (advancedActions) {
            saveButton = null;
            saveReloadButton = addRenderableWidget(Button.builder(Component.literal("Save + apply"), b -> save(true))
                    .bounds(footerXs[0], footerYs[0], footerButtonW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Reload from disk"), b -> {
                GemsClientConfigManager.reloadFromDisk();
                load();
                rebuild();
            }).bounds(footerXs[1], footerYs[1], footerButtonW, 20).build());
            Button resetSectionButton = addRenderableWidget(Button.builder(Component.literal("Reset section"), b -> {
                resetSection();
                rebuild();
            }).bounds(footerXs[2], footerYs[2], footerButtonW, 20).build());
            Button resetAllButton = addRenderableWidget(Button.builder(Component.literal("Reset server"), b -> {
                cfg = normalize(new GemsBalanceConfig());
                dirty = true;
                disablesCfg = new GemsDisablesConfig();
                disablesDirty = true;
                rebuild();
            }).bounds(footerXs[3], footerYs[3], footerButtonW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Copy JSON paths"), b -> {
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.keyboardHandler.setClipboard("Balance: " + balancePath()
                            + "\nDisables: " + disablesPath()
                            + "\nEffective: " + balancePath().resolveSibling("balance.effective.json"));
                }
            }).bounds(footerXs[4], footerYs[4], footerButtonW, 20).build());
            Button dumpButton = addRenderableWidget(Button.builder(Component.literal("Dump effective"),
                    b -> dumpLocalBalance())
                    .bounds(footerXs[5], footerYs[5], footerButtonW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
                advancedActions = false;
                toolStatus = null;
                rebuild();
            }).bounds(footerXs[6], footerYs[6], footerButtonW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(footerXs[7], footerYs[7], footerButtonW, 20).build());
            resetSectionButton.active = canEdit;
            resetAllButton.active = canEdit;
            dumpButton.active = canEdit;
        } else {
            saveReloadButton = null;
            saveButton = addRenderableWidget(Button.builder(Component.literal("Save changes"), b -> save(false))
                    .bounds(footerXs[0], footerYs[0], footerButtonW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(footerXs[1], footerYs[1], footerButtonW, 20).build());
            Button moreToolsButton = addRenderableWidget(Button.builder(Component.literal("More tools"), b -> {
                advancedActions = true;
                rebuild();
            }).bounds(footerXs[2], footerYs[2], footerButtonW, 20).build());
            moreToolsButton.active = canEdit;
        }

        updateButtonState();
    }

    private void updateButtonState() {
        if (saveButton != null) {
            saveButton.active = (canEdit && (dirty || disablesDirty) && validation.isValid())
                    || (canSaveClient && clientDirty);
        }
        if (saveReloadButton != null) {
            saveReloadButton.active = canEdit && (dirty || disablesDirty) && validation.isValid();
        }
    }

    private void selectSection(Section next) {
        section = next;
        scrollPx = 0;
        rebuild();
    }

    private void save(boolean reload) {
        boolean saveServer = dirty && canEdit && validation.isValid();
        boolean saveDisables = disablesDirty && canEdit;
        boolean saveClient = clientDirty && canSaveClient;
        if (!saveServer && !saveDisables && !saveClient) {
            return;
        }
        if (saveServer) {
            if (GemsConfigManager.writeBalanceForUi(cfg, loadedCfg)) {
                loadedCfg = GemsConfigManager.copyForUi(cfg);
                dirty = false;
                loadError = null;
                if (reload) {
                    reloadLocalBalance();
                }
            } else {
                loadError = "Could not save balance.json; the file was kept.";
            }
        }
        if (saveDisables) {
            if (GemsDisablesConfigManager.writeDisablesForUi(disablesCfg, loadedDisablesCfg)) {
                loadedDisablesCfg = GemsDisablesConfigManager.copyForUi(disablesCfg);
                disablesDirty = false;
                disablesLoadError = null;
                if (reload) {
                    reloadLocalDisables();
                }
            } else {
                disablesLoadError = "Could not save disables.json; the file was kept.";
            }
        }
        if (saveClient) {
            if (GemsClientConfigManager.save(clientCfg)) {
                sendPassiveToggle();
                clientDirty = false;
                clientError = null;
            } else {
                clientError = "Could not save client.json; the file was kept.";
            }
        }
        rebuild();
    }

    private void sendPassiveToggle() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ClientPlayNetworking.send(new ClientPassiveTogglePayload(clientCfg.passivesEnabled));
    }

    private static void reloadLocalBalance() {
        Minecraft client = Minecraft.getInstance();
        var server = client == null ? null : client.getSingleplayerServer();
        if (server != null) {
            server.execute(GemsBalance::reloadFromDisk);
        }
    }

    private void dumpLocalBalance() {
        Minecraft client = Minecraft.getInstance();
        var server = client == null ? null : client.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                Path output = GemsBalance.dumpEffectiveBalance();
                client.execute(() -> {
                    if (client.gui.screen() == this) {
                        toolSuccess = output != null;
                        toolStatus = toolSuccess
                                ? "Wrote config/gems/balance.effective.json"
                                : "Could not write config/gems/balance.effective.json; check log";
                    }
                });
            });
        } else {
            toolSuccess = false;
            toolStatus = "Local server unavailable; no file was written";
        }
    }

    private static void reloadLocalDisables() {
        Minecraft client = Minecraft.getInstance();
        var server = client == null ? null : client.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                var result = GemsDisablesConfigManager.loadOrCreateStrict();
                if (result.status() != GemsDisablesConfigManager.LoadStatus.ERROR
                        && result.config() != null) {
                    GemsDisables.apply(result.config());
                }
            });
        }
    }

    private void resetSection() {
        switch (section) {
            case CLIENT -> {
                clientCfg = new GemsClientConfig();
                clientDirty = true;
            }
            case VISUAL -> cfg.visual = new GemsBalanceConfig.Visual();
            case SYSTEMS -> cfg.systems = new GemsBalanceConfig.Systems();
            case ASTRA -> cfg.astra = new GemsBalanceConfig.Astra();
            case FIRE -> cfg.fire = new GemsBalanceConfig.Fire();
            case FLUX -> cfg.flux = new GemsBalanceConfig.Flux();
            case LIFE -> cfg.life = new GemsBalanceConfig.Life();
            case PUFF -> cfg.puff = new GemsBalanceConfig.Puff();
            case SPEED -> cfg.speed = new GemsBalanceConfig.Speed();
            case STRENGTH -> cfg.strength = new GemsBalanceConfig.Strength();
            case WEALTH -> cfg.wealth = new GemsBalanceConfig.Wealth();
            case TERROR -> cfg.terror = new GemsBalanceConfig.Terror();
            case SUMMONER -> cfg.summoner = new GemsBalanceConfig.Summoner();
            case SPACE -> cfg.space = new GemsBalanceConfig.Space();
            case REAPER -> cfg.reaper = new GemsBalanceConfig.Reaper();
            case PILLAGER -> cfg.pillager = new GemsBalanceConfig.Pillager();
            case SPY -> cfg.spy = new GemsBalanceConfig.Spy();
            case BEACON -> cfg.beacon = new GemsBalanceConfig.Beacon();
            case AIR -> cfg.air = new GemsBalanceConfig.Air();
            case VOID -> cfg.voidGem = new GemsBalanceConfig.VoidGem();
            case CHAOS -> cfg.chaos = new GemsBalanceConfig.Chaos();
            case PRISM -> cfg.prism = new GemsBalanceConfig.Prism();
            case DUELIST -> cfg.duelist = new GemsBalanceConfig.Duelist();
            case HUNTER -> cfg.hunter = new GemsBalanceConfig.Hunter();
            case SENTINEL -> cfg.sentinel = new GemsBalanceConfig.Sentinel();
            case TRICKSTER -> cfg.trickster = new GemsBalanceConfig.Trickster();
            case LEGENDARY -> cfg.legendary = new GemsBalanceConfig.Legendary();
            case BONUS_POOL -> cfg.bonusPool = new GemsBalanceConfig.BonusPool();
            case MASTERY -> cfg.mastery = new GemsBalanceConfig.Mastery();
            case RIVALRY -> cfg.rivalry = new GemsBalanceConfig.Rivalry();
            case LOADOUTS -> cfg.loadouts = new GemsBalanceConfig.Loadouts();
            case SYNERGIES -> cfg.synergies = new GemsBalanceConfig.Synergies();
            case AUGMENTS -> cfg.augments = new GemsBalanceConfig.Augments();
            case DISABLES -> {
                disablesCfg = new GemsDisablesConfig();
                disablesDirty = true;
            }
        }
        if (section != Section.CLIENT && section != Section.DISABLES) {
            dirty = true;
        }
    }

    private boolean rowVisible(int y) {
        return y >= fieldsTop && y + (stackedRows ? 38 : 20) <= footerTop - STATUS_RESERVE;
    }

    private int rowHeight() {
        return stackedRows ? 40 : ROW_H;
    }

    private int valueY(int y) {
        return stackedRows ? y + 18 : y;
    }

    private void addLabel(String label, String range, int x, int y, int width) {
        labels.add(new LabelSpec(label, range, x, y, width));
    }

    private int addBoolRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, null, labelX, y, labelW);

        CycleButton<Boolean> btn = CycleButton.onOffBuilder(getter.get())
                .create(fieldX, valueY(y), fieldW, 20, Component.empty(), (b, v) -> {
                    setter.accept(v);
                    dirty = true;
                    updateButtonState();
                });
        btn.active = canEdit;
        btn.setTooltip(Tooltip.create(Component.literal(label)));
        addRenderableWidget(btn);
        return y + rowHeight();
    }

    private int addClientBoolRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, null, labelX, y, labelW);

        CycleButton<Boolean> btn = CycleButton.onOffBuilder(getter.get())
                .create(fieldX, valueY(y), fieldW, 20, Component.empty(), (b, v) -> {
                    setter.accept(v);
                    clientDirty = true;
                    updateButtonState();
                });
        btn.active = canSaveClient;
        btn.setTooltip(Tooltip.create(Component.literal(label)));
        addRenderableWidget(btn);
        return y + rowHeight();
    }

    private int addClientControlModeRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<GemsClientConfig.ControlMode> getter, Consumer<GemsClientConfig.ControlMode> setter) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, null, labelX, y, labelW);

        CycleButton<GemsClientConfig.ControlMode> btn = CycleButton.builder(mode -> Component.literal(mode.name()), getter.get())
                .withValues(java.util.Arrays.asList(GemsClientConfig.ControlMode.values()))
                .create(fieldX, valueY(y), fieldW, 20, Component.empty(), (b, v) -> {
                    setter.accept(v);
                    clientDirty = true;
                    updateButtonState();
                });
		btn.active = canSaveClient;
		btn.setTooltip(Tooltip.create(Component.literal(label)));
	        addRenderableWidget(btn);
	        return y + rowHeight();
    }

    private int addIntRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, intRange(min, max), labelX, y, labelW);
        ValidationTracker.Flag flag = validation.flag();
        EditBox field = new EditBox(this.font, fieldX, valueY(y), fieldW, 20, Component.literal(label));
        field.setValue(Integer.toString(getter.get()));
        field.setTooltip(Tooltip.create(labelWithRange(label, intRange(min, max))));
        field.setEditable(canEdit);
        field.setResponder(s -> {
            Integer parsed = tryParseInt(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampInt(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addRenderableWidget(field);
        return y + rowHeight();
    }

    private int addStringRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<String> getter, Consumer<String> setter) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, null, labelX, y, labelW);
        EditBox field = new EditBox(this.font, fieldX, valueY(y), fieldW, 20, Component.literal(label));
        field.setValue(getter.get());
        field.setEditable(canEdit);
        field.setResponder(s -> {
            setter.accept(s);
            dirty = true;
            updateButtonState();
        });
        addRenderableWidget(field);
        return y + rowHeight();
    }

    private int addDisablesStringRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<String> getter, Consumer<String> setter) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, null, labelX, y, labelW);
        EditBox field = new EditBox(this.font, fieldX, valueY(y), fieldW, 20, Component.literal(label));
        field.setValue(getter.get());
        field.setEditable(canEdit);
        field.setResponder(s -> {
            setter.accept(s);
            disablesDirty = true;
            updateButtonState();
        });
        addRenderableWidget(field);
        return y + rowHeight();
    }

    private int addFloatRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Float> getter, Consumer<Float> setter, float min, float max) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, floatRange(min, max), labelX, y, labelW);
        ValidationTracker.Flag flag = validation.flag();
        EditBox field = new EditBox(this.font, fieldX, valueY(y), fieldW, 20, Component.literal(label));
        field.setValue(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setTooltip(Tooltip.create(labelWithRange(label, floatRange(min, max))));
        field.setEditable(canEdit);
        field.setResponder(s -> {
            Float parsed = tryParseFloat(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampFloat(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addRenderableWidget(field);
        return y + rowHeight();
    }

    private int addDoubleRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Double> getter, Consumer<Double> setter, double min, double max) {
        if (!rowVisible(y)) return y + rowHeight();
        addLabel(label, doubleRange(min, max), labelX, y, labelW);
        ValidationTracker.Flag flag = validation.flag();
        EditBox field = new EditBox(this.font, fieldX, valueY(y), fieldW, 20, Component.literal(label));
        field.setValue(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setTooltip(Tooltip.create(labelWithRange(label, doubleRange(min, max))));
        field.setEditable(canEdit);
        field.setResponder(s -> {
            Double parsed = tryParseDouble(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampDouble(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addRenderableWidget(field);
        return y + rowHeight();
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Float tryParseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampFloat(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Component labelWithRange(String label, String range) {
        return Component.literal(label).append(Component.literal(" [" + range + "]").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String intRange(int min, int max) {
        return min + ".." + max;
    }

    private static String floatRange(float min, float max) {
        return formatNumber(min) + ".." + formatNumber(max);
    }

    private static String doubleRange(double min, double max) {
        return formatNumber(min) + ".." + formatNumber(max);
    }

    private static String formatNumber(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, Math.max(1, end));
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(trimmed);
        }
        return out.toString();
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static Path balancePath() {
        return GemsConfigManager.balancePathForUi();
    }

    private static Path disablesPath() {
        return GemsDisablesConfigManager.disablesPathForUi();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (sidebarMaxScrollPx > 0
                && mouseX >= sidebarX && mouseX < sidebarX + sidebarW
                && mouseY >= sidebarY && mouseY < sidebarY + sidebarViewH) {
            int delta = (int) Math.round(verticalAmount * 12.0D);
            if (delta != 0) {
                sidebarScrollPx = clampInt(sidebarScrollPx - delta, 0, sidebarMaxScrollPx);
                rebuild();
                return true;
            }
        }
        if (maxScrollPx <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (mouseX < (compactLayout() ? MARGIN : sidebarX + sidebarW + 8)
                || mouseX > width - MARGIN + 4 || mouseY < fieldsTop - 4
                || mouseY > footerTop - STATUS_RESERVE) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta = (int) Math.round(verticalAmount * (compactLayout() ? rowHeight() : 12.0D));
        if (delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollPx = clampInt(scrollPx - delta, 0, maxScrollPx);
        rebuild();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent key) {
        // Offscreen rows have no widgets to Tab to. Page the list containing
        // the focused button; leave text editing keys to an active value field.
        if (getFocused() instanceof EditBox) return super.keyPressed(key);
        if (key.key() != InputConstants.KEY_PAGEUP && key.key() != InputConstants.KEY_PAGEDOWN
                && key.key() != InputConstants.KEY_HOME && key.key() != InputConstants.KEY_END) {
            return super.keyPressed(key);
        }
        boolean sidebarFocused = getFocused() instanceof AbstractWidget widget
                && widget.getX() >= sidebarX && widget.getX() < sidebarX + sidebarW
                && widget.getY() >= sidebarY && widget.getY() < sidebarY + sidebarViewH;
        if (sidebarFocused && sidebarMaxScrollPx > 0 || maxScrollPx == 0 && sidebarMaxScrollPx > 0) {
            int page = Math.max(1, sidebarViewH / 24) * 24;
            sidebarScrollPx = pageTarget(key.key(), sidebarScrollPx, sidebarMaxScrollPx, page);
            rebuild();
            return true;
        }
        if (maxScrollPx > 0) {
            int page = Math.max(1, (footerTop - fieldsTop - STATUS_RESERVE - 6)
                    / rowHeight()) * rowHeight();
            scrollPx = pageTarget(key.key(), scrollPx, maxScrollPx, page);
            rebuild();
            return true;
        }
        return super.keyPressed(key);
    }

    private static int pageTarget(int key, int current, int maximum, int page) {
        return clampInt(switch (key) {
            case InputConstants.KEY_PAGEUP -> current - page;
            case InputConstants.KEY_PAGEDOWN -> current + page;
            case InputConstants.KEY_HOME -> 0;
            case InputConstants.KEY_END -> maximum;
            default -> current;
        }, 0, maximum);
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.gui.setScreen(parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GemsUiTheme.backdrop(context, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GemsUiTheme.panel(context, MARGIN - 8, 8, width - MARGIN + 8, height - 4,
                GemsUiTheme.PANEL, true);
        if (compactLayout()) {
            GemsUiTheme.panel(context, sidebarX - 4, sidebarY - 4,
                    sidebarX + sidebarW + 4, sidebarY + 24, GemsUiTheme.RAIL, false);
            GemsUiTheme.panel(context, MARGIN - 4, fieldsTop - 4,
                    width - MARGIN + 4, footerTop - 4, GemsUiTheme.RAIL, false);
        } else {
            GemsUiTheme.panel(context, sidebarX - 4, sidebarY - 4,
                    sidebarX + sidebarW + 4, footerTop - 4, GemsUiTheme.RAIL, false);
            GemsUiTheme.panel(context, sidebarX + sidebarW + 8, fieldsTop - 4,
                    width - MARGIN + 4, footerTop - 4, GemsUiTheme.RAIL, false);
        }
        GemsUiTheme.rule(context, MARGIN - 4, footerTop - 5, width - MARGIN + 4, 0);
        for (LabelSpec label : labels) {
            GemsUiTheme.card(context, label.x() - 4, label.y() - 1,
                    width - MARGIN + 4, label.y() + rowHeight() - 2, GemsUiTheme.RULE);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        for (LabelSpec label : labels) {
            String full = label.range() == null ? label.label() : label.label() + "  [" + label.range() + "]";
            String shown = font.plainSubstrByWidth(full, Math.max(0, label.width() - 7));
            context.text(font, shown, label.x() + 4,
                    label.y() + (stackedRows ? 4 : 6), GemsUiTheme.PALE);
        }
        int tabW = categoryTabWidth();
        int activeTabX = MARGIN + category.ordinal() * (tabW + 4);
        context.fill(activeTabX + 2, 27, activeTabX + tabW - 2, 29, 0xFFE8C889);
        Section[] visibleSections = visibleSections(category);
        for (int i = 0; i < visibleSections.length; i++) {
            if (visibleSections[i] != section) continue;
            int selectedY = sidebarY + i * 24 - sidebarScrollPx;
            if (selectedY >= sidebarY && selectedY + 20 <= sidebarY + sidebarViewH) {
                context.fill(sidebarX + 1, selectedY + 2, sidebarX + 4,
                        selectedY + 18, 0xFFE8C889);
            }
            break;
        }
        int centerX = this.width / 2;
        context.centeredText(font, this.title, centerX, 16, 0xFFF0E9D8);
        int contentLeft = compactLayout() ? MARGIN : sidebarX + sidebarW + (width <= 500 ? 12 : 20);
        if (!compactLayout() && sidebarMaxScrollPx > 0) {
            context.text(font, "Scroll sections", sidebarX, 56, 0xFFB7C6CC);
        }
        if (compactLayout()) {
            String breadcrumb = category.label.toUpperCase(Locale.ROOT) + "  /  "
                    + section.label.toUpperCase(Locale.ROOT) + "  ·  "
                    + (section == Section.CLIENT ? "THIS DEVICE" : "WORLD RULES");
            context.text(font, font.plainSubstrByWidth(breadcrumb, width - MARGIN * 2), contentLeft, 52, GemsUiTheme.GOLD);
        } else {
            String heading = section.label + " · " + (section == Section.CLIENT ? "This device" : "World rules");
            context.text(font, font.plainSubstrByWidth(heading, width - MARGIN - contentLeft),
                    contentLeft, 56, GemsUiTheme.GOLD);
        }
        if (maxScrollPx > 0 && !compactLayout() && width - MARGIN - contentLeft >= 200) {
            String hint = "PgDn values";
            context.text(font, hint, width - MARGIN - font.width(hint), 56, 0xFFB7C6CC);
        }
        if (maxScrollPx > 0) {
            int trackTop = fieldsTop + 3;
            int trackBottom = footerTop - STATUS_RESERVE - 7;
            if (trackBottom > trackTop) {
                int thumbH = Math.max(8, (trackBottom - trackTop) * (trackBottom - trackTop)
                        / Math.max(1, contentEndY - fieldsTop));
                int thumbY = trackTop + (trackBottom - trackTop - thumbH) * scrollPx / maxScrollPx;
                context.fill(width - MARGIN + 1, trackTop, width - MARGIN + 3, trackBottom, GemsUiTheme.RULE);
                context.fill(width - MARGIN + 1, thumbY, width - MARGIN + 3, thumbY + thumbH, GemsUiTheme.GOLD);
            }
        }

        int noteTop = Math.max(fieldsTop + 28, contentEndY + 5);
        int noteLeft = contentLeft;
        int noteRight = width - MARGIN;
        boolean hasStatus = loadError != null || disablesLoadError != null || clientError != null
                || !validation.isValid() || dirty || disablesDirty || clientDirty
                || (advancedActions && toolStatus != null) || (!canEdit && section != Section.CLIENT);
        if (!hasStatus && scrollPx == 0 && noteTop + 26 < footerTop - 7
                && noteRight - noteLeft >= 220) {
            context.fill(noteLeft, noteTop, noteRight, noteTop + 26, GemsUiTheme.WELL);
            context.fill(noteLeft, noteTop, noteLeft + 3, noteTop + 26, GemsUiTheme.GOLD);
            context.text(font, section == Section.CLIENT ? "JUST FOR YOU" : "FOR THIS WORLD",
                    noteLeft + 10, noteTop + 4, 0xFFE8C889);
            String hint = sectionHint();
            int hintWidth = noteRight - noteLeft - 20;
            if (font.width(hint) > hintWidth) {
                hint = font.plainSubstrByWidth(hint, hintWidth - font.width("...")) + "...";
            }
            context.text(font, hint, noteLeft + 10, noteTop + 15, 0xFFF0E9D8);
        }

        String status = null;
        int statusColor = 0xFFB7C6CC;
        if (loadError != null) {
            status = loadError;
            statusColor = 0xFFFF8B82;
        } else if (disablesLoadError != null) {
            status = disablesLoadError;
            statusColor = 0xFFFF8B82;
        } else if (clientError != null) {
            status = clientError;
            statusColor = 0xFFFF8B82;
        } else if (advancedActions && toolStatus != null) {
            status = toolStatus;
            statusColor = toolSuccess ? 0xFF9BE0BE : 0xFFFF8B82;
        } else if (!validation.isValid()) {
            status = "Fix invalid values to enable Save.";
            statusColor = 0xFFFF8B82;
        } else if (dirty || disablesDirty || clientDirty) {
            status = "Unsaved changes.";
            statusColor = 0xFFE8C889;
        } else if (!canEdit && section != Section.CLIENT) {
            status = "World rules are managed on the server.";
            statusColor = 0xFFE8C889;
        }
        if (status != null && footerTop >= 30) {
            int availableWidth = width - contentLeft - MARGIN - 16;
            if (font.width(status) > availableWidth) {
                status = font.plainSubstrByWidth(status, Math.max(0, availableWidth - font.width("..."))) + "...";
            }
            context.fill(contentLeft, footerTop - STATUS_RESERVE, width - MARGIN, footerTop - 6, 0xEB102936);
            context.centeredText(font, Component.literal(status),
                    (contentLeft + width - MARGIN) / 2, footerTop - 19, statusColor);
        }
    }

    private static boolean canEditConfig() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return client.isLocalServer();
    }

    private String sectionHint() {
        return switch (section) {
            case CLIENT -> localWorld
                    ? "Choose your controls and whether passives are on."
                    : "Your controls are yours. The server sets world rules.";
            case VISUAL -> "Use fewer effects if the screen feels busy.";
            case SYSTEMS -> "Set heart limits and which mobs are excluded.";
            case BONUS_POOL -> "Set how many extra powers a player can hold.";
            case DISABLES -> "Turn off gems or powers by entering their IDs.";
            case LEGENDARY -> "Shape the rarest gem rewards and upgrades.";
            case MASTERY -> "Set how quickly players grow with a gem.";
            case RIVALRY -> "Tune rewards for friendly competition.";
            case LOADOUTS -> "Choose how many setups players can save.";
            case SYNERGIES -> "Choose the bonuses for playing together.";
            case AUGMENTS -> "Set how many upgrades items can carry.";
            default -> "Change " + section.label + " for everyone in this world.";
        };
    }

    private enum Category {
        GENERAL("General"),
        ELEMENTAL("Elemental"),
        COMBAT("Combat"),
        MISC("Misc");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private Section[] visibleSections(Category cat) {
        return localWorld ? Section.forCategory(cat)
                : cat == Category.GENERAL ? new Section[] { Section.CLIENT } : new Section[0];
    }

    private enum Section {
        // General
        CLIENT("Your Controls", Category.GENERAL),
        VISUAL("Visual", Category.GENERAL),
        SYSTEMS("World Rules", Category.GENERAL),
        BONUS_POOL("Bonus Powers", Category.GENERAL),
        LEGENDARY("Legendary", Category.GENERAL),
        MASTERY("Mastery", Category.GENERAL),
        RIVALRY("Rivalry", Category.GENERAL),
        LOADOUTS("Saved Setups", Category.GENERAL),
        SYNERGIES("Team Bonuses", Category.GENERAL),
        AUGMENTS("Augments", Category.GENERAL),
        DISABLES("Turn Off", Category.GENERAL),
        // Elemental
        ASTRA("Astra", Category.ELEMENTAL),
        FIRE("Fire", Category.ELEMENTAL),
        FLUX("Flux", Category.ELEMENTAL),
        LIFE("Life", Category.ELEMENTAL),
        PUFF("Puff", Category.ELEMENTAL),
        SPEED("Speed", Category.ELEMENTAL),
        STRENGTH("Strength", Category.ELEMENTAL),
        WEALTH("Wealth", Category.ELEMENTAL),
        SPACE("Space", Category.ELEMENTAL),
        AIR("Air", Category.ELEMENTAL),
        // Combat
        TERROR("Terror", Category.COMBAT),
        SUMMONER("Summoner", Category.COMBAT),
        REAPER("Reaper", Category.COMBAT),
        PILLAGER("Pillager", Category.COMBAT),
        DUELIST("Duelist", Category.COMBAT),
        HUNTER("Hunter", Category.COMBAT),
        SENTINEL("Sentinel", Category.COMBAT),
        TRICKSTER("Trickster", Category.COMBAT),
        // Misc
        SPY("Spy", Category.MISC),
        BEACON("Beacon", Category.MISC),
        VOID("Void", Category.MISC),
        CHAOS("Chaos", Category.MISC),
        PRISM("Prism", Category.MISC);

        private final String label;
        private final Category category;

        Section(String label, Category category) {
            this.label = label;
            this.category = category;
        }

        static Section[] forCategory(Category cat) {
            return java.util.Arrays.stream(values())
                    .filter(s -> s.category == cat)
                    .toArray(Section[]::new);
        }
    }

    private static GemsBalanceConfig normalize(GemsBalanceConfig cfg) {
        if (cfg.visual == null) {
            cfg.visual = new GemsBalanceConfig.Visual();
        }
        if (cfg.systems == null) {
            cfg.systems = new GemsBalanceConfig.Systems();
        }
        if (cfg.fire == null) {
            cfg.fire = new GemsBalanceConfig.Fire();
        }
        if (cfg.flux == null) {
            cfg.flux = new GemsBalanceConfig.Flux();
        }
        if (cfg.astra == null) {
            cfg.astra = new GemsBalanceConfig.Astra();
        }
        if (cfg.life == null) {
            cfg.life = new GemsBalanceConfig.Life();
        }
        if (cfg.puff == null) {
            cfg.puff = new GemsBalanceConfig.Puff();
        }
        if (cfg.speed == null) {
            cfg.speed = new GemsBalanceConfig.Speed();
        }
        if (cfg.strength == null) {
            cfg.strength = new GemsBalanceConfig.Strength();
        }
        if (cfg.wealth == null) {
            cfg.wealth = new GemsBalanceConfig.Wealth();
        }
        if (cfg.terror == null) {
            cfg.terror = new GemsBalanceConfig.Terror();
        }
        if (cfg.summoner == null) {
            cfg.summoner = new GemsBalanceConfig.Summoner();
        }
        if (cfg.space == null) {
            cfg.space = new GemsBalanceConfig.Space();
        }
        if (cfg.reaper == null) {
            cfg.reaper = new GemsBalanceConfig.Reaper();
        }
        if (cfg.pillager == null) {
            cfg.pillager = new GemsBalanceConfig.Pillager();
        }
        if (cfg.spy == null) {
            cfg.spy = new GemsBalanceConfig.Spy();
        }
        if (cfg.beacon == null) {
            cfg.beacon = new GemsBalanceConfig.Beacon();
        }
        if (cfg.air == null) {
            cfg.air = new GemsBalanceConfig.Air();
        }
        if (cfg.voidGem == null) {
            cfg.voidGem = new GemsBalanceConfig.VoidGem();
        }
        if (cfg.chaos == null) {
            cfg.chaos = new GemsBalanceConfig.Chaos();
        }
        if (cfg.prism == null) {
            cfg.prism = new GemsBalanceConfig.Prism();
        }
        if (cfg.duelist == null) {
            cfg.duelist = new GemsBalanceConfig.Duelist();
        }
        if (cfg.hunter == null) {
            cfg.hunter = new GemsBalanceConfig.Hunter();
        }
        if (cfg.sentinel == null) {
            cfg.sentinel = new GemsBalanceConfig.Sentinel();
        }
        if (cfg.trickster == null) {
            cfg.trickster = new GemsBalanceConfig.Trickster();
        }
        if (cfg.legendary == null) {
            cfg.legendary = new GemsBalanceConfig.Legendary();
        }
        if (cfg.bonusPool == null) {
            cfg.bonusPool = new GemsBalanceConfig.BonusPool();
        }
        if (cfg.mastery == null) {
            cfg.mastery = new GemsBalanceConfig.Mastery();
        }
        if (cfg.rivalry == null) {
            cfg.rivalry = new GemsBalanceConfig.Rivalry();
        }
        if (cfg.loadouts == null) {
            cfg.loadouts = new GemsBalanceConfig.Loadouts();
        }
        if (cfg.synergies == null) {
            cfg.synergies = new GemsBalanceConfig.Synergies();
        }
        if (cfg.augments == null) {
            cfg.augments = new GemsBalanceConfig.Augments();
        }
        return cfg;
    }

    private static final class ValidationTracker {
        private int invalid;

        void reset() {
            invalid = 0;
        }

        boolean isValid() {
            return invalid == 0;
        }

        Flag flag() {
            return new Flag(this);
        }

        private void onChange(boolean wasOk, boolean isOk) {
            if (wasOk == isOk) {
                return;
            }
            if (isOk) {
                invalid = Math.max(0, invalid - 1);
            } else {
                invalid++;
            }
        }

        private static final class Flag {
            private final ValidationTracker tracker;
            private boolean ok = true;

            private Flag(ValidationTracker tracker) {
                this.tracker = tracker;
            }

            void setOk(boolean ok) {
                boolean prev = this.ok;
                this.ok = ok;
                tracker.onChange(prev, ok);
            }
        }
    }
}
