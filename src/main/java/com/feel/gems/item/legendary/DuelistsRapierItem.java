package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
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
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Duelist's Rapier - parry window on right-click; successful parry grants guaranteed crit on next hit.
 */
public final class DuelistsRapierItem extends Item implements LegendaryItem {
    private static final String PARRY_WINDOW_END_KEY = "duelists_rapier_parry_end";
    private static final String CRIT_READY_KEY = "duelists_rapier_crit_ready";

    public DuelistsRapierItem(ToolMaterial material, Properties settings) {
        super(settings.sword(material, 2.0F, -2.0F).enchantable(15)); // Faster attack speed, slightly less damage
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "duelists_rapier").toString();
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

        // Activate parry window
        int windowTicks = GemsBalance.v().legendary().duelistsRapierParryWindowTicks();
        long parryEnd = world.getGameTime() + Math.max(0, windowTicks);
        PlayerStateManager.setPersistent(player, PARRY_WINDOW_END_KEY, String.valueOf(parryEnd));

        int cooldownTicks = GemsBalance.v().legendary().duelistsRapierCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0F, 1.5F);

        player.sendSystemMessage(Component.translatable("gems.item.duelists_rapier.parry_ready").withStyle(ChatFormatting.YELLOW), true);
        return InteractionResult.SUCCESS;
    }

    /**
     * Check if player is in parry window.
     */
    public static boolean isInParryWindow(ServerPlayer player) {
        String endStr = PlayerStateManager.getPersistent(player, PARRY_WINDOW_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearParryWindow(player);
            return false;
        }

        return true;
    }

    /**
     * Called when a successful parry occurs (player blocked during parry window).
     */
    public static void onSuccessfulParry(ServerPlayer player) {
        clearParryWindow(player);
        PlayerStateManager.setPersistent(player, CRIT_READY_KEY, "true");

        player.sendSystemMessage(Component.translatable("gems.item.duelists_rapier.perfect_parry").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * Check and consume guaranteed crit.
     */
    public static boolean hasAndConsumeGuaranteedCrit(ServerPlayer player) {
        String ready = PlayerStateManager.getPersistent(player, CRIT_READY_KEY);
        if (!"true".equals(ready)) return false;

        PlayerStateManager.clearPersistent(player, CRIT_READY_KEY);
        return true;
    }

    public static void clearParryWindow(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, PARRY_WINDOW_END_KEY);
    }

    public static void clearAll(ServerPlayer player) {
        clearParryWindow(player);
        PlayerStateManager.clearPersistent(player, CRIT_READY_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.duelists_rapier.tooltip.1"),
                Component.translatable("gems.item.duelists_rapier.tooltip.2"),
                Component.translatable("gems.item.duelists_rapier.tooltip.3")
        );
    }
}
