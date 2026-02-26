package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Hunger Resist - Permanent saturation effect to slow hunger.
 */
public final class BonusHungerResistPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_HUNGER_RESIST;
    }

    @Override
    public String name() {
        return "Hunger Resist";
    }

    @Override
    public String description() {
        return "Permanent Saturation effect to slow hunger drain.";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.SATURATION);
    }
}
