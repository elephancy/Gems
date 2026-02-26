package com.feel.gems.power.passive.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class PuffWindbornePassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.PUFF_WINDBORNE;
    }

    @Override
    public String name() {
        return "Windborne";
    }

    @Override
    public String description() {
        return "While airborne, you gain slow falling.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    @Override
    public void maintain(ServerPlayer player) {
        if (player.onGround()) {
            return;
        }
        var cfg = GemsBalance.v().puff();
        int duration = cfg.windborneDurationTicks();
        int amplifier = cfg.windborneSlowFallingAmplifier();
        if (duration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, amplifier, true, false, false));
        }
    }
}
