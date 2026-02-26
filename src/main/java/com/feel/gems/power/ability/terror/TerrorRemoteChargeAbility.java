package com.feel.gems.power.ability.terror;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.terror.TerrorRemoteChargeRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public final class TerrorRemoteChargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_REMOTE_CHARGE;
    }

    @Override
    public String name() {
        return "Remote Charge";
    }

    @Override
    public String description() {
        return "Arm a remote charge on a block, then detonate it from anywhere.";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (TerrorRemoteChargeRuntime.detonate(player)) {
            AbilityFeedback.burst(player, ParticleTypes.SMOKE, 10, 0.2D);
            AbilityFeedback.sound(player, SoundEvents.TNT_PRIMED, 0.8F, 0.9F);
            player.sendSystemMessage(Component.translatable("gems.ability.terror.remote_charge.detonated"), true);
            return true;
        }

        if (TerrorRemoteChargeRuntime.hasActiveCharge(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.remote_charge.already_armed"), true);
            return false;
        }

        // Skip cooldown check if admin no-cooldowns mode is enabled
        if (!GemsAdmin.noCooldowns(player) && TerrorRemoteChargeRuntime.isOnCooldown(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.remote_charge.on_cooldown"), true);
            return false;
        }

        if (!TerrorRemoteChargeRuntime.startArming(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.remote_charge.unavailable"), true);
            return false;
        }

        int windowSeconds = GemsBalance.v().terror().remoteChargeArmWindowTicks() / 20;
        player.sendSystemMessage(Component.translatable("gems.ability.terror.remote_charge.arm_prompt", windowSeconds), true);
        return true;
    }
}
