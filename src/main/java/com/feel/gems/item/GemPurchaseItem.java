package com.feel.gems.item;

import com.feel.gems.screen.TraderScreenHandler;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;




public final class GemPurchaseItem extends Item {
    private static final String KEY_PENDING = "gemPurchasePending";

    public GemPurchaseItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        markPending(player);
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.gems.purchase.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                return new TraderScreenHandler(syncId, inv);
            }
        });
        return InteractionResult.SUCCESS;
    }

    public static boolean consumePending(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!data.getBooleanOr(KEY_PENDING, false)) {
            return false;
        }
        data.remove(KEY_PENDING);
        return true;
    }

    private static void markPending(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_PENDING, true);
    }
}
