package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;




public final class HypnoStaffItem extends Item implements LegendaryItem {
    private static final String KEY_TARGET = "legendaryHypnoTarget";
    private static final String KEY_PROGRESS = "legendaryHypnoProgress";

    public HypnoStaffItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "hypno_staff").toString();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClientSide()) {
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        var cfg = GemsBalance.v().legendary();
        int range = cfg.hypnoRangeBlocks();
        double viewRange = Math.max(4.0D, cfg.hypnoViewRangeBlocks());
        LivingEntity target = Targeting.raycastLiving(player, range, viewRange);
        if (!(target instanceof Mob mob) || !HypnoControl.isAllowed(mob)) {
            resetProgress(player);
            return;
        }

        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID current = GemsNbt.getUuid(data, KEY_TARGET);
        if (current == null || !current.equals(mob.getUUID())) {
            GemsNbt.putUuid(data, KEY_TARGET, mob.getUUID());
            data.putInt(KEY_PROGRESS, 1);
            return;
        }

        int progress = data.getIntOr(KEY_PROGRESS, 0) + 1;
        data.putInt(KEY_PROGRESS, progress);

        int holdTicks = cfg.hypnoHoldTicks();
        sendProgress(player, progress, holdTicks);
        if (progress < holdTicks) {
            return;
        }
        if (HypnoControl.isHypno(mob) && player.getUUID().equals(HypnoControl.ownerUuid(mob))) {
            float heal = cfg.hypnoHealHearts() * 2.0F;
            if (heal > 0.0F) {
                mob.heal(heal);
            }
            player.sendSystemMessage(Component.translatable("gems.item.hypno_staff.refreshed"), true);
            player.releaseUsingItem();
            resetProgress(player);
            return;
        }
        boolean controlled = HypnoControl.tryControl(player, mob);
        if (controlled) {
            player.sendSystemMessage(Component.translatable("gems.item.hypno_staff.successful"), true);
            player.releaseUsingItem();
        } else {
            player.sendSystemMessage(Component.translatable("gems.item.hypno_staff.failed"), true);
        }
        resetProgress(player);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        if (world.isClientSide()) {
            return false;
        }
        if (user instanceof ServerPlayer player) {
            resetProgress(player);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.hypno_staff.desc"));
    }

    private static void resetProgress(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_TARGET);
        data.remove(KEY_PROGRESS);
    }

    private static void sendProgress(ServerPlayer player, int progress, int holdTicks) {
        if (holdTicks <= 0) {
            return;
        }
        int clamped = Math.min(progress, holdTicks);
        int percent = (int) Math.floor((clamped / (double) holdTicks) * 100.0D);
        int bars = 10;
        int filled = (int) Math.round((clamped / (double) holdTicks) * bars);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "=" : " ");
        }
        bar.append("]");
        player.sendSystemMessage(Component.translatable("gems.item.hypno_staff.progress", bar.toString(), percent), true);
    }
}
