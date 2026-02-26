package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.speed.SpeedAutoStepRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Toggle ability that grants auto-step like horses (step up full blocks).
 */
public final class SpeedAutoStepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_AUTO_STEP;
    }

    @Override
    public String name() {
        return "Auto-Step";
    }

    @Override
    public String description() {
        return "Toggle: automatically step up full blocks like a horse.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().autoStepCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        boolean wasActive = SpeedAutoStepRuntime.isActive(player);
        
        if (wasActive) {
            SpeedAutoStepRuntime.setActive(player, false);
            AbilityFeedback.sound(player, SoundEvents.IRON_DOOR_CLOSE, 0.6F, 1.4F);
            player.sendSystemMessage(Component.translatable("gems.ability.speed.auto_step.disabled"), true);
        } else {
            SpeedAutoStepRuntime.setActive(player, true);
            AbilityFeedback.sound(player, SoundEvents.HORSE_GALLOP, 0.7F, 1.2F);
            AbilityFeedback.burst(player, ParticleTypes.CLOUD, 8, 0.3D);
            player.sendSystemMessage(Component.translatable("gems.ability.speed.auto_step.enabled"), true);
        }
        
        return true;
    }
}
