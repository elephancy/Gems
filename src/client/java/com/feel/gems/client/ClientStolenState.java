package com.feel.gems.client;

import java.util.Set;
import net.minecraft.resources.Identifier;

public final class ClientStolenState {
    private static Set<Identifier> stolenPassives = Set.of();
    private static Set<Identifier> stolenAbilities = Set.of();

    private ClientStolenState() {
    }

    public static void update(Iterable<Identifier> passives, Iterable<Identifier> abilities) {
        stolenPassives = toSet(passives);
        stolenAbilities = toSet(abilities);
    }

    public static void reset() {
        stolenPassives = Set.of();
        stolenAbilities = Set.of();
    }

    public static Set<Identifier> stolenPassives() {
        return stolenPassives;
    }

    public static Set<Identifier> stolenAbilities() {
        return stolenAbilities;
    }

    public static boolean hasStolenPassives() {
        return !stolenPassives.isEmpty();
    }

    public static boolean hasStolenAbilities() {
        return !stolenAbilities.isEmpty();
    }

    private static Set<Identifier> toSet(Iterable<Identifier> ids) {
        if (ids == null) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (Identifier id : ids) {
            if (id != null) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }
}
