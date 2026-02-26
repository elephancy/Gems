package com.feel.gems.power.passive.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class StrengthAdrenalinePassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.STRENGTH_ADRENALINE;
    }

    @Override
    public String name() {
        return "Adrenaline";
    }

    @Override
    public String description() {
        return "Gain brief resistance when critically injured.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.RESISTANCE);
    }

    @Override
    public void maintain(ServerPlayer player) {
        var cfg = GemsBalance.v().strength();
        float threshold = cfg.adrenalineThresholdHearts() * 2.0F;
        if (player.getHealth() > threshold) {
            return;
        }
        int duration = cfg.adrenalineDurationTicks();
        int amplifier = cfg.adrenalineResistanceAmplifier();
        if (duration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amplifier, true, false, false));
        }
    }
}
