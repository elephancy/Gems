package com.feel.gems.power.runtime;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.power.ability.astra.ShadowAnchorAbility;
import com.feel.gems.power.ability.fire.FireballAbility;
import com.feel.gems.power.ability.sentinel.SentinelInterventionRuntime;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
import com.feel.gems.power.gem.speed.SpeedMomentum;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.gem.wealth.EnchantmentAmplification;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTeleport;
import com.feel.gems.util.GemsTime;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;




public final class AbilityRuntime {
    private static final String KEY_CAMPFIRE_UNTIL = "cosyCampfireUntil";
    private static final String KEY_HEAT_HAZE_UNTIL = "heatHazeUntil";
    private static final String KEY_SPEED_STORM_UNTIL = "speedStormUntil";
    private static final String KEY_SPEED_STORM_SCALE = "speedStormScale";
    private static final String KEY_SPEED_SLIPSTREAM_UNTIL = "speedSlipstreamUntil";
    private static final String KEY_SPEED_SLIPSTREAM_SCALE = "speedSlipstreamScale";
    private static final String KEY_SPEED_SLIPSTREAM_DIR_X = "speedSlipstreamDirX";
    private static final String KEY_SPEED_SLIPSTREAM_DIR_Z = "speedSlipstreamDirZ";
    private static final String KEY_SPEED_SLIPSTREAM_ORIGIN_X = "speedSlipstreamOriginX";
    private static final String KEY_SPEED_SLIPSTREAM_ORIGIN_Y = "speedSlipstreamOriginY";
    private static final String KEY_SPEED_SLIPSTREAM_ORIGIN_Z = "speedSlipstreamOriginZ";
    private static final String KEY_SPEED_AFTERIMAGE_UNTIL = "speedAfterimageUntil";
    private static final String KEY_SPEED_TEMPO_UNTIL = "speedTempoUntil";
    private static final String KEY_SPACE_GRAVITY_UNTIL = "spaceGravityUntil";
    private static final String KEY_SPACE_GRAVITY_CASTER = "spaceGravityCaster";

    private static final String KEY_UNBOUNDED_UNTIL = "unboundedUntil";
    private static final String KEY_UNBOUNDED_PREV_GAMEMODE = "unboundedPrevGamemode";

    private static final String KEY_ASTRAL_CAMERA_UNTIL = "astralCameraUntil";
    private static final String KEY_ASTRAL_CAMERA_PREV_GAMEMODE = "astralCameraPrevGamemode";
    private static final String KEY_ASTRAL_CAMERA_RETURN_DIM = "astralCameraReturnDim";
    private static final String KEY_ASTRAL_CAMERA_RETURN_POS = "astralCameraReturnPos";
    private static final String KEY_ASTRAL_CAMERA_RETURN_YAW = "astralCameraReturnYaw";
    private static final String KEY_ASTRAL_CAMERA_RETURN_PITCH = "astralCameraReturnPitch";

    private static final String KEY_LIFE_CIRCLE_UNTIL = "lifeCircleUntil";
    private static final String KEY_LIFE_CIRCLE_CASTER = "lifeCircleCaster";

    private static final String KEY_HEART_LOCK_UNTIL = "heartLockUntil";
    private static final String KEY_HEART_LOCK_CASTER = "heartLockCaster";
    private static final String KEY_HEART_LOCK_LOCKED_MAX = "heartLockLockedMax";

    private static final String KEY_BOUNTY_UNTIL = "bountyUntil";
    private static final String KEY_BOUNTY_TARGET = "bountyTarget";

    private static final String KEY_CHAD_UNTIL = "chadUntil";
    private static final String KEY_CHAD_HITS = "chadHits";

    private static final String KEY_RICH_RUSH_UNTIL = "richRushUntil";

    private static final String KEY_AMPLIFICATION_UNTIL = "amplificationUntil";

    private static final String KEY_DUELIST_RAPID_UNTIL = "duelistRapidUntil";
    private static final Identifier MOD_DUELIST_RAPID_ATTACK_SPEED = Identifier.fromNamespaceAndPath("gems", "duelist_rapid_attack_speed");

    // Reaper runtime state
    private static final String KEY_REAPER_WITHERING_UNTIL = "reaperWitheringUntil";
    private static final String KEY_REAPER_OATH_UNTIL = "reaperDeathOathUntil";
    private static final String KEY_REAPER_OATH_TARGET = "reaperDeathOathTarget";
    private static final String KEY_REAPER_STEED_LIST = "reaperGraveSteedList";
    private static final String KEY_REAPER_CLONE_UNTIL = "reaperShadowCloneUntil";
    private static final String KEY_REAPER_CLONE_UUIDS = "reaperShadowCloneUuids";
    private static final String KEY_REAPER_RETRIBUTION_UNTIL = "reaperRetributionUntil";

    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";
    private static final String CUSTOM_DATA_KEY_OWNER = "gemsOwner";
    private static final String CUSTOM_DATA_KEY_OWNER_NAME = "gemsOwnerName";
    private static final String CUSTOM_DATA_KEY_FIRST_OWNER = "gemsFirstOwner";
    private static final String CUSTOM_DATA_KEY_FIRST_OWNER_NAME = "gemsFirstOwnerName";
    private static final String CUSTOM_DATA_KEY_PREV_OWNER = "gemsPrevOwner";
    private static final String CUSTOM_DATA_KEY_PREV_OWNER_NAME = "gemsPrevOwnerName";

    private AbilityRuntime() {
    }

    public static void tickEverySecond(ServerPlayer player) {
        long now = GemsTime.now(player);

        FireballAbility.tickCharging(player, now);
        ShadowAnchorAbility.tick(player, now);
        tickCosyCampfire(player, now);
        tickHeatHazeZone(player, now);
        tickSpeedStorm(player, now);
        tickSpeedSlipstream(player, now);
        tickSpeedAfterimage(player, now);
        tickSpeedTempoShift(player, now);
        tickSpaceGravityField(player, now);
        tickLifeCircle(player, now);
        tickHeartLock(player, now);
        tickUnbounded(player, now);
        tickAstralCamera(player, now);
        tickBounty(player, now);
        tickAmplificationCleanup(player, now);
        tickReaperGraveSteed(player, now);
        tickReaperDeathOath(player, now);
        tickReaperBloodChargeCharging(player, now);
        tickReaperShadowClone(player, now);
        tickReaperRetribution(player, now);
        tickDuelistRapidStrike(player, now);
    }

