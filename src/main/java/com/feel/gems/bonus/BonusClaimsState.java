package com.feel.gems.bonus;

import com.feel.gems.GemsMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import java.util.*;

/**
 * Server-wide state tracking which bonus abilities/passives are claimed by which players.
 * Claims are unique per player - if player A claims an ability, player B cannot claim it.
 * Claims are released when:
 * - Player loses energy levels (drops below 10)
 * - Player switches to another gem
 */
public final class BonusClaimsState extends SavedData {
    private static final String STATE_ID = GemsMod.MOD_ID + "_bonus_claims";
    
    // Maps ability/passive ID -> player UUID who claimed it
    private final Map<Identifier, UUID> abilityClaims;
    private final Map<Identifier, UUID> passiveClaims;
    
    // Maps player UUID -> set of claimed ability/passive IDs (computed from above)
    private final Map<UUID, Set<Identifier>> playerAbilities;
    private final Map<UUID, Set<Identifier>> playerPassives;
    // Maps player UUID -> ordered list of claimed abilities (queue order for keybinds)
    private final Map<UUID, List<Identifier>> abilityOrder;

        static final Codec<BonusClaimsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, UUIDUtil.AUTHLIB_CODEC)
                    .optionalFieldOf("abilityClaims", Map.of())
                    .forGetter(state -> state.abilityClaims),
            Codec.unboundedMap(Identifier.CODEC, UUIDUtil.AUTHLIB_CODEC)
                    .optionalFieldOf("passiveClaims", Map.of())
                .forGetter(state -> state.passiveClaims),
            Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Identifier.CODEC.listOf())
                .optionalFieldOf("abilityOrder", Map.of())
                .forGetter(state -> state.abilityOrder)
        ).apply(instance, BonusClaimsState::new));

    private static final SavedDataType<BonusClaimsState> TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_ID), BonusClaimsState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    public BonusClaimsState() {
        this(Map.of(), Map.of(), Map.of());
    }

    public BonusClaimsState(Map<Identifier, UUID> abilityClaims, Map<Identifier, UUID> passiveClaims, Map<UUID, List<Identifier>> abilityOrder) {
        this.abilityClaims = new HashMap<>(abilityClaims == null ? Map.of() : abilityClaims);
        this.passiveClaims = new HashMap<>(passiveClaims == null ? Map.of() : passiveClaims);
        
        // Build reverse lookup maps
        this.playerAbilities = new HashMap<>();
        this.playerPassives = new HashMap<>();
        this.abilityOrder = new HashMap<>();
        
        for (var entry : this.abilityClaims.entrySet()) {
            playerAbilities.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
        }
        for (var entry : this.passiveClaims.entrySet()) {
            playerPassives.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
        }

        if (abilityOrder != null && !abilityOrder.isEmpty()) {
            for (var entry : abilityOrder.entrySet()) {
                UUID player = entry.getKey();
                List<Identifier> list = new ArrayList<>(entry.getValue());
                Set<Identifier> claimed = playerAbilities.getOrDefault(player, Set.of());
                list.removeIf(id -> !claimed.contains(id));
                this.abilityOrder.put(player, list);
            }
        }

        for (var entry : playerAbilities.entrySet()) {
            UUID player = entry.getKey();
            List<Identifier> list = this.abilityOrder.computeIfAbsent(player, k -> new ArrayList<>());
            for (Identifier id : entry.getValue()) {
                if (!list.contains(id)) {
                    list.add(id);
                }
            }
        }
    }

    public static BonusClaimsState get(MinecraftServer server) {
        SavedDataStorage manager = server.getLevel(Level.OVERWORLD).getDataStorage();
        return manager.computeIfAbsent(TYPE);
    }

    /**
     * Try to claim a bonus ability for a player.
     * @return true if claim succeeded, false if already claimed by someone else
     */
    public boolean claimAbility(UUID playerUuid, Identifier abilityId) {
        if (!BonusPoolRegistry.isBonusAbility(abilityId)) {
            return false;
        }
        
        UUID existing = abilityClaims.get(abilityId);
        if (existing != null && !existing.equals(playerUuid)) {
            return false; // Already claimed by another player
        }
        
        Set<Identifier> playerSet = playerAbilities.computeIfAbsent(playerUuid, k -> new HashSet<>());
        List<Identifier> order = abilityOrder.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        if (order.size() >= 2) {
            return false; // Player already has max 2 bonus abilities
        }
        
        abilityClaims.put(abilityId, playerUuid);
        playerSet.add(abilityId);
        if (!order.contains(abilityId)) {
            order.add(abilityId);
        }
        setDirty();
        return true;
    }

    /**
     * Try to claim a bonus passive for a player.
     * @return true if claim succeeded, false if already claimed by someone else
     */
    public boolean claimPassive(UUID playerUuid, Identifier passiveId) {
        if (!BonusPoolRegistry.isBonusPassive(passiveId)) {
            return false;
        }
        
        UUID existing = passiveClaims.get(passiveId);
        if (existing != null && !existing.equals(playerUuid)) {
            return false; // Already claimed by another player
        }
        
        Set<Identifier> playerSet = playerPassives.computeIfAbsent(playerUuid, k -> new HashSet<>());
        if (playerSet.size() >= 2) {
            return false; // Player already has max 2 bonus passives
        }
        
        passiveClaims.put(passiveId, playerUuid);
        playerSet.add(passiveId);
        setDirty();
        return true;
    }

    /**
     * Release all claims for a player.
     */
    public void releaseAllClaims(UUID playerUuid) {
        Set<Identifier> abilities = playerAbilities.remove(playerUuid);
        if (abilities != null) {
            for (Identifier id : abilities) {
                abilityClaims.remove(id);
            }
        }
        abilityOrder.remove(playerUuid);
        
        Set<Identifier> passives = playerPassives.remove(playerUuid);
        if (passives != null) {
            for (Identifier id : passives) {
                passiveClaims.remove(id);
            }
        }
        
        setDirty();
    }

    /**
     * Get all abilities claimed by a player.
     */
    public Set<Identifier> getPlayerAbilities(UUID playerUuid) {
        return Set.copyOf(playerAbilities.getOrDefault(playerUuid, Set.of()));
    }

    /**
     * Get the ordered list of claimed abilities for a player (queue order).
     */
    public List<Identifier> getPlayerAbilityOrder(UUID playerUuid) {
        return List.copyOf(abilityOrder.getOrDefault(playerUuid, List.of()));
    }

    /**
     * Get all passives claimed by a player.
     */
    public Set<Identifier> getPlayerPassives(UUID playerUuid) {
        return Set.copyOf(playerPassives.getOrDefault(playerUuid, Set.of()));
    }

    /**
     * Check if an ability is available to claim.
     */
    public boolean isAbilityAvailable(Identifier abilityId) {
        return !abilityClaims.containsKey(abilityId);
    }

    /**
     * Check if a passive is available to claim.
     */
    public boolean isPassiveAvailable(Identifier passiveId) {
        return !passiveClaims.containsKey(passiveId);
    }

    /**
     * Get all available (unclaimed) abilities.
     */
    public List<Identifier> getAvailableAbilities() {
        return BonusPoolRegistry.BONUS_ABILITIES.stream()
                .filter(this::isAbilityAvailable)
                .toList();
    }

    /**
     * Get all available (unclaimed) passives.
     */
    public List<Identifier> getAvailablePassives() {
        return BonusPoolRegistry.BONUS_PASSIVES.stream()
                .filter(this::isPassiveAvailable)
                .toList();
    }

    /**
     * Get the player who claimed an ability, or null if unclaimed.
     */
    public UUID getAbilityClaimant(Identifier abilityId) {
        return abilityClaims.get(abilityId);
    }

    /**
     * Get the player who claimed a passive, or null if unclaimed.
     */
    public UUID getPassiveClaimant(Identifier passiveId) {
        return passiveClaims.get(passiveId);
    }

    /**
     * Release a specific ability claim for a player.
     */
    public void releaseAbility(UUID playerUuid, Identifier abilityId) {
        if (abilityClaims.remove(abilityId, playerUuid)) {
            Set<Identifier> playerSet = playerAbilities.get(playerUuid);
            if (playerSet != null) {
                playerSet.remove(abilityId);
            }
            List<Identifier> order = abilityOrder.get(playerUuid);
            if (order != null) {
                order.remove(abilityId);
                if (order.isEmpty()) {
                    abilityOrder.remove(playerUuid);
                }
            }
            setDirty();
        }
    }

    /**
     * Release a specific passive claim for a player.
     */
    public void releasePassive(UUID playerUuid, Identifier passiveId) {
        if (passiveClaims.remove(passiveId, playerUuid)) {
            Set<Identifier> playerSet = playerPassives.get(playerUuid);
            if (playerSet != null) {
                playerSet.remove(passiveId);
            }
            setDirty();
        }
    }
}
