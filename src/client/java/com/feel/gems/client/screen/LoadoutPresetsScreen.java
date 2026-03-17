package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.client.ClientGemState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.net.LoadoutDeletePayload;
import com.feel.gems.net.LoadoutLoadPayload;
import com.feel.gems.net.LoadoutOpenRequestPayload;
import com.feel.gems.net.LoadoutSavePayload;
import com.feel.gems.net.LoadoutScreenPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client UI for managing per-gem loadout presets.
 */
public final class LoadoutPresetsScreen extends GemsScreenBase {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SETTINGS_ROW_HEIGHT = 22;
    private static final int ABILITY_ROW_HEIGHT = 22;
    private static final int ABILITY_BUTTON_WIDTH = 22;

    private final LoadoutScreenPayload payload;
    private final List<Button> loadButtons = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();
    private final List<Button> abilityUpButtons = new ArrayList<>();
    private final List<Button> abilityDownButtons = new ArrayList<>();

    private EditBox nameField;
    private Button saveButton;
    private Button closeButton;
    private Button prevGemButton;
    private Button nextGemButton;
    private CycleButton<Boolean> passivesToggle;
    private CycleButton<GemLoadout.HudPosition> hudPosToggle;
    private CycleButton<Boolean> showCooldownsToggle;
    private CycleButton<Boolean> showEnergyToggle;
    private CycleButton<Boolean> compactModeToggle;

    private boolean passivesEnabled = true;
    private GemLoadout.HudPosition hudPosition = GemLoadout.HudPosition.TOP_LEFT;
    private boolean showCooldowns = true;
    private boolean showEnergy = true;
    private boolean compactMode = false;

    private List<Identifier> abilityOrder = new ArrayList<>();
    private String initialName = "Preset";

