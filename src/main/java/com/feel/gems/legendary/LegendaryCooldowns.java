package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

public final class LegendaryCooldowns {
    private static final Map<UUID, Integer> LAST_CHARM_COUNTS = new ConcurrentHashMap<>();

    private LegendaryCooldowns() {
    }

    /**
     * Gets the cooldown multiplier for abilities. Chrono Charms stack multiplicatively.
     * 1 charm = 0.5x (50% cooldown), 2 charms = 0.25x (25% cooldown), etc.
     */
    public static float getCooldownMultiplier(ServerPlayer player) {
        if (player == null) {
            return 1.0F;
        }
        int charmCount = countChronoCharms(player);
        if (charmCount <= 0) {
            return 1.0F;
        }
        float baseMultiplier = GemsBalance.v().legendary().chronoCharmCooldownMultiplier();
        // Stack multiplicatively: 2 charms = mult^2, 3 charms = mult^3, etc.
        return (float) Math.pow(baseMultiplier, charmCount);
    }

    /**
     * Counts how many Chrono Charms the player has in their inventory.
     */
    public static int countChronoCharms(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.CHRONO_CHARM)) {
                count += inv.getItem(i).getCount();
            }
        }
        return count;
    }

    public static boolean hasChronoCharm(ServerPlayer player) {
        return countChronoCharms(player) > 0;
    }

    public static boolean updateCharmCount(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        int count = countChronoCharms(player);
        Integer last = LAST_CHARM_COUNTS.put(player.getUUID(), count);
        return last == null || last != count;
    }

    public static void clearCache(UUID playerId) {
        if (playerId != null) {
            LAST_CHARM_COUNTS.remove(playerId);
        }
    }
}
