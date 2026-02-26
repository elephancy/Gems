package com.feel.gems.power.ability.fire;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;



public final class HeatHazeZoneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEAT_HAZE_ZONE;
    }

    @Override
    public String name() {
        return "Heat Haze Zone";
    }

    @Override
    public String description() {
        return "Heat haze zone: allies gain Fire Resistance; enemies gain Mining Fatigue and Weakness.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().fire().heatHazeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.FIRE, GemsBalance.v().fire().heatHazeDurationTicks());
        AbilityRuntime.startHeatHazeZone(player, duration);
        AbilityFeedback.sound(player, SoundEvents.FIRECHARGE_USE, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.FLAME, 20, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.fire.heat_haze.active"), true);
        return true;
    }
}
