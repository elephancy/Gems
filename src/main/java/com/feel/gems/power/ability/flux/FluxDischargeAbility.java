package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.flux.FluxCharge;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FluxDischargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_DISCHARGE;
    }

    @Override
    public String name() {
        return "Flux Discharge";
    }

    @Override
    public String description() {
        return "Dump stored charge in a shockwave that damages and knocks back nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().fluxDischargeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().flux();
        int charge = FluxCharge.get(player);
        int minCharge = cfg.fluxDischargeMinCharge();
        if (charge < minCharge) {
            player.sendSystemMessage(Component.translatable("gems.ability.flux.not_enough_charge"), true);
            return false;
        }

        float damage = Math.min(cfg.fluxDischargeMaxDamage(), cfg.fluxDischargeBaseDamage() + charge * cfg.fluxDischargeDamagePerCharge());
        int radius = cfg.fluxDischargeRadiusBlocks();
        double knockback = cfg.fluxDischargeKnockback();
        ServerLevel world = player.level();
        int hits = 0;
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.hurtServer(world, player.damageSources().indirectMagic(player, player), damage);
            if (knockback > 0.0D) {
                Vec3 delta = other.position().subtract(player.position());
                if (delta.lengthSqr() > 1.0E-4D) {
                    Vec3 norm = delta.normalize();
                    other.push(norm.x * knockback, 0.25D, norm.z * knockback);
                    other.needsSync = true;
                }
            }
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 10, 0.35D);
            hits++;
        }

        FluxCharge.set(player, 0);
        FluxCharge.clearIfBelow100(player);
        GemExtraStateSync.send(player);

        AbilityFeedback.burst(player, ParticleTypes.ELECTRIC_SPARK, 24, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.5F);
        player.sendSystemMessage(Component.translatable("gems.ability.flux.discharge.hit", hits), true);
        return true;
    }
}
