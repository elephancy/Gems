package com.feel.gems.power.gem.summoner;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.util.GemsNbt;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;


public final class SummonerSummons {
    public static final String TAG_SUMMON = "gems_summon";
    private static final String TAG_OWNER_PREFIX = "gems_summon_owner:";
    private static final String TAG_UNTIL_PREFIX = "gems_summon_until:";

    private static final String KEY_SUMMONS = "summonerSummons";

    private static final Identifier BONUS_HEALTH_MODIFIER_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "summoner_bonus_health");

    private SummonerSummons() {
    }

    public static void mark(Entity entity, UUID owner, long untilTick) {
        entity.addTag(TAG_SUMMON);
        entity.addTag(TAG_OWNER_PREFIX + owner);
        entity.addTag(TAG_UNTIL_PREFIX + untilTick);
    }

    public static boolean isSummon(Entity entity) {
        return entity.entityTags().contains(TAG_SUMMON);
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

    public static long untilTick(Entity entity) {
        Set<String> tags = entity.entityTags();
        for (String tag : tags) {
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

    public static void trackSpawn(ServerPlayer owner, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_SUMMONS);
        list.add(GemsNbt.fromUuid(entity.getUUID()));
        root.put(KEY_SUMMONS, list);
    }

    public static List<UUID> ownedSummonUuids(ServerPlayer owner) {
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getList(KEY_SUMMONS).orElse(null);
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

    public record SummonStats(int count, int points) {
    }

    public static int pruneAndCount(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return 0;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_SUMMONS);
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
            Entity e = findEntity(server, uuid);
            if (e != null && e.isAlive() && isSummon(e) && owner.getUUID().equals(ownerUuid(e))) {
                next.add(GemsNbt.fromUuid(uuid));
                alive++;
            }
        }
        root.put(KEY_SUMMONS, next);
        return alive;
    }

    public static SummonStats pruneAndPoints(ServerPlayer owner, GemsBalance.Summoner cfg) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return new SummonStats(0, 0);
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        ListTag list = root.getListOrEmpty(KEY_SUMMONS);
        if (list.isEmpty()) {
            return new SummonStats(0, 0);
        }
        ListTag next = new ListTag();
        int alive = 0;
        int points = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid == null) {
                continue;
            }
            Entity e = findEntity(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            if (!owner.getUUID().equals(ownerUuid(mob))) {
                continue;
            }
            next.add(GemsNbt.fromUuid(uuid));
            alive++;
            points += costForEntity(cfg.costs(), mob.getType());
        }
        root.put(KEY_SUMMONS, next);
        return new SummonStats(alive, points);
    }

    public static void discardAll(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return;
        }
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (e != null) {
                e.discard();
            }
        }
        ((GemsPersistentDataHolder) owner).gems$getPersistentData().remove(KEY_SUMMONS);
    }

    public static void commandSummons(ServerPlayer owner, LivingEntity target, int rangeBlocks, int strengthAmplifier, int durationTicks) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) {
            return;
        }
        if (target instanceof Mob mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isSummon(mobTarget)) {
                return;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUUID()) && HypnoControl.isHypno(mobTarget)) {
                return;
            }
        }
        double rangeSq = rangeBlocks * (double) rangeBlocks;
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            if (mob.level() != target.level()) {
                continue;
            }
            if (mob.distanceToSqr(owner) > rangeSq) {
                continue;
            }
            mob.setTarget(target);
            if (strengthAmplifier >= 0 && durationTicks > 0) {
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, durationTicks, strengthAmplifier, true, false, false));
            }
        }
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
        UUID markTarget = SummonerCommanderMark.activeTargetUuid(owner);
        Entity markEntity = markTarget == null ? null : findEntity(server, markTarget);
        LivingEntity markLiving = markEntity instanceof LivingEntity living ? living : null;
        LivingEntity fallbackTarget = markLiving == null ? LegendaryTargeting.findTarget(owner, rangeBlocks, 0) : null;
        if (fallbackTarget == null && markLiving == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof Mob mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            refreshNetherMob(mob);
            if (mob.level() != owner.level()) {
                continue;
            }
            LivingEntity desiredTarget = markLiving != null ? markLiving : fallbackTarget;
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
                double rangeSq = rangeBlocks * (double) rangeBlocks;
                if (mob.distanceToSqr(owner) <= rangeSq) {
                    if (desiredTarget instanceof Mob mobTarget) {
                        UUID targetOwner = ownerUuid(mobTarget);
                        if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isSummon(mobTarget)) {
                            continue;
                        }
                        UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
                        if (hypnoOwner != null && hypnoOwner.equals(owner.getUUID()) && HypnoControl.isHypno(mobTarget)) {
                            continue;
                        }
                    }
                    mob.setTarget(desiredTarget);
                    continue;
                }
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
            if (targetOwner != null && targetOwner.equals(owner.getUUID()) && isSummon(mobTarget)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUUID()) && HypnoControl.isHypno(mobTarget)) {
                return null;
            }
        }
        return target;
    }

    public static void applyBonusHealth(Mob mob, float bonusHealth) {
        if (bonusHealth <= 0.0F) {
            return;
        }
        var inst = mob.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(BONUS_HEALTH_MODIFIER_ID);
        inst.addPermanentModifier(new AttributeModifier(BONUS_HEALTH_MODIFIER_ID, bonusHealth, AttributeModifier.Operation.ADD_VALUE));
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + bonusHealth));
    }

    public static void tuneControlledMob(Mob mob) {
        if (mob == null) {
            return;
        }
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.setPersistenceRequired();
        refreshNetherMob(mob);
        clearAnger(mob);
        clearBrainAggro(mob);
        if (mob instanceof PiglinBrute brute) {
            brute.setTarget(null);
            brute.setLastHurtByMob(null);
        }
    }

    public static void refreshNetherMob(Mob mob) {
        if (mob instanceof Piglin piglin) {
            piglin.setImmuneToZombification(true);
        }
        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        if (mob instanceof PiglinBrute brute) {
            brute.setImmuneToZombification(true);
        }
    }

    public static void clearAnger(Mob mob) {
        if (mob instanceof NeutralMob angerable) {
            angerable.stopBeingAngry();
        }
    }

    private static void clearBrainAggro(Mob mob) {
        var brain = mob.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
        brain.eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE);
        brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
    }

    public static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity e = world.getEntity(uuid);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public static long computeUntilTick(ServerPlayer owner, int lifetimeTicks) {
        if (lifetimeTicks <= 0) {
            return 0L;
        }
        return GemsTime.now(owner) + lifetimeTicks;
    }

    public static void applyCooldown(ServerPlayer owner) {
        if (GemsAdmin.noCooldowns(owner)) {
            return;
        }
        int cooldownTicks = GemsBalance.v().summoner().summonSlotCooldownTicks();
        if (cooldownTicks <= 0) {
            return;
        }
        cooldownTicks = applyCooldownModifiers(owner, cooldownTicks);
        long now = GemsTime.now(owner);
        long until = now + cooldownTicks;
        if (GemAbilityCooldowns.nextAllowedTick(owner, PowerIds.SUMMON_SLOT_1) > now) {
            return;
        }
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_1, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_2, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_3, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_4, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_5, until);
        GemCooldownSync.send(owner);
    }

    private static int applyCooldownModifiers(ServerPlayer player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player)
                * LegendaryCooldowns.getCooldownMultiplier(player)
                * com.feel.gems.augment.AugmentRuntime.cooldownMultiplier(player, GemId.SUMMONER);
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }

    public static int costForEntity(java.util.Map<String, Integer> costs, EntityType<?> type) {
        if (costs == null || costs.isEmpty() || type == null) {
            return 0;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return 0;
        }
        Integer cost = costs.get(id.toString());
        return cost == null ? 0 : Math.max(0, cost);
    }
}

