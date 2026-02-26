package com.feel.gems.augment;

import com.feel.gems.item.AugmentItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AugmentCrafting {
    private AugmentCrafting() {
    }

    public static void onCrafted(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof AugmentItem augmentItem)) {
            return;
        }
        if (AugmentRuntime.getInstance(stack) != null) {
            return;
        }
        AugmentInstance instance = AugmentRuntime.rollInstance(augmentItem.augmentId());
        if (instance == null) {
            return;
        }
        AugmentRuntime.assignInstance(stack, instance);
    }
}
