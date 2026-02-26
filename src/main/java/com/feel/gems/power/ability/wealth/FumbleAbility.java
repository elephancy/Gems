package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.gem.wealth.WealthFumble;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;


public final class FumbleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FUMBLE;
    }

    @Override
    public String name() {
        return "Fumble";
    }

    @Override
    public String description() {
        return "Fumble: enemies cannot use their offhand and cannot eat for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().fumbleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        int radius = GemsBalance.v().wealth().fumbleRadiusBlocks();
        int affected = 0;
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.WEALTH, GemsBalance.v().wealth().fumbleDurationTicks());
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
                if (!VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                WealthFumble.apply(otherPlayer, duration);
                otherPlayer.releaseUsingItem();
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, true, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, false));
            }
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.WITCH_THROW, 0.8F, 1.1F);
        player.sendSystemMessage(Component.translatable("gems.ability.wealth.fumble.affected", affected), true);
        return true;
    }
}
