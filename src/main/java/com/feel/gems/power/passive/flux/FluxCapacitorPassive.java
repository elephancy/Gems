package com.feel.gems.power.passive.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FluxCapacitorPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_CAPACITOR;
    }

    @Override
    public String name() {
        return "Flux Capacitor";
    }

    @Override
    public String description() {
        return "At high charge, gain a temporary absorption shield.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.ABSORPTION);
    }

    @Override
    public void maintain(ServerPlayer player) {
        var cfg = GemsBalance.v().flux();
        if (FluxCharge.get(player) < cfg.fluxCapacitorChargeThreshold()) {
            return;
        }
        int amplifier = cfg.fluxCapacitorAbsorptionAmplifier();
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, amplifier, true, false, false));
    }
}
