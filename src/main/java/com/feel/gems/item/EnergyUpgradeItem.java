package com.feel.gems.item;

import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;




public final class EnergyUpgradeItem extends Item {
    public EnergyUpgradeItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        GemPlayerState.initIfNeeded(player);
        int before = GemPlayerState.getEnergy(player);
        if (before >= GemPlayerState.getMaxEnergy(player)) {
            return InteractionResult.FAIL;
        }

        GemPlayerState.setEnergy(player, before + 1);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        stack.shrink(1);
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }
}
