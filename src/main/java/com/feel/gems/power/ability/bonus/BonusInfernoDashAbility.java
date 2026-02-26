package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Inferno Dash - Dash forward leaving a trail of fire.
 */
public final class BonusInfernoDashAbility implements GemAbility {
    private static final double DASH_DISTANCE = 10.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_INFERNO_DASH;
    }

    @Override
    public String name() {
        return "Inferno Dash";
    }

    @Override
    public String description() {
        return "Dash forward 10 blocks, leaving a trail of fire behind.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 end = start.add(direction.scale(DASH_DISTANCE));

        // Teleport player
        GemsTeleport.teleport(player, world, end.x, end.y, end.z, player.getYRot(), player.getXRot());

        // Create fire trail
        Vec3 current = start;
        for (int i = 0; i < DASH_DISTANCE; i++) {
            BlockPos pos = BlockPos.containing(current);
            BlockPos below = pos.below();
            if (world.getBlockState(pos).isAir() && world.getBlockState(below).isRedstoneConductor(world, below)) {
                world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
            world.sendParticles(ParticleTypes.FLAME, current.x, current.y + 0.5, current.z, 5, 0.2, 0.2, 0.2, 0.01);
            current = current.add(direction);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
