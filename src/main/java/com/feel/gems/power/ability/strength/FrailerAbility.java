package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;


public final class FrailerAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FRAILER;
    }

    @Override
    public String name() {
        return "Frailer";
    }

    @Override
    public String description() {
        return "Applies Weakness to a targeted enemy.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().frailerCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var target = Targeting.raycastLiving(player, GemsBalance.v().strength().frailerRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_trusted"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }

        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.STRENGTH, GemsBalance.v().strength().frailerDurationTicks());
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        AbilityFeedback.sound(player, SoundEvents.WITHER_SHOOT, 0.7F, 1.3F);
        if (player.level() instanceof ServerLevel world) {
            AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ASH, 14, 0.25D);
        }
        player.sendSystemMessage(Component.translatable("gems.ability.strength.frailer.weakened", target.getName().getString()), true);
        return true;
    }
}
