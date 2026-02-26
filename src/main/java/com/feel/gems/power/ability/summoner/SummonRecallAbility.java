package com.feel.gems.power.ability.summoner;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.feel.gems.power.gem.summoner.SummonerSummons;


public final class SummonRecallAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SUMMON_RECALL;
    }

    @Override
    public String name() {
        return "Recall";
    }

    @Override
    public String description() {
        return "Despawns all of your active summons.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().summoner().recallCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int before = SummonerSummons.pruneAndCount(player);
        if (before <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.summoner.recall.no_summons"), true);
            return false;
        }
        SummonerSummons.discardAll(player);
        SummonerSummons.applyCooldown(player);
        AbilityFeedback.burst(player, net.minecraft.core.particles.ParticleTypes.POOF, 18, 0.35D);
        AbilityFeedback.sound(player, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.7F, 1.2F);
        player.sendSystemMessage(Component.translatable("gems.ability.summoner.recall.recalled"), true);
        return true;
    }
}

