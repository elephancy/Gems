package com.feel.gems.power.gem.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.feel.gems.power.gem.voidgem.VoidImmunity;


public final class SpyBackstab {
    private SpyBackstab() {
    }

    public static void apply(ServerPlayer attacker, LivingEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.SPY_BACKSTEP)) {
            return;
        }
        if (target instanceof ServerPlayer other && VoidImmunity.shouldBlockEffect(attacker, other)) {
            return;
        }
        if (target instanceof ServerPlayer other && GemTrust.isTrusted(attacker, other)) {
            return;
        }
        float bonus = GemsBalance.v().spy().backstabBonusDamage();
        if (bonus <= 0.0F) {
            return;
        }

        Vec3 toAttacker = attacker.position().subtract(target.position());
        if (toAttacker.lengthSqr() <= 1.0E-4D) {
            return;
        }
        Vec3 targetLook = target.getViewVector(1.0F);
        if (targetLook.lengthSqr() <= 1.0E-4D) {
            return;
        }
        double dot = targetLook.normalize().dot(toAttacker.normalize());
        double cone = Math.cos(Math.toRadians(GemsBalance.v().spy().backstabAngleDegrees()));
        if (dot > -cone) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel world)) {
            return;
        }
        target.hurtServer(world, attacker.damageSources().playerAttack(attacker), bonus);
        AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 10, 0.2D);
        AbilityFeedback.sound(attacker, SoundEvents.PLAYER_ATTACK_CRIT, 0.7F, 1.1F);
    }
}
