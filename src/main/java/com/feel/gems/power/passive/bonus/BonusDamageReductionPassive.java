package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Damage Reduction - Permanent resistance effect.
 */
public final class BonusDamageReductionPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DAMAGE_REDUCTION;
    }

    @Override
    public String name() {
        return "Damage Reduction";
    }

    @Override
    public String description() {
        return "Permanent Resistance I effect (20% damage reduction).";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.RESISTANCE);
    }
}
