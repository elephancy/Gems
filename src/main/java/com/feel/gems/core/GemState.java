package com.feel.gems.core;

import java.util.List;
import net.minecraft.resources.Identifier;




/**
 * Player-facing gem state: identity and energy.
 */
public final class GemState {
    private final GemId id;
    private final GemEnergyState energy;

    public GemState(GemId id, GemEnergyState energy) {
        this.id = id;
        this.energy = energy;
    }

    public GemId id() {
        return id;
    }

    public GemEnergyState energy() {
        return energy;
    }

    public GemState withEnergy(GemEnergyState newEnergy) {
        return new GemState(id, newEnergy);
    }

    public GemState gainEnergy(int delta) {
        return withEnergy(energy.gain(delta));
    }

    public GemState loseEnergyOnDeath() {
        return withEnergy(energy.loseOne());
    }

    public List<Identifier> availablePassives() {
        GemDefinition def = GemRegistry.definition(id);
        return def.availablePassives(energy);
    }

    public List<Identifier> availableAbilities() {
        GemDefinition def = GemRegistry.definition(id);
        return def.availableAbilities(energy);
    }
}
