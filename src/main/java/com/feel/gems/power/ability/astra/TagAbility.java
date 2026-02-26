package com.feel.gems.power.ability.astra;

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



public final class TagAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TAG;
    }

    @Override
    public String name() {
        return "Tag";
    }

    @Override
    public String description() {
        return "Marks a target so they glow through walls for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().tagCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().astra().tagRangeBlocks());
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
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().tagDurationTicks());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, true, false, false));
        AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_PLING, 0.8F, 1.6F);
        AbilityFeedback.burstAt(player.level(), target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.GLOW, 14, 0.2D);
        player.sendSystemMessage(Component.translatable("gems.ability.astra.tag.tagged", target.getName()), true);
        return true;
    }
}
