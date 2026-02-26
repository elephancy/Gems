package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;




public final class GemItem extends Item {
    private final GemId gemId;

    public GemItem(GemId gemId, Properties settings) {
        super(settings);
        this.gemId = gemId;
    }

    public GemId gemId() {
        return gemId;
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

        var server = player.level().getServer();
        GemPlayerState.initIfNeeded(player);
        GemOwnership.ensureOwner(stack, player); // tag immediately so stale/no-owner items belong to the clicker

        UUID ownerUuid = GemOwnership.ownerUuid(stack);
        boolean ownedByPlayer = ownerUuid != null && ownerUuid.equals(player.getUUID());
        if (GemOwnership.isInvalidForEpoch(server, stack)) {
            if (ownedByPlayer) {
                // Refresh epoch for your own stale gem so it can still be activated (common when carrying multiple gems across deaths).
                GemOwnership.tagOwned(stack, player);
            } else if (GemOwnership.purgeIfInvalid(server, stack)) {
                player.setItemInHand(hand, ItemStack.EMPTY);
                player.sendSystemMessage(Component.translatable("gems.item.gem.reclaimed"), true);
                return InteractionResult.SUCCESS.heldItemTransformedTo(ItemStack.EMPTY);
            }
        }

        if (ownerUuid != null && !ownedByPlayer) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
            if (owner != null) {
                GemPlayerState.initIfNeeded(owner);
                if (GemPlayerState.getActiveGem(owner) == gemId) {
                    if (owner.isAlive()) {
                        GemOwnership.applyOwnerPenalty(owner);
                    }
                    player.sendSystemMessage(Component.translatable("gems.item.gem.not_owned"), true);
                    player.setItemInHand(hand, ItemStack.EMPTY);
                    return InteractionResult.SUCCESS.heldItemTransformedTo(ItemStack.EMPTY);
                }
            }
        }

        boolean ownsGem = GemPlayerState.getOwnedGems(player).contains(gemId);
        if (!ownsGem) {
            if (player.isCreative()) {
                GemPlayerState.addOwnedGem(player, gemId);
                ownsGem = true;
            } else {
                // Claim the gem from the item so non-active drops behave like vanilla loot.
                GemPlayerState.addOwnedGem(player, gemId);
                GemOwnership.tagOwned(stack, player);
                ownsGem = true;
            }
        }

        GemPlayerState.setActiveGem(player, gemId);
        AugmentRuntime.captureActiveGemAugments(player, gemId, stack);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        player.sendSystemMessage(Component.translatable("gems.item.gem.activated", gemId.name()), true);
        return InteractionResult.SUCCESS;
    }
}
