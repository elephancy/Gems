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
import com.feel.gems.util.GemsTeleport;

/**
 * Soul Swap - Swap positions with target player/mob.
 */
public final class BonusSoulSwapAbility implements GemAbility {
    private static final double RANGE = 25.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SOUL_SWAP;
    }

    @Override
    public String name() {
        return "Soul Swap";
    }

    @Override
    public String description() {
        return "Swap positions with a target within 25 blocks.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        LivingEntity target = null;
        double bestDot = 0.75;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            Vec3 toEntity = entity.getEyePosition().subtract(start);
            double dist = toEntity.length();
            if (dist > RANGE || dist <= 0.0D) {
                continue;
            }
            double dot = toEntity.normalize().dot(direction);
            if (dot < bestDot) {
                continue;
            }
            if (dot > bestDot || dist < bestDist) {
                bestDot = dot;
                bestDist = dist;
                target = entity;
            }
        }

        if (target == null) {
            return false;
        }

        Vec3 playerPos = player.position();
        Vec3 targetPos = target.position();
        float playerYaw = player.getYRot();
        float targetYaw = target.getYRot();

        // Particles at both locations before swap
        world.sendParticles(ParticleTypes.SOUL, playerPos.x, playerPos.y + 1, playerPos.z,
                20, 0.3, 0.5, 0.3, 0.05);
        world.sendParticles(ParticleTypes.SOUL, targetPos.x, targetPos.y + 1, targetPos.z,
                20, 0.3, 0.5, 0.3, 0.05);

        // Swap positions
        GemsTeleport.teleport(player, world, targetPos.x, targetPos.y, targetPos.z, playerYaw, player.getXRot());
        target.teleportTo(world, playerPos.x, playerPos.y, playerPos.z, Set.of(), targetYaw, target.getXRot(), false);

        // Particles after swap
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
