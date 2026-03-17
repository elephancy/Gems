package com.feel.gems.client.screen;

import com.feel.gems.net.SummonerLoadoutSavePayload;
import com.feel.gems.net.SummonerLoadoutScreenPayload;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;




/**
 * Client UI for choosing Summoner minions per slot.
 */
public final class SummonerLoadoutScreen extends GemsScreenBase {
    private static final int MAX_ROWS = 3;
    private static final int MIN_COLUMN_WIDTH = 190;
    private static final int ROW_HEIGHT = 44;

    private final int maxPoints;
    private final Map<String, Integer> costs;
    private final List<List<SummonerLoadouts.Entry>> initial;

    private final List<EntityOption> options;
    private final SlotEditor[] slots = new SlotEditor[5];

    private Button saveButton;
    private Button resetButton;
    private Button cancelButton;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int columns;
    private int columnWidth;
    private int gapX;
    private int startY;
    private java.util.List<net.minecraft.util.FormattedCharSequence> hintLines = java.util.List.of();
    private int hintY;

    public SummonerLoadoutScreen(SummonerLoadoutScreenPayload payload) {
        super(Component.translatable("gems.screen.summoner_loadout.title"));
        this.maxPoints = payload.maxPoints();
        this.costs = payload.costs() == null ? Map.of() : payload.costs();
        this.initial = List.of(
            safeEntries(payload.slot1()),
            safeEntries(payload.slot2()),
            safeEntries(payload.slot3()),
            safeEntries(payload.slot4()),
            safeEntries(payload.slot5())
        );
        this.options = buildOptions(this.costs);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        computeLayout();

        for (int i = 0; i < slots.length; i++) {
            slots[i] = new SlotEditor("Slot " + (i + 1), options, columnWidth);
            slots[i].applyEntries(initial.get(i));
        }

        layoutSlots();

        int buttonWidth = 96;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        int gap = 8;
        int totalWidth = (buttonWidth * 3) + (gap * 2);
        int startX = centerX - (totalWidth / 2);

        saveButton = addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.save"), btn -> save()).bounds(startX, bottomY, buttonWidth, buttonHeight).build());
        resetButton = addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.reset"), btn -> reset()).bounds(startX + buttonWidth + gap, bottomY, buttonWidth, buttonHeight).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gems.screen.button.cancel"), btn -> onClose()).bounds(startX + ((buttonWidth + gap) * 2), bottomY, buttonWidth, buttonHeight).build());

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        drawPanel(context, panelLeft, panelTop, panelRight, panelBottom);

        int centerX = this.width / 2;
        renderBase(context);
        int textY = hintY;
        for (var line : hintLines) {
            context.centeredText(this.font, line, centerX, textY, 0xA0A0A0);
            textY += this.font.lineHeight;
        }

        for (SlotEditor slot : slots) {
            if (slot != null) {
                slot.renderLabels(context, this.font);
            }
        }

        int bottomLine = this.height - 62;
        if (maxPoints > 0) {
            context.centeredText(this.font, Component.translatable("gems.screen.summoner_loadout.points_cap", maxPoints), centerX, bottomLine, 0xA0A0A0);
            bottomLine += 12;
        }
        // No per-slot cap; only active points limit.
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        }
    }

    private void layoutSlots() {
        int gridWidth = columns * columnWidth + (columns - 1) * gapX;
        int centerX = this.width / 2;
        int leftX = centerX - (gridWidth / 2);
        int rightX = leftX + columnWidth + gapX;

        panelLeft = Math.max(8, leftX - 16);
        panelRight = Math.min(this.width - 8, leftX + gridWidth + 16);
        panelTop = 8;
        panelBottom = Math.min(this.height - 48, startY + (rowsForLayout() * slotBlockHeight()) + 12);

        for (int i = 0; i < slots.length; i++) {
            SlotEditor slot = slots[i];
            int col = columns == 1 ? 0 : i % 2;
            int row = columns == 1 ? i : i / 2;
            int x = col == 0 ? leftX : rightX;
            int y = startY + row * slotBlockHeight();
            slot.position(x, y);
            slot.addWidgets(widget -> this.addRenderableWidget(widget)); // Use lambda for clarity
        }
    }

    private int rowsForLayout() {
        return columns == 1 ? slots.length : (slots.length + 1) / 2;
    }

    private void computeLayout() {
        columns = this.width < 640 ? 1 : 2;
        gapX = columns == 1 ? 0 : 24;
        int maxTextWidth = Math.min(this.width - 32, 520);
        hintLines = this.font.split(
                Component.translatable("gems.screen.summoner_loadout.hint"),
                maxTextWidth
        );
        hintY = 24;
        int hintHeight = hintLines.size() * this.font.lineHeight;
        startY = hintY + hintHeight + 12;

        int totalWidth = Math.min(this.width - 32, 540);
        if (columns == 2) {
            columnWidth = Math.max(MIN_COLUMN_WIDTH, (totalWidth - gapX) / 2);
        } else {
            columnWidth = Math.max(MIN_COLUMN_WIDTH, totalWidth);
        }

        int gridWidth = columns * columnWidth + (columns - 1) * gapX;
        int leftX = (this.width / 2) - (gridWidth / 2);
        panelLeft = Math.max(8, leftX - 16);
        panelRight = Math.min(this.width - 8, leftX + gridWidth + 16);
        panelTop = 8;
        panelBottom = Math.min(this.height - 48, startY + (rowsForLayout() * slotBlockHeight()) + 12);
    }

    private static int slotBlockHeight() {
        return (MAX_ROWS * ROW_HEIGHT) + 26;
    }

    private void save() {
        ClientPlayNetworking.send(new SummonerLoadoutSavePayload(
                slots[0].entries(),
                slots[1].entries(),
                slots[2].entries(),
                slots[3].entries(),
                slots[4].entries()
        ));
        onClose();
    }

    private void reset() {
        for (int i = 0; i < slots.length; i++) {
            slots[i].applyEntries(initial.get(i));
        }
    }

    private List<EntityOption> buildOptions(Map<String, Integer> costMap) {
        List<EntityOption> opts = new ArrayList<>();
        opts.add(EntityOption.none());
        for (Map.Entry<String, Integer> entry : costMap.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) {
                continue;
            }
            Component name = type.getDescription();
            EntityStats stats = EntityStats.from(type);
            opts.add(new EntityOption(id.toString(), name, entry.getValue(), stats));
        }
        opts.sort(Comparator.comparingInt(EntityOption::cost).thenComparing(o -> o.name.getString()));
        return opts;
    }

    private static List<SummonerLoadouts.Entry> safeEntries(List<SummonerLoadouts.Entry> entries) {
        return entries == null ? List.of() : entries;
    }

    private final class SlotEditor {
        private final String title;
        private final List<EntityOption> options;
        private final Row[] rows = new Row[MAX_ROWS];
        private Button addButton;
        private int baseX;
        private int baseY;

        private final int columnWidth;

        private SlotEditor(String title, List<EntityOption> options, int columnWidth) {
            this.title = title;
            this.options = options;
            this.columnWidth = columnWidth;
            for (int i = 0; i < MAX_ROWS; i++) {
                rows[i] = new Row(options, columnWidth);
            }
        }

        void position(int x, int y) {
            this.baseX = x;
            this.baseY = y;
            relayout();
        }

        void addWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> adder) {
            for (Row row : rows) {
                adder.accept(row.mobButton);
                adder.accept(row.searchField);
                adder.accept(row.countButton);
                adder.accept(row.removeButton);
            }
            if (addButton == null) {
                addButton = Button.builder(Component.translatable("gems.screen.summoner_loadout.add_row"), btn -> addRow()).bounds(baseX, baseY + (MAX_ROWS * ROW_HEIGHT) + 6, columnWidth - 26, 20).build();
            }
            adder.accept(addButton);
            refreshVisibility();
        }

        void renderLabels(GuiGraphicsExtractor context, net.minecraft.client.gui.Font font) {
            context.text(font, title, baseX, baseY - 12, 0xFFFFFFFF);
        }

        void applyEntries(List<SummonerLoadouts.Entry> entries) {
            int idx = 0;
            if (entries != null) {
                for (SummonerLoadouts.Entry entry : entries) {
                    if (idx >= MAX_ROWS) {
                        break;
                    }
                    rows[idx].set(entry);
                    idx++;
                }
            }
            for (; idx < MAX_ROWS; idx++) {
                rows[idx].clear();
            }
            refreshVisibility();
        }

        List<SummonerLoadouts.Entry> entries() {
            List<SummonerLoadouts.Entry> list = new ArrayList<>();
            for (Row row : rows) {
                if (!row.visible()) {
                    continue;
                }
                SummonerLoadouts.Entry e = row.asEntry();
                if (e != null) {
                    list.add(e);
                }
            }
            return list;
        }

        private void addRow() {
            for (Row row : rows) {
                if (!row.visible()) {
                    row.show(); // Show the first hidden row
                    refreshVisibility();
                    return;
                }
            }
        }

        private void removeRow(Row row) {
            row.clear();
            refreshVisibility();
        }

        private void refreshVisibility() {
            int visibleCount = 0;
            for (Row row : rows) {
                if (row.visible()) {
                    visibleCount++; // Count visible rows
                }
            }
            for (Row row : rows) {
                row.setVisible(row == rows[0] || row.visible());
            }
            if (addButton != null) {
                addButton.active = visibleCount < MAX_ROWS;
                addButton.visible = visibleCount < MAX_ROWS;
            }
        }

        private void relayout() {
            for (int i = 0; i < rows.length; i++) {
                int y = baseY + 4 + i * ROW_HEIGHT;
                Row row = rows[i];
                row.setPosition(baseX, y);
                row.setRemoveHandler(() -> removeRow(row));
            }
            if (addButton != null) {
                addButton.setPosition(baseX, baseY + (MAX_ROWS * ROW_HEIGHT) + 6);
            }
        }

        private final class Row {
            private final CycleButton<EntityOption> mobButton;
            private final net.minecraft.client.gui.components.EditBox searchField;
            private final CycleButton<Integer> countButton;
            private final Button removeButton;
            private Runnable removeAction = () -> {};
            private final int mobWidth;

            Row(List<EntityOption> options, int columnWidth) {
                mobWidth = Math.max(60, columnWidth - 108);
                this.mobButton = CycleButton.builder(EntityOption::label, options.get(0))
                        .withValues(options)
                        .create(0, 0, mobWidth, 20, Component.literal("Mob"), (btn, value) -> {
                            refreshVisibility();
                        });
                this.searchField = new net.minecraft.client.gui.components.EditBox(SummonerLoadoutScreen.this.font, 0, 0, mobWidth, 18, Component.literal("Search"));
                this.searchField.setMaxLength(64);
                this.searchField.setResponder(this::onSearchChanged);
                this.countButton = CycleButton.builder(i -> Component.literal("Count: " + i), 1)
                        .withValues(countOptions())
                            .create(0, 0, 70, 20, Component.literal("Count"), (btn, value) -> {});
                this.removeButton = Button.builder(Component.literal("X"), btn -> removeAction.run()).bounds(0, 0, 22, 20).build();
            }

            void setPosition(int x, int y) {
                mobButton.setPosition(x, y);
                searchField.setPosition(x, y + 22);
                countButton.setPosition(x + mobWidth + 6, y);
                removeButton.setPosition(x + mobWidth + 6 + 76, y);
            }

            void setRemoveHandler(Runnable runnable) {
                this.removeAction = runnable;
            }

            void set(SummonerLoadouts.Entry entry) {
                EntityOption opt = options.stream().filter(o -> o.id != null && o.id.equals(entry.entityId())).findFirst().orElse(options.get(0));
                mobButton.setValue(opt);
                countButton.setValue(Math.max(1, entry.count()));
                searchField.setValue("");
                show();
            }

            void clear() {
                mobButton.setValue(options.get(0));
                countButton.setValue(1);
                searchField.setValue("");
                mobButton.visible = false;
                searchField.visible = false;
                countButton.visible = false;
                removeButton.visible = false;
            }

            void show() {
                mobButton.visible = true;
                searchField.visible = true;
                countButton.visible = true;
                removeButton.visible = true;
            }

            boolean visible() {
                return mobButton.visible;
            }

            void setVisible(boolean visible) {
                mobButton.visible = visible;
                searchField.visible = visible;
                countButton.visible = visible;
                removeButton.visible = visible;
            }

            SummonerLoadouts.Entry asEntry() {
                EntityOption opt = mobButton.getValue();
                if (opt == null || opt.isNone()) {
                    return null;
                }
                int count = countButton.getValue();
                if (count <= 0) {
                    return null;
                }
                return new SummonerLoadouts.Entry(opt.id, count);
            }

            private List<Integer> countOptions() {
                int max = SummonerLoadoutScreen.this.maxPoints > 0 ? SummonerLoadoutScreen.this.maxPoints : 8;
                int cap = Math.max(1, max);
                List<Integer> values = new ArrayList<>(cap);
                for (int i = 1; i <= cap; i++) {
                    values.add(i);
                }
                return values;
            }

            private void onSearchChanged(String query) {
                if (query == null || query.isBlank()) {
                    return;
                }
                String needle = query.trim().toLowerCase();
                for (EntityOption opt : options) {
                    if (opt == null || opt.isNone()) {
                        continue;
                    }
                    String name = opt.name.getString().toLowerCase();
                    if (name.contains(needle) || opt.id.toLowerCase().contains(needle)) {
                        mobButton.setValue(opt);
                        return;
                    }
                }
            }
        }
    }

    private record EntityOption(String id, Component name, int cost, EntityStats stats) {
        static EntityOption none() {
            return new EntityOption(null, Component.translatable("gems.screen.summoner_loadout.none"), 0, EntityStats.none());
        }

        boolean isNone() {
            return id == null;
        }

        Component label() {
            if (isNone()) {
                return Component.translatable("gems.screen.summoner_loadout.none");
            }
            if (stats != null && stats.hasAny()) {
                return Component.literal(name.getString() + " (" + cost + " pts, HP " + stats.healthText() + ", ATK " + stats.attackText() + ")");
            }
            return Component.literal(name.getString() + " (" + cost + " pts)");
        }
    }

    private record EntityStats(double maxHealth, double attackDamage) {
        static EntityStats none() {
            return new EntityStats(-1.0D, -1.0D);
        }

        static EntityStats from(EntityType<?> type) {
            try {
                EntityType<? extends net.minecraft.world.entity.LivingEntity> livingType = asLivingType(type);
                if (livingType == null) {
                    return none();
                }
                net.minecraft.world.entity.ai.attributes.AttributeSupplier defaults = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(livingType);
                if (defaults == null) {
                    return none();
                }
                double hp = defaults.getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                double atk = defaults.hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                        ? defaults.getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                        : 0.0D;
                return new EntityStats(hp, atk);
            } catch (Throwable t) {
                return none();
            }
        }

        @SuppressWarnings("unchecked") // Safe: checked via isAssignableFrom above
        private static EntityType<? extends net.minecraft.world.entity.LivingEntity> asLivingType(EntityType<?> type) {
            if (type == null || !net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                return null;
            }
            return (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type;
        }

        boolean hasAny() {
            return maxHealth >= 0.0D || attackDamage >= 0.0D;
        }

        String healthText() {
            return format(maxHealth);
        }

        String attackText() {
            return format(attackDamage);
        }

        private static String format(double v) {
            if (v < 0.0D) {
                return "?";
            }
            double rounded = Math.rint(v);
            if (Math.abs(rounded - v) < 0.0001D) {
                return Long.toString((long) rounded);
            }
            return String.format(java.util.Locale.ROOT, "%.1f", v);
        }
    }
}
