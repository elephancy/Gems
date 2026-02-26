package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsNbt;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;




public final class LegendaryTargeting {
    private static final String KEY_LAST_TARGET = "legendaryLastTarget";
    private static final String KEY_LAST_TARGET_AT = "legendaryLastTargetAt";

    private LegendaryTargeting() {
    }

    public static void recordHit(ServerPlayer attacker, LivingEntity target) {
        CompoundTag data = ((com.feel.gems.state.GemsPersistentDataHolder) attacker).gems$getPersistentData();
        GemsNbt.putUuid(data, KEY_LAST_TARGET, target.getUUID());
        data.putLong(KEY_LAST_TARGET_AT, GemsTime.now(attacker));
    }

    public static LivingEntity findTarget(ServerPlayer player, int rangeBlocks, int timeoutTicks) {
        CompoundTag data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!GemsNbt.containsUuid(data, KEY_LAST_TARGET)) {
            return null;
        }
        long now = GemsTime.now(player);
        long last = data.getLongOr(KEY_LAST_TARGET_AT, 0L);
        if (timeoutTicks > 0 && now - last > timeoutTicks) {
            return null;
        }
        UUID uuid = GemsNbt.getUuid(data, KEY_LAST_TARGET);
        LivingEntity target = player.level().getServer() == null ? null : findLiving(player.level().getServer(), uuid);
        if (target == null || target.level() != player.level() || !target.isAlive()) {
            return null;
        }
        double max = rangeBlocks * (double) rangeBlocks;
        if (player.distanceToSqr(target) > max) {
            return null;
        }
        if (!player.hasLineOfSight(target)) {
            return null;
        }
        return target;
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
