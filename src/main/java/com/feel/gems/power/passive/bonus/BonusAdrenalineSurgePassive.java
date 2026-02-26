package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Adrenaline Surge - Boost movement and attack speed.
 */
public final class BonusAdrenalineSurgePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ADRENALINE_SURGE;
    }

    @Override
    public String name() {
        return "Adrenaline Surge";
    }

    @Override
    public String description() {
        return "Permanent Speed I and Haste I effects for enhanced combat.";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, Integer.MAX_VALUE, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.HASTE);
    }
}
