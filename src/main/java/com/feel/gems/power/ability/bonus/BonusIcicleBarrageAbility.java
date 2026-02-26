package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Icicle Barrage - Fire a volley of piercing icicles.
 */
public final class BonusIcicleBarrageAbility implements GemAbility {
    private static final int ICICLE_COUNT = 5;
    private static final double RANGE = 25.0;
    private static final float DAMAGE_PER_ICICLE = 4.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_ICICLE_BARRAGE;
    }

    @Override
    public String name() {
        return "Icicle Barrage";
    }

    @Override
    public String description() {
        return "Fire 5 piercing icicles that each deal 4 damage.";
    }

    @Override
    public int cooldownTicks() {
        return 250; // 12.5 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 baseDirection = player.getLookAngle();

        for (int i = 0; i < ICICLE_COUNT; i++) {
            // Spread icicles in a fan pattern
            double spreadAngle = (i - 2) * 0.1; // -0.2 to 0.2 radians
            Vec3 direction = baseDirection.yRot((float) spreadAngle);

            // Trace icicle path and damage entities
            Vec3 current = start;
            for (int step = 0; step < RANGE; step++) {
                current = current.add(direction);

                // Check for entity hits along the path
                AABB hitBox = new AABB(current.subtract(0.5, 0.5, 0.5), current.add(0.5, 0.5, 0.5));
                List<LivingEntity> hits = world.getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> e != player && e.isAlive());

                for (LivingEntity hit : hits) {
                    hit.hurtServer(world, world.damageSources().freeze(), DAMAGE_PER_ICICLE);
                    hit.setTicksFrozen(hit.getTicksFrozen() + 100);
                }

                // Icicle particle trail
                if (step % 2 == 0) {
                    world.sendParticles(ParticleTypes.SNOWFLAKE, current.x, current.y, current.z,
                            1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
