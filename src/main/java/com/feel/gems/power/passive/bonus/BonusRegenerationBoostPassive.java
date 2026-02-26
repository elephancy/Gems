package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Regeneration Boost - Enhanced natural regeneration.
 */
public final class BonusRegenerationBoostPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_REGENERATION_BOOST;
    }

    @Override
    public String name() {
        return "Regeneration Boost";
    }

    @Override
    public String description() {
        return "Permanent Regeneration I effect.";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.REGENERATION);
    }
}
