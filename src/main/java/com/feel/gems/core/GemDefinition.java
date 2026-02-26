package com.feel.gems.core;

import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;




/**
 * Static definition of a gem: ordered abilities and passives.
 */
public final class GemDefinition {
    private final GemId id;
    private final List<Identifier> passives;
    private final List<Identifier> abilities; // ordered unlock list

    public GemDefinition(GemId id, List<Identifier> passives, List<Identifier> abilities) {
        this.id = id;
        this.passives = List.copyOf(passives);
        this.abilities = List.copyOf(abilities);
    }

    public GemId id() {
        return id;
    }

    public List<Identifier> passives() {
        return passives;
    }

    public List<Identifier> abilities() {
        return abilities;
    }

    public List<Identifier> availablePassives(GemEnergyState energy) {
        if (!energy.canUsePassives()) {
            return List.of();
        }
        return passives;
    }

    public List<Identifier> availableAbilities(GemEnergyState energy) {
        if (!energy.canUseAbilities() || abilities.isEmpty()) {
            return List.of();
        }
        int unlocked = energy.unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            return List.of();
        }
        return Collections.unmodifiableList(abilities.subList(0, unlocked));
    }
}
