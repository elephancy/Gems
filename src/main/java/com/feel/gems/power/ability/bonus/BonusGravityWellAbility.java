package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BonusGravityWellAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_GRAVITY_WELL;
    }

    @Override
    public String name() {
        return "Gravity Well";
    }

    @Override
    public String description() {
        return "Create a gravity well that pulls enemies toward you.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 center = player.position();
        AABB area = player.getBoundingBox().inflate(12);
        
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1, center.z, 100, 6, 2, 6, 0.1);
        
        world.getEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    Vec3 toPlayer = center.subtract(e.position()).normalize().scale(1.5);
                    e.setDeltaMovement(toPlayer);
                    e.needsSync = true;
                });
        return true;
    }
}
