package com.feel.gems.power.ability.air;

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

public final class AirCrosswindAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_CROSSWIND;
    }

    @Override
    public String name() {
        return "Crosswind";
    }

    @Override
    public String description() {
        return "Send a cutting gust forward that knocks back and slows enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().crosswindCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().air();
        int range = cfg.crosswindRangeBlocks();
        int radius = cfg.crosswindRadiusBlocks();
        if (range <= 0 || radius <= 0) {
            player.sendSystemMessage(Component.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }

        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 start = player.position().add(0.0D, 1.0D, 0.0D);
        Vec3 end = start.add(dir.scale(range));
        AABB box = new AABB(start, end).inflate(radius);

        ServerLevel world = player.level();
        float damage = cfg.crosswindDamage();
        double knockback = cfg.crosswindKnockback();
        int slowDuration = AugmentRuntime.applyDurationMultiplier(player, GemId.AIR, cfg.crosswindSlownessDurationTicks());
        int slowAmp = cfg.crosswindSlownessAmplifier();
        int hits = 0;
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.hurtServer(world, player.damageSources().playerAttack(player), damage);
            if (knockback > 0.0D) {
                other.push(dir.x * knockback, 0.1D, dir.z * knockback);
                other.needsSync = true;
            }
            if (slowDuration > 0) {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowDuration, slowAmp, true, false, false));
            }
            hits++;
        }

        AbilityFeedback.ring(world, player.position().add(0.0D, 0.2D, 0.0D), Math.min(range, 12), ParticleTypes.GUST, 24);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.9F, 1.1F);
        player.sendSystemMessage(Component.translatable("gems.ability.air.crosswind.hit", hits), true);
        return true;
    }
}
