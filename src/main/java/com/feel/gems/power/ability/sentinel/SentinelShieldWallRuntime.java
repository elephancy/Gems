package com.feel.gems.power.ability.sentinel;

import com.feel.gems.trust.GemTrust;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SentinelShieldWallRuntime {
    private static final Map<UUID, WallData> ACTIVE_WALLS = new HashMap<>();
    private static final double REPEL_STRENGTH = 0.5;
    private static final int SLOW_TICKS = 30;
    private static final int SLOW_AMPLIFIER = 1;

    private SentinelShieldWallRuntime() {}

    public static void createWall(ServerPlayer owner, ServerLevel world, BlockPos basePos, Direction perpendicular, int width, int height, long endTime) {
        WallData existing = ACTIVE_WALLS.remove(owner.getUUID());
        if (existing != null) {
            ServerLevel restoreWorld = world;
            if (!existing.worldId.equals(world.dimension().identifier().toString())) {
                var server = world.getServer();
                Identifier id = Identifier.tryParse(existing.worldId);
                if (server != null && id != null) {
                    restoreWorld = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                }
            }
            if (restoreWorld != null) {
                restoreBlocks(restoreWorld, existing);
            }
        }

        Set<BlockPos> wallPositions = new HashSet<>();
        Map<BlockPos, BlockState> replaced = new HashMap<>();
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = basePos.relative(perpendicular, w).above(h);
                wallPositions.add(pos);
                BlockState atPos = world.getBlockState(pos);
                if (atPos.is(Blocks.BARRIER)) {
                    replaced.put(pos, Blocks.AIR.defaultBlockState());
                    world.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                } else if (atPos.isAir() || atPos.canBeReplaced()) {
                    replaced.put(pos, atPos);
                    world.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                }
            }
        }

        ACTIVE_WALLS.put(owner.getUUID(), new WallData(owner.getUUID(), wallPositions, replaced, endTime, owner.level().dimension().identifier().toString()));
    }

    public static boolean isInWall(Vec3 position, String worldId) {
        BlockPos blockPos = BlockPos.containing(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return true;
            }
        }
        return false;
    }

    public static UUID getWallOwner(Vec3 position, String worldId) {
        BlockPos blockPos = BlockPos.containing(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return wall.ownerId;
            }
        }
        return null;
    }

    /**
     * Tick the shield walls - removes expired walls and repels hostile entities.
     */
    public static void tick(long currentTime, String worldId, ServerLevel world) {
        if (ACTIVE_WALLS.isEmpty()) {
            return;
        }
        // Remove expired walls
        ACTIVE_WALLS.entrySet().removeIf(entry -> {
            WallData wall = entry.getValue();
            if (!wall.worldId.equals(worldId)) {
                return false;
            }
            if (currentTime <= wall.endTime) {
                return false;
            }
            restoreBlocks(world, wall);
            return true;
        });

        // Repel entities from active walls
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;

            ServerPlayer owner = world.getServer().getPlayerList().getPlayer(wall.ownerId);
            if (owner == null) continue;

            // Calculate wall bounding box
            Vec3 wallCenter = calculateWallCenter(wall.positions);
            AABB wallBox = calculateWallBox(wall.positions).inflate(0.5);

            // Find entities in or near the wall
            for (Entity entity : world.getEntities(null, wallBox)) {
                if (entity instanceof Projectile projectile) {
                    Entity shooter = projectile.getOwner();
                    if (shooter instanceof ServerPlayer shooterPlayer) {
                        if (shooterPlayer.getUUID().equals(wall.ownerId) || GemTrust.isTrusted(owner, shooterPlayer)) {
                            continue;
                        }
                    }
                    world.sendParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY(), entity.getZ(),
                            6, 0.2, 0.2, 0.2, 0.05);
                    projectile.discard();
                    continue;
                }
                if (!(entity instanceof LivingEntity living)) continue;

                // Don't repel the wall owner or their trusted allies
                if (entity instanceof ServerPlayer targetPlayer) {
                    if (targetPlayer.getUUID().equals(wall.ownerId)) continue;
                    if (VoidImmunity.shouldBlockEffect(owner, targetPlayer)) continue;
                    if (GemTrust.isTrusted(owner, targetPlayer)) continue;
                }

                // Only repel hostile mobs and enemy players
                if (!(entity instanceof Monster) && !(entity instanceof ServerPlayer)) {
                    continue;
                }

                // Calculate repulsion direction (away from wall center)
                Vec3 entityPos = entity.position();
                Vec3 repelDir = entityPos.subtract(wallCenter).normalize();
                if (repelDir.lengthSqr() < 0.01) {
                    // Entity is at wall center, push in random direction
                    repelDir = new Vec3(world.getRandom().nextDouble() - 0.5, 0, world.getRandom().nextDouble() - 0.5).normalize();
                }

                // Apply repulsion velocity
                Vec3 velocity = entity.getDeltaMovement();
                entity.setDeltaMovement(velocity.add(repelDir.scale(REPEL_STRENGTH)));
                living.needsSync = true;

                // Spawn particles at collision point
                world.sendParticles(ParticleTypes.END_ROD,
                        entityPos.x, entityPos.y + 1, entityPos.z,
                        3, 0.2, 0.2, 0.2, 0.02);

                if (wall.positions.contains(BlockPos.containing(entityPos))) {
                    living.addEffect(new MobEffectInstance(
                            MobEffects.SLOWNESS,
                            SLOW_TICKS,
                            SLOW_AMPLIFIER,
                            true, false, false
                    ));
                }
            }
        }
    }

    public static boolean hasAnyWalls() {
        return !ACTIVE_WALLS.isEmpty();
    }

    private static Vec3 calculateWallCenter(Set<BlockPos> positions) {
        if (positions.isEmpty()) return Vec3.ZERO;
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        int count = positions.size();
        return new Vec3(x / count, y / count, z / count);
    }

    private static AABB calculateWallBox(Set<BlockPos> positions) {
        if (positions.isEmpty()) return new AABB(0, 0, 0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static void clearWall(UUID ownerId, ServerLevel world) {
        WallData wall = ACTIVE_WALLS.remove(ownerId);
        if (wall != null && world != null) {
            restoreBlocks(world, wall);
        }
    }

    public static Collection<WallData> getActiveWalls() {
        return Collections.unmodifiableCollection(ACTIVE_WALLS.values());
    }

    private static void restoreBlocks(ServerLevel world, WallData wall) {
        if (wall.replaced.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : wall.replaced.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = world.getBlockState(pos);
            if (current.is(Blocks.BARRIER)) {
                world.setBlockAndUpdate(pos, entry.getValue());
            }
        }
    }

    public record WallData(UUID ownerId, Set<BlockPos> positions, Map<BlockPos, BlockState> replaced, long endTime, String worldId) {}
}
