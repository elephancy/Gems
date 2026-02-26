package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.UUID;

/**
 * Soul Shackle - link yourself to an enemy; damage you take is split between you both for 10s.
 */
public final class SoulShackleItem extends Item implements LegendaryItem {
    private static final String SHACKLE_TARGET_KEY = "soul_shackle_target";
    private static final String SHACKLE_END_KEY = "soul_shackle_end";

    public SoulShackleItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "soul_shackle").toString();
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide() || !(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);

        // Check cooldown
        if (player.getCooldowns().isOnCooldown(stack) && !GemsAdmin.noLegendaryCooldowns(player)) {
            return InteractionResult.FAIL;
        }

        // Raycast to find target
        int rangeBlocks = GemsBalance.v().legendary().soulShackleRangeBlocks();
        ServerPlayer target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_player_target").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (target == player) {
            return InteractionResult.FAIL;
        }

        int durationTicks = GemsBalance.v().legendary().soulShackleDurationTicks();
        long endTime = world.getGameTime() + Math.max(0, durationTicks);

        // Link player to target
        PlayerStateManager.setPersistent(player, SHACKLE_TARGET_KEY, target.getStringUUID());
        PlayerStateManager.setPersistent(player, SHACKLE_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().soulShackleCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

        player.sendSystemMessage(Component.translatable("gems.item.soul_shackle.linked", target.getName().getString()).withStyle(ChatFormatting.DARK_PURPLE), true);
        target.sendSystemMessage(Component.translatable("gems.item.soul_shackle.linked_victim", player.getName().getString()).withStyle(ChatFormatting.DARK_PURPLE), true);

        return InteractionResult.SUCCESS;
    }

    /**
     * Called when the shackle holder takes damage.
     * Returns the amount to transfer to the linked target.
     */
    public static float getDamageToTransfer(ServerPlayer holder, float damage) {
        String endStr = PlayerStateManager.getPersistent(holder, SHACKLE_END_KEY);
        if (endStr == null) return 0;

        long endTime = Long.parseLong(endStr);
        if (holder.level().getGameTime() > endTime) {
            clearShackle(holder);
            return 0;
        }

        return damage * GemsBalance.v().legendary().soulShackleSplitRatio();
    }

    public static ServerPlayer getShackledTarget(ServerPlayer holder) {
        String targetStr = PlayerStateManager.getPersistent(holder, SHACKLE_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(holder, SHACKLE_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (holder.level().getGameTime() > endTime) {
            clearShackle(holder);
            return null;
        }

        try {
            UUID targetId = UUID.fromString(targetStr);
            return holder.level().getServer().getPlayerList().getPlayer(targetId);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasShackle(ServerPlayer player) {
        return getShackledTarget(player) != null;
    }

    public static void clearShackle(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, SHACKLE_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, SHACKLE_END_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.soul_shackle.tooltip.1"),
                Component.translatable("gems.item.soul_shackle.tooltip.2"),
                Component.translatable("gems.item.soul_shackle.tooltip.3")
        );
    }
}
