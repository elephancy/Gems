package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;




public final class GemItemGlint {
    private GemItemGlint() {
    }

    public static void sync(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        GemId activeGem = GemPlayerState.getActiveGem(player);
        boolean shouldGlint = GemPlayerState.getEnergy(player) >= GemPlayerState.MAX_ENERGY;

        syncInventorySection(player.getInventory().getNonEquipmentItems(), activeGem, shouldGlint);
        syncInventoryStack(player.getOffhandItem(), activeGem, shouldGlint);
        syncInventoryStack(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), activeGem, shouldGlint);
        syncInventoryStack(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), activeGem, shouldGlint);
        syncInventoryStack(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), activeGem, shouldGlint);
        syncInventoryStack(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), activeGem, shouldGlint);
    }

    private static void syncInventorySection(Iterable<ItemStack> stacks, GemId activeGem, boolean shouldGlint) {
        for (ItemStack stack : stacks) {
            syncInventoryStack(stack, activeGem, shouldGlint);
        }
    }

    private static void syncInventoryStack(ItemStack stack, GemId activeGem, boolean shouldGlint) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof GemItem gemItem)) {
            return;
        }
        boolean isActiveGemItem = gemItem.gemId() == activeGem;
        setGlintFlag(stack, isActiveGemItem && shouldGlint);
    }

    private static void setGlintFlag(ItemStack stack, boolean value) {
        if (value) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}
