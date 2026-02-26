package com.feel.gems.power.ability.terror;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;


public final class TerrorTradeAbility implements GemAbility {
    private static final String KEY_USES = "terrorTradeUses";

    @Override
    public Identifier id() {
        return PowerIds.TERROR_TRADE;
    }

    @Override
    public String name() {
        return "Terror Trade";
    }

    @Override
    public String description() {
        return "Sacrifice yourself to attempt to kill a targeted enemy. Target totems will still save players.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().terrorTradeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().terror().terrorTradeRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target == player) {
            return false;
        }

        AssassinState.initIfNeeded(player);

        if (!(target instanceof ServerPlayer victim)) {
            return activateMobTrade(player, target);
        }
        if (!VoidImmunity.canBeTargeted(player, victim)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }
        boolean casterIsAssassin = AssassinState.isAssassin(player);
        if (casterIsAssassin) {
            return activateAssassinTrade(player, victim);
        }
        return activateNormalTrade(player, victim);
    }

    private static boolean activateMobTrade(ServerPlayer player, LivingEntity target) {
        ServerLevel world = player.level();
        AbilityFeedback.beam(world, player.getEyePosition(), target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.TOTEM_USE, 0.7F, 0.9F);

        target.hurtServer(world, target.damageSources().playerAttack(player), 10_000.0F);
        player.kill(world);
        return true;
    }

    private static boolean activateNormalTrade(ServerPlayer player, ServerPlayer victim) {
        ServerLevel world = player.level();
        AbilityFeedback.beam(world, player.getEyePosition(), victim.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(world, victim.position().add(0.0D, 1.0D, 0.0D), ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.TOTEM_USE, 0.7F, 0.9F);

        // Attempt to kill the target (totems may save them).
        // Use a source attributed to the caster so kills count normally (energy, scoring, etc.);
        // Totems can still save the target.
        victim.hurtServer(world, victim.damageSources().playerAttack(player), 10_000.0F);
        if (victim.isAlive()) {
            // Totems can save players; when that happens, apply a configurable penalty (defaults mimic normal death).
            applyTargetPenaltyForNormalTrade(victim);
        }

        // Kill the caster even if they have a totem.
        player.kill(world);
        return true;
    }

    private static void applyTargetPenaltyForNormalTrade(ServerPlayer victim) {
        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);

        int heartsPenalty = GemsBalance.v().terror().terrorTradeNormalTargetHeartsPenalty();
        int energyPenalty = GemsBalance.v().terror().terrorTradeNormalTargetEnergyPenalty();

        if (energyPenalty > 0) {
            GemPlayerState.addEnergy(victim, -energyPenalty);
        }

        if (AssassinState.isAssassin(victim)) {
            int after = heartsPenalty > 0 ? AssassinState.addAssassinHearts(victim, -heartsPenalty) : AssassinState.getAssassinHearts(victim);
            if (AssassinState.isEliminatedByHearts(after)) {
                AssassinState.setEliminated(victim, true);
            }
            GemPlayerState.applyMaxHearts(victim);
            GemPowers.sync(victim);
            GemStateSync.send(victim);
            return;
        }

        if (heartsPenalty > 0) {
            int before = GemPlayerState.getMaxHearts(victim);
            GemPlayerState.setMaxHearts(victim, Math.max(GemPlayerState.minMaxHearts(), before - heartsPenalty));
        }
        GemPlayerState.applyMaxHearts(victim);
        GemPowers.sync(victim);
        GemStateSync.send(victim);
    }

    private static boolean activateAssassinTrade(ServerPlayer player, ServerPlayer victim) {
        int maxUses = GemsBalance.v().terror().terrorTradeMaxUses();
        int used = com.feel.gems.state.PlayerNbt.getInt(player, KEY_USES, 0);
        if (used >= maxUses) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.trade.no_uses", maxUses), true);
            return false;
        }

        // Apply the permanent costs immediately so they persist through the death.
        applyCosts(player);
        com.feel.gems.state.PlayerNbt.putInt(player, KEY_USES, used + 1);

        // Attempt to kill the target (totems may save them).
        // Use a source attributed to the caster so kills count normally (energy, scoring, etc.);
        // Totems can still save the target.
        ServerLevel world = player.level();
        victim.hurtServer(world, victim.damageSources().playerAttack(player), 10_000.0F);
        AbilityFeedback.beam(world, player.getEyePosition(), victim.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(world, victim.position().add(0.0D, 1.0D, 0.0D), ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.TOTEM_USE, 0.7F, 0.9F);

        // Kill the caster even if they have a totem.
        player.kill(world);
        return true;
    }

    private static void applyCosts(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        int heartsCost = GemsBalance.v().terror().terrorTradeHeartsCost();
        int energyPenalty = GemsBalance.v().terror().terrorTradePermanentEnergyPenalty();

        if (AssassinState.isAssassin(player)) {
            int after = AssassinState.addAssassinHearts(player, -heartsCost);
            if (AssassinState.isEliminatedByHearts(after)) {
                AssassinState.setEliminated(player, true);
            }
        } else {
            int before = GemPlayerState.getMaxHearts(player);
            GemPlayerState.setMaxHearts(player, Math.max(GemPlayerState.minMaxHearts(), before - heartsCost));
        }
        GemPlayerState.applyMaxHearts(player);

        GemPlayerState.addEnergyCapPenalty(player, energyPenalty);
        GemPowers.sync(player);
        GemStateSync.send(player);
    }
}

