package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Purge - Remove all buffs from target enemy.
 */
public final class BonusPurgeAbility implements GemAbility {
    private static final double RANGE = 15.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PURGE;
    }

    @Override
    public String name() {
        return "Purge";
    }

    @Override
    public String description() {
        return "Remove all positive status effects from a target enemy.";
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

        // Remove all beneficial effects
        List<MobEffectInstance> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().value().isBeneficial()) {
                toRemove.add(effect);
            }
        }

        if (toRemove.isEmpty()) {
            return false;
        }

        for (MobEffectInstance effect : toRemove) {
            target.removeEffect(effect.getEffect());
        }

        // Visual effect
        world.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.1);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
