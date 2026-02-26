package com.feel.gems.power.ability.speed;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;


public final class SpeedTempoShiftAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_TEMPO_SHIFT;
    }

    @Override
    public String name() {
        return "Tempo Shift";
    }

    @Override
    public String description() {
        return "Creates a tempo field that accelerates ally cooldowns and slows enemies nearby.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().tempoShiftCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPEED, GemsBalance.v().speed().tempoShiftDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }

        AbilityRuntime.startSpeedTempoShift(player, duration);
        if (player.level() instanceof ServerLevel world) {
            AbilityFeedback.ring(world, player.position().add(0.0D, 0.1D, 0.0D), 3.0D, ParticleTypes.END_ROD, 24);
        }
        AbilityFeedback.sound(player, SoundEvents.TRIDENT_RETURN, 0.8F, 1.6F);
        return true;
    }
}
