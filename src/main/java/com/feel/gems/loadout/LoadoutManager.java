package com.feel.gems.loadout;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages loadout presets for players.
 * Loadouts unlock at energy 6+ (configurable).
 * Each gem can have up to 5 presets saved.
 */
public final class LoadoutManager {
    private static final String KEY_LOADOUTS = "gemLoadouts";
    private static final String KEY_ACTIVE_PRESET = "activePreset";

    private LoadoutManager() {
    }

    // ========== Energy Requirement ==========

    /**
     * Check if the player meets the energy requirement for loadout presets.
     */
    public static boolean canUseLoadouts(ServerPlayer player) {
        if (!GemsBalance.v().loadouts().enabled()) {
            return false;
        }
        int energy = GemPlayerState.getEnergy(player);
        return energy >= GemsBalance.v().loadouts().unlockEnergy();
    }

    // ========== Preset Management ==========

    /**
     * Get all saved presets for a gem.
     */
    public static List<GemLoadout> getPresets(ServerPlayer player, GemId gem) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag loadouts = root.getCompound(KEY_LOADOUTS).orElse(null);
        if (loadouts == null) {
            return List.of();
        }

        ListTag gemPresets = loadouts.getList(gem.name()).orElse(null);
        if (gemPresets == null || gemPresets.isEmpty()) {
            return List.of();
        }

