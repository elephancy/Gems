package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;

public final class DuelistMirrorMatchAbility implements GemAbility {
    public static final String DUEL_PARTNER_KEY = "duelist_mirror_match_partner";
    public static final String DUEL_END_TIME_KEY = "duelist_mirror_match_end";
    public static final String CAGE_CENTER_X_KEY = "duelist_mirror_match_cage_x";
    public static final String CAGE_CENTER_Y_KEY = "duelist_mirror_match_cage_y";
    public static final String CAGE_CENTER_Z_KEY = "duelist_mirror_match_cage_z";
    public static final String SPAWN_X_KEY = "duelist_mirror_match_spawn_x";
    public static final String SPAWN_Y_KEY = "duelist_mirror_match_spawn_y";
    public static final String SPAWN_Z_KEY = "duelist_mirror_match_spawn_z";
    public static final String SPAWN_DIM_KEY = "duelist_mirror_match_spawn_dim";

    // Arena dimensions: 32x32x15 (±16 on X/Z, +15 on Y from floor)
    public static final int CAGE_HALF_SIZE = 16;
    public static final int CAGE_HEIGHT = 15;

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_MIRROR_MATCH;
    }

    @Override
    public String name() {
        return "Mirror Match";
    }

    @Override
    public String description() {
        return "Swap skins and names with a targeted player, trapping both in a barrier cage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().mirrorMatchCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().duelist().mirrorMatchRangeBlocks();

        // Raycast to find target
        ServerPlayer target = Targeting.raycastPlayer(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        long endTime = Long.MAX_VALUE;

        // Find arena location high in the sky to avoid terrain interference
        BlockPos arenaCenter = findArenaLocation(world, player.blockPosition());

        // Save original spawn positions before teleporting
        saveSpawnPosition(player);
        saveSpawnPosition(target);

        // Build the arena cage with visible boundaries
        buildCage(world, arenaCenter);

        // Teleport both players into the arena
        double arenaY = arenaCenter.getY() + 2; // Floor + 2 blocks up for safety
        player.teleportTo(world, arenaCenter.getX() - 5, arenaY, arenaCenter.getZ(),
            java.util.Set.of(), 90.0F, 0.0F, true);
        target.teleportTo(world, arenaCenter.getX() + 5, arenaY, arenaCenter.getZ(),
            java.util.Set.of(), -90.0F, 0.0F, true);

        // Store mirror state for both players
        PlayerStateManager.setPersistent(player, DUEL_PARTNER_KEY, target.getStringUUID());
        PlayerStateManager.setPersistent(player, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_X_KEY, String.valueOf(arenaCenter.getX()));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_Y_KEY, String.valueOf(arenaCenter.getY()));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_Z_KEY, String.valueOf(arenaCenter.getZ()));

        // Target state
        PlayerStateManager.setPersistent(target, DUEL_PARTNER_KEY, player.getStringUUID());
        PlayerStateManager.setPersistent(target, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_X_KEY, String.valueOf(arenaCenter.getX()));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_Y_KEY, String.valueOf(arenaCenter.getY()));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_Z_KEY, String.valueOf(arenaCenter.getZ()));

        DuelistMirrorMatchRuntime.start(player, target);

        // Visual effects
        AbilityFeedback.burstAt(world, player.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        AbilityFeedback.sound(target, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);

        return true;
    }

    /**
     * Find a suitable arena location high in the sky above the player.
     */
    private static BlockPos findArenaLocation(ServerLevel world, BlockPos near) {
        // Place arena at Y=200 (or world height limit - arena height - margin)
        int targetY = Math.min(200, world.getMaxY() - CAGE_HEIGHT - 10);
        return new BlockPos(near.getX(), targetY, near.getZ());
    }

    /**
     * Save the player's current position for later teleport back.
     */
    private static void saveSpawnPosition(ServerPlayer player) {
        PlayerStateManager.setPersistent(player, SPAWN_X_KEY, String.valueOf((int) player.getX()));
        PlayerStateManager.setPersistent(player, SPAWN_Y_KEY, String.valueOf((int) player.getY()));
        PlayerStateManager.setPersistent(player, SPAWN_Z_KEY, String.valueOf((int) player.getZ()));
        PlayerStateManager.setPersistent(player, SPAWN_DIM_KEY,
            player.level().dimension().identifier().toString());
    }

    /**
     * Get the saved spawn position for teleporting back after duel.
     */
    public static BlockPos getSavedSpawnPosition(ServerPlayer player) {
        String xStr = PlayerStateManager.getPersistent(player, SPAWN_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, SPAWN_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, SPAWN_Z_KEY);
        if (xStr == null || yStr == null || zStr == null) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(xStr), Integer.parseInt(yStr), Integer.parseInt(zStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Build an arena cage with visible boundaries (magenta stained glass for walls, barrier for floor/ceiling).
     */
    public static void buildCage(ServerLevel world, BlockPos center) {
        int minX = center.getX() - CAGE_HALF_SIZE;
        int maxX = center.getX() + CAGE_HALF_SIZE;
        int minZ = center.getZ() - CAGE_HALF_SIZE;
        int maxZ = center.getZ() + CAGE_HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + CAGE_HEIGHT;

        // Build floor with barrier (invisible, but solid) and ceiling with glass
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockAndUpdate(new BlockPos(x, floorY, z), Blocks.BARRIER.defaultBlockState());
                world.setBlockAndUpdate(new BlockPos(x, ceilingY, z), Blocks.STAINED_GLASS.magenta().defaultBlockState());
            }
        }

        // Build walls with magenta stained glass (visible boundary)
        for (int y = floorY + 1; y < ceilingY; y++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlockAndUpdate(new BlockPos(x, y, minZ), Blocks.STAINED_GLASS.magenta().defaultBlockState());
                world.setBlockAndUpdate(new BlockPos(x, y, maxZ), Blocks.STAINED_GLASS.magenta().defaultBlockState());
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                world.setBlockAndUpdate(new BlockPos(minX, y, z), Blocks.STAINED_GLASS.magenta().defaultBlockState());
                world.setBlockAndUpdate(new BlockPos(maxX, y, z), Blocks.STAINED_GLASS.magenta().defaultBlockState());
            }
        }
    }

    /**
     * Check if a block is part of the arena cage.
     */
    private static boolean isCageBlock(ServerLevel world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.is(Blocks.BARRIER) || state.is(Blocks.STAINED_GLASS.magenta());
    }

    /**
     * Remove the arena cage (restore air blocks).
     */
    public static void removeCage(ServerLevel world, BlockPos center) {
        int minX = center.getX() - CAGE_HALF_SIZE;
        int maxX = center.getX() + CAGE_HALF_SIZE;
        int minZ = center.getZ() - CAGE_HALF_SIZE;
        int maxZ = center.getZ() + CAGE_HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + CAGE_HEIGHT;

        // Remove floor and ceiling
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isCageBlock(world, new BlockPos(x, floorY, z))) {
                    world.setBlockAndUpdate(new BlockPos(x, floorY, z), Blocks.AIR.defaultBlockState());
                }
                if (isCageBlock(world, new BlockPos(x, ceilingY, z))) {
                    world.setBlockAndUpdate(new BlockPos(x, ceilingY, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Remove walls
        for (int y = floorY + 1; y < ceilingY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isCageBlock(world, new BlockPos(x, y, minZ))) {
                    world.setBlockAndUpdate(new BlockPos(x, y, minZ), Blocks.AIR.defaultBlockState());
                }
                if (isCageBlock(world, new BlockPos(x, y, maxZ))) {
                    world.setBlockAndUpdate(new BlockPos(x, y, maxZ), Blocks.AIR.defaultBlockState());
                }
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                if (isCageBlock(world, new BlockPos(minX, y, z))) {
                    world.setBlockAndUpdate(new BlockPos(minX, y, z), Blocks.AIR.defaultBlockState());
                }
                if (isCageBlock(world, new BlockPos(maxX, y, z))) {
                    world.setBlockAndUpdate(new BlockPos(maxX, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /**
     * Get the cage center from a player's state, if they are in a duel.
     */
    public static BlockPos getCageCenter(ServerPlayer player) {
        String xStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_Z_KEY);
        if (xStr == null || yStr == null || zStr == null) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(xStr), Integer.parseInt(yStr), Integer.parseInt(zStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isInDuel(ServerPlayer player) {
        String partner = PlayerStateManager.getPersistent(player, DUEL_PARTNER_KEY);
        if (partner == null || partner.isEmpty()) {
            return false;
        }
        String endTimeStr = PlayerStateManager.getPersistent(player, DUEL_END_TIME_KEY);
        if (endTimeStr == null) {
            return false;
        }
        long endTime = Long.parseLong(endTimeStr);
        if (endTime <= 0) {
            return true;
        }
        return player.level().getGameTime() < endTime;
    }

    public static void clearDuel(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, DUEL_PARTNER_KEY);
        PlayerStateManager.clearPersistent(player, DUEL_END_TIME_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_X_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_Y_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_Z_KEY);
        PlayerStateManager.clearPersistent(player, SPAWN_X_KEY);
        PlayerStateManager.clearPersistent(player, SPAWN_Y_KEY);
        PlayerStateManager.clearPersistent(player, SPAWN_Z_KEY);
        PlayerStateManager.clearPersistent(player, SPAWN_DIM_KEY);
    }
}
