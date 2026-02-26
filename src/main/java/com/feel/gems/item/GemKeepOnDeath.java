package com.feel.gems.item;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;




/**
 * Keeps gem items from dropping on death by stashing them into persistent player NBT and restoring on respawn.
 */
public final class GemKeepOnDeath {
    private static final String KEY_KEPT_GEMS = "keptGems";

    private GemKeepOnDeath() {
    }

    public static void stash(ServerPlayer player) {
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_KEPT_GEMS);

        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);

        List<ItemStack> kept = new ArrayList<>();
        boolean found = false;

        var mainStacks = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (!found && stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(stack.copy());
                mainStacks.set(i, ItemStack.EMPTY);
                found = true;
            }
        }
        if (!found) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(offhand.copy());
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                found = true;
            }
        }
        if (!found) {
            net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET
            };
            for (var slot : armorSlots) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                    kept.add(stack.copy());
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    found = true;
                    break;
                }
            }
        }

        if (kept.isEmpty()) {
            return;
        }
        root.store(KEY_KEPT_GEMS, ItemStack.CODEC.listOf(), kept);
    }

    public static void restore(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        List<ItemStack> kept = root.read(KEY_KEPT_GEMS, ItemStack.CODEC.listOf()).orElse(List.of());
        if (kept.isEmpty()) {
            return;
        }
        root.remove(KEY_KEPT_GEMS);
        for (ItemStack stack : kept) {
            if (!stack.isEmpty()) {
                GemOwnership.tagOwned(stack, player);
                player.addItem(stack);
            }
        }
    }
}
