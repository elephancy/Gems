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

public final class BonusWindSlashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WIND_SLASH;
    }

    @Override
    public String name() {
        return "Wind Slash";
    }

    @Override
    public String description() {
        return "Send a cutting wind projectile in the direction you're facing.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        
        for (int i = 1; i <= 20; i++) {
            Vec3 pos = start.add(direction.scale(i));
            world.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0.02);
            
            AABB hitBox = new AABB(pos.subtract(1, 1, 1), pos.add(1, 1, 1));
            world.getEntities(player, hitBox, e -> e instanceof LivingEntity)
                    .forEach(e -> {
                        if (e instanceof LivingEntity living) {
                            living.hurtServer(world, player.damageSources().playerAttack(player), 6.0f);
                            living.setDeltaMovement(direction.scale(0.5));
                            living.needsSync = true;
                        }
                    });
        }
        return true;
    }
}
