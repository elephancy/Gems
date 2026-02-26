package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tidal Wave - Summon a wave that knocks back and slows enemies.
 */
public final class BonusTidalWaveAbility implements GemAbility {
    private static final double RANGE = 12.0;
    private static final double KNOCKBACK = 2.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_TIDAL_WAVE;
    }

    @Override
    public String name() {
        return "Tidal Wave";
    }

    @Override
    public String description() {
        return "Send a tidal wave forward, knocking back and slowing enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 direction = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 waveCenter = player.position().add(direction.scale(RANGE / 2));

        AABB box = new AABB(waveCenter.subtract(RANGE / 2, 3, RANGE / 2),
                         waveCenter.add(RANGE / 2, 3, RANGE / 2));

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            Vec3 knockbackDir = entity.position().subtract(player.position()).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackDir.scale(KNOCKBACK).add(0, 0.5, 0)));
            entity.needsSync = true;
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false, true));
        }

        // Particle wave effect
        for (int i = 0; i < 30; i++) {
            double offset = (i - 15) * 0.4;
            Vec3 particlePos = player.position().add(direction.scale(i * 0.4))
                    .add(direction.yRot((float) Math.PI / 2).scale(offset * 0.3));
            world.sendParticles(ParticleTypes.SPLASH, particlePos.x, particlePos.y + 0.5, particlePos.z,
                    3, 0.3, 0.3, 0.3, 0.1);
            world.sendParticles(ParticleTypes.BUBBLE, particlePos.x, particlePos.y + 0.5, particlePos.z,
                    2, 0.3, 0.3, 0.3, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
