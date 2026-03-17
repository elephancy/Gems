package com.feel.gems.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;


public final class ClientDisguiseState {
    private static final Map<UUID, UUID> DISGUISES = new HashMap<>();

    private ClientDisguiseState() {
    }

    private static UUID removeDisguise(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return DISGUISES.remove(playerId);
    }

    private static void restoreDisguise(UUID playerId, UUID target) {
        if (playerId == null || target == null) {
            return;
        }
        DISGUISES.put(playerId, target);
    }

    public static void update(UUID player, Optional<UUID> target) {
        if (player == null) {
            return;
        }
        if (target == null || target.isEmpty()) {
            DISGUISES.remove(player);
            return;
        }
        DISGUISES.put(player, target.get());
    }

    public static PlayerSkin overrideSkin(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        return overrideSkin(player.getUUID());
    }

    public static PlayerSkin overrideSkin(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        UUID target = DISGUISES.get(playerId);
        if (target == null) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return null;
        }
        // Avoid recursion if both players are disguised as each other (e.g., Mirror Match).
        UUID removed = removeDisguise(target);
        try {
            PlayerInfo entry = client.getConnection().getPlayerInfo(target);
            if (entry != null) {
                return entry.getSkin();
            }
            if (client.level != null) {
                var entity = client.level.getPlayerByUUID(target);
                if (entity instanceof AbstractClientPlayer targetPlayer) {
                    return targetPlayer.getSkin();
                }
            }
            return null;
        } finally {
            restoreDisguise(target, removed);
        }
    }

    public static Component overrideName(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        return overrideName(player.getUUID());
    }

    public static Component overrideName(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        UUID target = DISGUISES.get(playerId);
        if (target == null) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return null;
        }
        PlayerInfo entry = client.getConnection().getPlayerInfo(target);
        if (entry == null || entry.getProfile() == null) {
            return null;
        }
        return Component.literal(entry.getProfile().name());
    }

    public static void reset() {
        DISGUISES.clear();
    }
}
