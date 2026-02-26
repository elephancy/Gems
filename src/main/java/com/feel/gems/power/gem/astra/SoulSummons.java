package com.feel.gems.power.gem.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;


public final class SoulSummons {
    public static final String TAG_SOUL = "gems_soul";
    private static final String TAG_OWNER_PREFIX = "gems_soul_owner:";
    private static final String KEY_SOUL_SUMMONS = "astraSoulSummons";

    private SoulSummons() {
    }

    public static void mark(Entity entity, UUID owner) {
        entity.addTag(TAG_SOUL);
        entity.addTag(TAG_OWNER_PREFIX + owner);
    }

    public static boolean isSoul(Entity entity) {
        return entity.entityTags().contains(TAG_SOUL);
    }

    public static UUID ownerUuid(Entity entity) {
        Set<String> tags = entity.entityTags();
        for (String tag : tags) {
            if (!tag.startsWith(TAG_OWNER_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_OWNER_PREFIX.length());
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public static void trackSpawn(ServerPlayer owner, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_SOUL_SUMMONS);
        list.add(GemsNbt.fromUuid(entity.getUUID()));
        root.put(KEY_SOUL_SUMMONS, list);
    }

    public static List<UUID> ownedSoulUuids(ServerPlayer owner) {
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getList(KEY_SOUL_SUMMONS).orElse(null);
        if (list == null) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid != null) {
                out.add(uuid);
            }
        }
        return out;
    }

    public static int pruneAndCount(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return 0;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_SOUL_SUMMONS);
        if (list.isEmpty()) {
            return 0;
        }
        ListTag next = new ListTag();
        int alive = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid == null) {
                continue;
            }
            Entity e = SummonerSummons.findEntity(server, uuid);
            if (e != null && e.isAlive() && isSoul(e) && owner.getUUID().equals(ownerUuid(e))) {
                next.add(GemsNbt.fromUuid(uuid));
                alive++;
            }
        }
        root.put(KEY_SOUL_SUMMONS, next);
        return alive;
    }

    public static void followOwner(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return;
        }
        double followStart = GemsBalance.v().systems().controlledFollowStartBlocks();
        double followStop = GemsBalance.v().systems().controlledFollowStopBlocks();
        double followSpeed = GemsBalance.v().systems().controlledFollowSpeed();
        double followStartSq = followStart * followStart;
        double followStopSq = followStop * followStop;
        int rangeBlocks = GemsBalance.v().summoner().commandRangeBlocks();
        LivingEntity fallbackTarget = LegendaryTargeting.findTarget(owner, rangeBlocks, 0);
        if (fallbackTarget == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedSoulUuids(owner)) {
            Entity e = SummonerSummons.findEntity(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isSoul(mob)) {
                continue;
            }
            SummonerSummons.refreshNetherMob(mob);
            if (mob.level() != owner.level()) {
                continue;
            }
            LivingEntity desiredTarget = fallbackTarget;
            LivingEntity currentTarget = mob.getTarget();
            LivingEntity normalizedTarget = normalizeTarget(owner, currentTarget, rangeBlocks);
            if (currentTarget != null && (normalizedTarget == null || !currentTarget.isAlive())) {
                mob.setTarget(null);
                currentTarget = null;
                normalizedTarget = null;
            }
            if (normalizedTarget != null) {
                if (desiredTarget != null && normalizedTarget.getUUID().equals(desiredTarget.getUUID())) {
                    continue;
                }
                if (desiredTarget == null) {
                    mob.setTarget(null);
                } else {
                    mob.setTarget(desiredTarget);
                    continue;
                }
            }
            if (desiredTarget != null && desiredTarget.level() == mob.level()) {
                mob.setTarget(desiredTarget);
                continue;
            }
            double distSq = mob.distanceToSqr(owner);
            if (distSq > followStartSq) {
                mob.getNavigation().moveTo(owner, followSpeed);
            } else if (distSq < followStopSq) {
                mob.getNavigation().stop();
            }
        }
    }

    private static LivingEntity findOwnerThreat(ServerPlayer owner, int rangeBlocks) {
        LivingEntity attacker = owner.getLastHurtByMob();
        LivingEntity candidate = normalizeTarget(owner, attacker, rangeBlocks);
        if (candidate != null) {
            return candidate;
        }
        if (rangeBlocks <= 0) {
            return null;
        }
        double maxSq = rangeBlocks * (double) rangeBlocks;
        var box = owner.getBoundingBox().inflate(rangeBlocks);
        List<Mob> mobs = owner.level().getEntitiesOfClass(Mob.class, box, mob -> mob.getTarget() == owner);
        LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (Mob mob : mobs) {
            LivingEntity normalized = normalizeTarget(owner, mob, rangeBlocks);
            if (normalized == null) {
                continue;
            }
            double distSq = owner.distanceToSqr(mob);
            if (distSq > maxSq || distSq >= nearestSq) {
                continue;
            }
            nearestSq = distSq;
            nearest = mob;
        }
        return nearest;
    }

    private static LivingEntity normalizeTarget(ServerPlayer owner, LivingEntity target, int rangeBlocks) {
        if (target == null || !target.isAlive() || target.level() != owner.level()) {
            return null;
        }
        if (rangeBlocks > 0) {
            double maxSq = rangeBlocks * (double) rangeBlocks;
            if (owner.distanceToSqr(target) > maxSq) {
                return null;
            }
        }
        if (target instanceof Mob mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isSoul(mobTarget)) {
                return null;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUUID()) && SummonerSummons.isSummon(mobTarget)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUUID()) && HypnoControl.isHypno(mobTarget)) {
                return null;
            }
        }
        return target;
    }
}
