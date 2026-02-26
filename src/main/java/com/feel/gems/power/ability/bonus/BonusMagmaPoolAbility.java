package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BonusMagmaPoolAbility implements GemAbility {
    private static final int RANGE_BLOCKS = 16;
    private static final int RADIUS_BLOCKS = 2;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MAGMA_POOL;
    }

    @Override
    public String name() {
        return "Magma Pool";
    }

    @Override
    public String description() {
        return "Create a pool of lava at the target location.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        HitResult hit = player.pick(RANGE_BLOCKS, 1.0F, false);
        BlockPos ground = hit instanceof BlockHitResult bhr ? bhr.getBlockPos() : player.blockPosition().below();
        BlockPos center = ground.above();

        for (int dx = -RADIUS_BLOCKS; dx <= RADIUS_BLOCKS; dx++) {
            for (int dz = -RADIUS_BLOCKS; dz <= RADIUS_BLOCKS; dz++) {
                if ((dx * dx + dz * dz) > (RADIUS_BLOCKS * RADIUS_BLOCKS)) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                if (world.getBlockState(pos).canBeReplaced() || world.getBlockState(pos).isAir()) {
                    world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                }
            }
        }

        world.sendParticles(ParticleTypes.FLAME, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 50, 2, 0.5, 2, 0.1);
        return true;
    }
}
