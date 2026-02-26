package com.feel.gems.loadout;

import com.feel.gems.core.GemId;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * A loadout preset stores per-gem ability order, passive toggle state, and HUD layout preferences.
 * Loadouts are saved per-player and can be switched at energy 6+.
 */
public record GemLoadout(
        String name,
        GemId gem,
        List<Identifier> abilityOrder,
        boolean passivesEnabled,
        HudLayout hudLayout
) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_PRESETS_PER_GEM = 5;

    /**
     * HUD layout preferences.
     */
    public record HudLayout(
            HudPosition position,
            boolean showCooldowns,
            boolean showEnergy,
            boolean compactMode
    ) {
        public static HudLayout defaults() {
            return new HudLayout(HudPosition.TOP_LEFT, true, true, true);
        }
    }

    public enum HudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    /**
     * Create a default loadout for a gem.
     */
    public static GemLoadout defaultFor(GemId gem, String name, List<Identifier> defaultAbilities) {
        return new GemLoadout(
                name,
                gem,
                List.copyOf(defaultAbilities),
                true,
                HudLayout.defaults()
        );
    }

    /**
     * Validate and sanitize a loadout name.
     */
    public static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "Preset";
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        }
        return trimmed;
    }

    /**
     * Validate ability order against available abilities.
     * Returns a sanitized list containing only valid abilities, appending any missing ones.
     */
    public static List<Identifier> sanitizeAbilityOrder(List<Identifier> order, List<Identifier> available) {
        if (order == null || order.isEmpty()) {
            return List.copyOf(available);
        }
        // Only include abilities that are in the available list
        List<Identifier> sanitized = order.stream()
                .filter(available::contains)
                .distinct()
                .toList();
        if (sanitized.size() == available.size()) {
            return sanitized;
        }
        List<Identifier> result = new java.util.ArrayList<>(sanitized);
        for (Identifier id : available) {
            if (!result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }
}
