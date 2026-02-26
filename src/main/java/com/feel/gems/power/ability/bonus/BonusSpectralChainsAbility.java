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
 * Spectral Chains - Root enemies in place with ghostly chains.
 */
public final class BonusSpectralChainsAbility implements GemAbility {
    private static final double RANGE = 8.0;
    private static final int ROOT_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_CHAINS;
    }

    @Override
    public String name() {
        return "Spectral Chains";
    }

    @Override
    public String description() {
        return "Root all enemies within 8 blocks for 3 seconds.";
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
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ROOT_DURATION, 127, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ROOT_DURATION, 128, false, false, false));
            
            // Visual effect
            world.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1, entity.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
