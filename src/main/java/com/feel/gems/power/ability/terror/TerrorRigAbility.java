package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;


public final class TerrorRigAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_RIG;
    }

    @Override
    public String name() {
        return "Rig";
    }

    @Override
    public String description() {
        return "Rig a block to explode when an enemy steps on or uses it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().rigCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().terror().rigRangeBlocks();
        HitResult hit = player.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.sendSystemMessage(Component.translatable("gems.message.no_block_target"), true);
            return false;
        }
        if (!TerrorRigRuntime.arm(player, blockHit.getBlockPos())) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.rig.cannot_rig"), true);
            return false;
        }

        AbilityFeedback.burstAt(player.level(), blockHit.getLocation(), ParticleTypes.SMOKE, 10, 0.15D);
        AbilityFeedback.sound(player, SoundEvents.TNT_PRIMED, 0.7F, 1.2F);
        player.sendSystemMessage(Component.translatable("gems.ability.terror.rig.rigged"), true);
        return true;
    }
}
