package com.feel.gems.power.gem.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;


public final class TerrorDreadAuraPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_DREAD_AURA;
    }

    @Override
    public String name() {
        return "Dread Aura";
    }

    @Override
    public String description() {
        return "Untrusted players near you are afflicted with Darkness.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        int radius = GemsBalance.v().terror().dreadAuraRadiusBlocks();
        if (radius <= 0) {
            return;
        }

        int duration = 60;
        int amp = GemsBalance.v().terror().dreadAuraAmplifier();
        for (ServerPlayer other : world.getPlayers(p -> p != player && p.distanceToSqr(player) <= radius * (double) radius)) {
            if (VoidImmunity.shouldBlockEffect(player, other)) {
                continue;
            }
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, amp, true, false, false), player);
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        // No-op: Darkness expires naturally and should not be forcibly removed from others.
    }
}