    public static void startCosyCampfire(ServerPlayer player, int durationTicks) {
        persistent(player).putLong(KEY_CAMPFIRE_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startHeatHazeZone(ServerPlayer player, int durationTicks) {
        persistent(player).putLong(KEY_HEAT_HAZE_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startSpeedStorm(ServerPlayer player, int durationTicks, float scale) {
        long now = GemsTime.now(player);
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_SPEED_STORM_UNTIL, now + durationTicks);
        nbt.putFloat(KEY_SPEED_STORM_SCALE, scale);
    }

    public static void startSpeedSlipstream(ServerPlayer player, Vec3 direction, int durationTicks, float scale) {
        if (durationTicks <= 0) {
            return;
        }
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() <= 1.0E-4D) {
            return;
        }
        flat = flat.normalize();

        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_SPEED_SLIPSTREAM_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putFloat(KEY_SPEED_SLIPSTREAM_SCALE, scale);
        nbt.putDouble(KEY_SPEED_SLIPSTREAM_DIR_X, flat.x);
        nbt.putDouble(KEY_SPEED_SLIPSTREAM_DIR_Z, flat.z);
        nbt.putDouble(KEY_SPEED_SLIPSTREAM_ORIGIN_X, player.getX());
        nbt.putDouble(KEY_SPEED_SLIPSTREAM_ORIGIN_Y, player.getY());
        nbt.putDouble(KEY_SPEED_SLIPSTREAM_ORIGIN_Z, player.getZ());
    }

    public static void startSpeedAfterimage(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_SPEED_AFTERIMAGE_UNTIL, GemsTime.now(player) + durationTicks);

        int speedAmp = GemsBalance.v().speed().afterimageSpeedAmplifier();
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, speedAmp, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, true, false, false));
    }

    public static void startSpeedTempoShift(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_SPEED_TEMPO_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startDuelistRapidStrike(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            clearDuelistRapidStrike(player);
            return;
        }
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_DUELIST_RAPID_UNTIL, GemsTime.now(player) + durationTicks);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(MOD_DUELIST_RAPID_ATTACK_SPEED);
            attackSpeed.addPermanentModifier(new AttributeModifier(
                    MOD_DUELIST_RAPID_ATTACK_SPEED,
                    100.0F,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    public static void breakSpeedAfterimage(ServerPlayer player) {
        if (!isSpeedAfterimageActive(player)) {
            return;
        }
        clearSpeedAfterimage(player);
        AbilityFeedback.burst(player, net.minecraft.core.particles.ParticleTypes.SMOKE, 10, 0.2D);
    }

    private static boolean isSpeedAfterimageActive(ServerPlayer player) {
        return persistent(player).getLongOr(KEY_SPEED_AFTERIMAGE_UNTIL, 0L) > GemsTime.now(player);
    }

    public static void startSpaceGravityField(ServerPlayer player, int durationTicks) {
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_SPACE_GRAVITY_UNTIL, GemsTime.now(player) + durationTicks);
        GemsNbt.putUuid(nbt, KEY_SPACE_GRAVITY_CASTER, player.getUUID());
    }

    public static void startReaperWitheringStrikes(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_REAPER_WITHERING_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static boolean isReaperWitheringStrikesActive(ServerPlayer player, long now) {
        long until = persistent(player).getLongOr(KEY_REAPER_WITHERING_UNTIL, 0L);
        return until > now;
    }

    public static void startReaperDeathOath(ServerPlayer player, UUID target, int durationTicks) {
        if (durationTicks <= 0 || target == null) {
            return;
        }
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_REAPER_OATH_UNTIL, GemsTime.now(player) + durationTicks);
        GemsNbt.putUuid(nbt, KEY_REAPER_OATH_TARGET, target);
    }

    public static UUID reaperDeathOathTarget(ServerPlayer player) {
        return GemsNbt.getUuid(persistent(player), KEY_REAPER_OATH_TARGET);
    }

    public static void clearReaperDeathOath(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_REAPER_OATH_UNTIL);
        nbt.remove(KEY_REAPER_OATH_TARGET);
    }

    public static void startReaperGraveSteed(ServerPlayer player, UUID steedUuid, int durationTicks) {
        if (durationTicks <= 0 || steedUuid == null) {
            return;
        }
        CompoundTag nbt = persistent(player);
        long until = GemsTime.now(player) + durationTicks;
        ListTag list = nbt.getList(KEY_REAPER_STEED_LIST).orElse(new ListTag());
        CompoundTag entry = new CompoundTag();
        GemsNbt.putUuid(entry, "uuid", steedUuid);
        entry.putLong("until", until);
        list.add(entry);
        nbt.put(KEY_REAPER_STEED_LIST, list);
    }

    public static void startReaperShadowClone(ServerPlayer player, java.util.List<UUID> cloneUuids, int durationTicks) {
        if (durationTicks <= 0 || cloneUuids == null || cloneUuids.isEmpty()) {
            return;
        }
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_REAPER_CLONE_UNTIL, GemsTime.now(player) + durationTicks);
        writeUuidList(nbt, KEY_REAPER_CLONE_UUIDS, cloneUuids);
    }

    public static void startReaperRetribution(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_REAPER_RETRIBUTION_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static boolean isReaperRetributionActive(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_REAPER_RETRIBUTION_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_REAPER_RETRIBUTION_UNTIL);
            return false;
        }
        return true;
    }

    public static boolean isReaperDeathOathActive(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_REAPER_OATH_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            clearReaperDeathOath(player);
            return false;
        }
        return true;
    }

    public static void startReaperBloodChargeCharging(ServerPlayer player, int maxChargeTicks) {
        if (maxChargeTicks <= 0) {
            return;
        }
        CompoundTag nbt = persistent(player);
        nbt.putLong(ReaperBloodCharge.KEY_CHARGING_UNTIL, GemsTime.now(player) + maxChargeTicks);
        nbt.putInt(ReaperBloodCharge.KEY_CHARGED_TICKS, 0);
    }

    public static boolean finishReaperBloodChargeCharging(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(ReaperBloodCharge.KEY_CHARGING_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        int charged = nbt.getIntOr(ReaperBloodCharge.KEY_CHARGED_TICKS, 0);
        int maxCharge = GemsBalance.v().reaper().bloodChargeMaxChargeTicks();
        float mult = ReaperBloodCharge.computeMultiplier(charged, Math.max(1, maxCharge));
        int buffDuration = GemsBalance.v().reaper().bloodChargeBuffDurationTicks();
        ReaperBloodCharge.setBuff(player, mult, buffDuration);
        ReaperBloodCharge.clearCharging(player);
        return true;
    }

    public static void startUnbounded(ServerPlayer player, int durationTicks) {
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_UNBOUNDED_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putString(KEY_UNBOUNDED_PREV_GAMEMODE, player.gameMode.getGameModeForPlayer().getName());
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void startAstralCamera(ServerPlayer player, int durationTicks) {
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_ASTRAL_CAMERA_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putString(KEY_ASTRAL_CAMERA_PREV_GAMEMODE, player.gameMode.getGameModeForPlayer().getName());
        nbt.putString(KEY_ASTRAL_CAMERA_RETURN_DIM, player.level().dimension().identifier().toString());
        nbt.putLong(KEY_ASTRAL_CAMERA_RETURN_POS, player.blockPosition().asLong());
        nbt.putFloat(KEY_ASTRAL_CAMERA_RETURN_YAW, player.getYRot());
        nbt.putFloat(KEY_ASTRAL_CAMERA_RETURN_PITCH, player.getXRot());
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void startLifeCircle(ServerPlayer caster, int durationTicks) {
        CompoundTag nbt = persistent(caster);
        nbt.putLong(KEY_LIFE_CIRCLE_UNTIL, GemsTime.now(caster) + durationTicks);
        GemsNbt.putUuid(nbt, KEY_LIFE_CIRCLE_CASTER, caster.getUUID());
    }

    public static void startHeartLock(ServerPlayer caster, ServerPlayer target, int durationTicks) {
        CompoundTag nbt = persistent(target);
        nbt.putLong(KEY_HEART_LOCK_UNTIL, GemsTime.now(target) + durationTicks);
        GemsNbt.putUuid(nbt, KEY_HEART_LOCK_CASTER, caster.getUUID());

        double currentMax = target.getAttributeValue(Attributes.MAX_HEALTH);
        double lockedMax = Math.max(2.0D, target.getHealth());
        nbt.putFloat(KEY_HEART_LOCK_LOCKED_MAX, (float) lockedMax);
        double delta = lockedMax - currentMax;
        Identifier modifierId = heartLockModifierId(caster.getUUID());
        applyMaxHealthModifier(target, modifierId, delta);
    }

    public static void startBounty(ServerPlayer hunter, UUID target, int durationTicks) {
        CompoundTag nbt = persistent(hunter);
        nbt.putLong(KEY_BOUNTY_UNTIL, GemsTime.now(hunter) + durationTicks);
        GemsNbt.putUuid(nbt, KEY_BOUNTY_TARGET, target);
    }

    public static void startChadStrength(ServerPlayer player, int durationTicks) {
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_CHAD_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putInt(KEY_CHAD_HITS, 0);
    }

    public static boolean isChadStrengthActive(ServerPlayer player) {
        long now = GemsTime.now(player);
        return persistent(player).getLongOr(KEY_CHAD_UNTIL, 0L) > now;
    }

    public static int incrementChadHit(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        int next = nbt.getIntOr(KEY_CHAD_HITS, 0) + 1;
        nbt.putInt(KEY_CHAD_HITS, next);
        return next;
    }

    public static void startRichRush(ServerPlayer player, int durationTicks) {
        persistent(player).putLong(KEY_RICH_RUSH_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startAmplification(ServerPlayer player, int durationTicks) {
        persistent(player).putLong(KEY_AMPLIFICATION_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static boolean isRichRushActive(ServerPlayer player) {
        long now = GemsTime.now(player);
        return persistent(player).getLongOr(KEY_RICH_RUSH_UNTIL, 0L) > now;
    }

    public static void setOwnerIfMissing(ItemStack stack, UUID owner) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing != null && GemsNbt.containsUuid(existing.copyTag(), CUSTOM_DATA_KEY_OWNER)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> GemsNbt.putUuid(nbt, CUSTOM_DATA_KEY_OWNER, owner));
    }

    /**
     * Sets owner UUID and name on an item (for display purposes).
     * Also sets first owner if not already set.
     */
    public static void setOwnerWithName(ItemStack stack, UUID owner, String ownerName) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            UUID current = GemsNbt.getUuid(nbt, CUSTOM_DATA_KEY_OWNER);
            if (current != null && !current.equals(owner)) {
                GemsNbt.putUuid(nbt, CUSTOM_DATA_KEY_PREV_OWNER, current);
                String currentName = nbt.getStringOr(CUSTOM_DATA_KEY_OWNER_NAME, "");
                if (!currentName.isEmpty()) {
                    nbt.putString(CUSTOM_DATA_KEY_PREV_OWNER_NAME, currentName);
                }
            }
            GemsNbt.putUuid(nbt, CUSTOM_DATA_KEY_OWNER, owner);
            if (ownerName != null && !ownerName.isEmpty()) {
                nbt.putString(CUSTOM_DATA_KEY_OWNER_NAME, ownerName);
            }
            // Set first owner only if not already set
            if (!GemsNbt.containsUuid(nbt, CUSTOM_DATA_KEY_FIRST_OWNER)) {
                GemsNbt.putUuid(nbt, CUSTOM_DATA_KEY_FIRST_OWNER, owner);
                if (ownerName != null && !ownerName.isEmpty()) {
                    nbt.putString(CUSTOM_DATA_KEY_FIRST_OWNER_NAME, ownerName);
                }
            }
        });
    }

    public static UUID getOwner(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        return GemsNbt.getUuid(custom.copyTag(), CUSTOM_DATA_KEY_OWNER);
    }

    /**
     * Gets the owner name stored on an item, if available.
     */
    public static String getOwnerName(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag nbt = custom.copyTag();
        if (!nbt.contains(CUSTOM_DATA_KEY_OWNER_NAME)) {
            return null;
        }
        return nbt.getString(CUSTOM_DATA_KEY_OWNER_NAME).orElse(null);
    }

    /**
     * Gets the FIRST owner UUID from an item (the original crafter/finder).
     */
    public static UUID getFirstOwner(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        return GemsNbt.getUuid(custom.copyTag(), CUSTOM_DATA_KEY_FIRST_OWNER);
    }

    /**
     * Gets the PREVIOUS owner UUID from an item, if available.
     */
    public static UUID getPreviousOwner(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        return GemsNbt.getUuid(custom.copyTag(), CUSTOM_DATA_KEY_PREV_OWNER);
    }

    /**
     * Gets the FIRST owner name stored on an item, if available.
     */
    public static String getFirstOwnerName(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag nbt = custom.copyTag();
        if (!nbt.contains(CUSTOM_DATA_KEY_FIRST_OWNER_NAME)) {
            return null;
        }
        return nbt.getString(CUSTOM_DATA_KEY_FIRST_OWNER_NAME).orElse(null);
    }

    /**
     * Gets the PREVIOUS owner name stored on an item, if available.
     */
    public static String getPreviousOwnerName(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag nbt = custom.copyTag();
        if (!nbt.contains(CUSTOM_DATA_KEY_PREV_OWNER_NAME)) {
            return null;
        }
        return nbt.getString(CUSTOM_DATA_KEY_PREV_OWNER_NAME).orElse(null);
    }

    private static java.util.List<UUID> readUuidList(CompoundTag nbt, String key) {
        ListTag list = nbt.getList(key).orElse(null);
        if (list == null || list.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<UUID> out = new java.util.ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid != null) {
                out.add(uuid);
            }
        }
        return out;
    }

    private static void writeUuidList(CompoundTag nbt, String key, java.util.Collection<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID uuid : uuids) {
            list.add(GemsNbt.fromUuid(uuid));
        }
        nbt.put(key, list);
    }

    private static void tickCosyCampfire(ServerPlayer player, long now) {
        if (persistent(player).getLongOr(KEY_CAMPFIRE_UNTIL, 0L) <= now) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        int radius = GemsBalance.v().fire().cosyCampfireRadiusBlocks();
        int amp = GemsBalance.v().fire().cosyCampfireRegenAmplifier();
        Vec3 origin = player.position();
        AbilityFeedback.ring(world, origin.add(0.0D, 0.2D, 0.0D), Math.min(6.0D, radius), net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 20);
        for (LivingEntity other : livingInRadius(world, origin, radius)) {
            if (!(other instanceof ServerPlayer otherPlayer) || !GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, amp, true, false, false));
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.HEART, 1, 0.1D);
        }
    }

    private static void tickHeatHazeZone(ServerPlayer player, long now) {
        if (persistent(player).getLongOr(KEY_HEAT_HAZE_UNTIL, 0L) <= now) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        int duration = 40;
        int radius = GemsBalance.v().fire().heatHazeRadiusBlocks();
        int fatigueAmp = GemsBalance.v().fire().heatHazeEnemyMiningFatigueAmplifier();
        int weaknessAmp = GemsBalance.v().fire().heatHazeEnemyWeaknessAmplifier();
        Vec3 origin = player.position();
        AbilityFeedback.ring(world, origin.add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.core.particles.ParticleTypes.FLAME, 24);
        for (LivingEntity other : livingInRadius(world, origin, radius)) {
            if (other instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                continue;
            }
            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
            if (ally) {
                other.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.FLAME, 1, 0.05D);
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, fatigueAmp, true, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, weaknessAmp, true, false, false));
                AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.SMOKE, 1, 0.08D);
            }
        }
    }

    private static void tickSpeedStorm(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        if (nbt.getLongOr(KEY_SPEED_STORM_UNTIL, 0L) <= now) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        float scale = nbt.getFloatOr(KEY_SPEED_STORM_SCALE, 1.0F);
        if (scale <= 0.0F) {
            scale = 1.0F;
        }
        int radius = SpeedMomentum.scaleInt(GemsBalance.v().speed().speedStormRadiusBlocks(), scale, 1);
        int allySpeed = SpeedMomentum.scaleInt(GemsBalance.v().speed().speedStormAllySpeedAmplifier(), scale, 0);
        int allyHaste = SpeedMomentum.scaleInt(GemsBalance.v().speed().speedStormAllyHasteAmplifier(), scale, 0);
        int enemySlow = SpeedMomentum.scaleInt(GemsBalance.v().speed().speedStormEnemySlownessAmplifier(), scale, 0);
        int enemyFatigue = SpeedMomentum.scaleInt(GemsBalance.v().speed().speedStormEnemyMiningFatigueAmplifier(), scale, 0);
        Vec3 origin = player.position();
        AbilityFeedback.ring(world, origin.add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.core.particles.ParticleTypes.CLOUD, 24);
        for (LivingEntity other : livingInRadius(world, origin, radius)) {
            if (other instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                continue;
            }
            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
            if (ally) {
                other.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, allySpeed, true, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, allyHaste, true, false, false));
                AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.CLOUD, 1, 0.1D);
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, enemySlow, true, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, enemyFatigue, true, false, false));
                AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, 1, 0.1D);
            }
        }
    }

    private static void tickSpeedSlipstream(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SPEED_SLIPSTREAM_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            clearSpeedSlipstream(nbt);
            return;
        }
        if (!nbt.contains(KEY_SPEED_SLIPSTREAM_DIR_X) || !nbt.contains(KEY_SPEED_SLIPSTREAM_DIR_Z)) {
            clearSpeedSlipstream(nbt);
            return;
        }

        float scale = nbt.getFloatOr(KEY_SPEED_SLIPSTREAM_SCALE, 1.0F);
        if (scale <= 0.0F) {
            scale = 1.0F;
        }

        Vec3 dir = new Vec3(nbt.getDoubleOr(KEY_SPEED_SLIPSTREAM_DIR_X, 0.0D), 0.0D, nbt.getDoubleOr(KEY_SPEED_SLIPSTREAM_DIR_Z, 0.0D));
        if (dir.lengthSqr() <= 1.0E-4D) {
            clearSpeedSlipstream(nbt);
            return;
        }
        dir = dir.normalize();

        Vec3 origin = new Vec3(
                nbt.getDoubleOr(KEY_SPEED_SLIPSTREAM_ORIGIN_X, player.getX()),
                nbt.getDoubleOr(KEY_SPEED_SLIPSTREAM_ORIGIN_Y, player.getY()),
                nbt.getDoubleOr(KEY_SPEED_SLIPSTREAM_ORIGIN_Z, player.getZ())
        );

        double length = SpeedMomentum.scaleDouble(GemsBalance.v().speed().slipstreamLengthBlocks(), scale);
        double radius = SpeedMomentum.scaleDouble(GemsBalance.v().speed().slipstreamRadiusBlocks(), scale);
        if (length <= 0.0D || radius <= 0.0D) {
            return;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        Vec3 end = origin.add(dir.scale(length));
        AABB box = new AABB(origin, end).inflate(radius, 2.0D, radius);

        int allyAmp = SpeedMomentum.scaleInt(GemsBalance.v().speed().slipstreamAllySpeedAmplifier(), scale, 0);
        int enemyAmp = SpeedMomentum.scaleInt(GemsBalance.v().speed().slipstreamEnemySlownessAmplifier(), scale, 0);
        double knockback = GemsBalance.v().speed().slipstreamEnemyKnockback() * scale;

        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive())) {
            if (other instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                continue;
            }
            Vec3 pos = other.position();
            double t = pos.subtract(origin).dot(dir);
            if (t < 0.0D || t > length) {
                continue;
            }
            Vec3 closest = origin.add(dir.scale(t));
            if (pos.distanceToSqr(closest) > radius * radius) {
                continue;
            }

            boolean trusted = other instanceof ServerPlayer otherPlayer && (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer));
            if (trusted) {
                other.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, allyAmp, true, false, false));
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, enemyAmp, true, false, false));
                if (knockback > 0.0D) {
                    other.push(dir.x * knockback, 0.02D, dir.z * knockback);
                    other.needsSync = true;
                }
            }
        }
    }

    private static void tickSpeedAfterimage(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SPEED_AFTERIMAGE_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            clearSpeedAfterimage(player);
        }
    }

    private static void tickSpeedTempoShift(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SPEED_TEMPO_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            nbt.remove(KEY_SPEED_TEMPO_UNTIL);
            return;
        }
        if (GemPlayerState.getEnergy(player) <= 0) {
            nbt.remove(KEY_SPEED_TEMPO_UNTIL);
            return;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.SPEED) {
            if (activeGem != GemId.PRISM || !PrismSelectionsState.hasAbility(player, PowerIds.SPEED_TEMPO_SHIFT)) {
                nbt.remove(KEY_SPEED_TEMPO_UNTIL);
                return;
            }
        }

        int radius = GemsBalance.v().speed().tempoShiftRadiusBlocks();
        int allyDelta = GemsBalance.v().speed().tempoShiftAllyCooldownTicksPerSecond();
        int enemyDelta = GemsBalance.v().speed().tempoShiftEnemyCooldownTicksPerSecond();
        if (radius <= 0 || (allyDelta <= 0 && enemyDelta <= 0)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        double radiusSq = radius * (double) radius;
        for (ServerPlayer other : world.players()) {
            if (other.distanceToSqr(player) > radiusSq) {
                continue;
            }
            if (other != player && VoidImmunity.shouldBlockEffect(player, other)) {
                continue;
            }
            boolean trusted = other == player || GemTrust.isTrusted(player, other);
            int delta = trusted ? -allyDelta : enemyDelta;
            if (delta == 0) {
                continue;
            }
            if (shiftCooldowns(other, delta, now)) {
                GemCooldownSync.send(other);
            }
        }
    }

    private static void tickDuelistRapidStrike(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_DUELIST_RAPID_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            clearDuelistRapidStrike(player);
        }
    }

    private static void clearDuelistRapidStrike(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_DUELIST_RAPID_UNTIL);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(MOD_DUELIST_RAPID_ATTACK_SPEED);
        }
    }

    private static void clearSpeedAfterimage(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_SPEED_AFTERIMAGE_UNTIL);

        player.removeEffect(MobEffects.INVISIBILITY);
        int amp = GemsBalance.v().speed().afterimageSpeedAmplifier();
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == amp && !speed.isInfiniteDuration()) {
            player.removeEffect(MobEffects.SPEED);
        }
    }

    private static boolean shiftCooldowns(ServerPlayer player, int deltaTicks, long now) {
        GemId activeGem = GemPlayerState.getActiveGem(player);
        List<Identifier> abilities;
        if (activeGem == GemId.PRISM) {
            var server = player.level().getServer();
            if (server == null) {
                return false;
            }
            abilities = PrismSelectionsState.get(server).getSelection(player.getUUID()).allAbilities();
        } else {
            abilities = GemRegistry.definition(activeGem).abilities();
        }
        boolean changed = false;
        for (Identifier id : abilities) {
            long next = GemAbilityCooldowns.nextAllowedTick(player, id);
            if (next <= now) {
                continue;
            }
            long updated = deltaTicks < 0 ? Math.max(now, next + deltaTicks) : next + deltaTicks;
            if (updated != next) {
                GemAbilityCooldowns.setNextAllowedTick(player, id, updated);
                changed = true;
            }
        }
        return changed;
    }

    private static void clearSpeedSlipstream(CompoundTag nbt) {
        nbt.remove(KEY_SPEED_SLIPSTREAM_UNTIL);
        nbt.remove(KEY_SPEED_SLIPSTREAM_SCALE);
        nbt.remove(KEY_SPEED_SLIPSTREAM_DIR_X);
        nbt.remove(KEY_SPEED_SLIPSTREAM_DIR_Z);
        nbt.remove(KEY_SPEED_SLIPSTREAM_ORIGIN_X);
        nbt.remove(KEY_SPEED_SLIPSTREAM_ORIGIN_Y);
        nbt.remove(KEY_SPEED_SLIPSTREAM_ORIGIN_Z);
    }

    private static void tickSpaceGravityField(ServerPlayer caster, long now) {
        CompoundTag nbt = persistent(caster);
        long until = nbt.getLongOr(KEY_SPACE_GRAVITY_UNTIL, 0L);
        if (until <= 0) {
            return;
        }

        UUID casterId = GemsNbt.getUuid(nbt, KEY_SPACE_GRAVITY_CASTER);
        if (casterId == null) {
            casterId = caster.getUUID();
        }
        Identifier modifierId = spaceGravityModifierId(casterId);

        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
            return;
        }

        if (now >= until) {
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                applyGravityMultiplier(p, modifierId, 1.0F);
            }
            return;
        }

        if (!(caster.level() instanceof ServerLevel world)) {
            return;
        }
        int radius = GemsBalance.v().space().gravityFieldRadiusBlocks();
        float allyMult = GemsBalance.v().space().gravityFieldAllyGravityMultiplier();
        float enemyMult = GemsBalance.v().space().gravityFieldEnemyGravityMultiplier();
        double radiusSq = radius * (double) radius;
        AbilityFeedback.ring(world, caster.position().add(0.0D, 0.25D, 0.0D), Math.min(7.0D, radius), net.minecraft.core.particles.ParticleTypes.END_ROD, 18);

        // Update all players once per second: apply inside radius, remove if they left.
        for (ServerPlayer other : world.players()) {
            if (other != caster && VoidImmunity.shouldBlockEffect(caster, other)) {
                applyGravityMultiplier(other, modifierId, 1.0F);
                continue;
            }
            double distSq = other.distanceToSqr(caster);
            if (distSq > radiusSq) {
                applyGravityMultiplier(other, modifierId, 1.0F);
                continue;
            }

            float mult = GemTrust.isTrusted(caster, other) ? allyMult : enemyMult;
            applyGravityMultiplier(other, modifierId, mult);
        }
    }

    private static void applyGravityMultiplier(ServerPlayer player, Identifier modifierId, float multiplier) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        gravity.removeModifier(modifierId);
        if (multiplier != 1.0F) {
            double delta = (double) multiplier - 1.0D;
            gravity.addTransientModifier(new AttributeModifier(modifierId, delta, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static Identifier spaceGravityModifierId(UUID caster) {
        return Identifier.fromNamespaceAndPath("gems", "space_gravity_" + caster);
    }

    private static void tickLifeCircle(ServerPlayer caster, long now) {
        CompoundTag nbt = persistent(caster);
        long until = nbt.getLongOr(KEY_LIFE_CIRCLE_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        UUID casterId = GemsNbt.getUuid(nbt, KEY_LIFE_CIRCLE_CASTER);
        if (casterId == null) {
            casterId = caster.getUUID();
        }
        Identifier bonusId = lifeCircleBonusId(casterId);
        Identifier penaltyId = lifeCirclePenaltyId(casterId);

        if (!(caster.level() instanceof ServerLevel world)) {
            return;
        }

        if (until <= now) {
            for (ServerPlayer p : world.players()) {
                removeMaxHealthModifier(p, bonusId);
                removeMaxHealthModifier(p, penaltyId);
            }
            nbt.remove(KEY_LIFE_CIRCLE_UNTIL);
            nbt.remove(KEY_LIFE_CIRCLE_CASTER);
            return;
        }

        int radius = GemsBalance.v().life().lifeCircleRadiusBlocks();
        double deltaHealth = GemsBalance.v().life().lifeCircleMaxHealthDelta();
        AbilityFeedback.ring(world, caster.position().add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.core.particles.ParticleTypes.HEART, 24);
        for (ServerPlayer other : world.players()) {
            if (other != caster && VoidImmunity.shouldBlockEffect(caster, other)) {
                removeMaxHealthModifier(other, bonusId);
                removeMaxHealthModifier(other, penaltyId);
                continue;
            }
            double distSq = other.distanceToSqr(caster);
            boolean inRange = distSq <= radius * (double) radius;
            if (!inRange) {
                removeMaxHealthModifier(other, bonusId);
                removeMaxHealthModifier(other, penaltyId);
                continue;
            }

            if (GemTrust.isTrusted(caster, other)) {
                AttributeInstance maxHealth = other.getAttribute(Attributes.MAX_HEALTH);
                double prevBonus = 0.0D;
                if (maxHealth != null) {
                    AttributeModifier existing = maxHealth.getModifier(bonusId);
                    if (existing != null) {
                        prevBonus = existing.amount();
                    }
                }
                applyMaxHealthModifier(other, bonusId, deltaHealth);
                removeMaxHealthModifier(other, penaltyId);
                if (maxHealth != null) {
                    double delta = deltaHealth - prevBonus;
                    if (delta > 0.0D) {
                        float newMax = (float) maxHealth.getValue();
                        float newHealth = Math.min(newMax, other.getHealth() + (float) delta);
                        if (newHealth > other.getHealth()) {
                            other.setHealth(newHealth);
                        }
                    }
                }
            } else {
                applyMaxHealthModifier(other, penaltyId, -deltaHealth);
                removeMaxHealthModifier(other, bonusId);
            }
        }
    }

    private static void tickHeartLock(ServerPlayer target, long now) {
        CompoundTag nbt = persistent(target);
        long until = nbt.getLongOr(KEY_HEART_LOCK_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        UUID caster = GemsNbt.getUuid(nbt, KEY_HEART_LOCK_CASTER);
        if (caster == null) {
            return;
        }
        Identifier modifierId = heartLockModifierId(caster);
        if (until <= now) {
            removeMaxHealthModifier(target, modifierId);
            nbt.remove(KEY_HEART_LOCK_UNTIL);
            nbt.remove(KEY_HEART_LOCK_CASTER);
            nbt.remove(KEY_HEART_LOCK_LOCKED_MAX);
            return;
        }

        AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // Ensure the lock calculation uses the max health without the Heart Lock modifier,
        // otherwise a "perfectly locked" state (delta = 0) would remove the modifier and oscillate.
        maxHealth.removeModifier(modifierId);
        double baseMax = maxHealth.getValue();

        float locked = nbt.getFloatOr(KEY_HEART_LOCK_LOCKED_MAX, (float) baseMax);
        double lockedMax = Math.max(2.0D, locked);
        double delta = lockedMax - baseMax;
        applyMaxHealthModifier(target, modifierId, delta);
    }

    private static void tickUnbounded(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_UNBOUNDED_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (until > now) {
            return;
        }

        GameType prev = parseGameMode(nbt.getStringOr(KEY_UNBOUNDED_PREV_GAMEMODE, ""));
        if (prev == null || prev == GameType.SPECTATOR) {
            prev = GameType.SURVIVAL;
        }
        player.setGameMode(prev);
        ensureNotStuck(player);

        nbt.remove(KEY_UNBOUNDED_UNTIL);
        nbt.remove(KEY_UNBOUNDED_PREV_GAMEMODE);
    }

    private static void tickAstralCamera(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_ASTRAL_CAMERA_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (until > now) {
            return;
        }

        GameType prev = parseGameMode(nbt.getStringOr(KEY_ASTRAL_CAMERA_PREV_GAMEMODE, ""));
        if (prev == null || prev == GameType.SPECTATOR) {
            prev = GameType.SURVIVAL;
        }

        BlockPos returnPos = BlockPos.of(nbt.getLongOr(KEY_ASTRAL_CAMERA_RETURN_POS, player.blockPosition().asLong()));
        float yaw = nbt.getFloatOr(KEY_ASTRAL_CAMERA_RETURN_YAW, player.getYRot());
        float pitch = nbt.getFloatOr(KEY_ASTRAL_CAMERA_RETURN_PITCH, player.getXRot());

        if (!(player.level() instanceof ServerLevel returnWorld)) {
            return;
        }
        String dimRaw = nbt.getStringOr(KEY_ASTRAL_CAMERA_RETURN_DIM, "");
        Identifier dimId = Identifier.tryParse(dimRaw);
        if (dimId != null) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
            var resolved = returnWorld.getServer().getLevel(key);
            if (resolved != null) {
                returnWorld = resolved;
            }
        }

        player.setGameMode(prev);
        GemsTeleport.teleport(player, returnWorld, returnPos.getX() + 0.5D, returnPos.getY(), returnPos.getZ() + 0.5D, yaw, pitch);
        ensureNotStuck(player);

        nbt.remove(KEY_ASTRAL_CAMERA_UNTIL);
        nbt.remove(KEY_ASTRAL_CAMERA_PREV_GAMEMODE);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_DIM);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_POS);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_YAW);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_PITCH);
    }

    private static void tickBounty(ServerPlayer hunter, long now) {
        CompoundTag nbt = persistent(hunter);
        long until = nbt.getLongOr(KEY_BOUNTY_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (until <= now) {
            nbt.remove(KEY_BOUNTY_UNTIL);
            nbt.remove(KEY_BOUNTY_TARGET);
            return;
        }
        UUID targetUuid = GemsNbt.getUuid(nbt, KEY_BOUNTY_TARGET);
        if (targetUuid == null) {
            return;
        }
        MinecraftServer server = hunter.level().getServer();
        if (server == null) {
            return;
        }
        if (com.feel.gems.power.gem.spy.SpySystem.hidesTracking(server, targetUuid)) {
            nbt.remove(KEY_BOUNTY_UNTIL);
            nbt.remove(KEY_BOUNTY_TARGET);
            hunter.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target != null) {
            double dist = Math.sqrt(hunter.distanceToSqr(target));
            hunter.sendSystemMessage(Component.translatable("gems.bounty.tracking", target.getName().getString(), (int) dist), true);
            return;
        }

        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, targetUuid);
        if (snapshot == null || snapshot.pos() == null || snapshot.dimension() == null) {
            hunter.sendSystemMessage(Component.translatable("gems.bounty.target_offline"), true);
            return;
        }
        BlockPos pos = snapshot.pos();
        hunter.sendSystemMessage(Component.translatable(
                "gems.bounty.last_seen",
                snapshot.name(),
                String.valueOf(pos.getX()),
                String.valueOf(pos.getY()),
                String.valueOf(pos.getZ()),
                snapshot.dimension().toString()
        ), true);
    }

    private static void tickAmplificationCleanup(ServerPlayer player, long now) {
        long until = persistent(player).getLongOr(KEY_AMPLIFICATION_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now < until) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            restoreAmplifiedIfExpired(world, stack, now);
        }
        restoreAmplifiedIfExpired(world, player.getOffhandItem(), now);
        restoreAmplifiedIfExpired(world, player.getItemBySlot(EquipmentSlot.HEAD), now);
        restoreAmplifiedIfExpired(world, player.getItemBySlot(EquipmentSlot.CHEST), now);
        restoreAmplifiedIfExpired(world, player.getItemBySlot(EquipmentSlot.LEGS), now);
        restoreAmplifiedIfExpired(world, player.getItemBySlot(EquipmentSlot.FEET), now);
        restoreAmplifiedIfExpired(world, player.getItemBySlot(EquipmentSlot.BODY), now);

        persistent(player).remove(KEY_AMPLIFICATION_UNTIL);
    }

    private static void restoreAmplifiedIfExpired(ServerLevel world, ItemStack stack, long now) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        CompoundTag nbt = custom.copyTag();
        CompoundTag marker = nbt.getCompound(CUSTOM_DATA_KEY_AMPLIFY).orElse(null);
        if (marker == null || marker.isEmpty()) {
            return;
        }
        long until = marker.getLongOr("until", 0L);
        if (until > now) {
            return;
        }

        ListTag list = marker.getList("enchants").orElse(null);
        if (list != null && !list.isEmpty()) {
            EnchantmentAmplification.restoreFromList(world, stack, list);
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.remove(CUSTOM_DATA_KEY_AMPLIFY));
    }

    private static void tickReaperGraveSteed(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_REAPER_STEED_LIST).orElse(null);
        if (list == null || list.isEmpty()) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        ListTag updated = new ListTag();
        float decay = GemsBalance.v().reaper().graveSteedDecayDamagePerSecond();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElse(null);
            if (entry == null) {
                continue;
            }
            UUID steedUuid = GemsNbt.getUuid(entry, "uuid");
            long until = entry.getLongOr("until", 0L);
            if (steedUuid == null || until <= 0) {
                continue;
            }
            var e = SummonerSummons.findEntity(server, steedUuid);
            if (!(e instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse horse) || !horse.isAlive()) {
                continue;
            }

            horse.hurtTime = 0;

            if (now >= until) {
                horse.discard();
                continue;
            }

            if (decay > 0.0F && horse.level() instanceof ServerLevel horseWorld) {
                horse.hurtServer(horseWorld, player.damageSources().indirectMagic(player, player), decay);
                AbilityFeedback.burstAt(horseWorld, horse.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.SMOKE, 1, 0.12D);
            }

            updated.add(entry);
        }

        if (updated.isEmpty()) {
            nbt.remove(KEY_REAPER_STEED_LIST);
        } else {
            nbt.put(KEY_REAPER_STEED_LIST, updated);
        }
    }

    private static void tickReaperDeathOath(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_REAPER_OATH_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            clearReaperDeathOath(player);
            return;
        }

        UUID targetUuid = reaperDeathOathTarget(player);
        if (targetUuid == null) {
            clearReaperDeathOath(player);
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            clearReaperDeathOath(player);
            return;
        }

        var e = SummonerSummons.findEntity(server, targetUuid);
        if (!(e instanceof LivingEntity target) || !target.isAlive()) {
            clearReaperDeathOath(player);
            return;
        }

        float dmg = GemsBalance.v().reaper().deathOathSelfDamagePerSecond();
        nonlethalDrain(player, dmg);
        double dist = player.distanceTo(target);
        player.sendSystemMessage(Component.translatable("gems.reaper.death_oath_tracking", target.getName().getString(), (int) dist), true);
    }

    private static void tickReaperBloodChargeCharging(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(ReaperBloodCharge.KEY_CHARGING_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            ReaperBloodCharge.clearCharging(player);
            player.sendSystemMessage(Component.translatable("gems.reaper.blood_charge_fizzled"), true);
            return;
        }

        float dmg = GemsBalance.v().reaper().bloodChargeSelfDamagePerSecond();
        nonlethalDrain(player, dmg);
        int charged = nbt.getIntOr(ReaperBloodCharge.KEY_CHARGED_TICKS, 0);
        charged = Math.addExact(charged, 20);
        nbt.putInt(ReaperBloodCharge.KEY_CHARGED_TICKS, charged);
    }

    private static void tickReaperShadowClone(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_REAPER_CLONE_UNTIL, 0L);
        if (until <= 0) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUIDS);
            return;
        }

        java.util.List<UUID> clones = readUuidList(nbt, KEY_REAPER_CLONE_UUIDS);
        if (clones.isEmpty()) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            return;
        }

        if (now >= until) {
            for (UUID uuid : clones) {
                var e = SummonerSummons.findEntity(server, uuid);
                if (e != null) {
                    e.discard();
                }
            }
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUIDS);
            return;
        }

        java.util.List<UUID> remaining = new java.util.ArrayList<>();
        for (UUID uuid : clones) {
            var e = SummonerSummons.findEntity(server, uuid);
            if (e == null || !e.isAlive()) {
                continue;
            }
            remaining.add(uuid);
            if (player.level() instanceof ServerLevel world) {
                AbilityFeedback.burstAt(world, e.position().add(0.0D, 1.0D, 0.0D), net.minecraft.core.particles.ParticleTypes.SOUL, 1, 0.08D);
            }
        }

        if (remaining.isEmpty()) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUIDS);
            return;
        }

        if (remaining.size() != clones.size()) {
            writeUuidList(nbt, KEY_REAPER_CLONE_UUIDS, remaining);
        }
    }

    private static void tickReaperRetribution(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_REAPER_RETRIBUTION_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            nbt.remove(KEY_REAPER_RETRIBUTION_UNTIL);
        }
    }

    private static void nonlethalDrain(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        float health = player.getHealth();
        float newHealth = Math.max(1.0F, health - amount);
        if (newHealth < health) {
            player.setHealth(newHealth);
        }
    }

    private static void applyMaxHealthModifier(ServerPlayer player, Identifier id, double deltaHealth) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        if (deltaHealth != 0.0D) {
            maxHealth.addTransientModifier(new AttributeModifier(id, deltaHealth, AttributeModifier.Operation.ADD_VALUE));
        }
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static void removeMaxHealthModifier(ServerPlayer player, Identifier id) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static Identifier lifeCircleBonusId(UUID caster) {
        return Identifier.fromNamespaceAndPath("gems", "life_circle_bonus_" + caster);
    }

    private static Identifier lifeCirclePenaltyId(UUID caster) {
        return Identifier.fromNamespaceAndPath("gems", "life_circle_penalty_" + caster);
    }

    private static Identifier heartLockModifierId(UUID caster) {
        return Identifier.fromNamespaceAndPath("gems", "heart_lock_" + caster);
    }

    private static java.util.List<LivingEntity> livingInRadius(ServerLevel world, Vec3 center, double radius) {
        AABB box = AABB.ofSize(center, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        return world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.distanceToSqr(center) <= radius * radius);
    }

    public static void cleanupOnDisconnect(MinecraftServer server, ServerPlayer caster) {
        CompoundTag nbt = persistent(caster);
        long lifeCircleUntil = nbt.getLongOr(KEY_LIFE_CIRCLE_UNTIL, 0L);
        if (lifeCircleUntil > 0) {
            UUID casterId = GemsNbt.getUuid(nbt, KEY_LIFE_CIRCLE_CASTER);
            if (casterId == null) {
                casterId = caster.getUUID();
            }
            Identifier bonusId = lifeCircleBonusId(casterId);
            Identifier penaltyId = lifeCirclePenaltyId(casterId);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                removeMaxHealthModifier(p, bonusId);
                removeMaxHealthModifier(p, penaltyId);
            }
            nbt.remove(KEY_LIFE_CIRCLE_UNTIL);
            nbt.remove(KEY_LIFE_CIRCLE_CASTER);
        }

        long gravityUntil = nbt.getLongOr(KEY_SPACE_GRAVITY_UNTIL, 0L);
        if (gravityUntil > 0) {
            UUID casterId = GemsNbt.getUuid(nbt, KEY_SPACE_GRAVITY_CASTER);
            if (casterId == null) {
                casterId = caster.getUUID();
            }
            Identifier modifierId = spaceGravityModifierId(casterId);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                applyGravityMultiplier(p, modifierId, 1.0F);
            }
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
        }

        ListTag steeds = nbt.getList(KEY_REAPER_STEED_LIST).orElse(null);
        if (steeds != null) {
            for (int i = 0; i < steeds.size(); i++) {
                CompoundTag entry = steeds.getCompound(i).orElse(null);
                if (entry == null) {
                    continue;
                }
                UUID uuid = GemsNbt.getUuid(entry, "uuid");
                if (uuid != null) {
                    var e = SummonerSummons.findEntity(server, uuid);
                    if (e != null) {
                        e.discard();
                    }
                }
            }
        }
        nbt.remove(KEY_REAPER_STEED_LIST);

        java.util.List<UUID> cloneUuids = readUuidList(nbt, KEY_REAPER_CLONE_UUIDS);
        for (UUID cloneUuid : cloneUuids) {
            var e = SummonerSummons.findEntity(server, cloneUuid);
            if (e != null) {
                e.discard();
            }
        }
        nbt.remove(KEY_REAPER_CLONE_UNTIL);
        nbt.remove(KEY_REAPER_CLONE_UUIDS);

        nbt.remove(KEY_REAPER_WITHERING_UNTIL);
        clearReaperDeathOath(caster);
        nbt.remove(KEY_REAPER_RETRIBUTION_UNTIL);
        ReaperBloodCharge.clearCharging(caster);
        ((GemsPersistentDataHolder) caster).gems$getPersistentData().remove(ReaperBloodCharge.KEY_BUFF_UNTIL);
        ((GemsPersistentDataHolder) caster).gems$getPersistentData().remove(ReaperBloodCharge.KEY_BUFF_MULT);
        clearDuelistRapidStrike(caster);
        SentinelInterventionRuntime.cleanup(server, caster);
    }

    private static GameType parseGameMode(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameType mode : GameType.values()) {
            if (mode.getName().equals(name)) {
                return mode;
            }
        }
        return null;
    }

    private static void ensureNotStuck(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        if (!world.getBlockCollisions(player, player.getBoundingBox()).iterator().hasNext()) {
            return;
        }

        BlockPos base = player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        var box = player.getBoundingBox();

        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    p.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    Vec3 candidate = new Vec3(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
                    var moved = box.move(candidate.subtract(player.position()));
                    if (!world.getBlockCollisions(player, moved).iterator().hasNext()) {
                        GemsTeleport.teleport(player, world, candidate.x, candidate.y, candidate.z, player.getYRot(), player.getXRot());
                        return;
                    }
                }
            }
        }
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
