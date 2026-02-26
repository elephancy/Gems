package com.feel.gems.item.legendary;

import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.screen.GemSeerScreenHandler;
import com.feel.gems.util.GemsTooltipFormat;
import com.feel.gems.net.GemSeerOpeningPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/**
 * Gem Seer - A legendary item that reveals information about any online player's gems.
 * Opens a selection screen to choose a player, then displays their active gem, energy level, and owned gems.
 * Does not have a cooldown and is not consumed on use.
 */
public final class GemSeerItem extends Item implements LegendaryItem {
    private static final String LEGENDARY_ID = "gem_seer";

    public GemSeerItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return LEGENDARY_ID;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        GemSeerScreenHandler.OpeningData data = GemSeerScreenHandler.buildOpeningData(player);
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.gems.gem_seer.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new GemSeerScreenHandler(syncId, inv, data);
            }

        }).ifPresent(menuId -> ServerPlayNetworking.send(player, new GemSeerOpeningPayload(menuId, data)));

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.gem_seer.desc"));
    }
}
