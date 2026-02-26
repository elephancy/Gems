package com.feel.gems.event;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.core.GemId;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.ExperienceBladeItem;
import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.legendary.LegendaryDuels;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.gem.hunter.HunterTrophyHunterRuntime;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.terror.TerrorBloodPrice;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.ability.sentinel.SentinelInterventionRuntime;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.stats.GemsStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

public final class GemsPlayerDeath {
    private static final List<Consumer<ServerPlayer>> DEATH_HEAD_CALLBACKS = new ArrayList<>();

    private GemsPlayerDeath() {
    }

    /** Optional addons can record their own death context without coupling Gems to them. */
    public static void registerDeathHeadCallback(Consumer<ServerPlayer> callback) {
        DEATH_HEAD_CALLBACKS.add(Objects.requireNonNull(callback));
    }

    public static void onDeathHead(ServerPlayer victim, @Nullable DamageSource source) {
        if (victim.level().isClientSide()) {
            return;
        }
        for (Consumer<ServerPlayer> callback : DEATH_HEAD_CALLBACKS) {
            callback.accept(victim);
        }
        ExperienceBladeItem.clearOnDeath(victim);
        SummonerSummons.discardAll(victim);
        GemKeepOnDeath.stash(victim);
    }

    public static void onDeathTail(ServerPlayer victim, DamageSource source) {
        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);
        SpySystem.incrementDeaths(victim);
        com.feel.gems.power.bonus.BonusPassiveRuntime.resetSecondWind(victim);
        boolean skipHeartDrop = GemOwnership.consumeSkipHeartDrop(victim);
        GemId victimActiveGem = GemPlayerState.getActiveGem(victim);

        boolean victimWasAssassin = AssassinState.isAssassin(victim);
        int victimEnergyBefore = GemPlayerState.getEnergy(victim);
        GemPlayerState.addEnergy(victim, -1);

        int victimHeartsBefore = GemPlayerState.getMaxHearts(victim);
        int assassinTriggerHearts = Math.max(GemPlayerState.minMaxHearts(), com.feel.gems.config.GemsBalance.v().systems().assassinTriggerHearts());
        boolean victimAtAssassinTrigger = !victimWasAssassin && victimHeartsBefore <= assassinTriggerHearts;

        ServerPlayer killer = resolveKiller(victim, source);
        if (killer != null && killer != victim) {
            GemPlayerState.initIfNeeded(killer);
            AssassinState.initIfNeeded(killer);
            LeaderboardTracker.incrementKills(killer);
            boolean killerWasAssassin = AssassinState.isAssassin(killer);
            int killerEnergyBefore = GemPlayerState.getEnergy(killer);
            if (victimEnergyBefore > 0) {
                GemPlayerState.addEnergy(killer, 1);
            }
            GemPowers.sync(killer);
            GemItemGlint.sync(killer);
            GemStateSync.send(killer);

            boolean finalKill = victimAtAssassinTrigger;
            AssassinState.recordKill(killer, finalKill, victimWasAssassin);

            GemsStats.recordPlayerKill(killer, victim, killerWasAssassin, victimWasAssassin, finalKill);
            BountyBoard.handleKill(victim, killer, 0, 0);

            if (GemPowers.isPassiveActive(killer, PowerIds.TERROR_BLOOD_PRICE)) {
                TerrorBloodPrice.onPlayerKill(killer);
            }

            // Hunter Trophy Hunter passive - gain a random passive from the victim
            if (GemPowers.isPassiveActive(killer, PowerIds.HUNTER_TROPHY_HUNTER)) {
                HunterTrophyHunterRuntime.onPlayerKill(killer, victim);
            }

            // Hunter's Trophy Necklace legendary - permanently gain a random passive
            HuntersTrophyNecklaceItem.onKillPlayer(killer, victim);

            // Using another player's active gem should never let you keep re-killing them.
            GemOwnership.removeOwnedGemFromInventory(killer, victim.getUUID(), victimActiveGem);

            if (victimWasAssassin && killerWasAssassin) {
                var cfg = com.feel.gems.config.GemsBalance.v().systems();
                int loss = cfg.assassinVsAssassinVictimHeartsLoss();
                int gain = cfg.assassinVsAssassinKillerHeartsGain();

                // Assassin-vs-Assassin: killer takes the victim's accumulated assassin points.
                AssassinState.transferAssassinPoints(victim, killer);
                if (AssassinState.maybeUnlockChoice(killer)) {
                    AssassinState.sendChoicePrompt(killer);
                }

                int after = AssassinState.addAssassinHearts(victim, -loss);
                int minHearts = Math.min(5, AssassinState.maxHearts());
                if (after < minHearts) {
                    after = AssassinState.setAssassinHearts(victim, minHearts);
                }
                if (gain > 0) {
                    AssassinState.addAssassinHearts(killer, gain);
                }
                if (AssassinState.isEliminatedByHearts(after)) {
                    AssassinState.setEliminated(victim, true);
                }
            }

            AssassinTeams.sync(victim.level().getServer(), killer);

            if (killerEnergyBefore >= GemPlayerState.MAX_ENERGY && victimEnergyBefore > 0) {
                victim.spawnAtLocation(victim.level(), new ItemStack(ModItems.ENERGY_UPGRADE));
            }
        }

        GemsStats.recordPlayerDeath(victim, killer, victimWasAssassin, victimAtAssassinTrigger);

        if (!victimWasAssassin) {
            if (victimAtAssassinTrigger) {
                AssassinState.becomeAssassin(victim);
            } else if (!skipHeartDrop && victimHeartsBefore > GemPlayerState.minMaxHearts()) {
                GemPlayerState.setMaxHearts(victim, victimHeartsBefore - 1);
                ItemStack heart = new ItemStack(ModItems.HEART);
                AbilityRuntime.setOwnerWithName(heart, victim.getUUID(), victim.getName().getString());
                victim.spawnAtLocation(victim.level(), heart);
            }
        }

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victim.getGameProfile()));
        victim.spawnAtLocation(victim.level(), head);

        GemPlayerState.applyMaxHearts(victim);
        GemPowers.sync(victim);
        GemItemGlint.sync(victim);
        GemStateSync.send(victim);
        AssassinTeams.sync(victim.level().getServer(), victim);

        // Challenger's Gauntlet duels: return participants, transfer drops, and clean up arenas.
        LegendaryDuels.onDuelParticipantDeathTail(victim, source);

        // Mirror Match: clear duel state without teleporting the loser back
        com.feel.gems.power.ability.duelist.DuelistMirrorMatchRuntime.onDeath(victim);

        // Sentinel intervention should not persist through death.
        SentinelInterventionRuntime.cleanup(victim.level().getServer(), victim);
    }

    @Nullable
    private static ServerPlayer resolveKiller(ServerPlayer victim, @Nullable DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof ServerPlayer player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        Entity last = victim.getLastHurtByMob();
        if (last instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
