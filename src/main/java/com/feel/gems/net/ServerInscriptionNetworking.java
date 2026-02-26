package com.feel.gems.net;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side networking for legendary inscription management.
 */
public final class ServerInscriptionNetworking {
    private ServerInscriptionNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(InscriptionOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player(), payload.mainHand())));

        ServerPlayNetworking.registerGlobalReceiver(InscriptionRemovePayload.ID, (payload, context) ->
                context.server().execute(() -> handleRemove(context.player(), payload)));
    }

    private static void handleRemove(ServerPlayer player, InscriptionRemovePayload payload) {
        ItemStack stack = payload.mainHand() ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof LegendaryItem)) {
            player.sendSystemMessage(Component.translatable("gems.augment.need_legendary").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean removed = AugmentRuntime.removeLegendaryAugment(stack, payload.index());
        if (removed) {
            player.sendSystemMessage(Component.translatable("gems.augment.removed").withStyle(ChatFormatting.GREEN), true);
            AugmentRuntime.applyLegendaryModifiers(player);
        }
        openScreen(player, payload.mainHand());
    }

    private static void openScreen(ServerPlayer player, boolean mainHand) {
        ItemStack stack = mainHand ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof LegendaryItem)) {
            player.sendSystemMessage(Component.translatable("gems.augment.need_legendary").withStyle(ChatFormatting.RED), true);
            return;
        }
        List<AugmentInstance> inscriptions = AugmentRuntime.getLegendaryAugments(stack);
        List<InscriptionScreenPayload.Entry> entries = new ArrayList<>(inscriptions.size());
        for (AugmentInstance instance : inscriptions) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            String name = def != null ? Component.translatable(def.nameKey()).getString() : instance.augmentId();
            String desc = def != null ? Component.translatable(def.descriptionKey()).getString() : "";
            String rarity = instance.rarity().name();
            entries.add(new InscriptionScreenPayload.Entry(
                    instance.augmentId(),
                    name,
                    desc,
                    rarity,
                    instance.magnitude()
            ));
        }
        int maxSlots = GemsBalance.v().augments().legendaryMaxSlots();
        String itemKey = stack.getItem().getDescriptionId();
        ServerPlayNetworking.send(player, new InscriptionScreenPayload(itemKey, mainHand, entries, maxSlots));
    }
}
