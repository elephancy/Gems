package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;



public final class PillagerRavageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_RAVAGE;
    }

    @Override
    public String name() {
        return "Ravage";
    }

    @Override
    public String description() {
        return "Bashes a target with heavy knockback.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().ravageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().pillager().ravageRangeBlocks();
        if (range <= 0) {
            return false;
        }

        float damage = GemsBalance.v().pillager().ravageDamage();
        double knockback = GemsBalance.v().pillager().ravageKnockback();
        Vec3 forward = player.getViewVector(1.0F).normalize();
        double cosHalfArc = Math.cos(Math.toRadians(55.0D)); // ~110 degree frontal cone

        boolean hit = false;
        Vec3 origin = player.position();
        AABB box = new AABB(origin, origin).inflate(range, 1.5D, range);
        DamageSource src = player.damageSources().playerAttack(player);
        ServerLevel world = player.level();

        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3 to = target.position().subtract(origin);
            double distSq = to.lengthSqr();
            if (distSq > (double) (range * range)) {
                continue;
            }
            Vec3 dir = to.normalize();
            if (forward.dot(dir) < cosHalfArc) {
                continue;
            }
            if (target instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (target instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }

            target.hurtServer(world, src, damage);
            target.push(dir.x * knockback, 0.15D, dir.z * knockback);
            target.needsSync = true;
            hit = true;
        }

        if (!hit) {
            player.sendSystemMessage(Component.translatable("gems.ability.pillager.ravage.no_targets"), true);
            return false;
        }

        AbilityFeedback.burstAt(world, player.position().add(0.0D, 0.8D, 0.0D), ParticleTypes.SWEEP_ATTACK, 4, 0.1D);
        AbilityFeedback.sound(player, SoundEvents.RAVAGER_ATTACK, 0.9F, 1.0F);
        return true;
    }
}

