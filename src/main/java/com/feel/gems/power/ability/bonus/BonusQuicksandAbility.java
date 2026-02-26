package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Quicksand - Create a zone that slows and sinks enemies.
 */
public final class BonusQuicksandAbility implements GemAbility {
    private static final double RANGE = 25.0;
    private static final double ZONE_RADIUS = 4.0;
    private static final int DURATION = 100; // 5 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_QUICKSAND;
    }

    @Override
    public String name() {
        return "Quicksand";
    }

    @Override
    public String description() {
        return "Create a quicksand zone that slows and sinks enemies for 5s.";
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

        HitResult hit = world.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 targetCenter = hit.getLocation();
        BlockPos centerPos = BlockPos.containing(targetCenter);

        // Place soul sand blocks temporarily
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = centerPos.offset(x, -1, z);
                if (world.getBlockState(pos).isRedstoneConductor(world, pos)) {
                    // Schedule replacement back (simplified - in production use proper task system)
                    world.setBlockAndUpdate(pos.above(), Blocks.COBWEB.defaultBlockState());
                }
            }
        }

        // Apply slowness to entities in zone
        AABB box = new AABB(centerPos).inflate(ZONE_RADIUS);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION, 3, false, false, true));
        }

        // Particle effect
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * ZONE_RADIUS * 2;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * ZONE_RADIUS * 2;
            world.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    targetCenter.x + offsetX, targetCenter.y + 0.2, targetCenter.z + offsetZ,
                    1, 0, 0, 0, 0);
        }

        world.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
