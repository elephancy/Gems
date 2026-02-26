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
 * Soul Link - Link with target, sharing damage taken.
 * Target takes 50% of damage dealt to you for 10 seconds.
 */
public final class BonusSoulLinkAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final int DURATION = 200; // 10 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SOUL_LINK;
    }

    @Override
    public String name() {
        return "Soul Link";
    }

    @Override
    public String description() {
        return "Link with a target. They take 50% of damage you receive for 10s.";
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
        Vec3 end = start.add(direction.scale(RANGE));

        LivingEntity target = null;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(start);
            double dot = toEntity.normalize().dot(direction);
            if (dot > 0.9 && toEntity.length() < RANGE) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        // Mark both with glowing to show the link
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION, 0, false, false, true), player);
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION, 0, false, false, true), player);

        // Store link data (implementation via event handler checks for this effect combo)
        // The actual damage sharing is handled in BonusPassiveHandler

        world.sendParticles(ParticleTypes.ENCHANT,
                (player.getX() + target.getX()) / 2,
                (player.getY() + target.getY()) / 2 + 1,
                (player.getZ() + target.getZ()) / 2,
                30, 1, 1, 1, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}
