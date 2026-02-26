package com.feel.gems.net;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.feel.gems.item.GemItem;

/**
 * Server-side networking for augment management.
 */
public final class ServerAugmentNetworking {
    private ServerAugmentNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AugmentOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player(), payload.mainHand())));

        ServerPlayNetworking.registerGlobalReceiver(AugmentRemovePayload.ID, (payload, context) ->
                context.server().execute(() -> handleRemove(context.player(), payload)));
    }

    private static void handleRemove(ServerPlayer player, AugmentRemovePayload payload) {
        GemPlayerState.initIfNeeded(player);
        ItemStack stack = payload.mainHand() ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof GemItem)) {
            player.sendSystemMessage(Component.translatable("gems.augment.need_gem").withStyle(ChatFormatting.RED), true);
            return;
        }
        GemItem gemItem = (GemItem) stack.getItem();
        boolean removed = AugmentRuntime.removeGemAugment(stack, payload.index());
        if (removed) {
            player.sendSystemMessage(Component.translatable("gems.augment.removed").withStyle(ChatFormatting.GREEN), true);
            if (GemPlayerState.getActiveGem(player) == gemItem.gemId()) {
                AugmentRuntime.captureActiveGemAugments(player, gemItem.gemId(), stack);
                GemCooldownSync.send(player);
            }
        }
        openScreen(player, payload.mainHand());
    }

    private static void openScreen(ServerPlayer player, boolean mainHand) {
        GemPlayerState.initIfNeeded(player);
        ItemStack stack = mainHand ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof GemItem gemItem)) {
            player.sendSystemMessage(Component.translatable("gems.augment.need_gem").withStyle(ChatFormatting.RED), true);
            return;
        }
        List<AugmentInstance> augments = AugmentRuntime.getGemAugments(stack);
        List<AugmentScreenPayload.AugmentEntry> entries = new ArrayList<>(augments.size());
        for (AugmentInstance instance : augments) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            String name = def != null ? Component.translatable(def.nameKey()).getString() : instance.augmentId();
            String desc = def != null ? Component.translatable(def.descriptionKey()).getString() : "";
            String rarity = instance.rarity().name();
            entries.add(new AugmentScreenPayload.AugmentEntry(
                    instance.augmentId(),
                    name,
                    desc,
                    rarity,
                    instance.magnitude()
            ));
        }
        int maxSlots = GemsBalance.v().augments().gemMaxSlots();
        ServerPlayNetworking.send(player, new AugmentScreenPayload(gemItem.gemId().name(), mainHand, entries, maxSlots));
    }
}
