package com.feel.gems.bonus;

import com.feel.gems.GemsMod;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import java.util.*;
import com.feel.gems.config.GemsBalance;

/**
 * Tracks Prism gem ability/passive selections per player.
 * Prism gem at energy 10/10 can pick:
 * - Up to 3 abilities from normal gems
 * - Up to 3 passives from normal gems
 * - Up to 2 bonus abilities from the bonus pool
 * - Up to 2 bonus passives from the bonus pool
 *
 * Bonus abilities/passives are also claimed globally via {@link BonusClaimsState}; Prism selections track
 * which specific bonus abilities/passives a Prism player is using.
 */
public final class PrismSelectionsState extends SavedData {
    private static final String STATE_ID = GemsMod.MOD_ID + "_prism_selections";
    
    // Maps player UUID -> their Prism selections
    private final Map<UUID, PrismSelection> selections;

    private transient MinecraftServer attachedServer;

    public record PrismSelection(
            List<Identifier> gemAbilities,      // Max 3 from normal gems
            List<Identifier> bonusAbilities,    // Max 2 from bonus pool
            List<Identifier> gemPassives,       // Max 3 from normal gems
            List<Identifier> bonusPassives      // Max 2 from bonus pool
    ) {
        public static final Codec<PrismSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.listOf().optionalFieldOf("gemAbilities", List.of()).forGetter(PrismSelection::gemAbilities),
                Identifier.CODEC.listOf().optionalFieldOf("bonusAbilities", List.of()).forGetter(PrismSelection::bonusAbilities),
                Identifier.CODEC.listOf().optionalFieldOf("gemPassives", List.of()).forGetter(PrismSelection::gemPassives),
                Identifier.CODEC.listOf().optionalFieldOf("bonusPassives", List.of()).forGetter(PrismSelection::bonusPassives)
        ).apply(instance, PrismSelection::new));

        public static PrismSelection empty() {
            return new PrismSelection(List.of(), List.of(), List.of(), List.of());
        }

        public List<Identifier> allAbilities() {
            ArrayList<Identifier> combined = new ArrayList<>(gemAbilities.size() + bonusAbilities.size());
            combined.addAll(gemAbilities);
            combined.addAll(bonusAbilities);
            return List.copyOf(combined);
        }

        public List<Identifier> allPassives() {
            ArrayList<Identifier> combined = new ArrayList<>(gemPassives.size() + bonusPassives.size());
            combined.addAll(gemPassives);
            combined.addAll(bonusPassives);
            return List.copyOf(combined);
        }

        public int totalAbilities() {
            return gemAbilities.size() + bonusAbilities.size();
        }

        public int totalPassives() {
            return gemPassives.size() + bonusPassives.size();
        }
    }

    static final Codec<PrismSelectionsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, PrismSelection.CODEC)
                    .optionalFieldOf("selections", Map.of())
                    .forGetter(state -> state.selections)
    ).apply(instance, PrismSelectionsState::new));

    private static final SavedDataType<PrismSelectionsState> TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_ID), PrismSelectionsState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    public PrismSelectionsState() {
        this(Map.of());
    }

    public PrismSelectionsState(Map<UUID, PrismSelection> selections) {
        this.selections = new HashMap<>(selections == null ? Map.of() : selections);
    }

    public static PrismSelectionsState get(MinecraftServer server) {
        SavedDataStorage manager = server.getLevel(Level.OVERWORLD).getDataStorage();
        PrismSelectionsState state = manager.computeIfAbsent(TYPE);
        state.attach(server);
        return state;
    }

    private void attach(MinecraftServer server) {
        this.attachedServer = server;
    }

    public PrismSelection getSelection(UUID playerUuid) {
        PrismSelection current = selections.get(playerUuid);
        if (current == null) {
            return PrismSelection.empty();
        }
        PrismSelection sanitized = sanitize(playerUuid, current);
        return sanitized;
    }

    private PrismSelection sanitize(UUID playerUuid, PrismSelection current) {
        boolean changed = false;

        List<Identifier> newGemAbilities = new ArrayList<>(current.gemAbilities().size());
        for (Identifier id : current.gemAbilities()) {
            if (id == null || BonusPoolRegistry.isBonusAbility(id) || BonusPoolRegistry.isBlacklisted(id) || ModAbilities.get(id) == null) {
                changed = true;
                continue;
            }
            newGemAbilities.add(id);
        }

        List<Identifier> newBonusAbilities = new ArrayList<>(current.bonusAbilities().size());
        for (Identifier id : current.bonusAbilities()) {
            if (id == null || !BonusPoolRegistry.isBonusAbility(id) || BonusPoolRegistry.isBlacklisted(id) || ModAbilities.get(id) == null) {
                changed = true;
                continue;
            }
            newBonusAbilities.add(id);
        }

        List<Identifier> newGemPassives = new ArrayList<>(current.gemPassives().size());
        for (Identifier id : current.gemPassives()) {
            if (id == null || BonusPoolRegistry.isBonusPassive(id) || BonusPoolRegistry.isBlacklisted(id) || ModPassives.get(id) == null) {
                changed = true;
                continue;
            }
            newGemPassives.add(id);
        }

        List<Identifier> newBonusPassives = new ArrayList<>(current.bonusPassives().size());
        for (Identifier id : current.bonusPassives()) {
            if (id == null || !BonusPoolRegistry.isBonusPassive(id) || BonusPoolRegistry.isBlacklisted(id) || ModPassives.get(id) == null) {
                changed = true;
                continue;
            }
            newBonusPassives.add(id);
        }

        int maxGemAbilities = Math.max(0, GemsBalance.v().prism().maxGemAbilities());
        int maxGemPassives = Math.max(0, GemsBalance.v().prism().maxGemPassives());
        if (newGemAbilities.size() > maxGemAbilities) {
            newGemAbilities = newGemAbilities.subList(0, maxGemAbilities);
            changed = true;
        }
        if (newGemPassives.size() > maxGemPassives) {
            newGemPassives = newGemPassives.subList(0, maxGemPassives);
            changed = true;
        }
        if (newBonusAbilities.size() > 2) {
            newBonusAbilities = newBonusAbilities.subList(0, 2);
            changed = true;
        }
        if (newBonusPassives.size() > 2) {
            newBonusPassives = newBonusPassives.subList(0, 2);
            changed = true;
        }

        if (!changed) {
            return current;
        }

        PrismSelection sanitized = new PrismSelection(
                List.copyOf(newGemAbilities),
                List.copyOf(newBonusAbilities),
                List.copyOf(newGemPassives),
                List.copyOf(newBonusPassives)
        );
        selections.put(playerUuid, sanitized);
        setDirty();
        return sanitized;
    }

    public static boolean hasAbility(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        PrismSelection selection = get(server).getSelection(player.getUUID());
        if (BonusPoolRegistry.isBonusAbility(abilityId)) {
            return selection.bonusAbilities().contains(abilityId);
        }
        return selection.gemAbilities().contains(abilityId);
    }

    public static boolean hasAnyAbility(ServerPlayer player, Collection<Identifier> abilityIds) {
        if (player == null || abilityIds == null || abilityIds.isEmpty()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        PrismSelection selection = get(server).getSelection(player.getUUID());
        for (Identifier id : abilityIds) {
            if (BonusPoolRegistry.isBonusAbility(id)) {
                if (selection.bonusAbilities().contains(id)) {
                    return true;
                }
                continue;
            }
            if (selection.gemAbilities().contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to add a gem ability to player's Prism selection.
     * @return true if added, false if limit reached or blacklisted
     */
    public boolean addGemAbility(UUID playerUuid, Identifier abilityId) {
        if (BonusPoolRegistry.isBonusAbility(abilityId)) {
            return false;
        }
        if (BonusPoolRegistry.isBlacklisted(abilityId)) {
            return false;
        }
        if (ModAbilities.get(abilityId) == null) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.gemAbilities.size() >= GemsBalance.v().prism().maxGemAbilities()) {
            return false;
        }
        if (current.gemAbilities.contains(abilityId) || current.bonusAbilities.contains(abilityId)) {
            return false;
        }
        
        List<Identifier> newGemAbilities = new ArrayList<>(current.gemAbilities);
        newGemAbilities.add(abilityId);
        selections.put(playerUuid, new PrismSelection(newGemAbilities, current.bonusAbilities, current.gemPassives, current.bonusPassives));
        setDirty();
        return true;
    }

    /**
     * Try to add a bonus ability to player's Prism selection.
     * @return true if added, false if limit reached or not a bonus ability
     */
    public boolean addBonusAbility(UUID playerUuid, Identifier abilityId) {
        if (!BonusPoolRegistry.isBonusAbility(abilityId)) {
            return false;
        }
        if (BonusPoolRegistry.isBlacklisted(abilityId)) {
            return false;
        }
        if (ModAbilities.get(abilityId) == null) {
            return false;
        }

        PrismSelection current = getSelection(playerUuid);
        if (current.bonusAbilities.size() >= 2) {
            return false;
        }
        if (current.gemAbilities.contains(abilityId) || current.bonusAbilities.contains(abilityId)) {
            return false;
        }

        if (attachedServer != null) {
            BonusClaimsState claims = BonusClaimsState.get(attachedServer);
            if (!claims.claimAbility(playerUuid, abilityId)) {
                return false;
            }
        }

        List<Identifier> newBonusAbilities = new ArrayList<>(current.bonusAbilities);
        newBonusAbilities.add(abilityId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, newBonusAbilities, current.gemPassives, current.bonusPassives));
        setDirty();
        return true;
    }

    /**
     * Try to add a gem passive to player's Prism selection.
     * @return true if added, false if limit reached or blacklisted
     */
    public boolean addGemPassive(UUID playerUuid, Identifier passiveId) {
        if (BonusPoolRegistry.isBonusPassive(passiveId)) {
            return false;
        }
        if (BonusPoolRegistry.isBlacklisted(passiveId)) {
            return false;
        }
        if (ModPassives.get(passiveId) == null) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.gemPassives.size() >= GemsBalance.v().prism().maxGemPassives()) {
            return false;
        }
        if (current.gemPassives.contains(passiveId) || current.bonusPassives.contains(passiveId)) {
            return false;
        }
        
        List<Identifier> newGemPassives = new ArrayList<>(current.gemPassives);
        newGemPassives.add(passiveId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, newGemPassives, current.bonusPassives));
        setDirty();
        return true;
    }

    /**
     * Try to add a bonus passive to player's Prism selection.
     * @return true if added, false if limit reached or not a bonus passive
     */
    public boolean addBonusPassive(UUID playerUuid, Identifier passiveId) {
        if (!BonusPoolRegistry.isBonusPassive(passiveId)) {
            return false;
        }
        if (BonusPoolRegistry.isBlacklisted(passiveId)) {
            return false;
        }
        if (ModPassives.get(passiveId) == null) {
            return false;
        }

        PrismSelection current = getSelection(playerUuid);
        if (current.bonusPassives.size() >= 2) {
            return false;
        }
        if (current.gemPassives.contains(passiveId) || current.bonusPassives.contains(passiveId)) {
            return false;
        }

        if (attachedServer != null) {
            BonusClaimsState claims = BonusClaimsState.get(attachedServer);
            if (!claims.claimPassive(playerUuid, passiveId)) {
                return false;
            }
        }

        List<Identifier> newBonusPassives = new ArrayList<>(current.bonusPassives);
        newBonusPassives.add(passiveId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, current.gemPassives, newBonusPassives));
        setDirty();
        return true;
    }

    /**
     * Clear all selections for a player.
     */
    public void clearSelections(UUID playerUuid) {
        PrismSelection removed = selections.remove(playerUuid);
        if (removed != null && attachedServer != null) {
            BonusClaimsState claims = BonusClaimsState.get(attachedServer);
            for (Identifier id : removed.bonusAbilities()) {
                claims.releaseAbility(playerUuid, id);
            }
            for (Identifier id : removed.bonusPassives()) {
                claims.releasePassive(playerUuid, id);
            }
        }
        if (removed != null) {
            setDirty();
        }
    }

    /**
     * Remove a specific ability from player's selections.
     */
    public void removeAbility(UUID playerUuid, Identifier abilityId) {
        PrismSelection current = getSelection(playerUuid);
        List<Identifier> newGemAbilities = new ArrayList<>(current.gemAbilities);
        List<Identifier> newBonusAbilities = new ArrayList<>(current.bonusAbilities);
        
        boolean changed = newGemAbilities.remove(abilityId) || newBonusAbilities.remove(abilityId);
        if (changed) {
            if (attachedServer != null && BonusPoolRegistry.isBonusAbility(abilityId)) {
                BonusClaimsState.get(attachedServer).releaseAbility(playerUuid, abilityId);
            }
            selections.put(playerUuid, new PrismSelection(newGemAbilities, newBonusAbilities, current.gemPassives, current.bonusPassives));
            setDirty();
        }
    }

    /**
     * Remove a specific passive from player's selections.
     */
    public void removePassive(UUID playerUuid, Identifier passiveId) {
        PrismSelection current = getSelection(playerUuid);
        List<Identifier> newGemPassives = new ArrayList<>(current.gemPassives);
        List<Identifier> newBonusPassives = new ArrayList<>(current.bonusPassives);
        
        boolean changed = newGemPassives.remove(passiveId) || newBonusPassives.remove(passiveId);
        if (changed) {
            if (attachedServer != null && BonusPoolRegistry.isBonusPassive(passiveId)) {
                BonusClaimsState.get(attachedServer).releasePassive(playerUuid, passiveId);
            }
            selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, newGemPassives, newBonusPassives));
            setDirty();
        }
    }
}
