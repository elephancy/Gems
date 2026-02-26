package com.feel.gems.util;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class GemsTooltipFormat {
    private GemsTooltipFormat() {
    }

    public static void appendDescription(Consumer<Component> tooltip, Component... lines) {
        if (tooltip == null || lines == null || lines.length == 0) {
            return;
        }
        tooltip.accept(Component.translatable("gems.tooltip.description").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (Component line : lines) {
            if (line == null) {
                continue;
            }
            tooltip.accept(Component.literal(" - ").append(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
