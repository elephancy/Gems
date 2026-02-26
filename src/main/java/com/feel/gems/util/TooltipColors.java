package com.feel.gems.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Standardized tooltip colors for consistent UI across all Gems items.
 * 
 * Color usage guidelines:
 * - DESCRIPTION (GRAY): Primary item description, main functionality
 * - DETAIL (DARK_GRAY): Secondary details, usage instructions, mechanics
 * - HIGHLIGHT (GOLD): Important features, special effects, legendary qualities
 * - SECTION (GRAY): Section headers in tooltips
 * - LIST_ITEM (DARK_GRAY): Individual items in lists (passives, abilities)
 * - INSTRUCTION (AQUA): Keybinds, controls, how-to-use info
 * - SUB_ITEM (DARK_AQUA): Sub-items, detailed values, statistics
 * - ERROR (RED): Warnings, disabled features, cooldowns
 * - SUCCESS (GREEN): Active effects, enabled features
 */
public final class TooltipColors {
    /** Primary description color - main functionality */
    public static final ChatFormatting DESCRIPTION = ChatFormatting.GRAY;
    
    /** Secondary details color - usage instructions, mechanics */
    public static final ChatFormatting DETAIL = ChatFormatting.DARK_GRAY;
    
    /** Important feature highlight color - special effects, legendary qualities */
    public static final ChatFormatting HIGHLIGHT = ChatFormatting.GOLD;
    
    /** Section header color */
    public static final ChatFormatting SECTION = ChatFormatting.GRAY;
    
    /** List item color - passives, abilities in lists */
    public static final ChatFormatting LIST_ITEM = ChatFormatting.DARK_GRAY;
    
    /** Instruction color - keybinds, controls */
    public static final ChatFormatting INSTRUCTION = ChatFormatting.AQUA;
    
    /** Sub-item color - detailed values, statistics */
    public static final ChatFormatting SUB_ITEM = ChatFormatting.DARK_AQUA;
    
    /** Error/warning color */
    public static final ChatFormatting ERROR = ChatFormatting.RED;
    
    /** Success/active color */
    public static final ChatFormatting SUCCESS = ChatFormatting.GREEN;
    
    private TooltipColors() {
    }
    
    /**
     * Format text as primary description.
     */
    public static MutableComponent description(String key) {
        return Component.translatable(key).withStyle(DESCRIPTION);
    }
    
    /**
     * Format text as secondary detail.
     */
    public static MutableComponent detail(String key) {
        return Component.translatable(key).withStyle(DETAIL);
    }
    
    /**
     * Format text as important highlight.
     */
    public static MutableComponent highlight(String key) {
        return Component.translatable(key).withStyle(HIGHLIGHT);
    }
    
    /**
     * Format text as section header.
     */
    public static MutableComponent section(String key) {
        return Component.translatable(key).withStyle(SECTION);
    }
    
    /**
     * Format text as instruction.
     */
    public static MutableComponent instruction(String key) {
        return Component.translatable(key).withStyle(INSTRUCTION);
    }
    
    /**
     * Format literal text as list item with prefix.
     */
    public static MutableComponent listItem(String text) {
        return Component.literal(" - " + text).withStyle(LIST_ITEM);
    }
    
    /**
     * Format literal text as sub-item with prefix.
     */
    public static MutableComponent subItem(String text) {
        return Component.literal(" - " + text).withStyle(SUB_ITEM);
    }
}
