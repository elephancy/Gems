package com.feel.gems.util;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

public final class GemsTeleport {
    private GemsTeleport() {
    }

    public static boolean teleport(ServerPlayer player, ServerLevel world, double x, double y, double z, float yaw, float pitch) {
        return player.teleportTo(world, x, y, z, Set.<Relative>of(), yaw, pitch, true);
    }
}
