package com.feel.gems.power.ability.astra;

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



public final class UnboundedAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.UNBOUNDED;
    }

    @Override
    public String name() {
        return "Unbounded";
    }

    @Override
    public String description() {
        return "Briefly grants Spectator mode, then returns you to normal gameplay.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().unboundedCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().unboundedDurationTicks());
        AbilityRuntime.startUnbounded(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.PORTAL, 14, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.astra.unbounded.active"), true);
        return true;
    }
}
