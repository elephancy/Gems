package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Nether Portal - short-range teleport through a rift.
 */
public final class BonusNetherPortalAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_NETHER_PORTAL;
    }

    @Override
    public String name() {
        return "Nether Portal";
    }

    @Override
    public String description() {
        return "Short-range teleport through a nether rift.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().netherPortalCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().bonusPool().netherPortalDistanceBlocks;
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F);
        Vec3 end = start.add(dir.scale(range));
        BlockHitResult hit = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        BlockPos base = hit.getBlockPos().relative(hit.getDirection());
        Direction.Axis axis = player.getDirection().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        boolean built = buildPortal(world, base, axis);
        if (!built) {
            return false;
        }
        world.sendParticles(ParticleTypes.PORTAL, base.getX() + 0.5D, base.getY() + 1.0, base.getZ() + 0.5D, 24, 0.6, 0.8, 0.6, 0.02);
        return true;
    }

    private static boolean buildPortal(ServerLevel world, BlockPos base, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos origin = base;
        int width = 4;
        int height = 5;
        if (!canFitPortal(world, origin, right, width, height)) {
            return false;
        }
        for (int y = 0; y < height; y++) {
            for (int w = 0; w < width; w++) {
                if (y != 0 && y != height - 1 && w != 0 && w != width - 1) {
                    continue;
                }
                BlockPos pos = origin.offset(right.getStepX() * w, y, right.getStepZ() * w);
                world.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        for (int y = 1; y < height - 1; y++) {
            for (int w = 1; w < width - 1; w++) {
                BlockPos pos = origin.offset(right.getStepX() * w, y, right.getStepZ() * w);
                world.setBlock(pos, portal, 3);
            }
        }
        return true;
    }

    private static boolean canFitPortal(ServerLevel world, BlockPos origin, Direction right, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int w = 0; w < width; w++) {
                BlockPos pos = origin.offset(right.getStepX() * w, y, right.getStepZ() * w);
                BlockState state = world.getBlockState(pos);
                if (!state.isAir() && !state.canBeReplaced() && !state.is(Blocks.OBSIDIAN)) {
                    return false;
                }
            }
        }
        return true;
    }
}
