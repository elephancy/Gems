package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DuelistLungeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DUELIST_LUNGE;
    }

    @Override
    public String name() {
        return "Lunge";
    }

    @Override
    public String description() {
        return "Dash forward with your sword, dealing damage to the first enemy hit.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().lungeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        int distance = GemsBalance.v().duelist().lungeDistanceBlocks();
        float damage = GemsBalance.v().duelist().lungeDamage();

        // Apply velocity
        double vel = distance / 4.0;
        player.setDeltaMovement(dir.x * vel, 0.1D, dir.z * vel);
        player.needsSync = true;
        AbilityFeedback.syncVelocity(player);

        // Find first enemy in path
        AABB box = player.getBoundingBox().expandTowards(dir.scale(distance)).inflate(1.0D);
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;

        for (Entity e : world.getEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            double dist = e.distanceToSqr(player);
            if (dist < closest) {
                closest = dist;
                target = (LivingEntity) e;
            }
        }

        if (target != null) {
            target.hurtServer(world, player.damageSources().playerAttack(player), damage);
            AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SWEEP_ATTACK, 15, 0.3D);
        }

        AbilityFeedback.beam(world, player.position().add(0.0D, 1.0D, 0.0D),
                player.position().add(dir.scale(distance)).add(0.0D, 1.0D, 0.0D),
                ParticleTypes.SWEEP_ATTACK, 10);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
        return true;
    }
}
