package com.feel.gems.power.runtime;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.passive.StatusEffectPassive;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;




public final class GemPowers {
    private static final String KEY_APPLIED_PASSIVES = "appliedPassives";
    private static final String KEY_APPLIED_BONUS_PASSIVES = "appliedBonusPassives";
    private static final String KEY_APPLIED_PRISM_PASSIVES = "appliedPrismPassives";
    private static final String KEY_APPLIED_TROPHY_PASSIVES = "appliedTrophyPassives";

    private GemPowers() {
    }

    public static void sync(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        boolean suppressed = AbilityRestrictions.isSuppressed(player);

        // Prism gem has special handling - uses selected passives instead of definition
        if (activeGem == GemId.PRISM) {
            syncPrismPassives(player, energy, passivesEnabled, suppressed);
            // Prism can still claim and use the global bonus pool at energy 10.
            syncBonusPassives(player, energy, passivesEnabled, suppressed);
            syncTrophyPassives(player, energy, passivesEnabled, suppressed);
            return;
        }

        // Sync gem passives
        List<Identifier> targetPassives = List.of();
        if (energy > 0 && passivesEnabled && !suppressed) {
            List<Identifier> raw = GemRegistry.definition(activeGem).passives();
            if (!raw.isEmpty()) {
                ArrayList<Identifier> filtered = new ArrayList<>(raw.size());
                for (Identifier id : raw) {
                    if (!GemsDisables.isPassiveDisabledFor(player, id)
                            && !com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                        filtered.add(id);
                    }
                }
                targetPassives = List.copyOf(filtered);
            }
        }

        CompoundTag data = persistentRoot(player);
        Set<Identifier> applied = readIdentifierSet(data, KEY_APPLIED_PASSIVES);
        Set<Identifier> target = new HashSet<>(targetPassives);

        for (Identifier id : applied) {
            if (!target.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        for (Identifier id : target) {
            if (!applied.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_PASSIVES, target);

        // Sync bonus passives (only at energy 10)
        syncBonusPassives(player, energy, passivesEnabled, suppressed);

        // Sync Trophy Necklace passives (permanent stolen passives)
        syncTrophyPassives(player, energy, passivesEnabled, suppressed);
    }

    /**
     * Sync bonus passives for a player. Called from sync() and on login.
     * Releases all bonus claims if energy drops below 10.
     */
    private static void syncBonusPassives(ServerPlayer player, int energy, boolean passivesEnabled, boolean suppressed) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        CompoundTag data = persistentRoot(player);
        Set<Identifier> appliedBonus = readIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES);

        // If energy < 10, release all bonus claims and remove passives
        if (energy < 10) {
            Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUUID());
            Set<Identifier> playerAbilities = claims.getPlayerAbilities(player.getUUID());
            
            if (!playerPassives.isEmpty() || !playerAbilities.isEmpty()) {
                // Remove applied bonus passives
                for (Identifier id : appliedBonus) {
                    GemPassive passive = ModPassives.get(id);
                    if (passive != null) {
                        passive.remove(player);
                    }
                }
                writeIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES, Set.of());
                
                // Release all claims
                claims.releaseAllClaims(player.getUUID());
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.bonus.powers_released"), false);
            }
            return;
        }

