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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Reversal Mirror - right-click to activate for 5s; all incoming damage is reflected back to the attacker.
 * Works on all types of attacks (not just projectiles).
 */
public final class ReversalMirrorItem extends Item implements LegendaryItem {
    private static final String ACTIVE_END_KEY = "reversal_mirror_end";
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ReversalMirrorItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "reversal_mirror").toString();
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

        // Activate mirror
        int durationTicks = GemsBalance.v().legendary().reversalMirrorDurationTicks();
        long endTime = world.getGameTime() + Math.max(0, durationTicks);
        PlayerStateManager.setPersistent(player, ACTIVE_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().reversalMirrorCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);

        player.sendSystemMessage(Component.translatable("gems.item.reversal_mirror.activated").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.SUCCESS;
    }

    public static boolean isActive(ServerPlayer player) {
        String endStr = PlayerStateManager.getPersistent(player, ACTIVE_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearActive(player);
            return false;
        }

        return true;
    }

    public static void clearActive(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, ACTIVE_END_KEY);
    }

    /**
     * Called when the player takes damage while mirror is active.
     * Reflects the damage back to the attacker.
     */
    public static void reflectDamage(ServerPlayer victim, Entity attacker, float amount, ServerLevel world) {
        tryReflectDamage(victim, attacker, amount, world);
    }

    /**
     * Attempts to reflect damage back to the attacker.
     *
     * @return true when damage was reflected
     */
    public static boolean tryReflectDamage(ServerPlayer victim, Entity attacker, float amount, ServerLevel world) {
        if (!isActive(victim)) return false;
        if (amount <= 0.0F) return false;
        if (attacker == null || !(attacker instanceof LivingEntity living)) return false;
        if (living == victim) return false;

        // Prevent infinite reflection loops (e.g. two players both have mirrors active).
        if (REFLECTING.get()) {
            return false;
        }

        REFLECTING.set(Boolean.TRUE);
        try {
            living.hurtServer(world, victim.damageSources().thorns(victim), amount);
        } finally {
            REFLECTING.set(Boolean.FALSE);
        }

        world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.reversal_mirror.tooltip.1"),
                Component.translatable("gems.item.reversal_mirror.tooltip.2"),
                Component.translatable("gems.item.reversal_mirror.tooltip.3")
        );
    }
}
