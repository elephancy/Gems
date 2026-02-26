package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
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
import net.minecraft.world.phys.AABB;

/**
 * Smoke Screen - Create a smoke cloud granting invisibility inside.
 */
public final class BonusSmokeScreenAbility implements GemAbility {
    private static final double RADIUS = 5.0;
    private static final int DURATION = 100; // 5 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SMOKE_SCREEN;
    }

    @Override
    public String name() {
        return "Smoke Screen";
    }

    @Override
    public String description() {
        return "Create a smoke cloud that grants invisibility for 5 seconds.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;

        // Grant invisibility
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, 0, false, false, true));

        // Create smoke particles
        for (int i = 0; i < 100; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * RADIUS * 2;
            double offsetY = world.getRandom().nextDouble() * 3;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * RADIUS * 2;
            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                    1, 0.1, 0.1, 0.1, 0.01);
        }

        // Also give nearby allies invisibility
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<ServerPlayer> allies = world.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p != player && !p.isCreative());
        for (ServerPlayer ally : allies) {
            if (VoidImmunity.shouldBlockEffect(player, ally)) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION / 2, 0, false, false, true));
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
