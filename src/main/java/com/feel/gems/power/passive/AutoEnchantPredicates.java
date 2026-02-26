package com.feel.gems.power.passive;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;




public final class AutoEnchantPredicates {
    private AutoEnchantPredicates() {
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.has(DataComponents.WEAPON) && !(item instanceof BowItem) && !(item instanceof CrossbowItem)) {
            return true;
        }
        return stack.is(ItemTags.AXES) || item instanceof TridentItem;
    }

    public static boolean isTool(ItemStack stack) {
        return stack.has(DataComponents.TOOL);
    }

    public static boolean isGearOrTool(ItemStack stack) {
        return isTool(stack) || stack.isDamageableItem();
    }

    public static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem;
    }
}
