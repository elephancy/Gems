package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BonusIceWallAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ICE_WALL;
    }

    @Override
    public String name() {
        return "Ice Wall";
    }

    @Override
    public String description() {
        return "Create a wall of ice where you're looking.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(10));
        
        HitResult hit = world.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        
        BlockPos center = BlockPos.containing(hit.getLocation());
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);
        if (perpendicular.lengthSqr() < 1.0E-6D) {
            perpendicular = new Vec3(1, 0, 0);
        } else {
            perpendicular = perpendicular.normalize();
        }
        
        for (int h = 0; h < 4; h++) {
            for (int w = -2; w <= 2; w++) {
                int dx = (int) Math.round(perpendicular.x * w);
                int dz = (int) Math.round(perpendicular.z * w);
                BlockPos pos = center.offset(dx, h, dz);
                if (world.getBlockState(pos).canBeReplaced() || world.getBlockState(pos).isAir()) {
                    world.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                }
            }
        }
        
        world.sendParticles(ParticleTypes.SNOWFLAKE, center.getX(), center.getY() + 2, center.getZ(), 40, 2, 2, 2, 0.1);
        return true;
    }
}
