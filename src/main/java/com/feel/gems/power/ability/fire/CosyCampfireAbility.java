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



public final class CosyCampfireAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.COSY_CAMPFIRE;
    }

    @Override
    public String name() {
        return "Cosy Campfire";
    }

    @Override
    public String description() {
        return "Creates an aura that grants trusted players Regeneration IV.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().fire().cosyCampfireCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.FIRE, GemsBalance.v().fire().cosyCampfireDurationTicks());
        AbilityRuntime.startCosyCampfire(player, duration);
        AbilityFeedback.sound(player, SoundEvents.CAMPFIRE_CRACKLE, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.fire.cosy_campfire.active"), true);
        return true;
    }
}
