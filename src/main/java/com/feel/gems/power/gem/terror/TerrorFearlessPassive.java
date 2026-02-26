package com.feel.gems.power.gem.terror;

import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;


public final class TerrorFearlessPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_FEARLESS;
    }

    @Override
    public String name() {
        return "Fearless";
    }

    @Override
    public String description() {
        return "Cleanses Blindness and Darkness from the holder.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayer player) {
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
    }

    @Override
    public void remove(ServerPlayer player) {
        // No-op.
    }
}