        List<GemLoadout> result = new ArrayList<>();
        for (int i = 0; i < gemPresets.size(); i++) {
            CompoundTag preset = gemPresets.getCompound(i).orElse(null);
            if (preset != null) {
                GemLoadout loadout = fromNbt(preset, gem);
                if (loadout != null) {
                    result.add(loadout);
                }
            }
        }
        return result;
    }

    /**
     * Save a preset for a gem. Returns the index of the saved preset.
     */
    public static int savePreset(ServerPlayer player, GemLoadout loadout) {
        if (!canUseLoadouts(player)) {
            player.sendSystemMessage(
                    Component.translatable("gems.loadout.locked", GemsBalance.v().loadouts().unlockEnergy())
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return -1;
        }

        GemId gem = loadout.gem();
        List<GemLoadout> existing = new ArrayList<>(getPresets(player, gem));

        // Check max presets
        if (existing.size() >= GemLoadout.MAX_PRESETS_PER_GEM) {
            player.sendSystemMessage(
                    Component.translatable("gems.loadout.max_presets", GemLoadout.MAX_PRESETS_PER_GEM)
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return -1;
        }

        existing.add(loadout);
        saveAllPresets(player, gem, existing);

        player.sendSystemMessage(
                Component.translatable("gems.loadout.saved", loadout.name()).withStyle(ChatFormatting.GREEN),
                false
        );
        return existing.size() - 1;
    }

    /**
     * Delete a preset by index.
     */
    public static boolean deletePreset(ServerPlayer player, GemId gem, int index) {
        List<GemLoadout> existing = new ArrayList<>(getPresets(player, gem));
        if (index < 0 || index >= existing.size()) {
            return false;
        }

        String name = existing.get(index).name();
        existing.remove(index);
        saveAllPresets(player, gem, existing);

        player.sendSystemMessage(
                Component.translatable("gems.loadout.deleted", name).withStyle(ChatFormatting.YELLOW),
                false
        );
        return true;
    }

    /**
     * Load (activate) a preset by index.
     */
    public static boolean loadPreset(ServerPlayer player, GemId gem, int index) {
        if (!canUseLoadouts(player)) {
            player.sendSystemMessage(
                    Component.translatable("gems.loadout.locked", GemsBalance.v().loadouts().unlockEnergy())
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }

        List<GemLoadout> presets = getPresets(player, gem);
        if (index < 0 || index >= presets.size()) {
            player.sendSystemMessage(
                    Component.translatable("gems.loadout.not_found").withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }

        GemLoadout loadout = presets.get(index);

        // Apply the loadout
        applyLoadout(player, loadout);

        // Save active preset index
        setActivePresetIndex(player, gem, index);

        player.sendSystemMessage(
                Component.translatable("gems.loadout.loaded", loadout.name()).withStyle(ChatFormatting.GREEN),
                false
        );
        return true;
    }

    /**
     * Apply a loadout to the player.
     * Respects current energy gating for abilities.
     */
    public static void applyLoadout(ServerPlayer player, GemLoadout loadout) {
        // Apply passives enabled state
        GemPlayerState.setPassivesEnabled(player, loadout.passivesEnabled());

        // Store ability order preference
        saveAbilityOrder(player, loadout.gem(), loadout.abilityOrder());

        // Store HUD layout preference
        saveHudLayout(player, loadout.hudLayout());
    }

    /**
     * Create a loadout from the player's current settings.
     */
    public static GemLoadout createFromCurrent(ServerPlayer player, String name) {
        GemId gem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(gem);

        List<Identifier> abilityOrder = getAbilityOrder(player, gem);
        if (abilityOrder.isEmpty()) {
            abilityOrder = def.abilities();
        }

        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        GemLoadout.HudLayout hudLayout = getHudLayout(player);

        return new GemLoadout(
                GemLoadout.sanitizeName(name),
                gem,
                abilityOrder,
                passivesEnabled,
                hudLayout
        );
    }

    // ========== Ability Order ==========

    private static final String KEY_ABILITY_ORDER = "abilityOrder";

    /**
     * Get the player's custom ability order for a gem.
     */
    public static List<Identifier> getAbilityOrder(ServerPlayer player, GemId gem) {
        if (!canUseLoadouts(player)) {
            return GemRegistry.definition(gem).abilities();
        }
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag orders = root.getCompound(KEY_ABILITY_ORDER).orElse(null);
        if (orders == null) {
            return GemRegistry.definition(gem).abilities();
        }

        ListTag list = orders.getList(gem.name()).orElse(null);
        if (list == null || list.isEmpty()) {
            return GemRegistry.definition(gem).abilities();
        }

        List<Identifier> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String idStr = list.getStringOr(i, "");
            if (!idStr.isEmpty()) {
                Identifier id = Identifier.tryParse(idStr);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return GemLoadout.sanitizeAbilityOrder(result, GemRegistry.definition(gem).abilities());
    }

    /**
     * Save the player's custom ability order for a gem.
     */
    public static void saveAbilityOrder(ServerPlayer player, GemId gem, List<Identifier> order) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag orders = root.getCompound(KEY_ABILITY_ORDER).orElse(new CompoundTag());

        List<Identifier> sanitized = GemLoadout.sanitizeAbilityOrder(order, GemRegistry.definition(gem).abilities());
        ListTag list = new ListTag();
        for (Identifier id : sanitized) {
            list.add(StringTag.valueOf(id.toString()));
        }
        orders.put(gem.name(), list);
        root.put(KEY_ABILITY_ORDER, orders);
    }

    // ========== HUD Layout ==========

    private static final String KEY_HUD_LAYOUT = "hudLayout";

    /**
     * Get the player's HUD layout preferences.
     */
    public static GemLoadout.HudLayout getHudLayout(ServerPlayer player) {
        if (!canUseLoadouts(player)) {
            return GemLoadout.HudLayout.defaults();
        }
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag layout = root.getCompound(KEY_HUD_LAYOUT).orElse(null);
        if (layout == null) {
            return GemLoadout.HudLayout.defaults();
        }

        String posName = layout.getStringOr("position", "TOP_LEFT");
        GemLoadout.HudPosition position;
        try {
            position = GemLoadout.HudPosition.valueOf(posName);
        } catch (IllegalArgumentException e) {
            position = GemLoadout.HudPosition.TOP_LEFT;
        }

        boolean showCooldowns = layout.getBoolean("showCooldowns").orElse(true);
        boolean showEnergy = layout.getBoolean("showEnergy").orElse(true);
        boolean compactMode = layout.getBoolean("compactMode").orElse(false);

        return new GemLoadout.HudLayout(position, showCooldowns, showEnergy, compactMode);
    }

    /**
     * Save the player's HUD layout preferences.
     */
    public static void saveHudLayout(ServerPlayer player, GemLoadout.HudLayout layout) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag nbt = new CompoundTag();
        nbt.putString("position", layout.position().name());
        nbt.putBoolean("showCooldowns", layout.showCooldowns());
        nbt.putBoolean("showEnergy", layout.showEnergy());
        nbt.putBoolean("compactMode", layout.compactMode());
        root.put(KEY_HUD_LAYOUT, nbt);
    }

    // ========== Active Preset Tracking ==========

    /**
     * Get the active preset index for a gem, or -1 if none.
     */
    public static int getActivePresetIndex(ServerPlayer player, GemId gem) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag active = root.getCompound(KEY_ACTIVE_PRESET).orElse(null);
        if (active == null) {
            return -1;
        }
        return active.getIntOr(gem.name(), -1);
    }

    /**
     * Set the active preset index for a gem.
     */
    public static void setActivePresetIndex(ServerPlayer player, GemId gem, int index) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag active = root.getCompound(KEY_ACTIVE_PRESET).orElse(new CompoundTag());
        if (index < 0) {
            active.remove(gem.name());
            if (active.isEmpty()) {
                root.remove(KEY_ACTIVE_PRESET);
            } else {
                root.put(KEY_ACTIVE_PRESET, active);
            }
            return;
        }
        active.putInt(gem.name(), index);
        root.put(KEY_ACTIVE_PRESET, active);
    }

    /**
     * Reset the current gem's loadout to defaults when loadouts are locked.
     */
    public static void resetToDefaults(ServerPlayer player) {
        GemId gem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(gem);
        saveAbilityOrder(player, gem, def.abilities());
        saveHudLayout(player, GemLoadout.HudLayout.defaults());
        GemPlayerState.setPassivesEnabled(player, true);
        setActivePresetIndex(player, gem, -1);
    }

    // ========== Internal Helpers ==========

    private static void saveAllPresets(ServerPlayer player, GemId gem, List<GemLoadout> presets) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag loadouts = root.getCompound(KEY_LOADOUTS).orElse(new CompoundTag());

        ListTag list = new ListTag();
        for (GemLoadout loadout : presets) {
            list.add(toNbt(loadout));
        }
        loadouts.put(gem.name(), list);
        root.put(KEY_LOADOUTS, loadouts);
    }

    private static CompoundTag toNbt(GemLoadout loadout) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", loadout.name());
        nbt.putString("gem", loadout.gem().name());
        nbt.putBoolean("passivesEnabled", loadout.passivesEnabled());

        ListTag abilities = new ListTag();
        for (Identifier id : loadout.abilityOrder()) {
            abilities.add(StringTag.valueOf(id.toString()));
        }
        nbt.put("abilityOrder", abilities);

        CompoundTag hudNbt = new CompoundTag();
        hudNbt.putString("position", loadout.hudLayout().position().name());
        hudNbt.putBoolean("showCooldowns", loadout.hudLayout().showCooldowns());
        hudNbt.putBoolean("showEnergy", loadout.hudLayout().showEnergy());
        hudNbt.putBoolean("compactMode", loadout.hudLayout().compactMode());
        nbt.put("hudLayout", hudNbt);

        return nbt;
    }

    private static GemLoadout fromNbt(CompoundTag nbt, GemId gem) {
        String name = nbt.getStringOr("name", "Preset");
        boolean passivesEnabled = nbt.getBoolean("passivesEnabled").orElse(true);

        List<Identifier> abilityOrder = new ArrayList<>();
        ListTag abilities = nbt.getList("abilityOrder").orElse(null);
        if (abilities != null) {
            for (int i = 0; i < abilities.size(); i++) {
                String idStr = abilities.getStringOr(i, "");
                if (!idStr.isEmpty()) {
                    Identifier id = Identifier.tryParse(idStr);
                    if (id != null) {
                        abilityOrder.add(id);
                    }
                }
            }
        }

        GemLoadout.HudLayout hudLayout = GemLoadout.HudLayout.defaults();
        CompoundTag hudNbt = nbt.getCompound("hudLayout").orElse(null);
        if (hudNbt != null) {
            String posName = hudNbt.getStringOr("position", "TOP_LEFT");
            GemLoadout.HudPosition position;
            try {
                position = GemLoadout.HudPosition.valueOf(posName);
            } catch (IllegalArgumentException e) {
                position = GemLoadout.HudPosition.TOP_LEFT;
            }
            boolean showCooldowns = hudNbt.getBoolean("showCooldowns").orElse(true);
            boolean showEnergy = hudNbt.getBoolean("showEnergy").orElse(true);
            boolean compactMode = hudNbt.getBoolean("compactMode").orElse(false);
            hudLayout = new GemLoadout.HudLayout(position, showCooldowns, showEnergy, compactMode);
        }

        return new GemLoadout(name, gem, abilityOrder, passivesEnabled, hudLayout);
    }
}
