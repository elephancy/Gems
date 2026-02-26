package com.feel.gems.power.gem.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


/**
 * Lightweight runtime for Space gem "anomalies" that require per-tick physics (black/white holes and delayed orbital strikes).
 *
 * <p>Important: keep this performant, it should early-exit when no anomalies exist and keep radii clamped by config.</p>
 */
public final class SpaceAnomalies {
    private static final List<Anomaly> ACTIVE = new ArrayList<>();
    private static final int HOLE_TICK_STRIDE = 3; // reduce per-tick physics work
    private static final int HOLE_VFX_TICK_STRIDE = 5; // keep particles/sounds less frequent than physics
    private static final float GRAVITY_WELL_STRENGTH = 1.5F;

    private SpaceAnomalies() {
    }

    public static void spawnBlackHole(ServerPlayer caster, Vec3 pos) {
        if (!(caster.level() instanceof ServerLevel world)) {
            return;
        }
        long now = GemsTime.now(world);
        int duration = AugmentRuntime.applyDurationMultiplier(caster, GemId.SPACE, GemsBalance.v().space().blackHoleDurationTicks());
        ACTIVE.add(Anomaly.blackHole(caster.getUUID(), world.dimension(), pos, now, duration));

        AbilityFeedback.burstAt(world, pos, ParticleTypes.REVERSE_PORTAL, 18, 0.35D);
        AbilityFeedback.ring(world, pos.add(0.0D, 0.15D, 0.0D), Math.min(6.0D, GemsBalance.v().space().blackHoleRadiusBlocks()), ParticleTypes.REVERSE_PORTAL, 28);
        AbilityFeedback.sound(caster, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9F, 0.8F);
        AbilityFeedback.sound(caster, SoundEvents.WARDEN_SONIC_CHARGE, 0.6F, 0.7F);
        AbilityFeedback.soundAt(world, pos, SoundEvents.PORTAL_AMBIENT, 0.35F, 0.7F);
    }

    public static void spawnWhiteHole(ServerPlayer caster, Vec3 pos) {
        if (!(caster.level() instanceof ServerLevel world)) {
            return;
        }
        long now = GemsTime.now(world);
        int duration = AugmentRuntime.applyDurationMultiplier(caster, GemId.SPACE, GemsBalance.v().space().whiteHoleDurationTicks());
        ACTIVE.add(Anomaly.whiteHole(caster.getUUID(), world.dimension(), pos, now, duration));

        AbilityFeedback.burstAt(world, pos, ParticleTypes.END_ROD, 14, 0.35D);
        AbilityFeedback.burstAt(world, pos, ParticleTypes.CLOUD, 10, 0.45D);
        AbilityFeedback.ring(world, pos.add(0.0D, 0.15D, 0.0D), Math.min(6.0D, GemsBalance.v().space().whiteHoleRadiusBlocks()), ParticleTypes.END_ROD, 28);
        AbilityFeedback.sound(caster, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.4F);
        AbilityFeedback.sound(caster, SoundEvents.WARDEN_SONIC_CHARGE, 0.6F, 1.6F);
        AbilityFeedback.soundAt(world, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.4F);
    }

