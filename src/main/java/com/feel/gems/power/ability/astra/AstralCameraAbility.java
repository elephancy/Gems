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



public final class AstralCameraAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ASTRAL_CAMERA;
    }

    @Override
    public String name() {
        return "Astral Camera";
    }

    @Override
    public String description() {
        return "Astral camera: scout in Spectator mode, then return to your original position.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().astralCameraCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().astralCameraDurationTicks());
        AbilityRuntime.startAstralCamera(player, duration);
        AbilityFeedback.sound(player, SoundEvents.SPYGLASS_USE, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.END_ROD, 18, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.astra.camera.active"), true);
        return true;
    }
}
