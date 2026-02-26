package com.feel.gems.synergy;

import com.feel.gems.core.GemId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Defines a synergy combo between gems.
 * A synergy triggers when abilities from the required gems are cast within a short window.
 * 
 * There are two types of synergies:
 * 1. Gem-based synergies: Any ability from each required gem triggers the synergy
 * 2. Ability-based synergies: Specific abilities must be used (e.g., Fireball + Flux Beam)
 */
public record SynergyDefinition(
        String id,
        String translationKey,
        Set<GemId> requiredGems,
        Optional<Set<Identifier>> requiredAbilities,
        int windowTicks,
        int cooldownTicks,
        SynergyEffect effect
) {
    /**
     * Builder for gem-based synergies (any ability from the gems).
     */
    public static SynergyDefinition gemBased(
            String id, String translationKey, Set<GemId> requiredGems,
            int windowTicks, int cooldownTicks, SynergyEffect effect) {
        return new SynergyDefinition(id, translationKey, requiredGems, Optional.empty(),
                windowTicks, cooldownTicks, effect);
    }

    /**
     * Builder for ability-based synergies (specific abilities required).
     */
    public static SynergyDefinition abilityBased(
            String id, String translationKey, Set<GemId> requiredGems, Set<Identifier> requiredAbilities,
            int windowTicks, int cooldownTicks, SynergyEffect effect) {
        return new SynergyDefinition(id, translationKey, requiredGems, Optional.of(requiredAbilities),
                windowTicks, cooldownTicks, effect);
    }

    /**
     * Check if this synergy requires specific abilities.
     */
    public boolean isAbilitySpecific() {
        return requiredAbilities.isPresent() && !requiredAbilities.get().isEmpty();
    }
    /**
     * The effect that occurs when a synergy triggers.
     */
    @FunctionalInterface
    public interface SynergyEffect {
        /**
         * Apply the synergy effect to the triggering players.
         * @param participants All players who contributed to the synergy (in ability cast order)
         */
        void apply(List<SynergyParticipant> participants);
    }

    /**
     * Record of a player's contribution to a synergy.
     */
    public record SynergyParticipant(
            net.minecraft.server.level.ServerPlayer player,
            GemId gem,
            Identifier abilityId,
            long castTick
    ) {}
}
