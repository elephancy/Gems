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

/**
 * Gladiator's Mark - brand a player; both you and the marked player deal 50% more damage 
 * to each other for 60s.
 */
public final class GladiatorsMarkItem extends Item implements LegendaryItem {
    private static final String MARK_TARGET_KEY = "gladiators_mark_target";
    private static final String MARK_END_KEY = "gladiators_mark_end";

    public GladiatorsMarkItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gladiators_mark").toString();
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
        int rangeBlocks = GemsBalance.v().legendary().gladiatorsMarkRangeBlocks();
        ServerPlayer target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_player_target").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (target == player) {
            return InteractionResult.FAIL;
        }

        int durationTicks = GemsBalance.v().legendary().gladiatorsMarkDurationTicks();
        long endTime = world.getGameTime() + Math.max(0, durationTicks);

        // Mark both players as linked
        PlayerStateManager.setPersistent(player, MARK_TARGET_KEY, target.getStringUUID());
        PlayerStateManager.setPersistent(player, MARK_END_KEY, String.valueOf(endTime));

        PlayerStateManager.setPersistent(target, MARK_TARGET_KEY, player.getStringUUID());
        PlayerStateManager.setPersistent(target, MARK_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().gladiatorsMarkCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.5F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.5F);

        player.sendSystemMessage(Component.translatable("gems.item.gladiators_mark.marked", target.getName().getString()).withStyle(ChatFormatting.RED), true);
        target.sendSystemMessage(Component.translatable("gems.item.gladiators_mark.marked_victim", player.getName().getString()).withStyle(ChatFormatting.RED), true);

        return InteractionResult.SUCCESS;
    }

    public static boolean isMarkedAgainst(ServerPlayer attacker, ServerPlayer target) {
        String markedTarget = PlayerStateManager.getPersistent(attacker, MARK_TARGET_KEY);
        if (markedTarget == null || !markedTarget.equals(target.getStringUUID())) {
            return false;
        }

        String endStr = PlayerStateManager.getPersistent(attacker, MARK_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (attacker.level().getGameTime() > endTime) {
            clearMark(attacker);
            return false;
        }

        return true;
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().legendary().gladiatorsMarkDamageMultiplier();
    }

    public static void clearMark(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, MARK_END_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.gladiators_mark.tooltip.1"),
                Component.translatable("gems.item.gladiators_mark.tooltip.2"),
                Component.translatable("gems.item.gladiators_mark.tooltip.3")
        );
    }
}
