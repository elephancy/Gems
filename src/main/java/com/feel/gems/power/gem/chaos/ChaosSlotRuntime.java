package com.feel.gems.power.gem.chaos;

import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.net.ChaosSlotPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.synergy.SynergyRuntime;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.legendary.LegendaryCooldowns;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Runtime for Chaos gem's independent ability slots.
 * Each slot can be activated with LAlt+1-N to get a random ability+passive.
 * The ability/passive lasts for a duration, during which the ability can be used.
 * After the duration, the slot becomes inactive until re-activated.
 */
public final class ChaosSlotRuntime {
    /**
     * Returns the configured number of Chaos slots (1-9).
     */
    public static int slotCount() {
        return GemsBalance.v().chaos().slotCount();
    }

    private static int slotDurationTicks() {
        return Math.max(1, GemsBalance.v().chaos().slotDurationTicks());
    }

    private static int slotAbilityCooldownTicks() {
        return Math.max(0, GemsBalance.v().chaos().slotAbilityCooldownTicks());
    }

    private static int slotAbilityCooldownTicks(ServerPlayer player) {
        int base = slotAbilityCooldownTicks();
        if (base <= 0) {
            return 0;
        }
        if (com.feel.gems.admin.GemsAdmin.noCooldowns(player)) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player)
                * LegendaryCooldowns.getCooldownMultiplier(player)
                * com.feel.gems.augment.AugmentRuntime.cooldownMultiplier(player, GemId.CHAOS);
        int adjusted = (int) Math.ceil(base * mult);
        return Math.max(1, adjusted);
    }
    
    // Maps player UUID -> their chaos slot states
    private static final Map<UUID, ChaosPlayerState> playerStates = new HashMap<>();
    
    public record SlotState(
            Identifier abilityId,
            String abilityName,
            Identifier passiveId,
            String passiveName,
            long activationTick,
            long lastAbilityUseTick
    ) {
        public static SlotState inactive() {
            return new SlotState(null, "", null, "", 0, 0);
        }
        
        public boolean isActive(long currentTick) {
            return abilityId != null && (currentTick - activationTick) < slotDurationTicks();
        }
        
        public int remainingTicks(long currentTick) {
            if (abilityId == null) return 0;
            return (int) Math.max(0, slotDurationTicks() - (currentTick - activationTick));
        }
        
        public boolean canUseAbility(long currentTick) {
            return isActive(currentTick) && (currentTick - lastAbilityUseTick) >= slotAbilityCooldownTicks();
        }
        
        public int abilityCooldownRemaining(long currentTick) {
            return (int) Math.max(0, slotAbilityCooldownTicks() - (currentTick - lastAbilityUseTick));
        }
        
        public SlotState withAbilityUsed(long tick) {
            return new SlotState(abilityId, abilityName, passiveId, passiveName, activationTick, tick);
        }
    }
    
    public record ChaosPlayerState(SlotState[] slots) {
        public ChaosPlayerState() {
            this(createEmptySlots());
        }

        private static SlotState[] createEmptySlots() {
            SlotState[] slots = new SlotState[slotCount()];
            Arrays.fill(slots, SlotState.inactive());
            return slots;
        }
        
        public SlotState getSlot(int index) {
            if (index < 0 || index >= slotCount()) return SlotState.inactive();
            // Handle dynamic slot count changes gracefully
            if (index >= slots.length) return SlotState.inactive();
            return slots[index];
        }
        
        public ChaosPlayerState withSlot(int index, SlotState state) {
            int count = slotCount();
            SlotState[] newSlots = new SlotState[count];
            for (int i = 0; i < count; i++) {
                if (i < slots.length) {
                    newSlots[i] = slots[i];
                } else {
                    newSlots[i] = SlotState.inactive();
                }
            }
            if (index >= 0 && index < count) {
                newSlots[index] = state;
            }
            return new ChaosPlayerState(newSlots);
        }
    }

    private ChaosSlotRuntime() {
    }

    public static void tick(MinecraftServer server) {
        long currentTick = server.overworld().getGameTime();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GemPlayerState.initIfNeeded(player);
            if (GemPlayerState.getActiveGem(player) != GemId.CHAOS) {
                if (playerStates.containsKey(player.getUUID())) {
                    clearState(player);
                    syncAllToClient(player, new ChaosPlayerState(), currentTick);
                }
                continue;
            }
            
            int energy = GemPlayerState.getEnergy(player);
            if (energy < 1) {
                if (playerStates.containsKey(player.getUUID())) {
                    clearState(player);
                    syncAllToClient(player, new ChaosPlayerState(), currentTick);
                }
                continue;
            }
            
            ChaosPlayerState state = playerStates.computeIfAbsent(player.getUUID(), k -> new ChaosPlayerState());
            
            // Check for expired slots and remove passives
            boolean changed = false;
            for (int i = 0; i < slotCount(); i++) {
                SlotState slot = state.getSlot(i);
                if (slot.abilityId != null && !slot.isActive(currentTick)) {
                    // Slot expired - remove passive
                    if (slot.passiveId != null) {
                        GemPassive passive = ModPassives.get(slot.passiveId);
                        if (passive != null) {
                            passive.remove(player);
                        }
                    }
                    state = state.withSlot(i, SlotState.inactive());
                    changed = true;
                    player.sendSystemMessage(Component.translatable("gems.chaos.slot_expired", i + 1).withStyle(ChatFormatting.GRAY), true);
                }
            }
            
            if (changed) {
                playerStates.put(player.getUUID(), state);
            }
            
            // Sync to client every second
            if (currentTick % 20 == 0) {
                syncAllToClient(player, state, currentTick);
            }
        }
    }

    /**
     * Activate a chaos slot - either use the ability if active, or roll new ability/passive if inactive.
     */
    public static boolean activateSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slotCount()
                || GemPlayerState.getActiveGem(player) != GemId.CHAOS
                || GemPlayerState.getEnergy(player) < 1) {
            return false;
        }
        
        long currentTick = player.level().getGameTime();
        ChaosPlayerState state = playerStates.computeIfAbsent(player.getUUID(), k -> new ChaosPlayerState());
        SlotState slot = state.getSlot(slotIndex);
        
        if (slot.isActive(currentTick)) {
            // Slot is active - try to use the ability
            return useSlotAbility(player, slotIndex, state, slot, currentTick);
        } else {
            // Slot is inactive - roll new ability and passive
            rollNewSlot(player, slotIndex, state, currentTick);
            return true;
        }
    }

    /**
     * Returns true if the player currently has an active Chaos slot with the given ability.
     */
    public static boolean hasActiveAbility(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return false;
        }
        ChaosPlayerState state = playerStates.get(player.getUUID());
        if (state == null) {
            return false;
        }
        long currentTick = player.level().getGameTime();
        for (int i = 0; i < slotCount(); i++) {
            SlotState slot = state.getSlot(i);
            if (abilityId.equals(slot.abilityId()) && slot.isActive(currentTick)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean useSlotAbility(ServerPlayer player, int slotIndex, ChaosPlayerState state, SlotState slot, long currentTick) {
        if (GemPlayerState.getEnergy(player) < 2) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return false;
        }
        int cooldownTicks = slotAbilityCooldownTicks(player);
        boolean canUse = slot.isActive(currentTick) && (currentTick - slot.lastAbilityUseTick) >= cooldownTicks;
        if (!canUse) {
            int remaining = (int) Math.max(0, cooldownTicks - (currentTick - slot.lastAbilityUseTick));
            int seconds = (remaining + 19) / 20;
            player.sendSystemMessage(Component.translatable("gems.chaos.slot_on_cooldown", slotIndex + 1, seconds).withStyle(ChatFormatting.RED), true);
            return false;
        }
        
        GemAbility ability = ModAbilities.get(slot.abilityId);
        if (ability == null) {
            player.sendSystemMessage(Component.translatable("gems.chaos.ability_not_found").withStyle(ChatFormatting.RED), true);
            return false;
        }
        
        boolean success = ability.activate(player);
        if (success) {
            // Track mastery progress for Chaos gem
            GemMastery.incrementUsage(player, GemId.CHAOS);
            
            // Track for synergy combos - use the actual ability's gem
            GemId abilityGem = ModAbilities.findGemForAbility(slot.abilityId);
            if (abilityGem != null) {
                SynergyRuntime.onAbilityCast(player, abilityGem, slot.abilityId);
            }
            
            SlotState newSlot = slot.withAbilityUsed(currentTick);
            ChaosPlayerState newState = state.withSlot(slotIndex, newSlot);
            playerStates.put(player.getUUID(), newState);
            syncAllToClient(player, newState, currentTick);
        }
        return success;
    }
    
    private static void rollNewSlot(ServerPlayer player, int slotIndex, ChaosPlayerState state, long currentTick) {
        // Remove old passive if any
        SlotState oldSlot = state.getSlot(slotIndex);
        if (oldSlot.passiveId != null) {
            GemPassive oldPassive = ModPassives.get(oldSlot.passiveId);
            if (oldPassive != null) {
                oldPassive.remove(player);
            }
        }
        
        // Pick random ability and passive
        Identifier newAbilityId = pickRandomAbility(player.getRandom());
        Identifier newPassiveId = pickRandomPassive(player.getRandom());
        
        String abilityName = newAbilityId != null ? getAbilityName(newAbilityId) : "None";
        String passiveName = newPassiveId != null ? getPassiveName(newPassiveId) : "None";
        
        SlotState newSlot = new SlotState(newAbilityId, abilityName, newPassiveId, passiveName, currentTick, 0);
        ChaosPlayerState newState = state.withSlot(slotIndex, newSlot);
        playerStates.put(player.getUUID(), newState);
        
        // Apply new passive
        if (newPassiveId != null) {
            GemPassive passive = ModPassives.get(newPassiveId);
            if (passive != null) {
                passive.apply(player);
            }
        }
        
        syncAllToClient(player, newState, currentTick);
    }

    /**
     * Sync all slot states to the client.
     */
    public static void syncAllToClient(ServerPlayer player, ChaosPlayerState state, long currentTick) {
        List<ChaosSlotPayload.SlotData> slots = new ArrayList<>();
        for (int i = 0; i < slotCount(); i++) {
            SlotState slot = state.getSlot(i);
            slots.add(new ChaosSlotPayload.SlotData(
                slot.abilityName,
                slot.abilityId != null ? slot.abilityId.toString() : "",
                slot.passiveName,
                slot.passiveId != null ? slot.passiveId.toString() : "",
                slot.remainingTicks(currentTick) / 20,
                (int) Math.max(0, slotAbilityCooldownTicks(player) - (currentTick - slot.lastAbilityUseTick)) / 20
            ));
        }
        ServerPlayNetworking.send(player, new ChaosSlotPayload(slots));
    }

    public static boolean reduceCooldowns(ServerPlayer player, float factor) {
        if (factor >= 1.0F) {
            return false;
        }
        ChaosPlayerState state = playerStates.get(player.getUUID());
        if (state == null) {
            return false;
        }
        long now = player.level().getGameTime();
        int cooldownTicks = slotAbilityCooldownTicks(player);
        if (cooldownTicks <= 0) {
            return false;
        }
        boolean changed = false;
        ChaosPlayerState updated = state;
        for (int i = 0; i < slotCount(); i++) {
            SlotState slot = updated.getSlot(i);
            if (!slot.isActive(now)) {
                continue;
            }
            long elapsed = now - slot.lastAbilityUseTick;
            int remaining = (int) Math.max(0, cooldownTicks - elapsed);
            if (remaining <= 0) {
                continue;
            }
            int reducedRemaining = (int) Math.ceil(remaining * factor);
            long newLastUse = now - (cooldownTicks - reducedRemaining);
            if (newLastUse > slot.lastAbilityUseTick) {
                updated = updated.withSlot(i, new SlotState(
                        slot.abilityId,
                        slot.abilityName,
                        slot.passiveId,
                        slot.passiveName,
                        slot.activationTick,
                        newLastUse
                ));
                changed = true;
            }
        }
        if (changed) {
            playerStates.put(player.getUUID(), updated);
            syncAllToClient(player, updated, now);
        }
        return changed;
    }

    private static Identifier pickRandomAbility(net.minecraft.util.RandomSource random) {
        List<Identifier> allAbilities = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.CHAOS || gemId == GemId.VOID || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                for (Identifier id : def.abilities()) {
                    // Skip blacklisted abilities that have dependencies
                    if (!BonusPoolRegistry.isChaosAbilityBlacklisted(id)) {
                        allAbilities.add(id);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        if (allAbilities.isEmpty()) {
            return null;
        }
        return allAbilities.get(random.nextInt(allAbilities.size()));
    }

    private static Identifier pickRandomPassive(net.minecraft.util.RandomSource random) {
        List<Identifier> allPassives = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.CHAOS || gemId == GemId.VOID || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                for (Identifier id : def.passives()) {
                    // Skip blacklisted passives that have dependencies
                    if (!BonusPoolRegistry.isChaosPassiveBlacklisted(id)) {
                        allPassives.add(id);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        if (allPassives.isEmpty()) {
            return null;
        }
        return allPassives.get(random.nextInt(allPassives.size()));
    }

    private static String getAbilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability != null ? ability.name() : id.getPath();
    }

    private static String getPassiveName(Identifier id) {
        GemPassive passive = ModPassives.get(id);
        return passive != null ? passive.name() : id.getPath();
    }

    /**
     * Get the current state for a player.
     */
    public static ChaosPlayerState getState(UUID playerUuid) {
        return playerStates.getOrDefault(playerUuid, new ChaosPlayerState());
    }

    /**
     * Clear all state when player switches away from Chaos gem.
     */
    public static void clearState(ServerPlayer player) {
        ChaosPlayerState state = playerStates.remove(player.getUUID());
        if (state != null) {
            for (SlotState slot : state.slots()) {
                if (slot.passiveId != null) {
                    GemPassive passive = ModPassives.get(slot.passiveId);
                    if (passive != null) {
                        passive.remove(player);
                    }
                }
            }
        }
    }
}
