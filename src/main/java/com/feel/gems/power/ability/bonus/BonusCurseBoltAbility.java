package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Curse Bolt - Drain life from nearby enemies, healing yourself.
 */
public final class BonusCurseBoltAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CURSE_BOLT;
    }

    @Override
    public String name() {
        return "Curse Bolt";
    }

    @Override
    public String description() {
        return "Drain life from nearby enemies, healing yourself.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        AABB area = player.getBoundingBox().inflate(8);

        float totalDrain = 0.0F;
        for (LivingEntity living : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != player && !(e instanceof ServerPlayer) && e.isAlive()
        )) {
            float before = living.getHealth();
            living.hurtServer(world, player.damageSources().indirectMagic(player, player), 4.0F);
            float dealt = Math.max(0.0F, before - living.getHealth());
            if (dealt > 0.0F) {
                totalDrain += dealt * 0.5F;
            }
            world.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1, living.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }

        if (totalDrain > 0.0F) {
            player.heal(totalDrain);
        }
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.05);
        return true;
    }
}
