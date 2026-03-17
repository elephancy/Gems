package com.feel.gems.client;

import com.feel.gems.core.GemId;
import com.feel.gems.net.ServerDisablesPayload;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class ClientDisables {
    private static boolean initialized = false;
    private static EnumSet<GemId> disabledGems = EnumSet.noneOf(GemId.class);
    private static Set<Identifier> disabledAbilities = Set.of();
    private static Set<Identifier> disabledPassives = Set.of();
    private static Set<Identifier> disabledBonusAbilities = Set.of();
    private static Set<Identifier> disabledBonusPassives = Set.of();

    private ClientDisables() {
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void reset() {
        initialized = false;
        disabledGems = EnumSet.noneOf(GemId.class);
        disabledAbilities = Set.of();
        disabledPassives = Set.of();
        disabledBonusAbilities = Set.of();
        disabledBonusPassives = Set.of();
    }

    public static void update(ServerDisablesPayload payload) {
        EnumSet<GemId> gems = EnumSet.noneOf(GemId.class);
        if (payload.disabledGemOrdinals() != null) {
            GemId[] values = GemId.values();
            for (int ordinal : payload.disabledGemOrdinals()) {
                if (ordinal >= 0 && ordinal < values.length) {
                    gems.add(values[ordinal]);
                }
            }
        }

        disabledGems = gems;
        disabledAbilities = parse(payload.disabledAbilityIds());
        disabledPassives = parse(payload.disabledPassiveIds());
        disabledBonusAbilities = parse(payload.disabledBonusAbilityIds());
        disabledBonusPassives = parse(payload.disabledBonusPassiveIds());
        initialized = true;
    }

    private static Set<Identifier> parse(java.util.List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> out = new HashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(s.trim());
            if (id != null) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    public static boolean isGemDisabled(GemId id) {
        return disabledGems.contains(id);
    }

    public static boolean isAbilityDisabled(Identifier id) {
        return disabledAbilities.contains(id);
    }

    public static boolean isPassiveDisabled(Identifier id) {
        return disabledPassives.contains(id);
    }

    public static boolean isBonusAbilityDisabled(Identifier id) {
        return disabledBonusAbilities.contains(id);
    }

    public static boolean isBonusPassiveDisabled(Identifier id) {
        return disabledBonusPassives.contains(id);
    }
}

