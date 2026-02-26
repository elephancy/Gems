package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Corpse Explosion - Detonate nearby corpses (recently killed entities) for AoE damage.
 */
public final class BonusCorpseExplosionAbility implements GemAbility {
    private static final Map<UUID, MarkedTarget> MARKED = new HashMap<>();
    private static final Map<UUID, Long> SUPPRESS_CHAIN_UNTIL = new HashMap<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_CORPSE_EXPLOSION;
    }

    @Override
    public String name() {
        return "Corpse Explosion";
    }

    @Override
    public String description() {
        return "Detonate nearby corpses for AoE damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().corpseExplosionCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        var cfg = GemsBalance.v().bonusPool();
        pruneMarks(world);

        int range = cfg.corpseExplosionCorpseRangeBlocks;
        if (range <= 0) {
            return false;
        }
        int durationTicks = Math.max(1, cfg.corpseExplosionMarkDurationSeconds * 20);
        long expiresAt = world.getGameTime() + durationTicks;

        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player);
        if (targets.isEmpty()) {
            return false;
        }
        boolean markedAny = false;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer other) {
                if (GemTrust.isTrusted(player, other) || VoidImmunity.shouldBlockEffect(player, other)) {
                    continue;
                }
            }
            MARKED.put(target.getUUID(), new MarkedTarget(world, player.getUUID(), expiresAt));
            world.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.5, target.getZ(),
                    8, 0.3, 0.4, 0.3, 0.02);
            markedAny = true;
        }
        if (!markedAny) {
            return false;
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6f, 1.1f);
        return true;
    }

    public static void recordCorpse(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel world)) {
            return;
        }
        Long suppressed = SUPPRESS_CHAIN_UNTIL.remove(entity.getUUID());
        if (suppressed != null && world.getGameTime() <= suppressed) {
            return;
        }
        MarkedTarget marked = MARKED.remove(entity.getUUID());
        if (marked == null || marked.world != world) {
            return;
        }
        long now = world.getGameTime();
        if (now > marked.expiresAt) {
            return;
        }
        explodeAt(marked, entity.position());
    }

    private static void explodeAt(MarkedTarget marked, Vec3 pos) {
        ServerLevel world = marked.world;
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(marked.ownerUuid);
        var cfg = GemsBalance.v().bonusPool();
        long now = world.getGameTime();

        AABB explosionBox = new AABB(pos, pos).inflate(cfg.corpseExplosionRadiusBlocks);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, explosionBox,
                e -> e.isAlive());

        for (LivingEntity entity : nearby) {
            if (owner != null && entity instanceof ServerPlayer other) {
                if (GemTrust.isTrusted(owner, other) || VoidImmunity.shouldBlockEffect(owner, other)) {
                    continue;
                }
            }
            if (owner != null && entity == owner) {
                continue;
            }
            SUPPRESS_CHAIN_UNTIL.put(entity.getUUID(), now + 4);
            entity.hurtServer(world, world.damageSources().explosion(null, owner), cfg.corpseExplosionDamage);
        }

        world.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z,
                6, 0.5, 0.5, 0.5, 0);
        world.sendParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z,
                20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.9f, 1.1f);
    }

    private static void pruneMarks(ServerLevel world) {
        long now = world.getGameTime();
        Iterator<Map.Entry<UUID, MarkedTarget>> iter = MARKED.entrySet().iterator();
        while (iter.hasNext()) {
            MarkedTarget mark = iter.next().getValue();
            long markNow = mark.world.equals(world) ? now : mark.world.getGameTime();
            if (markNow > mark.expiresAt) {
                iter.remove();
            }
        }
        Iterator<Map.Entry<UUID, Long>> suppressIter = SUPPRESS_CHAIN_UNTIL.entrySet().iterator();
        while (suppressIter.hasNext()) {
            Map.Entry<UUID, Long> entry = suppressIter.next();
            if (entry.getValue() < now) {
                suppressIter.remove();
            }
        }
    }

    private static final class MarkedTarget {
        final ServerLevel world;
        final UUID ownerUuid;
        final long expiresAt;

        private MarkedTarget(ServerLevel world, UUID ownerUuid, long expiresAt) {
            this.world = world;
            this.ownerUuid = ownerUuid;
            this.expiresAt = expiresAt;
        }
    }
}
