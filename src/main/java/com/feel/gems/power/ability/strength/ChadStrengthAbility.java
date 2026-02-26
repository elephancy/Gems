package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;


public final class ChadStrengthAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.CHAD_STRENGTH;
    }

    @Override
    public String name() {
        return "Chad Strength";
    }

    @Override
    public String description() {
        return "For a short time, every 4th hit deals bonus damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().chadCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.STRENGTH, GemsBalance.v().strength().chadDurationTicks());
        AbilityRuntime.startChadStrength(player, duration);
        AbilityFeedback.sound(player, ModSounds.METAL_PIPE, 1.0F, 1.0F);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.CRIT, 16, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.strength.chad_strength.active"), true);
        return true;
    }
}
