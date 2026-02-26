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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tremor - Cause the ground to shake, slowing enemies in area.
 */
public final class BonusTremorAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final int SLOW_DURATION = 80; // 4 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_TREMOR;
    }

    @Override
    public String name() {
        return "Tremor";
    }

    @Override
    public String description() {
        return "Shake the ground, slowing all enemies within 10 blocks.";
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

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayer otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_DURATION, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SLOW_DURATION, 1, false, false, true));

            // Small knockback/stagger
            Vec3 vel = entity.getDeltaMovement();
            double up = Math.max(0.0D, vel.y) + 0.35D;
            entity.setDeltaMovement(vel.x, up, vel.z);
            entity.needsSync = true;
        }

        // Ground crack particles
        for (int i = 0; i < 40; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double radius = world.getRandom().nextDouble() * RANGE;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            world.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, player.getY() + 0.1, z,
                    1, 0, 0.1, 0, 0.01);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
