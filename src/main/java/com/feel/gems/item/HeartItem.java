package com.feel.gems.item;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;




public final class HeartItem extends Item {
    public HeartItem(Properties settings) {
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

        AssassinState.initIfNeeded(player);
        if (AssassinState.isAssassin(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.item.heart.assassin_blocked"), true);
            return InteractionResult.FAIL;
        }

        UUID ownerUuid = AbilityRuntime.getOwner(stack);
        if (ownerUuid != null && !ownerUuid.equals(player.getUUID())) {
            ServerPlayer owner = player.level().getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null && GemTrust.isTrusted(owner, player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.item.heart.teammate_blocked"), true);
                return InteractionResult.FAIL;
            }
        }

        GemPlayerState.initIfNeeded(player);
        int before = GemPlayerState.getMaxHearts(player);
        if (before >= GemPlayerState.MAX_MAX_HEARTS) {
            return InteractionResult.FAIL;
        }

        GemPlayerState.setMaxHearts(player, before + 1);
        GemPlayerState.applyMaxHearts(player);
        GemStateSync.send(player);
        stack.shrink(1);
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }
}
