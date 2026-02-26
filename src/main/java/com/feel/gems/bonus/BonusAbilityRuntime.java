package com.feel.gems.bonus;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsTickScheduler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Runtime for activating claimed bonus abilities.
 * Bonus abilities are cast using LAlt + 5 and LAlt + 6 (slot 0 and 1).
 */
public final class BonusAbilityRuntime {
    /** Ability index offset for bonus abilities in cooldown sync messages. */
    public static final int BONUS_ABILITY_INDEX_OFFSET = 100;

    private static final Map<UUID, PendingBonusCast> PENDING_CASTS = new ConcurrentHashMap<>();

    private BonusAbilityRuntime() {
    }

    /**
     * Activate a bonus ability by slot index (0 or 1).
     */
    public static void activateBySlot(ServerPlayer player, int slotIndex) {
        GemPlayerState.initIfNeeded(player);

        if (AbilityRestrictions.isStunned(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.stunned"), true);
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.suppressed"), true);
            return;
        }

        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendSystemMessage(Component.translatable("gems.bonus.need_energy_use"), true);
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        List<Identifier> abilityList = claims.getPlayerAbilityOrder(player.getUUID());

        if (abilityList.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.bonus.no_abilities_claimed"), true);
            return;
        }

        if (slotIndex < 0 || slotIndex >= abilityList.size()) {
            player.sendSystemMessage(Component.translatable("gems.bonus.invalid_slot"), true);
            return;
        }

        Identifier abilityId = abilityList.get(slotIndex);

        activateByAbility(player, abilityId, slotIndex, true);
    }

    private static void activateByAbility(ServerPlayer player, Identifier abilityId, int slotIndex, boolean allowQueue) {
        if (abilityId == null) {
            return;
        }

        if (GemsDisables.isBonusAbilityDisabledFor(player, abilityId)) {
            player.sendSystemMessage(Component.translatable("gems.bonus.ability_disabled"), true);
            return;
        }

        GemAbility ability = ModAbilities.get(abilityId);
        if (ability == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.not_registered", abilityId.toString()), true);
            return;
        }
        if (AbilityDisables.isDisabled(player, abilityId)) {
            player.sendSystemMessage(Component.translatable("gems.ability.stolen"), true);
            return;
        }

        boolean noCooldowns = GemsAdmin.noCooldowns(player);
        long now = GemsTime.now(player);
        if (!noCooldowns) {
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, abilityId);
            if (nextAllowed > now) {
                long remainingTicks = nextAllowed - now;
                if (allowQueue) {
                    queueBonusCast(player, abilityId, remainingTicks);
                } else {
                    player.sendSystemMessage(Component.translatable("gems.ability.on_cooldown", ability.name(), ticksToSeconds(remainingTicks)), true);
                }
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        clearPendingCast(player.getUUID(), abilityId);

        GemsStats.recordAbilityUse(player, abilityId);

        com.feel.gems.power.gem.trickster.TricksterPassiveRuntime.applyChaosEffect(player);

        int cooldown = Math.max(0, ability.cooldownTicks());
        cooldown = applyCooldownModifiers(player, cooldown);
        if (cooldown > 0 && !noCooldowns) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            // Send both the individual cooldown update and a full sync to ensure client has the data
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(-1, BONUS_ABILITY_INDEX_OFFSET + slotIndex, cooldown));
            // Full sync ensures client has complete bonus ability list for proper cooldown display
            com.feel.gems.net.GemStateSync.sendBonusAbilitiesSync(player);
        }
    }

    private static void queueBonusCast(ServerPlayer player, Identifier abilityId, long remainingTicks) {
        MinecraftServer server = player.level().getServer();
        if (server == null || remainingTicks <= 0) {
            return;
        }

        PendingBonusCast existing = PENDING_CASTS.remove(player.getUUID());
        if (existing != null) {
            existing.handle().cancel();
        }

        int delay = (int) Math.min(Integer.MAX_VALUE, Math.max(1, remainingTicks));
        GemsTickScheduler.Handle handle = GemsTickScheduler.schedule(server, delay, s -> tryActivateQueued(s, player.getUUID(), abilityId));
        PENDING_CASTS.put(player.getUUID(), new PendingBonusCast(abilityId, handle));
        GemAbility ability = ModAbilities.get(abilityId);
        String name = ability != null ? ability.name() : abilityId.toString();
        player.sendSystemMessage(Component.translatable("gems.bonus.queued", name), true);
    }

    private static void tryActivateQueued(MinecraftServer server, UUID playerId, Identifier abilityId) {
        PendingBonusCast pending = PENDING_CASTS.get(playerId);
        if (pending == null || !pending.abilityId().equals(abilityId)) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            PENDING_CASTS.remove(playerId);
            return;
        }

        BonusClaimsState claims = BonusClaimsState.get(server);
        List<Identifier> abilityList = claims.getPlayerAbilityOrder(player.getUUID());
        if (!abilityList.contains(abilityId)) {
            PENDING_CASTS.remove(playerId);
            return;
        }
        int slotIndex = abilityList.indexOf(abilityId);
        if (slotIndex < 0) {
            PENDING_CASTS.remove(playerId);
            return;
        }

        activateByAbility(player, abilityId, slotIndex, false);
    }

    private static void clearPendingCast(UUID playerId, Identifier abilityId) {
        PendingBonusCast pending = PENDING_CASTS.get(playerId);
        if (pending != null && pending.abilityId().equals(abilityId)) {
            pending.handle().cancel();
            PENDING_CASTS.remove(playerId);
        }
    }

    private record PendingBonusCast(Identifier abilityId, GemsTickScheduler.Handle handle) {}

    private static int ticksToSeconds(long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.max(1, (ticks + 19) / 20);
    }

    private static int applyCooldownModifiers(ServerPlayer player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = GemsBalance.v().bonusPool().bonusAbilityCooldownMultiplier();
        mult *= BonusPassiveRuntime.getCooldownMultiplier(player);
        mult *= LegendaryCooldowns.getCooldownMultiplier(player);
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }
}
