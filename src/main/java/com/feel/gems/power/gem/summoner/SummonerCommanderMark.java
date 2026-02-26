package com.feel.gems.power.gem.summoner;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;


public final class SummonerCommanderMark {
    private static final String KEY_UNTIL = "summonerMarkUntil";
    private static final String KEY_TARGET = "summonerMarkTarget";

    private SummonerCommanderMark() {
    }

    public static void mark(ServerPlayer owner, Entity target, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        root.putLong(KEY_UNTIL, GemsTime.now(owner) + durationTicks);
        GemsNbt.putUuid(root, KEY_TARGET, target.getUUID());
    }

    public static UUID activeTargetUuid(ServerPlayer owner) {
        CompoundTag root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        long until = root.getLongOr(KEY_UNTIL, 0L);
        if (until <= GemsTime.now(owner)) {
            return null;
        }
        return GemsNbt.getUuid(root, KEY_TARGET);
    }
}

