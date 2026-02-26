package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class BonusEarthshatterAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_EARTHSHATTER;
    }

    @Override
    public String name() {
        return "Earthshatter";
    }

    @Override
    public String description() {
        return "Slam the ground, damaging and launching nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        AABB area = player.getBoundingBox().inflate(6);
        
        world.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 20, 3, 0.5, 3, 0.1);
        
        world.getEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        living.hurtServer(world, player.damageSources().playerAttack(player), 8.0f);
                        living.setDeltaMovement(living.getDeltaMovement().add(0, 0.8, 0));
                        living.needsSync = true;
                    }
                });
        return true;
    }
}
