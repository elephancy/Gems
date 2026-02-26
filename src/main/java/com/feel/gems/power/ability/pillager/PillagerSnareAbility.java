package com.feel.gems.power.ability.pillager;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;



public final class PillagerSnareAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_SNARE;
    }

    @Override
    public String name() {
        return "Snare Shot";
    }

    @Override
    public String description() {
        return "Snare a target you can see with a slowing, glowing mark.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().snareCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().pillager();
        LivingEntity target = Targeting.raycastLiving(player, cfg.snareRangeBlocks());
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

        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.PILLAGER, cfg.snareDurationTicks());
        int slowAmp = cfg.snareSlownessAmplifier();
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.pillager.snare.disabled"), true);
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, true, false, false));
        if (slowAmp >= 0) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, slowAmp, true, false, false));
        }

        AbilityFeedback.burst(player, ParticleTypes.CRIT, 8, 0.25D);
        AbilityFeedback.burstAt(player.level(), target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D), ParticleTypes.CRIT, 10, 0.25D);
        AbilityFeedback.sound(player, SoundEvents.ARROW_SHOOT, 0.9F, 1.2F);
        player.sendSystemMessage(Component.translatable("gems.ability.pillager.snare.landed"), true);
        return true;
    }
}
