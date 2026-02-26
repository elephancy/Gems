package com.feel.gems.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Conservative preflight for rewards made of different item types. */
public final class InventorySpace {
    private InventorySpace() { }

    public static boolean hasEmptySlots(ServerPlayer player, int required) {
        int empty = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() && ++empty >= required) return true;
        }
        return false;
    }

    /** Insert a whole reward into a carried slot without creative mode voiding it. */
    public static boolean insertWhole(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()) return false;
        var carried = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < carried.size(); slot++) {
            if (carried.get(slot).isEmpty()) {
                player.getInventory().setItem(slot, stack);
                return true;
            }
        }
        return false;
    }
}
