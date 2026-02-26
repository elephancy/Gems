package com.feel.gems.power.runtime;

import java.util.List;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.loadout.LoadoutManager;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.gem.chaos.ChaosSlotRuntime;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.synergy.SynergyRuntime;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.util.GemsTime;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;




public final class GemAbilities {
    private GemAbilities() {
    }

    public static void activateByIndex(ServerPlayer player, int abilityIndex) {
        GemPlayerState.initIfNeeded(player);

        if (AbilityRestrictions.isStunned(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.stunned"), true);
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.suppressed"), true);
            return;
        }

        GemId gemId = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Chaos gem uses independent ability slots and is unlocked by its passive at level 1.
        if (gemId == GemId.CHAOS && abilityIndex >= 0 && abilityIndex < ChaosSlotRuntime.slotCount()) {
            ChaosSlotRuntime.activateSlot(player, abilityIndex);
            return;
        }

        if (energy <= 1) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Prism gem uses selected abilities instead of definition
        if (gemId == GemId.PRISM) {
            activatePrismAbility(player, abilityIndex, energy);
            return;
        }

        List<Identifier> abilities = LoadoutManager.getAbilityOrder(player, gemId);
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= unlocked) {
            player.sendSystemMessage(Component.translatable("gems.ability.not_unlocked"), true);
            return;
        }

        Identifier abilityId = abilities.get(abilityIndex);
        if (GemsDisables.isAbilityDisabledFor(player, abilityId)) {
            player.sendSystemMessage(Component.translatable("gems.ability.disabled_server"), true);
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
                player.sendSystemMessage(Component.translatable("gems.ability.on_cooldown", ability.name(), ticksToSeconds(remainingTicks)), true);
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        GemsStats.recordAbilityUse(player, abilityId);

        // Track mastery progress
        GemMastery.incrementUsage(player, gemId);

        // Track for synergy combos
        SynergyRuntime.onAbilityCast(player, gemId, abilityId);

        com.feel.gems.power.gem.trickster.TricksterPassiveRuntime.applyChaosEffect(player);
        SpySystem.onAbilityUsed(player.level().getServer(), player, abilityId);

        int cooldown = Math.max(0, ability.cooldownTicks());
        cooldown = applyCooldownModifiers(player, cooldown, gemId);
        if (cooldown > 0 && !noCooldowns) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(gemId.ordinal(), abilityIndex, cooldown));
        }
    }

    private static int ticksToSeconds(long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.max(1, (ticks + 19) / 20);
    }

    public static int adjustedCooldownTicks(ServerPlayer player, GemId gemId, int baseTicks) {
        return applyCooldownModifiers(player, baseTicks, gemId);
    }

    /**
     * Activate a Prism gem ability from player's selections.
     */
    private static void activatePrismAbility(ServerPlayer player, int abilityIndex, int energy) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
        List<Identifier> abilities = selection.gemAbilities();

        if (abilities.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Prism abilities unlock progressively like other gems
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= unlocked) {
            player.sendSystemMessage(Component.translatable("gems.ability.not_unlocked"), true);
            return;
        }

        Identifier abilityId = abilities.get(abilityIndex);
        if (GemsDisables.isAbilityDisabledFor(player, abilityId) || 
            GemsDisables.isBonusAbilityDisabledFor(player, abilityId)) {
            player.sendSystemMessage(Component.translatable("gems.ability.disabled_server"), true);
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
                player.sendSystemMessage(Component.translatable("gems.ability.on_cooldown", ability.name(), ticksToSeconds(remainingTicks)), true);
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        GemsStats.recordAbilityUse(player, abilityId);

        // Track mastery progress for Prism
        GemMastery.incrementUsage(player, GemId.PRISM);

        // Track mastery progress for the source gem (if any)
        GemId abilityGem = ModAbilities.findGemForAbility(abilityId);
        if (abilityGem != null && abilityGem != GemId.PRISM) {
            GemMastery.incrementUsage(player, abilityGem);
        }

        // Track for synergy combos - find which gem this ability belongs to
        if (abilityGem != null) {
            SynergyRuntime.onAbilityCast(player, abilityGem, abilityId);
        }

        SpySystem.onAbilityUsed(server, player, abilityId);

        int cooldown = Math.max(0, ability.cooldownTicks());
        GemId cooldownGem = abilityGem != null ? abilityGem : GemId.PRISM;
        cooldown = applyCooldownModifiers(player, cooldown, cooldownGem);
        if (cooldown > 0 && !noCooldowns) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(GemId.PRISM.ordinal(), abilityIndex, cooldown));
        }
    }

    private static int applyCooldownModifiers(ServerPlayer player, int baseTicks, GemId gemId) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player)
                * LegendaryCooldowns.getCooldownMultiplier(player)
                * com.feel.gems.augment.AugmentRuntime.cooldownMultiplier(player, gemId);
        if (com.feel.gems.state.GemPlayerState.getActiveGem(player) == GemId.PRISM && gemId != GemId.PRISM) {
            mult *= com.feel.gems.augment.AugmentRuntime.cooldownMultiplier(player, GemId.PRISM);
        }
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }
}
