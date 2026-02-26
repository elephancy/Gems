package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mind Spike - Psychic damage that also reveals enemy location.
 */
public final class BonusMindSpikeAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 6.0f;
    private static final int GLOW_DURATION = 200; // 10 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MIND_SPIKE;
    }

    @Override
    public String name() {
        return "Mind Spike";
    }

    @Override
    public String description() {
        return "Deal 6 psychic damage and reveal target's location for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        LivingEntity target = null;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            Vec3 toEntity = entity.getEyePosition().subtract(start);
            double dot = toEntity.normalize().dot(direction);
            if (dot > 0.9 && toEntity.length() < RANGE) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        // Psychic damage
        target.hurtServer(world, player.damageSources().indirectMagic(player, player), DAMAGE);

        // Apply glowing to reveal location
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0, false, false, true));

        // Brief confusion
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false, true));

        // Visual effect - line from player to target
        Vec3 targetPos = target.getEyePosition();
        Vec3 diff = targetPos.subtract(start);
        for (int i = 0; i < 20; i++) {
            Vec3 particlePos = start.add(diff.scale(i / 20.0));
            world.sendParticles(ParticleTypes.ENCHANT, particlePos.x, particlePos.y, particlePos.z,
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        world.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                15, 0.2, 0.2, 0.2, 0.05);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PHANTOM_HURT, SoundSource.PLAYERS, 1.0f, 2.0f);
        return true;
    }
}
