package com.feel.gems.power.ability.puff;

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



public final class DashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DASH;
    }

    @Override
    public String name() {
        return "Dash";
    }

    @Override
    public String description() {
        return "Dashes forward and damages enemies in your path.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().dashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        double dashVel = GemsBalance.v().puff().dashVelocity();
        player.push(dir.x * dashVel, 0.1D, dir.z * dashVel);
        player.needsSync = true;
        AbilityFeedback.syncVelocity(player);
        AbilityFeedback.beam(world, player.position().add(0.0D, 1.0D, 0.0D), player.position().add(dir.scale(3.0D)).add(0.0D, 1.0D, 0.0D), ParticleTypes.CLOUD, 10);

        AABB box = player.getBoundingBox().expandTowards(dir.scale(GemsBalance.v().puff().dashHitRangeBlocks())).inflate(1.0D);
        for (Entity e : world.getEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            ((LivingEntity) e).hurtServer(world, player.damageSources().playerAttack(player), GemsBalance.v().puff().dashDamage());
            AbilityFeedback.burstAt(world, e.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 8, 0.25D);
        }

        AbilityFeedback.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.7F, 1.3F);
        return true;
    }
}
