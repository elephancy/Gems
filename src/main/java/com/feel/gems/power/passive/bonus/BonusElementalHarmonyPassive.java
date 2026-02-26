package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Elemental Harmony - Resistance to fire, ice, and lightning damage.
 */
public final class BonusElementalHarmonyPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ELEMENTAL_HARMONY;
    }

    @Override
    public String name() {
        return "Elemental Harmony";
    }

    @Override
    public String description() {
        return "Take 50% less damage from fire, freezing, and lightning.";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
    }
}
