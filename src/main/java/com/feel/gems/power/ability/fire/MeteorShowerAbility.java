package com.feel.gems.power.ability.fire;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.RangeLimitedProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;



public final class MeteorShowerAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.METEOR_SHOWER;
    }

    @Override
    public String name() {
        return "Meteor Shower";
    }

    @Override
    public String description() {
        return "Calls multiple meteors that explode on impact along a targeted line.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().fire().meteorShowerCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        HitResult hit = player.pick(GemsBalance.v().fire().meteorShowerTargetRangeBlocks(), 1.0F, false);
        BlockPos center = BlockPos.containing(hit.getLocation());
        AbilityFeedback.ring(world, new Vec3(center.getX() + 0.5D, center.getY() + 0.2D, center.getZ() + 0.5D), 3.0D, ParticleTypes.FLAME, 24);

        int count = GemsBalance.v().fire().meteorShowerCount();
        int spread = GemsBalance.v().fire().meteorShowerSpreadBlocks();
        int height = GemsBalance.v().fire().meteorShowerHeightBlocks();
        float velocity = GemsBalance.v().fire().meteorShowerVelocity();
        int explosionPower = GemsBalance.v().fire().meteorShowerExplosionPower();
        double lineLength = Math.max(0.0D, spread);
        double halfLength = lineLength / 2.0D;
        double step = count > 1 ? lineLength / (count - 1.0D) : 0.0D;
        Vec3 look = player.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flat = flat.normalize();
        }

        for (int i = 0; i < count; i++) {
            double offset = -halfLength + (step * i);
            Vec3 impact = new Vec3(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D)
                    .add(flat.scale(offset));
            double impactX = impact.x;
            double impactZ = impact.z;
            Vec3 spawn = new Vec3(impactX, center.getY() + height, impactZ);
            Vec3 dir = new Vec3(0.0D, -1.0D, 0.0D);

            // Use zero acceleration (for consistency) and set explicit velocity.
            LargeFireball meteor = new LargeFireball(world, player, Vec3.ZERO, explosionPower);
            meteor.snapTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
            meteor.setDeltaMovement(dir.scale(Math.max(0.1D, velocity)));
            meteor.addTag("gems_meteor");
            if (meteor instanceof RangeLimitedProjectile limited) {
                limited.gems$setRangeLimit(spawn, height + 64.0D);
            }
            world.addFreshEntity(meteor);
        }

        AbilityFeedback.sound(player, SoundEvents.BLAZE_SHOOT, 1.0F, 0.8F);
        return true;
    }
}
