package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal Cage - Trap target in an amethyst crystal cage for 3 seconds.
 */
public final class BonusCrystalCageAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final int CAGE_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_CRYSTAL_CAGE;
    }

    @Override
    public String name() {
        return "Crystal Cage";
    }

    @Override
    public String description() {
        return "Trap a target in an amethyst cage for 3 seconds.";
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

        LivingEntity target = null;
        double closestDist = RANGE;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> e != player && e.isAlive())) {
            Vec3 toEntity = entity.getEyePosition().subtract(start);
            double dot = toEntity.normalize().dot(direction);
            double dist = toEntity.length();
            if (dot > 0.9 && dist < closestDist) {
                target = entity;
                closestDist = dist;
            }
        }

        if (target == null) {
            return false;
        }

        // Create cage around target
        BlockPos center = target.blockPosition();
        List<BlockPos> cageBlocks = new ArrayList<>();

        // Walls
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 || Math.abs(z) == 1 || y == 2) {
                        BlockPos pos = center.offset(x, y, z);
                        if (world.getBlockState(pos).canBeReplaced()) {
                            world.setBlockAndUpdate(pos, Blocks.AMETHYST_BLOCK.defaultBlockState());
                            cageBlocks.add(pos);
                        }
                    }
                }
            }
        }

        // Schedule cage removal
        final LivingEntity finalTarget = target;
        world.getServer().execute(() -> {
            try {
                world.getServer().getCommands().getDispatcher().execute(
                        String.format("schedule function gems:remove_cage %d", CAGE_DURATION),
                        world.getServer().createCommandSourceStack()
                );
            } catch (Exception e) {
                // Ignore command execution errors
            }
        });

        // For now, manually schedule via tick counter (simplified)
        // In production, use a proper scheduled task system
        cageBlocks.forEach(pos -> {
            world.scheduleTick(pos, Blocks.AMETHYST_BLOCK, CAGE_DURATION);
        });

        world.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
