package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class SentinelShieldWallAbility implements GemAbility {
    public static final String WALL_ACTIVE_KEY = "sentinel_wall_active";
    public static final String WALL_END_KEY = "sentinel_wall_end";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_SHIELD_WALL;
    }

    @Override
    public String name() {
        return "Shield Wall";
    }

    @Override
    public String description() {
        return "Deploy an energy barrier that blocks projectiles and slows enemies passing through.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().shieldWallCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.SENTINEL, GemsBalance.v().sentinel().shieldWallDurationTicks());
        int width = GemsBalance.v().sentinel().shieldWallWidthBlocks();
        int height = GemsBalance.v().sentinel().shieldWallHeightBlocks();

        // Get facing direction
        Direction facing = player.getDirection();
        Direction perpendicular = facing.getClockWise();

        // Position wall in front of player
        BlockPos basePos = player.blockPosition().relative(facing, 2);

        long endTime = world.getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, WALL_ACTIVE_KEY, "true");
        PlayerStateManager.setPersistent(player, WALL_END_KEY, String.valueOf(endTime));

        // Store wall position data for runtime checks
        SentinelShieldWallRuntime.createWall(player, world, basePos, perpendicular, width, height, endTime);

        // Visual effect - particle wall
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos wallPos = basePos.relative(perpendicular, w).above(h);
                AbilityFeedback.burstAt(world, Vec3.atCenterOf(wallPos), ParticleTypes.END_ROD, 5, 0.2D);
            }
        }

        AbilityFeedback.sound(player, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.5F);
        return true;
    }
}
