package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
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
import net.minecraft.world.phys.Vec3;

public final class PuffGustAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PUFF_GUST;
    }

    @Override
    public String name() {
        return "Gust";
    }

    @Override
    public String description() {
        return "Unleash a gust that launches nearby enemies and slows them.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().gustCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().puff();
        int radius = cfg.gustRadiusBlocks();
        if (radius <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.puff.gust.disabled"), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        double knockback = cfg.gustKnockback();
        double up = cfg.gustUpVelocityY();
        int slownessDuration = AugmentRuntime.applyDurationMultiplier(player, GemId.PUFF, cfg.gustSlownessDurationTicks());
        int slownessAmplifier = cfg.gustSlownessAmplifier();
        int slowFallDuration = AugmentRuntime.applyDurationMultiplier(player, GemId.PUFF, cfg.gustSlowFallingDurationTicks());
        int hits = 0;
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            Vec3 delta = other.position().subtract(player.position());
            if (delta.lengthSqr() > 1.0E-4D) {
                Vec3 norm = delta.normalize();
                other.push(norm.x * knockback, up, norm.z * knockback);
                other.needsSync = true;
            }
            if (slownessDuration > 0) {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slownessDuration, slownessAmplifier, true, false, false));
            }
            if (slowFallDuration > 0) {
                other.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFallDuration, 0, true, false, false));
            }
            hits++;
        }

        if (slowFallDuration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFallDuration, 0, true, false, false));
        }

        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 30, 0.6D);
        AbilityFeedback.sound(player, SoundEvents.PHANTOM_FLAP, 0.8F, 0.9F);
        player.sendSystemMessage(Component.translatable("gems.ability.puff.gust.hit", hits), true);
        return true;
    }
}
