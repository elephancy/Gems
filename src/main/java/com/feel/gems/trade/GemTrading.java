package com.feel.gems.trade;

import com.feel.gems.core.GemId;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;




public final class GemTrading {
    private GemTrading() {
    }

    public record Result(boolean success, boolean consumedTrader, boolean alreadyOwned) {
    }

    public record PurchaseResult(boolean success, boolean consumedToken, boolean alreadyOwned) {
    }

    public static Result trade(ServerPlayer player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        if (GemsDisables.isGemDisabledFor(player, gemId)) {
            player.sendSystemMessage(Component.translatable("gems.trade.gem_disabled"), true);
            return new Result(false, false, false);
        }

        GemId activeBefore = GemPlayerState.getActiveGem(player);
        if (gemId == activeBefore) {
            player.sendSystemMessage(Component.translatable("gems.trade.gem_already_active"), true);
            return new Result(false, false, true);
        }

        Item activeItem = ModItems.gemItem(activeBefore);
        if (!hasGemOnPlayer(player, activeItem)) {
            player.sendSystemMessage(Component.translatable("gems.trade.need_active_gem"), true);
            return new Result(false, false, false);
        }

        boolean consumedTrader = consumeTrader(player);
        if (!consumedTrader) {
            player.sendSystemMessage(Component.translatable("gems.trade.need_gem_trader"), true);
            return new Result(false, false, false);
        }
        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);

        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.remove(activeBefore); // trade away only the active gem
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemStateSync.send(player);
        Item keep = ModItems.gemItem(gemId);
        Item tradedAway = activeItem;
        removeGemItems(player, tradedAway, keep);
        ensurePlayerHasItem(player, keep);
        GemItemGlint.sync(player);

        return new Result(true, true, alreadyOwned);
    }

    public static PurchaseResult purchase(ServerPlayer player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        if (GemsDisables.isGemDisabledFor(player, gemId)) {
            player.sendSystemMessage(Component.translatable("gems.trade.gem_disabled"), true);
            return new PurchaseResult(false, false, false);
        }

        boolean consumedToken = consumePurchaseToken(player);
        if (!consumedToken) {
            player.sendSystemMessage(Component.translatable("gems.trade.need_purchase_token"), true);
            return new PurchaseResult(false, false, false);
        }

        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);
        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemStateSync.send(player);

        Item keep = ModItems.gemItem(gemId);
        ensurePlayerHasItem(player, keep);
        GemItemGlint.sync(player);

        return new PurchaseResult(true, true, alreadyOwned);
    }

    private static boolean consumeTrader(ServerPlayer player) {
        Item trader = ModItems.GEM_TRADER;
        ItemStack main = player.getMainHandItem();
        if (main.is(trader)) {
            main.shrink(1);
            return true;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(trader)) {
            off.shrink(1);
            return true;
        }
        var mainStacks = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.is(trader)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    mainStacks.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean consumePurchaseToken(ServerPlayer player) {
        Item token = ModItems.GEM_PURCHASE;
        ItemStack main = player.getMainHandItem();
        if (main.is(token)) {
            main.shrink(1);
            return true;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(token)) {
            off.shrink(1);
            return true;
        }
        var mainStacks = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.is(token)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    mainStacks.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private static void ensurePlayerHasItem(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return;
            }
        }
        if (player.getOffhandItem().is(item)) {
            return;
        }
        if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(item)) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        GemOwnership.tagOwned(stack, player);
        player.addItem(stack);
    }

    private static void removeGemItems(ServerPlayer player, Item toRemove, Item toKeep) {
        purgeInv(player.getInventory().getNonEquipmentItems(), toRemove, toKeep);
        purgeEquipment(player, net.minecraft.world.entity.EquipmentSlot.OFFHAND, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.world.entity.EquipmentSlot.HEAD, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.world.entity.EquipmentSlot.CHEST, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.world.entity.EquipmentSlot.LEGS, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.world.entity.EquipmentSlot.FEET, toRemove, toKeep);
        purgeInventory(player.getEnderChestInventory(), toRemove, toKeep);
    }

    private static void purgeEquipment(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot slot, Item toRemove, Item toKeep) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof com.feel.gems.item.GemItem)) {
            return;
        }
        if (stack.is(toKeep) || !stack.is(toRemove)) {
            return;
        }
        player.setItemSlot(slot, ItemStack.EMPTY);
    }

    private static void purgeInv(java.util.List<ItemStack> stacks, Item toRemove, Item toKeep) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof com.feel.gems.item.GemItem)) {
                continue;
            }
            if (stack.is(toKeep)) {
                continue;
            }
            if (!stack.is(toRemove)) {
                continue;
            }
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    private static void purgeInventory(net.minecraft.world.Container inv, Item toRemove, Item toKeep) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof com.feel.gems.item.GemItem)) {
                continue;
            }
            if (stack.is(toKeep)) {
                continue;
            }
            if (!stack.is(toRemove)) {
                continue;
            }
            inv.setItem(i, ItemStack.EMPTY);
        }
    }

    private static boolean hasGemOnPlayer(ServerPlayer player, Item item) {
        if (item == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return true;
            }
        }
        if (player.getOffhandItem().is(item)) {
            return true;
        }
        if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(item)) {
            return true;
        }
        return false;
    }
}
