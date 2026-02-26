package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.legendary.LegendaryDuels;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
 * Challenger's Gauntlet - right-click a player to challenge them to a duel.
 * Both players are teleported to a small arena, winner gets energy; loser loses energy.
 *
 * The arena and return trip are handled server-side by {@link LegendaryDuels}.
 */
public final class ChallengersGauntletItem extends Item implements LegendaryItem {
    public ChallengersGauntletItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "challengers_gauntlet").toString();
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
        int rangeBlocks = GemsBalance.v().legendary().challengersGauntletRangeBlocks();
        ServerPlayer target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_player_target").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (target == player) {
            return InteractionResult.FAIL;
        }

        if (!LegendaryDuels.startGauntletDuel(player, target)) {
            player.sendSystemMessage(Component.translatable("gems.item.challengers_gauntlet.unavailable").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        int cooldownTicks = GemsBalance.v().legendary().challengersGauntletCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(stack, cooldownTicks);
        }

        // Visual effects
        ServerLevel serverWorld = player.level();
        serverWorld.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        serverWorld.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GOAT_HORN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.sendSystemMessage(Component.translatable("gems.item.challengers_gauntlet.challenged", target.getName().getString()).withStyle(ChatFormatting.GOLD), false);
        target.sendSystemMessage(Component.translatable("gems.item.challengers_gauntlet.challenged_victim", player.getName().getString()).withStyle(ChatFormatting.GOLD), false);
        target.sendSystemMessage(Component.translatable("gems.item.challengers_gauntlet.duel_begin").withStyle(ChatFormatting.YELLOW), false);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.challengers_gauntlet.tooltip.1"),
                Component.translatable("gems.item.challengers_gauntlet.tooltip.2"),
                Component.translatable("gems.item.challengers_gauntlet.tooltip.3")
        );
    }
}
