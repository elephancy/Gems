package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;



public final class ReaperScytheSweepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_SCYTHE_SWEEP;
    }

    @Override
    public String name() {
        return "Scythe Sweep";
    }

    @Override
    public String description() {
        return "Cleave enemies in an arc in front of you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().scytheSweepCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        int range = GemsBalance.v().reaper().scytheSweepRangeBlocks();
        int arc = GemsBalance.v().reaper().scytheSweepArcDegrees();
        float damage = GemsBalance.v().reaper().scytheSweepDamage();
        double knockback = GemsBalance.v().reaper().scytheSweepKnockback();
        if (range <= 0 || arc <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.scythe_sweep.disabled"), true);
            return false;
        }

        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double cos = Math.cos(Math.toRadians(arc / 2.0D));

        AABB box = player.getBoundingBox().inflate(range, 2.0D, range);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
        int hit = 0;
        for (LivingEntity living : candidates) {
            Vec3 to = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            double dist = to.length();
            if (dist <= 0.001D || dist > range) {
                continue;
            }
            Vec3 dir = to.normalize();
            if (dir.dot(look) < cos) {
                continue;
            }
            if (living instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (living instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            if (damage > 0.0F) {
                living.hurtServer(world, player.damageSources().playerAttack(player), damage);
            }
            if (knockback > 0.0D) {
                Vec3 kb = dir.scale(knockback);
                living.setDeltaMovement(living.getDeltaMovement().add(kb.x, 0.15D, kb.z));
                living.needsSync = true;
            }
            AbilityFeedback.burstAt(world, living.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL, 3, 0.12D);
            hit++;
        }

        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.7F);
        AbilityFeedback.burstAt(world, origin, ParticleTypes.SWEEP_ATTACK, 1, 0.0D);
        player.sendSystemMessage(Component.translatable("gems.ability.reaper.scythe_sweep.hit", hit), true);
        return hit > 0;
    }
}
