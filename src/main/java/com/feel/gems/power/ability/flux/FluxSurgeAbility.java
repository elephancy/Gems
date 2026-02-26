package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.particle.ModParticles;
import com.feel.gems.sound.ModSounds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FluxSurgeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_SURGE;
    }

    @Override
    public String name() {
        return "Flux Surge";
    }

    @Override
    public String description() {
        return "Spend charge to gain a short speed/resistance burst and shove nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().fluxSurgeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().flux();
        int charge = FluxCharge.get(player);
        int cost = cfg.fluxSurgeChargeCost();
        if (charge < cost) {
            player.sendSystemMessage(Component.translatable("gems.ability.flux.not_enough_charge"), true);
            return false;
        }

        FluxCharge.set(player, charge - cost);
        FluxCharge.clearIfBelow100(player);
        GemExtraStateSync.send(player);

        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.FLUX, cfg.fluxSurgeDurationTicks());
        if (duration > 0) {
            int speedAmp = cfg.fluxSurgeSpeedAmplifier();
            int resistAmp = cfg.fluxSurgeResistanceAmplifier();
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, speedAmp, true, false, false));
            if (resistAmp >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, resistAmp, true, false, false));
            }
        }

        ServerLevel world = player.level();
        int radius = cfg.fluxSurgeRadiusBlocks();
        if (radius > 0) {
            double knockback = cfg.fluxSurgeKnockback();
            AABB box = new AABB(player.blockPosition()).inflate(radius);
            for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
                if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
                if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                Vec3 delta = other.position().subtract(player.position()).normalize();
                other.push(delta.x * knockback, 0.2D, delta.z * knockback);
                other.needsSync = true;
            }
        }

        AbilityFeedback.burst(player, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 20, 0.45D);
        AbilityFeedback.burst(player, ModParticles.GEM_STAR, 7, 0.4D);
        AbilityFeedback.sound(player, ModSounds.SURGE_PULSE, 0.8F, 1.1F);
        return true;
    }
}
