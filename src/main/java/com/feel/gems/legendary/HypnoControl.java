package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.MobBlacklist;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.power.gem.summoner.SummonerSummons;




public final class HypnoControl {
    public static final String TAG_HYPNO = "gems_hypno";
    private static final String TAG_OWNER_PREFIX = "gems_hypno_owner:";
    private static final String TAG_UNTIL_PREFIX = "gems_hypno_until:";
    private static final String KEY_HYPNO_MOBS = "legendaryHypnoMobs";
    private static final java.util.Map<UUID, Entity> ENTITY_CACHE = new java.util.HashMap<>();

    private HypnoControl() {
    }

    public static boolean tryControl(ServerPlayer owner, Mob mob) {
        if (!isAllowed(mob)) {
            return false;
        }
        UUID existing = ownerUuid(mob);
        if (existing != null && !existing.equals(owner.getUUID())) {
            return false;
        }
        int max = GemsBalance.v().legendary().hypnoMaxControlled();
        if (max > 0 && pruneAndCount(owner) >= max) {
            return false;
        }
        long now = GemsTime.now(owner);
        int duration = GemsBalance.v().legendary().hypnoDurationTicks();
        mark(mob, owner.getUUID(), duration > 0 ? now + duration : 0L);
        track(owner, mob);
        SummonerSummons.tuneControlledMob(mob);
        mob.setTarget(null);
        return true;
    }

    public static boolean isHypno(Entity entity) {
        return entity.entityTags().contains(TAG_HYPNO);
    }

    public static UUID ownerUuid(Entity entity) {
        for (String tag : entity.entityTags()) {
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

    public static long untilTick(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(TAG_UNTIL_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_UNTIL_PREFIX.length());
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static int pruneAndCount(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return 0;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_HYPNO_MOBS);
        if (list.isEmpty()) {
            return 0;
        }
        ListTag next = new ListTag();
        int alive = 0;
        long now = GemsTime.now(owner);
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid == null) {
                continue;
            }
            Entity e = findEntityCached(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isHypno(mob)) {
                ENTITY_CACHE.remove(uuid);
                continue;
            }
            if (!owner.getUUID().equals(ownerUuid(mob))) {
                ENTITY_CACHE.remove(uuid);
                continue;
            }
            long until = untilTick(mob);
            if (until > 0 && now >= until) {
                release(mob);
                ENTITY_CACHE.remove(uuid);
                continue;
            }
            next.add(GemsNbt.fromUuid(uuid));
            alive++;
        }
        root.put(KEY_HYPNO_MOBS, next);
        return alive;
    }

    public static void releaseAll(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return;
        }
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntityCached(server, uuid);
            if (e instanceof Mob mob) {
                release(mob);
            }
            ENTITY_CACHE.remove(uuid);
        }
        ((GemsPersistentDataHolder) owner).gems$getPersistentData().remove(KEY_HYPNO_MOBS);
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
        int rangeBlocks = GemsBalance.v().legendary().hypnoRangeBlocks();
        LivingEntity fallbackTarget = normalizeTarget(owner, LegendaryTargeting.findTarget(owner, rangeBlocks, 0), rangeBlocks);
        if (fallbackTarget == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntityCached(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isHypno(mob)) {
                ENTITY_CACHE.remove(uuid);
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
        if (target instanceof ServerPlayer playerTarget && GemTrust.isTrusted(owner, playerTarget)) {
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
            if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isHypno(mobTarget)) {
                return null;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUUID()) && SummonerSummons.isSummon(mobTarget)) {
                return null;
            }
        }
        return target;
    }

    public static void commandMobs(ServerPlayer owner, LivingEntity target, int rangeBlocks) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return;
        }
        if (rangeBlocks <= 0) {
            return;
        }
        if (target instanceof ServerPlayer playerTarget && GemTrust.isTrusted(owner, playerTarget)) {
            return;
        }
        if (target instanceof Mob mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isHypno(mobTarget)) {
                return;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUUID()) && SummonerSummons.isSummon(mobTarget)) {
                return;
            }
        }
        double rangeSq = rangeBlocks * (double) rangeBlocks;
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntityCached(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isHypno(mob)) {
                ENTITY_CACHE.remove(uuid);
                continue;
            }
            if (mob.level() != target.level()) {
                continue;
            }
            if (mob.distanceToSqr(owner) > rangeSq) {
                continue;
            }
            mob.setTarget(target);
        }
    }

    public static boolean isAllowed(Mob mob) {
        return !MobBlacklist.isBlacklisted(mob);
    }

    private static void mark(Mob mob, UUID owner, long untilTick) {
        List<String> tags = List.copyOf(mob.entityTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_OWNER_PREFIX) || tag.startsWith(TAG_UNTIL_PREFIX)) {
                mob.removeTag(tag);
            }
        }
        mob.addTag(TAG_HYPNO);
        mob.addTag(TAG_OWNER_PREFIX + owner);
        if (untilTick > 0) {
            mob.addTag(TAG_UNTIL_PREFIX + untilTick);
        }
    }

    private static void release(Mob mob) {
        List<String> tags = List.copyOf(mob.entityTags());
        for (String tag : tags) {
            if (tag.equals(TAG_HYPNO) || tag.startsWith(TAG_OWNER_PREFIX) || tag.startsWith(TAG_UNTIL_PREFIX)) {
                mob.removeTag(tag);
            }
        }
        ENTITY_CACHE.remove(mob.getUUID());
    }

    private static void track(ServerPlayer owner, Mob mob) {
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_HYPNO_MOBS);
        list.add(GemsNbt.fromUuid(mob.getUUID()));
        root.put(KEY_HYPNO_MOBS, list);
    }

    private static List<UUID> ownedMobUuids(ServerPlayer owner) {
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getList(KEY_HYPNO_MOBS).orElse(null);
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

    private static Entity findEntityCached(MinecraftServer server, UUID uuid) {
        Entity cached = ENTITY_CACHE.get(uuid);
        if (cached != null && !cached.isRemoved() && cached.level().getServer() == server) {
            return cached;
        }
        ENTITY_CACHE.remove(uuid);
        for (ServerLevel world : server.getAllLevels()) {
            Entity e = world.getEntity(uuid);
            if (e != null) {
                ENTITY_CACHE.put(uuid, e);
                return e;
            }
        }
        return null;
    }
}
