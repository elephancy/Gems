package com.feel.gems.screen;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;




public final class PocketsStorage {
    private static final String KEY_POCKETS = "pocketsInv";
    private static final String KEY_ITEMS = "items";

    private PocketsStorage() {
    }

    public static SimpleContainer load(ServerPlayer player) {
        int rows = Math.max(1, GemsBalance.v().wealth().pocketsRows());
        SimpleContainer inv = new SimpleContainer(rows * 9);
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag data = root.getCompound(KEY_POCKETS).orElse(null);
        if (data != null) {
            var view = TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), data);
            inv.fromItemList(view.listOrEmpty(KEY_ITEMS, ItemStack.OPTIONAL_CODEC));
        }
        return inv;
    }

    public static void save(ServerPlayer player, SimpleContainer inv) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        var view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
        inv.storeAsItemList(view.list(KEY_ITEMS, ItemStack.OPTIONAL_CODEC));
        root.put(KEY_POCKETS, view.buildResult());
    }
}

