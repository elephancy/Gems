package com.feel.gems.item;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.augment.AugmentTarget;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class AugmentItem extends Item {
    private final String augmentId;

    public AugmentItem(String augmentId, Properties settings) {
        super(settings.stacksTo(16));
        this.augmentId = augmentId;
    }

    public String augmentId() {
        return augmentId;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = user.getItemInHand(hand);
        AugmentInstance instance = AugmentRuntime.getInstance(stack);
        if (instance == null) {
            instance = AugmentRuntime.rollInstance(augmentId);
            if (instance != null) {
                AugmentRuntime.assignInstance(stack, instance);
            }
        }
        if (instance == null) {
            return InteractionResult.PASS;
        }

        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null) {
            return InteractionResult.PASS;
        }

        if (def.target() == AugmentTarget.GEM) {
            ItemStack target = hand == InteractionHand.MAIN_HAND ? user.getOffhandItem() : user.getMainHandItem();
            if (!(target.getItem() instanceof GemItem)) {
                player.sendSystemMessage(Component.translatable("gems.augment.need_gem").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            if (AugmentRuntime.applyGemAugment(player, target, instance)) {
                consume(player, stack);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        ItemStack target = hand == InteractionHand.MAIN_HAND ? user.getOffhandItem() : user.getMainHandItem();
        if (!(target.getItem() instanceof LegendaryItem)) {
            player.sendSystemMessage(Component.translatable("gems.augment.need_legendary").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (AugmentRuntime.applyLegendaryAugment(player, target, instance)) {
            consume(player, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        AugmentInstance instance = AugmentRuntime.getInstance(stack);
        AugmentDefinition def = AugmentRegistry.get(augmentId);
        if (def != null) {
            GemsTooltipFormat.appendDescription(tooltip, Component.translatable(def.descriptionKey()));
        }
        if (instance != null) {
            tooltip.accept(Component.translatable("gems.augment.rarity", instance.rarity().name()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }
        stack.shrink(1);
    }
}
