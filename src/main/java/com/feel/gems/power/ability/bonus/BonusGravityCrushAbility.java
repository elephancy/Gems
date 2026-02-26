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
 * Gravity Crush - Slam target to ground, rooting and damaging them.
 */
public final class BonusGravityCrushAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 10.0f;
    private static final int ROOT_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_GRAVITY_CRUSH;
    }

    @Override
    public String name() {
        return "Gravity Crush";
    }

    @Override
    public String description() {
        return "Slam a target to the ground, dealing 10 damage and rooting for 3s.";
    }

    @Override
    public int cooldownTicks() {
        return 350; // 17.5 seconds
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

        // Slam target down
        target.setDeltaMovement(0, -2.0, 0);
        target.needsSync = true;

        // Deal damage
        target.hurtServer(world, player.damageSources().indirectMagic(player, player), DAMAGE);

        // Root effect
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ROOT_DURATION, 127, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ROOT_DURATION, 128, false, false, false));

        // Ground impact particles
        for (int i = 0; i < 20; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double radius = world.getRandom().nextDouble() * 2;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            world.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, target.getY() + 0.1, z,
                    1, 0, 0.1, 0, 0.01);
        }

        world.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(),
                1, 0, 0, 0, 0);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
