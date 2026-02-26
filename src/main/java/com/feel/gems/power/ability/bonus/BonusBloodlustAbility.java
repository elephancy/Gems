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

/**
 * Bloodlust - Gain attack speed based on nearby enemies.
 */
public final class BonusBloodlustAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final int DURATION = 200; // 10 seconds
    private static final int MAX_AMPLIFIER = 4; // Haste V max

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLOODLUST;
    }

    @Override
    public String name() {
        return "Bloodlust";
    }

    @Override
    public String description() {
        return "Gain attack speed based on nearby enemies (up to Haste V).";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        AABB box = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer p && p.isCreative()));

        int enemyCount = 0;
        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayer otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            enemyCount++;
        }
        if (enemyCount == 0) {
            return false;
        }

        int amplifier = Math.min(enemyCount - 1, MAX_AMPLIFIER);
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, DURATION, amplifier, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, DURATION, Math.min(amplifier / 2, 1), false, true, true));

        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(),
                10 + enemyCount * 2, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8f, 1.5f);
        return true;
    }
}
