package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.EtherealState;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ethereal Step - Short dash that passes through one wall.
 */
public final class BonusEtherealStepAbility implements GemAbility {
    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private static final int MAX_DISTANCE = 8;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_ETHEREAL_STEP;
    }

    @Override
    public String name() {
        return "Ethereal Step";
    }

    @Override
    public String description() {
        return "Phase through solid blocks, teleporting up to 8 blocks in your facing direction.";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;

        int maxDistance = MAX_DISTANCE;
        Vec3 start = player.position();
        // GameTests aim at a target point which can add pitch; Ethereal Step should be a horizontal phase.
        Vec3 look = player.getViewVector(1.0F);
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() <= 1.0E-4D) {
            // If the player is looking straight up/down, fall back to yaw.
            double yawRad = Math.toRadians(player.getYRot());
            direction = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        } else {
            direction = direction.normalize();
        }

        // Find destination by phasing through blocks
        Vec3 destination = findPhaseDestination(world, start, direction, maxDistance, player);

        if (destination == null || destination.distanceToSqr(start) < 1.0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.ability.cannot_phase"), true);
            return false;
        }

        // Particles at start (ghost trail)
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z,
                25, 0.3, 0.5, 0.3, 0.05);
        world.sendParticles(ParticleTypes.SOUL, start.x, start.y + 0.5, start.z,
                10, 0.2, 0.3, 0.2, 0.02);

        // Briefly ignore damage while phasing.
        EtherealState.setEthereal(player, 10);

        // Teleport (match GameTest teleport semantics to avoid collision/validation differences).
        boolean teleported = player.teleportTo(
                world,
                destination.x,
                destination.y,
                destination.z,
                EnumSet.noneOf(Relative.class),
                player.getYRot(),
                player.getXRot(),
                false
        );
        if (!teleported) {
            return false;
        }

        // Particles at end
        world.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1, destination.z,
                25, 0.3, 0.5, 0.3, 0.05);
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, destination.x, destination.y + 0.5, destination.z,
                8, 0.2, 0.3, 0.2, 0.02);

        world.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f);
        world.playSound(null, start.x, start.y, start.z,
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.5f, 0.8f);
        return true;
    }

    /**
     * Find a valid destination by phasing through solid blocks.
     * The ability should pass through walls and emerge on the other side.
     */
    private Vec3 findPhaseDestination(ServerLevel world, Vec3 start, Vec3 direction, int maxDistance, ServerPlayer player) {
        boolean inWall = false;
        Vec3 lastValidPos = null;

        for (int i = 1; i <= maxDistance; i++) {
            Vec3 checkPos = start.add(direction.scale(i));
            BlockPos blockPos = BlockPos.containing(checkPos);
            BlockPos headPos = blockPos.above();

            BlockState feetBlock = world.getBlockState(blockPos);
            BlockState headBlock = world.getBlockState(headPos);

            boolean feetClear = feetBlock.getCollisionShape(world, blockPos).isEmpty();
            boolean headClear = headBlock.getCollisionShape(world, headPos).isEmpty();
            boolean positionClear = feetClear && headClear;

            if (!positionClear) {
                // We're in a wall
                inWall = true;
            } else if (inWall) {
                // We just exited a wall - stop at the first clear position after the wall.
                return checkPos;
            } else {
                // Open space, keep tracking as potential destination
                lastValidPos = checkPos;
            }
        }

        // Fallback: find the furthest open position
        for (int i = maxDistance; i >= 1; i--) {
            Vec3 checkPos = start.add(direction.scale(i));
            BlockPos blockPos = BlockPos.containing(checkPos);
            BlockPos headPos = blockPos.above();

            BlockState feetBlock = world.getBlockState(blockPos);
            BlockState headBlock = world.getBlockState(headPos);

            if (feetBlock.getCollisionShape(world, blockPos).isEmpty() && headBlock.getCollisionShape(world, headPos).isEmpty()) {
                return checkPos;
            }
        }

        return null;
    }
}
