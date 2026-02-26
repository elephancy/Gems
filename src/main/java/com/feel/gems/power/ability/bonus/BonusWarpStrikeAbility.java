package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Warp Strike - Teleport behind target and strike.
 */
public final class BonusWarpStrikeAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 8.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_WARP_STRIKE;
    }

    @Override
    public String name() {
        return "Warp Strike";
    }

    @Override
    public String description() {
        return "Teleport behind a target and deal 8 damage.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(RANGE));

        LivingEntity target = null;
        double nearestHit = RANGE * RANGE;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            var hit = entity.getBoundingBox().inflate(0.25D).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < nearestHit) {
                target = entity;
                nearestHit = distance;
            }
        }

        if (target == null) {
            return false;
        }

        // Calculate position behind target
        Vec3 targetFacing = Vec3.directionFromRotation(0, target.getYRot()).normalize();
        Vec3 behindPos = target.position().subtract(targetFacing.scale(2));

        // Particles at start
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);

        // Teleport behind target
        GemsTeleport.teleport(player, world, behindPos.x, behindPos.y, behindPos.z,
                target.getYRot() + 180, player.getXRot());

        // Deal damage
        target.hurtServer(world, world.damageSources().playerAttack(player), DAMAGE);

        // Particles at end
        world.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + targetFacing.x,
                player.getY() + 1, player.getZ() + targetFacing.z, 1, 0, 0, 0, 0);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