    private int listTop;
    private int labelX;
    private int loadX;
    private int deleteX;
    private int abilityTop;
    private int abilityTextWidth;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public LoadoutPresetsScreen(LoadoutScreenPayload payload) {
        super(Component.translatable("gems.screen.loadout_presets.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        super.init();
        if (abilityOrder.isEmpty()) {
            initStateFromPayload();
        }
        rebuild();
    }

    private void initStateFromPayload() {
        LoadoutScreenPayload.Preset activePreset = getActivePreset();
        if (activePreset != null) {
            applyPresetToEditor(activePreset);
            if (!activePreset.name().isBlank()) {
                initialName = activePreset.name();
            }
        }
        List<Identifier> available = GemRegistry.definition(payload.gem()).abilities();
        abilityOrder = new ArrayList<>(GemLoadout.sanitizeAbilityOrder(payload.abilityOrder(), available));
    }

    private void rebuild() {
        clearWidgets();
        loadButtons.clear();
        deleteButtons.clear();
        abilityUpButtons.clear();
        abilityDownButtons.clear();

        int centerX = this.width / 2;
        int panelWidth = Math.min(380, this.width - 40);
        panelLeft = centerX - (panelWidth / 2);
        panelRight = centerX + (panelWidth / 2);
        panelTop = 18;
        panelBottom = this.height - 36;

        int top = 28;
        int settingsTop = top + 50;
        int settingsRows = 5;
        int abilityLabelTop = settingsTop + (settingsRows * SETTINGS_ROW_HEIGHT) + 8;
        abilityTop = abilityLabelTop + SETTINGS_ROW_HEIGHT;
        listTop = abilityTop + (Math.max(1, abilityOrder.size()) * ABILITY_ROW_HEIGHT) + 10;
        labelX = panelLeft + 10;
        loadX = panelRight - 130;
        deleteX = panelRight - 60;

        String nameText = nameField != null ? nameField.getValue() : initialName;
        nameField = new EditBox(this.font, panelLeft + 10, top + 26, panelWidth - 150, 18, Component.translatable("gems.screen.loadout_presets.name"));
        nameField.setValue(nameText);
        addRenderableWidget(nameField);

        saveButton = Button.builder(Component.translatable("gems.screen.loadout_presets.save"), btn -> savePreset())
                .bounds(panelRight - 130, top + 24, 120, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(saveButton);

        prevGemButton = Button.builder(Component.literal("<"), btn -> requestGem(offsetGem(-1)))
            .bounds(panelLeft + 10, top, 20, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(prevGemButton);

        nextGemButton = Button.builder(Component.literal(">"), btn -> requestGem(offsetGem(1)))
            .bounds(panelLeft + 140, top, 20, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(nextGemButton);

        closeButton = Button.builder(Component.translatable("gems.screen.button.close"), btn -> onClose())
                .bounds(panelRight - 80, this.height - 28, 70, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(closeButton);

        int labelW = 140;
        int fieldW = panelWidth - labelW - 30;
        int labelX = panelLeft + 10;
        int fieldX = panelLeft + 10 + labelW + 10;
        int rowY = settingsTop;

        addLabel(labelX, rowY, labelW, "Passives Enabled");
        passivesToggle = addToggle(fieldX, rowY, fieldW, passivesEnabled, v -> passivesEnabled = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "HUD Position");
        hudPosToggle = CycleButton.builder(pos -> Component.literal(pos.name()), hudPosition)
            .withValues(java.util.Arrays.asList(GemLoadout.HudPosition.values()))
            .create(fieldX, rowY, fieldW, BUTTON_HEIGHT, Component.empty(), (b, v) -> hudPosition = v);
        addRenderableWidget(hudPosToggle);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Show Cooldowns");
        showCooldownsToggle = addToggle(fieldX, rowY, fieldW, showCooldowns, v -> showCooldowns = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Show Energy");
        showEnergyToggle = addToggle(fieldX, rowY, fieldW, showEnergy, v -> showEnergy = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Compact Mode");
        compactModeToggle = addToggle(fieldX, rowY, fieldW, compactMode, v -> compactMode = v);

        addLabel(labelX, abilityLabelTop, panelWidth - 20, "Ability Order");
        abilityTextWidth = (panelWidth - 20) - (ABILITY_BUTTON_WIDTH * 2) - 10;
        buildAbilityButtons();

        buildPresetButtons(panelRight);
    }

    private void buildAbilityButtons() {
        int buttonX = panelRight - (ABILITY_BUTTON_WIDTH * 2) - 12;
        for (int i = 0; i < Math.max(1, abilityOrder.size()); i++) {
            int index = i;
            int y = abilityTop + (i * ABILITY_ROW_HEIGHT);
            Button up = Button.builder(Component.literal("^"), btn -> moveAbility(index, -1))
                    .bounds(buttonX, y - 2, ABILITY_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            Button down = Button.builder(Component.literal("v"), btn -> moveAbility(index, 1))
                    .bounds(buttonX + ABILITY_BUTTON_WIDTH + 4, y - 2, ABILITY_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            boolean active = index < abilityOrder.size();
            up.active = active && index > 0;
            down.active = active && index < abilityOrder.size() - 1;
            abilityUpButtons.add(up);
            abilityDownButtons.add(down);
            addRenderableWidget(up);
            addRenderableWidget(down);
        }
    }

    private void buildPresetButtons(int panelRight) {
        loadButtons.clear();
        deleteButtons.clear();

        int maxPresets = Math.max(1, payload.maxPresets());
        for (int i = 0; i < maxPresets; i++) {
            int y = listTop + (i * ROW_HEIGHT);
            int index = i;

            Button loadBtn = Button.builder(Component.translatable("gems.screen.loadout_presets.load"), btn -> loadPreset(index))
                    .bounds(loadX, y - 2, 60, BUTTON_HEIGHT)
                    .build();
            Button deleteBtn = Button.builder(Component.translatable("gems.screen.loadout_presets.delete"), btn -> deletePreset(index))
                    .bounds(deleteX, y - 2, 50, BUTTON_HEIGHT)
                    .build();

            boolean hasPreset = index < payload.presets().size();
            loadBtn.active = hasPreset;
            deleteBtn.active = hasPreset;

            loadButtons.add(loadBtn);
            deleteButtons.add(deleteBtn);
            addRenderableWidget(loadBtn);
            addRenderableWidget(deleteBtn);
        }
    }

    private void savePreset() {
        if (!ClientGemState.isInitialized()) {
            return;
        }
        String name = nameField.getValue();
        ClientPlayNetworking.send(new LoadoutSavePayload(
                payload.gem(),
                name == null ? "" : name,
                List.copyOf(abilityOrder),
                passivesEnabled,
                hudPosition,
                showCooldowns,
                showEnergy,
                compactMode
        ));
        requestGem(payload.gem());
    }

    private void moveAbility(int index, int delta) {
        if (index < 0 || index >= abilityOrder.size()) {
            return;
        }
        int target = index + delta;
        if (target < 0 || target >= abilityOrder.size()) {
            return;
        }
        Identifier current = abilityOrder.get(index);
        abilityOrder.set(index, abilityOrder.get(target));
        abilityOrder.set(target, current);
        rebuild();
    }

    private void loadPreset(int index) {
        ClientPlayNetworking.send(new LoadoutLoadPayload(payload.gem(), index));
        requestGem(payload.gem());
    }

    private void deletePreset(int index) {
        ClientPlayNetworking.send(new LoadoutDeletePayload(payload.gem(), index));
        requestGem(payload.gem());
    }

    private void requestGem(GemId gem) {
        ClientPlayNetworking.send(new LoadoutOpenRequestPayload(gem));
    }

    private GemId offsetGem(int delta) {
        GemId[] gems = GemId.values();
        int index = 0;
        for (int i = 0; i < gems.length; i++) {
            if (gems[i] == payload.gem()) {
                index = i;
                break;
            }
        }
        int next = (index + delta + gems.length) % gems.length;
        return gems[next];
    }

    @Override
    public boolean keyPressed(KeyEvent key) {
        if (nameField != null && nameField.keyPressed(key)) {
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawPanel(context, panelLeft, panelTop, panelRight, panelBottom);

        renderBase(context);

        String gemLabel = Component.translatable("gems.screen.loadout_presets.gem", payload.gem().name()).getString();
        context.text(this.font, gemLabel, (this.width / 2) - 20, 30, COLOR_GRAY);

        int abilityRows = Math.max(1, abilityOrder.size());
        for (int i = 0; i < abilityRows; i++) {
            String label = Component.translatable("gems.screen.loadout_presets.empty").getString();
            if (i < abilityOrder.size()) {
                Identifier id = abilityOrder.get(i);
                GemAbility ability = ModAbilities.get(id);
                label = ability != null ? ability.name() : id.getPath();
            }
            String line = (i + 1) + ". " + label;
            context.text(this.font, this.font.plainSubstrByWidth(line, abilityTextWidth), labelX, abilityTop + (i * ABILITY_ROW_HEIGHT), COLOR_WHITE);
        }

        int maxPresets = Math.max(1, payload.maxPresets());
        for (int i = 0; i < maxPresets; i++) {
            String label = Component.translatable("gems.screen.loadout_presets.empty").getString();
            if (i < payload.presets().size()) {
                LoadoutScreenPayload.Preset preset = payload.presets().get(i);
                label = preset.name();
            }
            if (i == payload.activeIndex()) {
                label = label + " " + Component.translatable("gems.screen.loadout_presets.active").getString();
            }
            context.text(this.font, (i + 1) + ". " + label, labelX, listTop + (i * ROW_HEIGHT), COLOR_WHITE);
        }

    }

    private LoadoutScreenPayload.Preset getActivePreset() {
        if (payload.presets().isEmpty() || payload.activeIndex() < 0) {
            return null;
        }
        int idx = Math.max(0, Math.min(payload.activeIndex(), payload.presets().size() - 1));
        return payload.presets().get(idx);
    }

    private void applyPresetToEditor(LoadoutScreenPayload.Preset preset) {
        passivesEnabled = preset.passivesEnabled();
        hudPosition = preset.hudPosition();
        showCooldowns = preset.showCooldowns();
        showEnergy = preset.showEnergy();
        compactMode = preset.compactMode();
    }

    private void addLabel(int x, int y, int w, String text) {
        Button label = Button.builder(Component.literal(text), b -> {})
                .bounds(x, y, w, BUTTON_HEIGHT)
                .build();
        label.active = false;
        addRenderableWidget(label);
    }

    private CycleButton<Boolean> addToggle(int x, int y, int w, boolean initial, java.util.function.Consumer<Boolean> setter) {
        CycleButton<Boolean> btn = CycleButton.onOffBuilder(initial)
                .create(x, y, w, BUTTON_HEIGHT, Component.empty(), (b, v) -> setter.accept(v));
        addRenderableWidget(btn);
        return btn;
    }
}
