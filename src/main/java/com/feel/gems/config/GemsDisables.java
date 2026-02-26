package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Server-authoritative content disables.
 */
public final class GemsDisables {
    private static volatile Values VALUES = Values.defaults();

    private GemsDisables() {
    }

    public static Values v() {
        return VALUES;
    }

    public static void init() {
        GemsDisablesConfigManager.LoadResult load = GemsDisablesConfigManager.loadOrCreateWithFallback();
        apply(load.config());
    }

    public static void apply(GemsDisablesConfig cfg) {
        if (cfg == null) {
            cfg = new GemsDisablesConfig();
        }

        Set<GemId> disabledGems = new HashSet<>();
        if (cfg.disabledGems != null) {
            for (String raw : cfg.disabledGems) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    disabledGems.add(GemId.valueOf(raw.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    GemsMod.LOGGER.warn("[disables] Unknown gem id '{}'", raw);
                }
            }
        }

        Set<Identifier> disabledAbilities = parseIdentifiers(cfg.disabledAbilities, "ability");
        Set<Identifier> disabledPassives = parseIdentifiers(cfg.disabledPassives, "passive");
        Set<Identifier> disabledBonusAbilities = parseIdentifiers(cfg.disabledBonusAbilities, "bonus ability");
        Set<Identifier> disabledBonusPassives = parseIdentifiers(cfg.disabledBonusPassives, "bonus passive");

        VALUES = new Values(Set.copyOf(disabledGems), disabledAbilities, disabledPassives, disabledBonusAbilities, disabledBonusPassives);
    }

    private static Set<Identifier> parseIdentifiers(java.util.List<String> values, String label) {
        Set<Identifier> out = new HashSet<>();
        if (values == null) {
            return Set.of();
        }
        for (String raw : values) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(raw.trim());
            if (id == null) {
                GemsMod.LOGGER.warn("[disables] Invalid {} id '{}'", label, raw);
                continue;
            }
            out.add(id);
        }
        return Set.copyOf(out);
    }

    public static boolean isGemDisabled(GemId gemId) {
        return VALUES.disabledGems.contains(gemId);
    }

    public static boolean isGemDisabledFor(ServerPlayer player, GemId gemId) {
        return isGemDisabled(gemId) && !isOp(player);
    }

    public static boolean isAbilityDisabled(Identifier abilityId) {
        return VALUES.disabledAbilities.contains(abilityId);
    }

    public static boolean isAbilityDisabledFor(ServerPlayer player, Identifier abilityId) {
        return isAbilityDisabled(abilityId) && !isOp(player);
    }

    public static boolean isPassiveDisabled(Identifier passiveId) {
        return VALUES.disabledPassives.contains(passiveId);
    }

    public static boolean isPassiveDisabledFor(ServerPlayer player, Identifier passiveId) {
        return isPassiveDisabled(passiveId) && !isOp(player);
    }

    public static boolean isBonusAbilityDisabled(Identifier abilityId) {
        return VALUES.disabledBonusAbilities.contains(abilityId);
    }

    public static boolean isBonusAbilityDisabledFor(ServerPlayer player, Identifier abilityId) {
        return isBonusAbilityDisabled(abilityId) && !isOp(player);
    }

    public static boolean isBonusPassiveDisabled(Identifier passiveId) {
        return VALUES.disabledBonusPassives.contains(passiveId);
    }

    public static boolean isBonusPassiveDisabledFor(ServerPlayer player, Identifier passiveId) {
        return isBonusPassiveDisabled(passiveId) && !isOp(player);
    }

    private static boolean isOp(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            var source = player.createCommandSourceStack();
            return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)));
        } catch (Throwable t) {
            return false;
        }
    }

    public record Values(Set<GemId> disabledGems, Set<Identifier> disabledAbilities, Set<Identifier> disabledPassives,
                         Set<Identifier> disabledBonusAbilities, Set<Identifier> disabledBonusPassives) {
        static Values defaults() {
            return new Values(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
        }
    }
}
