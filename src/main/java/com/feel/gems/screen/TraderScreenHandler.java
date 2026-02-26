package com.feel.gems.screen;

import com.feel.gems.core.GemId;
import com.feel.gems.item.GemPurchaseItem;
import com.feel.gems.trade.GemTrading;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import com.feel.gems.state.GemPlayerState;




public final class TraderScreenHandler extends AbstractContainerMenu {
    private static final int MODE_TRADE = 0;
    private static final int MODE_PURCHASE = 1;

    private final DataSlot mode = DataSlot.standalone();

    public TraderScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.TRADER, syncId);
        addDataSlot(mode);

        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (GemPurchaseItem.consumePending(serverPlayer)) {
                mode.set(MODE_PURCHASE);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }

        GemId[] gems = GemId.values();
        if (id < 0 || id >= gems.length) {
            return false;
        }

        GemId gemId = gems[id];
        if (isPurchaseMode()) {
            GemTrading.PurchaseResult result = GemTrading.purchase(serverPlayer, gemId);
            if (!result.success()) {
                return false;
            }
            serverPlayer.sendSystemMessage(Component.translatable("gems.trade.purchased", gemId.name()), true);
        } else {
            if (GemPlayerState.getActiveGem(serverPlayer) == gemId) {
                serverPlayer.sendSystemMessage(Component.translatable("gems.trade.gem_already_active"), true);
                return false;
            }
            GemTrading.Result result = GemTrading.trade(serverPlayer, gemId);
            if (!result.success()) {
                return false;
            }
            serverPlayer.sendSystemMessage(Component.translatable("gems.trade.traded_for", gemId.name()), true);
        }
        serverPlayer.closeContainer();
        return true;
    }

    public boolean isPurchaseMode() {
        return mode.get() == MODE_PURCHASE;
    }
}
