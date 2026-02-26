package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Chrono Charm - reduces ability cooldowns while carried.
 */
public final class ChronoCharmItem extends Item implements LegendaryItem {
    public ChronoCharmItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "chrono_charm").toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.chrono_charm.tooltip.1"),
                Component.translatable("gems.item.chrono_charm.tooltip.2")
        );
    }
}
