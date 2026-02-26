package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Vortex Strike - Spin attack that pulls enemies closer and damages them.
 */
public final class BonusVortexStrikeAbility implements GemAbility {
    private static final double RANGE = 6.0;
    private static final float DAMAGE = 6.0f;
    private static final double PULL_STRENGTH = 0.8;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VORTEX_STRIKE;
    }

    @Override
    public String name() {
        return "Vortex Strike";
    }

    @Override
    public String description() {
        return "Spin attack that pulls enemies within 6 blocks toward you.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        AABB box = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer p && p.isCreative()));

        if (entities.isEmpty()) {
            return false;
        }

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayer otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            // Deal damage (avoid vanilla melee knockback fighting the pull).
            entity.hurtServer(world, player.damageSources().indirectMagic(player, player), DAMAGE);

            // Pull toward player.
            Vec3 pullDir = player.position().subtract(entity.position()).normalize().scale(PULL_STRENGTH);
            Vec3 vel = entity.getDeltaMovement().add(pullDir.x, 0.1, pullDir.z);
            entity.setDeltaMovement(vel);
            entity.needsSync = true;
        }

        // Spinning particle effect
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i + ring * 0.5;
                double radius = 2 + ring;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                world.sendParticles(ParticleTypes.SWEEP_ATTACK, x, player.getY() + 0.5, z,
                        1, 0, 0, 0, 0);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
