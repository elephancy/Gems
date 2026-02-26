package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class TricksterMirageRuntime {
    private static final String MIRAGE_CLONE_UUID_KEY = "trickster_mirage_clone_uuid";
    private static final String MIRAGE_END_KEY = "trickster_mirage_end";
    private static final String MIRAGE_OFFSET_X_KEY = "trickster_mirage_offset_x";
    private static final String MIRAGE_OFFSET_Y_KEY = "trickster_mirage_offset_y";
    private static final String MIRAGE_OFFSET_Z_KEY = "trickster_mirage_offset_z";

    private TricksterMirageRuntime() {}

    public static void createMirage(ServerPlayer player, UUID cloneUuid, Vec3 offset, long endTime) {
        PlayerStateManager.setPersistent(player, MIRAGE_CLONE_UUID_KEY, cloneUuid.toString());
        PlayerStateManager.setPersistent(player, MIRAGE_END_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_X_KEY, String.valueOf(offset.x));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_Y_KEY, String.valueOf(offset.y));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_Z_KEY, String.valueOf(offset.z));
    }

    public static boolean hasMirages(ServerPlayer player) {
        return getCloneUuid(player) != null;
    }

    public static void tickMirage(ServerPlayer player) {
        UUID cloneUuid = getCloneUuid(player);
        if (cloneUuid == null) {
            return;
        }
        ServerLevel world = player.level();
        long endTime = getEndTime(player);
        if (endTime > 0 && world.getGameTime() > endTime) {
            Entity entity = world.getEntity(cloneUuid);
            if (entity != null) {
                entity.discard();
            }
            applyBuff(player);
            clearMirages(player);
            return;
        }
        Entity entity = world.getEntity(cloneUuid);
        if (!(entity instanceof ShadowCloneEntity clone) || !clone.isAlive()) {
            applyBuff(player);
            clearMirages(player);
            return;
        }
        Vec3 offset = getOffset(player);
        Vec3 nextPos = player.position().add(offset);
        clone.snapTo(nextPos.x, nextPos.y, nextPos.z, player.getYRot(), player.getXRot());
        clone.setYHeadRot(player.getYHeadRot());
        boolean sneaking = player.isShiftKeyDown();
        clone.setShiftKeyDown(sneaking);
        clone.setPose(sneaking ? Pose.CROUCHING : Pose.STANDING);
        clone.setSprinting(player.isSprinting());
        clone.setDeltaMovement(Vec3.ZERO);
    }

    public static void onMirageCloneHit(ShadowCloneEntity clone) {
        if (clone == null || !(clone.level() instanceof ServerLevel world)) {
            return;
        }
        UUID ownerId = clone.getOwnerUuid();
        if (ownerId == null || world.getServer() == null) {
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        applyBuff(owner);
        clearMirages(owner);
    }

    public static void clearMirages(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, MIRAGE_CLONE_UUID_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_END_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_X_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_Y_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_Z_KEY);
    }

    public static Vec3 getMiragePosition(ServerPlayer player) {
        UUID cloneUuid = getCloneUuid(player);
        if (cloneUuid == null) {
            return null;
        }
        return player.position().add(getOffset(player));
    }

    public static void tickMirageParticles(ServerPlayer player) {
        Vec3 pos = getMiragePosition(player);
        if (pos == null) {
            return;
        }
        ServerLevel world = player.level();
        long time = world.getGameTime();
        Vec3 particlePos = pos.add(0, 1.0, 0);

        double angle = (time * 0.15) % (2 * Math.PI);
        double ringRadius = 0.9;
        double offsetX = Math.cos(angle) * ringRadius;
        double offsetZ = Math.sin(angle) * ringRadius;

        AbilityFeedback.burstAt(world, particlePos.add(offsetX, 0.1, offsetZ), ParticleTypes.ENCHANT, 4, 0.45D);
        AbilityFeedback.burstAt(world, particlePos.add(-offsetX, 0.6, -offsetZ), ParticleTypes.END_ROD, 3, 0.35D);

        if (time % 8 == 0) {
            AbilityFeedback.burstAt(world, particlePos, ParticleTypes.PORTAL, 6, 0.6D);
        }
        if (time % 12 == 0) {
            AbilityFeedback.burstAt(world, particlePos.add(0, 0.6, 0), ParticleTypes.WITCH, 2, 0.3D);
        }
        if (time % 16 == 0) {
            AbilityFeedback.burstAt(world, particlePos.add(0, -0.2, 0), ParticleTypes.SOUL, 2, 0.2D);
        }
    }

    private static UUID getCloneUuid(ServerPlayer player) {
        String raw = PlayerStateManager.getPersistent(player, MIRAGE_CLONE_UUID_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long getEndTime(ServerPlayer player) {
        String raw = PlayerStateManager.getPersistent(player, MIRAGE_END_KEY);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Vec3 getOffset(ServerPlayer player) {
        String xStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_Z_KEY);
        if (xStr == null || yStr == null || zStr == null) {
            return Vec3.ZERO;
        }
        try {
            return new Vec3(Double.parseDouble(xStr), Double.parseDouble(yStr), Double.parseDouble(zStr));
        } catch (NumberFormatException e) {
            return Vec3.ZERO;
        }
    }

    private static void applyBuff(ServerPlayer player) {
        var cfg = GemsBalance.v().trickster();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, cfg.mirageBuffDurationTicks());
        if (durationTicks <= 0) {
            return;
        }
        int amp = Math.max(0, cfg.mirageBuffAmplifier());
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, amp, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, true, false, true));
    }
}
