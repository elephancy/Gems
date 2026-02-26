package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class TricksterShadowCloneRuntime {
    private static final String CLONE_X_KEY = "trickster_clone_x";
    private static final String CLONE_Y_KEY = "trickster_clone_y";
    private static final String CLONE_Z_KEY = "trickster_clone_z";
    private static final String CLONE_END_KEY = "trickster_clone_end";

    private TricksterShadowCloneRuntime() {}

    public static void createClone(ServerPlayer player, Vec3 position, int durationTicks) {
        long endTime = player.level().getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, CLONE_X_KEY, String.valueOf(position.x));
        PlayerStateManager.setPersistent(player, CLONE_Y_KEY, String.valueOf(position.y));
        PlayerStateManager.setPersistent(player, CLONE_Z_KEY, String.valueOf(position.z));
        PlayerStateManager.setPersistent(player, CLONE_END_KEY, String.valueOf(endTime));
    }

    public static Vec3 getClonePosition(ServerPlayer player) {
        String endStr = PlayerStateManager.getPersistent(player, CLONE_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearClone(player);
            return null;
        }

        String xStr = PlayerStateManager.getPersistent(player, CLONE_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, CLONE_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, CLONE_Z_KEY);

        if (xStr == null || yStr == null || zStr == null) return null;

        return new Vec3(
            Double.parseDouble(xStr),
            Double.parseDouble(yStr),
            Double.parseDouble(zStr)
        );
    }

    public static void moveClone(ServerPlayer player, Vec3 newPosition) {
        PlayerStateManager.setPersistent(player, CLONE_X_KEY, String.valueOf(newPosition.x));
        PlayerStateManager.setPersistent(player, CLONE_Y_KEY, String.valueOf(newPosition.y));
        PlayerStateManager.setPersistent(player, CLONE_Z_KEY, String.valueOf(newPosition.z));
    }

    public static boolean hasClone(ServerPlayer player) {
        return getClonePosition(player) != null;
    }

    public static void clearClone(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, CLONE_X_KEY);
        PlayerStateManager.clearPersistent(player, CLONE_Y_KEY);
        PlayerStateManager.clearPersistent(player, CLONE_Z_KEY);
        PlayerStateManager.clearPersistent(player, CLONE_END_KEY);
    }
}
