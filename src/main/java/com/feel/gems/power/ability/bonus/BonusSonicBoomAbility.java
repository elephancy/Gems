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
 * Sonic Boom - Emit a shockwave that deafens and staggers enemies.
 */
public final class BonusSonicBoomAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final float DAMAGE = 6.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SONIC_BOOM;
    }

    @Override
    public String name() {
        return "Sonic Boom";
    }

    @Override
    public String description() {
        return "Emit a shockwave that damages, staggers, and deafens enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        AABB box = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            // Damage
            entity.hurtServer(world, world.damageSources().sonicBoom(player), DAMAGE);
            
            // Knockback away from player
            Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(1.5);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
            entity.needsSync = true;

            // Stagger effects
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false, true));
        }

        // Expanding ring particle effect
        for (int ring = 1; ring <= 5; ring++) {
            double radius = ring * 2;
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                world.sendParticles(ParticleTypes.SONIC_BOOM, x, player.getY() + 1, z, 1, 0, 0, 0, 0);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.2f);
        return true;
    }
}
