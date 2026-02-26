package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Banishment - Teleport enemy far away randomly.
 */
public final class BonusBanishmentAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final double BANISH_DISTANCE = 50.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BANISHMENT;
    }

    @Override
    public String name() {
        return "Banishment";
    }

    @Override
    public String description() {
        return "Teleport target enemy 50 blocks away in a random direction.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        LivingEntity target = null;
        double bestAimDistance = Double.POSITIVE_INFINITY;
        double bestForwardDistance = Double.POSITIVE_INFINITY;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            Vec3 toEntity = entity.getEyePosition().subtract(start);
            double forwardDistance = toEntity.dot(direction);
            double distanceSquared = toEntity.lengthSqr();
            if (forwardDistance <= 0.0D || distanceSquared >= RANGE * RANGE
                    || forwardDistance * forwardDistance <= 0.81D * distanceSquared) {
                continue;
            }
            double aimDistance = distanceSquared - forwardDistance * forwardDistance;
            if (aimDistance < bestAimDistance
                    || (aimDistance == bestAimDistance && forwardDistance < bestForwardDistance)) {
                target = entity;
                bestAimDistance = aimDistance;
                bestForwardDistance = forwardDistance;
            }
        }

        if (target == null) {
            return false;
        }

        // Random direction teleport
        double angle = world.getRandom().nextDouble() * Math.PI * 2;
        double offsetX = Math.cos(angle) * BANISH_DISTANCE;
        double offsetZ = Math.sin(angle) * BANISH_DISTANCE;

        Vec3 banishPos = target.position().add(offsetX, 0, offsetZ);

        // Particles at original location
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                30, 0.3, 0.5, 0.3, 0.1);

        // Teleport
        target.teleportTo(world, banishPos.x, banishPos.y, banishPos.z, Set.of(), target.getYRot(), target.getXRot(), false);

        // Particles at new location
        world.sendParticles(ParticleTypes.PORTAL, banishPos.x, banishPos.y + 1, banishPos.z,
                30, 0.3, 0.5, 0.3, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