        // Energy is 10 - sync bonus passives
        Set<Identifier> targetBonusPassives = Set.of();
        if (passivesEnabled && !suppressed) {
            Set<Identifier> claimed = claims.getPlayerPassives(player.getUUID());
            if (!claimed.isEmpty()) {
                java.util.HashSet<Identifier> filtered = new java.util.HashSet<>();
                for (Identifier id : claimed) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)
                            && !com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                        filtered.add(id);
                    }
                }
                targetBonusPassives = Set.copyOf(filtered);
            }
        }

        // Remove passives no longer in target
        for (Identifier id : appliedBonus) {
            if (!targetBonusPassives.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        // Apply new passives
        for (Identifier id : targetBonusPassives) {
            if (!appliedBonus.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES, targetBonusPassives);
    }

    /**
     * Sync Prism gem passives from player's selections.
     * Prism uses custom selections instead of gem definition passives.
     */
    private static void syncPrismPassives(ServerPlayer player, int energy, boolean passivesEnabled, boolean suppressed) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        CompoundTag data = persistentRoot(player);
        Set<Identifier> appliedPrism = readIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES);

        // If energy <= 0 or passives disabled or suppressed, remove all Prism passives
        if (energy <= 0 || !passivesEnabled || suppressed) {
            for (Identifier id : appliedPrism) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
            writeIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES, Set.of());
            return;
        }

        // Get player's Prism selections
        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
        List<Identifier> selectedPassives = selection.gemPassives();

        // Filter out disabled passives
        Set<Identifier> targetPrismPassives = new HashSet<>();
        for (Identifier id : selectedPassives) {
            if (!GemsDisables.isPassiveDisabledFor(player, id)
                && !GemsDisables.isBonusPassiveDisabledFor(player, id)
                && !com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                targetPrismPassives.add(id);
            }
        }

        // Remove passives no longer selected
        for (Identifier id : appliedPrism) {
            if (!targetPrismPassives.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        // Apply new passives
        for (Identifier id : targetPrismPassives) {
            if (!appliedPrism.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES, targetPrismPassives);
    }

    public static void maintain(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            return;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return;
        }

        com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.refreshLostPassives(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        
        // Prism gem uses selected passives instead of definition
        if (activeGem == GemId.PRISM) {
            maintainPrismPassives(player);
            if (energy >= 10) {
                maintainBonusPassives(player);
            }
            maintainTrophyPassives(player);
            return;
        }
        
        GemDefinition def = GemRegistry.definition(activeGem);
        CompoundTag data = persistentRoot(player);
        if (readIdentifierSet(data, KEY_APPLIED_PASSIVES).isEmpty() && !def.passives().isEmpty()) {
            sync(player);
        }
        for (Identifier passiveId : def.passives()) {
            if (GemsDisables.isPassiveDisabledFor(player, passiveId)) {
                continue;
            }
            if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, passiveId)) {
                GemPassive passive = ModPassives.get(passiveId);
                if (passive != null) {
                    passive.remove(player);
                }
                continue;
            }
            GemPassive passive = ModPassives.get(passiveId);
            if (passive instanceof StatusEffectPassive) {
                passive.apply(player);
            }
            if (passive instanceof GemMaintainedPassive maintained) {
                maintained.maintain(player);
            }
        }

        // Maintain bonus passives (at energy 10)
        if (energy >= 10) {
            maintainBonusPassives(player);
        }

        maintainTrophyPassives(player);
    }

    /**
     * Maintain Prism passives - reapply status effects and call maintain on maintained passives.
     */
    private static void maintainPrismPassives(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
        List<Identifier> selectedPassives = selection.gemPassives();

        for (Identifier passiveId : selectedPassives) {
            if (GemsDisables.isPassiveDisabledFor(player, passiveId) || 
                GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
                continue;
            }
            if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, passiveId)) {
                GemPassive passive = ModPassives.get(passiveId);
                if (passive != null) {
                    passive.remove(player);
                }
                continue;
            }
            GemPassive passive = ModPassives.get(passiveId);
            if (passive instanceof StatusEffectPassive) {
                passive.apply(player);
            }
            if (passive instanceof GemMaintainedPassive maintained) {
                maintained.maintain(player);
            }
        }
    }

    /**
     * Maintain bonus passives - reapply status effects and call maintain on maintained passives.
     */
    private static void maintainBonusPassives(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUUID());

        for (Identifier passiveId : playerPassives) {
            if (GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
                continue;
            }
            if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, passiveId)) {
                GemPassive passive = ModPassives.get(passiveId);
                if (passive != null) {
                    passive.remove(player);
                }
                continue;
            }
            GemPassive passive = ModPassives.get(passiveId);
            if (passive instanceof StatusEffectPassive) {
                passive.apply(player);
            }
            if (passive instanceof GemMaintainedPassive maintained) {
                maintained.maintain(player);
            }
        }
    }

    public static boolean isPassiveActive(ServerPlayer player, Identifier passiveId) {
        GemPlayerState.initIfNeeded(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, passiveId)) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        
        // Prism gem uses selected passives
        if (activeGem == GemId.PRISM) {
            if (isPrismPassiveActive(player, passiveId)) {
                return true;
            }
            if (energy >= 10 && isBonusPassiveActive(player, passiveId)) {
                return true;
            }
            return isTrophyPassiveActive(player, passiveId);
        }
        
        if (GemRegistry.definition(activeGem).passives().contains(passiveId) && !GemsDisables.isPassiveDisabledFor(player, passiveId)) {
            return true;
        }
        // Check bonus passives (at energy 10)
        if (energy >= 10 && isBonusPassiveActive(player, passiveId)) {
            return true;
        }
        return isTrophyPassiveActive(player, passiveId);
    }

    /**
     * Check if a Prism passive is active for a player.
     */
    public static boolean isPrismPassiveActive(ServerPlayer player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) <= 0) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
        List<Identifier> selectedPassives = selection.gemPassives();
        return selectedPassives.contains(passiveId) && 
               !GemsDisables.isPassiveDisabledFor(player, passiveId) &&
               !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Check if a bonus passive is active for a player.
     */
    public static boolean isBonusPassiveActive(ServerPlayer player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) < 10) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUUID());
        if (playerPassives.contains(passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
            return true;
        }

        // Defensive fallback: treat already-applied bonus passives as active. This keeps gameplay consistent
        // if the persistent claims state is temporarily out-of-sync (GameTests create many ephemeral players).
        CompoundTag data = persistentRoot(player);
        Set<Identifier> appliedBonus = readIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES);
        return appliedBonus.contains(passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Check if a Trophy Necklace stolen passive is active for a player.
     */
    public static boolean isTrophyPassiveActive(ServerPlayer player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) <= 0) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        var stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        if (!stolen.contains(passiveId)) {
            return false;
        }
        if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveDisabled(player, passiveId)) {
            return false;
        }
        return !GemsDisables.isPassiveDisabledFor(player, passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Get the list of active passive identifiers for a player.
     * Returns empty if energy is 0, passives are disabled, or the player is suppressed.
     */
    public static List<Identifier> getActivePassives(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            return List.of();
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return List.of();
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return List.of();
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        
        // Prism gem uses selected passives
        if (activeGem == GemId.PRISM) {
            MinecraftServer server = player.level().getServer();
            if (server == null) return List.of();
            PrismSelectionsState prismState = PrismSelectionsState.get(server);
            PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
            List<Identifier> selectedPassives = selection.gemPassives();
            ArrayList<Identifier> result = new ArrayList<>(selectedPassives.size());
            for (Identifier id : selectedPassives) {
                if (!GemsDisables.isPassiveDisabledFor(player, id) && 
                    !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                        continue;
                    }
                    result.add(id);
                }
            }
            if (energy >= 10) {
                BonusClaimsState claims = BonusClaimsState.get(server);
                for (Identifier id : claims.getPlayerPassives(player.getUUID())) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                        if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                            continue;
                        }
                        result.add(id);
                    }
                }
            }
            for (Identifier id : com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player)) {
                if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveDisabled(player, id)) {
                    continue;
                }
                if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    result.add(id);
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        }
        
        List<Identifier> allPassives = GemRegistry.definition(activeGem).passives();
        if (allPassives.isEmpty()) {
            return List.of();
        }
        ArrayList<Identifier> result = new ArrayList<>(allPassives.size());
        for (Identifier id : allPassives) {
            if (!GemsDisables.isPassiveDisabledFor(player, id)) {
                if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                    continue;
                }
                result.add(id);
            }
        }
        if (energy >= 10) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                for (Identifier id : BonusClaimsState.get(server).getPlayerPassives(player.getUUID())) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                        if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveStolenFrom(player, id)) {
                            continue;
                        }
                        result.add(id);
                    }
                }
            }
        }
        for (Identifier id : com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player)) {
            if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveDisabled(player, id)) {
                continue;
            }
            if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                result.add(id);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private static void syncTrophyPassives(ServerPlayer player, int energy, boolean passivesEnabled, boolean suppressed) {
        CompoundTag data = persistentRoot(player);
        Set<Identifier> applied = readIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES);

        if (energy <= 0 || !passivesEnabled || suppressed) {
            for (Identifier id : applied) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
            writeIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES, Set.of());
            return;
        }

        Set<Identifier> stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        Set<Identifier> target = Set.of();
        if (!stolen.isEmpty()) {
            java.util.HashSet<Identifier> filtered = new java.util.HashSet<>();
            for (Identifier id : stolen) {
                if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveDisabled(player, id)) {
                    continue;
                }
                if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    filtered.add(id);
                }
            }
            target = Set.copyOf(filtered);
        }

        for (Identifier id : applied) {
            if (!target.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        for (Identifier id : target) {
            if (!applied.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES, target);
    }

    private static void maintainTrophyPassives(ServerPlayer player) {
        Set<Identifier> stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        if (stolen.isEmpty()) {
            return;
        }
        for (Identifier passiveId : stolen) {
            if (com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.isPassiveDisabled(player, passiveId)) {
                continue;
            }
            if (GemsDisables.isPassiveDisabledFor(player, passiveId) || GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
                continue;
            }
            GemPassive passive = ModPassives.get(passiveId);
            if (passive instanceof StatusEffectPassive) {
                passive.apply(player);
            }
            if (passive instanceof GemMaintainedPassive maintained) {
                maintained.maintain(player);
            }
        }
    }

    private static CompoundTag persistentRoot(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static Set<Identifier> readIdentifierSet(CompoundTag root, String key) {
        ListTag list = root.getList(key).orElse(null);
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> result = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String raw = list.getStringOr(i, "");
            Identifier id = Identifier.tryParse(raw);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private static void writeIdentifierSet(CompoundTag root, String key, Set<Identifier> ids) {
        ListTag list = new ListTag();
        for (Identifier id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        root.put(key, list);
    }
}