    public static void scheduleOrbitalLaser(ServerPlayer caster, BlockPos target, boolean miningMode) {
        if (!(caster.level() instanceof ServerLevel world)) {
            return;
        }
        long now = GemsTime.now(world);
        ACTIVE.add(Anomaly.laser(caster.getUUID(), world.dimension(), target, miningMode, now));
        Vec3 center = Vec3.atCenterOf(target);
        AbilityFeedback.burstAt(world, center, ParticleTypes.END_ROD, 16, 0.35D);
        AbilityFeedback.burstAt(world, center, ParticleTypes.ELECTRIC_SPARK, 10, 0.35D);
        AbilityFeedback.ring(world, center.add(0.0D, 0.2D, 0.0D), 3.0D, ParticleTypes.END_ROD, 32);
        AbilityFeedback.ring(world, center.add(0.0D, 0.2D, 0.0D), 2.0D, ParticleTypes.ELECTRIC_SPARK, 18);
        AbilityFeedback.sound(caster, SoundEvents.BEACON_ACTIVATE, 0.8F, miningMode ? 0.7F : 1.2F);
        AbilityFeedback.soundAt(world, center, SoundEvents.AMETHYST_CLUSTER_HIT, 0.6F, miningMode ? 0.8F : 1.2F);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        long now = GemsTime.now(server);

        Iterator<Anomaly> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Anomaly a = it.next();
            if (a.untilTick <= now) {
                it.remove();
                continue;
            }

            ServerLevel world = server.getLevel(a.dimension);
            if (world == null) {
                it.remove();
                continue;
            }

            ServerPlayer caster = server.getPlayerList().getPlayer(a.caster);
            if (caster == null) {
                // GameTests can spawn "mock" players that aren't registered in the PlayerManager; fall back to
                // resolving the caster from the world player list so anomaly behavior remains testable.
                caster = world.players().stream().filter(p -> p.getUUID().equals(a.caster)).findFirst().orElse(null);
                if (caster == null) {
                    it.remove();
                    continue;
                }
            }

            if (a.kind == Kind.ORBITAL_LASER) {
                if (a.strikeAtTick > now) {
                    // Telegraph a small pulse while waiting.
                    if ((now % 5) == 0) {
                        Vec3 center = Vec3.atCenterOf(a.laserTarget);
                        AbilityFeedback.burstAt(world, center, ParticleTypes.END_ROD, 2, 0.15D);
                        AbilityFeedback.burstAt(world, center, ParticleTypes.ELECTRIC_SPARK, 2, 0.15D);
                    }
                    if ((now % 20) == 0) {
                        AbilityFeedback.soundAt(world, Vec3.atCenterOf(a.laserTarget), SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, a.miningMode ? 0.7F : 1.2F);
                    }
                    continue;
                }
                strikeOrbitalLaser(caster, world, a);
                it.remove();
                continue;
            }

            if (now >= a.nextPhysicsTick) {
                tickHole(caster, world, a, now);
                a.nextPhysicsTick = now + HOLE_TICK_STRIDE;
            }
        }
    }

    private static void strikeOrbitalLaser(ServerPlayer caster, ServerLevel world, Anomaly a) {
        BlockPos target = a.laserTarget;
        Vec3 center = Vec3.atCenterOf(target);

        int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        double fromY = topY + 24.0D;
        Vec3 from = new Vec3(center.x, fromY, center.z);
        AbilityFeedback.beam(world, from, center, ParticleTypes.END_ROD, 64);
        AbilityFeedback.burstAt(world, center, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.burstAt(world, center, ParticleTypes.ELECTRIC_SPARK, 22, 0.45D);
        AbilityFeedback.burstAt(world, center, ParticleTypes.SMOKE, 16, 0.55D);
        AbilityFeedback.ring(world, center.add(0.0D, 0.2D, 0.0D), 3.5D, ParticleTypes.END_ROD, 36);

        float thunderVol = a.miningMode ? 0.35F : 0.5F;
        float explodeVol = a.miningMode ? 0.25F : 0.4F;
        float pitch = a.miningMode ? 0.9F : 1.3F;
        AbilityFeedback.sound(caster, SoundEvents.LIGHTNING_BOLT_THUNDER, thunderVol, pitch);
        AbilityFeedback.sound(caster, SoundEvents.GENERIC_EXPLODE, explodeVol, a.miningMode ? 0.8F : 1.1F);
        AbilityFeedback.soundAt(world, center, SoundEvents.LIGHTNING_BOLT_THUNDER, thunderVol, pitch);
        AbilityFeedback.soundAt(world, center, SoundEvents.GENERIC_EXPLODE, explodeVol, a.miningMode ? 0.8F : 1.1F);
        AbilityFeedback.soundAt(world, center, SoundEvents.BEACON_DEACTIVATE, 0.55F, a.miningMode ? 1.2F : 0.8F);

        if (a.miningMode) {
            mineAt(caster, world, target);
        } else {
            damageAt(caster, world, center);
        }
    }

    private static void damageAt(ServerPlayer caster, ServerLevel world, Vec3 center) {
        int radius = GemsBalance.v().space().orbitalLaserRadiusBlocks();
        float damage = GemsBalance.v().space().orbitalLaserDamage();
        if (radius <= 0 || damage <= 0.0F) {
            return;
        }

        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != caster);
        for (LivingEntity living : targets) {
            if (living instanceof ServerPlayer other && GemTrust.isTrusted(caster, other)) {
                continue;
            }
            if (living instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(caster, otherPlayer)) {
                continue;
            }
            living.hurtServer(world, caster.damageSources().indirectMagic(caster, caster), damage);
        }
        AbilityFeedback.burstAt(world, center, ParticleTypes.ELECTRIC_SPARK, 18, 0.4D);
    }

    private static void mineAt(ServerPlayer caster, ServerLevel world, BlockPos target) {
        int radius = GemsBalance.v().space().orbitalLaserMiningRadiusBlocks();
        int maxBlocks = GemsBalance.v().space().orbitalLaserMiningMaxBlocks();
        float hardnessCap = GemsBalance.v().space().orbitalLaserMiningHardnessCap();
        if (radius <= 0 || maxBlocks <= 0) {
            return;
        }

        int mined = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(target.getX() + dx, target.getY() + dy, target.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!isMiningAllowed(state)) {
                        continue;
                    }
                    float hardness = state.getDestroySpeed(world, pos);
                    if (hardness < 0.0F || hardness > hardnessCap) {
                        continue;
                    }
                    if (world.destroyBlock(pos, true, caster)) {
                        mined++;
                        AbilityFeedback.burstAt(world, Vec3.atCenterOf(pos), ParticleTypes.CLOUD, 2, 0.15D);
                        if (mined >= maxBlocks) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean isMiningAllowed(BlockState state) {
        // Mining mode should be useful on most blocks (and can mine hard blocks like obsidian),
        // but it must not break special/protected blocks.
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) {
            return false;
        }
        if (state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)) {
            return false;
        }
        if (state.is(Blocks.NETHER_PORTAL)) {
            return false;
        }
        if (state.is(Blocks.COMMAND_BLOCK) || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return false;
        }
        if (state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW)) {
            return false;
        }
        return true;
    }

    private static void tickHole(ServerPlayer caster, ServerLevel world, Anomaly a, long now) {
        int radius = a.radiusBlocks;
        if (radius <= 0) {
            return;
        }

        Vec3 center = a.center;
        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());

        float strength = a.strengthMultiplier * GRAVITY_WELL_STRENGTH;
        int vfxTargets = 0;
        for (LivingEntity living : targets) {
            if (living == caster) {
                continue;
            }
            if (living instanceof ServerPlayer other && GemTrust.isTrusted(caster, other)) {
                continue;
            }
            if (living instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(caster, otherPlayer)) {
                continue;
            }
            Vec3 delta = (a.kind == Kind.BLACK_HOLE) ? center.subtract(living.position()) : living.position().subtract(center);
            Vec3 push = delta.normalize().scale(strength);
            living.setDeltaMovement(living.getDeltaMovement().add(push));
            living.needsSync = true;

            // Bounded per-tick VFX to hint the direction of force without spamming on large mobs clusters.
            if ((now % HOLE_VFX_TICK_STRIDE) == 0 && vfxTargets < 6) {
                var trail = (a.kind == Kind.BLACK_HOLE) ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.CLOUD;
                AbilityFeedback.burstAt(world, living.position().add(0.0D, living.getBbHeight() * 0.6D, 0.0D), trail, 1, 0.08D);
                vfxTargets++;
            }
        }

        if (now >= a.nextDamageTick) {
            a.nextDamageTick = now + 20;
            if (a.damagePerSecond > 0.0F) {
                float dmg = a.damagePerSecond;
                for (LivingEntity living : targets) {
                    if (living == caster) {
                        continue;
                    }
                    if (living instanceof ServerPlayer other && GemTrust.isTrusted(caster, other)) {
                        continue;
                    }
                    if (living instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(caster, otherPlayer)) {
                        continue;
                    }
                    living.hurtServer(world, caster.damageSources().indirectMagic(caster, caster), dmg);
                }
            }
        }

        if ((now % HOLE_VFX_TICK_STRIDE) == 0) {
            var core = (a.kind == Kind.BLACK_HOLE) ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD;
            var haze = (a.kind == Kind.BLACK_HOLE) ? ParticleTypes.SMOKE : ParticleTypes.CLOUD;
            AbilityFeedback.burstAt(world, center, core, 6, 0.25D);
            AbilityFeedback.burstAt(world, center, haze, 4, 0.35D);
        }

        if ((now % 20) == 0) {
            double ringRadius = Math.min(7.0D, radius);
            if (a.kind == Kind.BLACK_HOLE) {
                AbilityFeedback.ring(world, center.add(0.0D, 0.15D, 0.0D), ringRadius, ParticleTypes.PORTAL, 18);
                AbilityFeedback.ring(world, center.add(0.0D, 0.15D, 0.0D), Math.max(0.8D, ringRadius * 0.55D), ParticleTypes.SMOKE, 14);
                AbilityFeedback.soundAt(world, center, SoundEvents.PORTAL_AMBIENT, 0.22F, 0.65F);
            } else {
                AbilityFeedback.ring(world, center.add(0.0D, 0.15D, 0.0D), ringRadius, ParticleTypes.END_ROD, 18);
                AbilityFeedback.ring(world, center.add(0.0D, 0.15D, 0.0D), Math.max(0.8D, ringRadius * 0.55D), ParticleTypes.ELECTRIC_SPARK, 14);
                AbilityFeedback.soundAt(world, center, SoundEvents.AMETHYST_BLOCK_CHIME, 0.22F, 1.45F);
            }
        }
    }

    private enum Kind {
        BLACK_HOLE,
        WHITE_HOLE,
        ORBITAL_LASER
    }

    private static final class Anomaly {
        final Kind kind;
        final UUID caster;
        final net.minecraft.resources.ResourceKey<Level> dimension;
        final Vec3 center;
        final long startTick;
        final long untilTick;
        final int durationTicks;

        final int radiusBlocks;
        final float strengthMultiplier;
        final float damagePerSecond;
        long nextDamageTick;
        long nextPhysicsTick;

        final BlockPos laserTarget;
        final boolean miningMode;
        final long strikeAtTick;

        private Anomaly(Kind kind, UUID caster, net.minecraft.resources.ResourceKey<Level> dimension, Vec3 center, long startTick, long untilTick,
                        int durationTicks, int radiusBlocks, float strengthMultiplier, float damagePerSecond, long nextDamageTick,
                        BlockPos laserTarget, boolean miningMode, long strikeAtTick) {
            this.kind = kind;
            this.caster = caster;
            this.dimension = dimension;
            this.center = center;
            this.startTick = startTick;
            this.untilTick = untilTick;
            this.durationTicks = durationTicks;
            this.radiusBlocks = radiusBlocks;
            this.strengthMultiplier = strengthMultiplier;
            this.damagePerSecond = damagePerSecond;
            this.nextDamageTick = nextDamageTick;
            this.nextPhysicsTick = nextDamageTick; // align first physics tick with first damage tick window
            this.laserTarget = laserTarget;
            this.miningMode = miningMode;
            this.strikeAtTick = strikeAtTick;
        }

        static Anomaly blackHole(UUID caster, net.minecraft.resources.ResourceKey<Level> dim, Vec3 center, long now, int duration) {
            int radius = GemsBalance.v().space().blackHoleRadiusBlocks();
            float strength = GemsBalance.v().space().blackHolePullStrength();
            float dmg = GemsBalance.v().space().blackHoleDamagePerSecond();
            return new Anomaly(Kind.BLACK_HOLE, caster, dim, center, now, now + duration, duration, radius, strength, dmg, now + 20, null, false, 0);
        }

        static Anomaly whiteHole(UUID caster, net.minecraft.resources.ResourceKey<Level> dim, Vec3 center, long now, int duration) {
            int radius = GemsBalance.v().space().whiteHoleRadiusBlocks();
            float strength = GemsBalance.v().space().whiteHolePushStrength();
            float dmg = GemsBalance.v().space().whiteHoleDamagePerSecond();
            return new Anomaly(Kind.WHITE_HOLE, caster, dim, center, now, now + duration, duration, radius, strength, dmg, now + 20, null, false, 0);
        }

        static Anomaly laser(UUID caster, net.minecraft.resources.ResourceKey<Level> dim, BlockPos target, boolean miningMode, long now) {
            int delay = GemsBalance.v().space().orbitalLaserDelayTicks();
            return new Anomaly(Kind.ORBITAL_LASER, caster, dim, Vec3.atCenterOf(target), now, now + delay + 200, 0, 0, 0.0F, 0.0F, 0, target, miningMode, now + delay);
        }
    }
}
