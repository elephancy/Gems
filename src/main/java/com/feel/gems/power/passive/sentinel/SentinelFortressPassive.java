package com.feel.gems.power.passive.sentinel;

import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.gem.sentinel.SentinelPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Fortress: While standing still for 2 seconds, gain Resistance II.
 */
public final class SentinelFortressPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_FORTRESS;
    }

    @Override
    public String name() {
        return "Fortress";
    }

    @Override
    public String description() {
        return "While standing still for 2 seconds, gain Resistance II.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.RESISTANCE);
        SentinelPassiveRuntime.clearFortress(player);
    }

    @Override
    public void maintain(ServerPlayer player) {
        SentinelPassiveRuntime.tickFortress(player);
    }
}
