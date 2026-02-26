package com.feel.gems.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;




/**
 * Canonical server-side tick time source for durations/cooldowns.
 *
 * <p>Uses the overworld's game time as a single monotonic reference shared across dimensions,
 * so timers don't drift when players change dimensions.</p>
 */
public final class GemsTime {
    private GemsTime() {
    }

    public static long now(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server == null ? player.level().getGameTime() : now(server);
    }

    public static long now(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return world.getGameTime();
        }
        return now(server);
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }
}

