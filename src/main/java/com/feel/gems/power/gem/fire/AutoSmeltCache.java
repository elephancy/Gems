package com.feel.gems.power.gem.fire;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;


/**
 * Shared recipe lookup cache for auto-smelt.
 *
 * <p>Kept outside mixins because some Mixin runtimes disallow non-private static methods in mixin classes.</p>
 */
public final class AutoSmeltCache {
    private static final Object2ObjectOpenHashMap<Item, ItemStack> SMELT_CACHE = new Object2ObjectOpenHashMap<>();

    private AutoSmeltCache() {
    }

    public static void clear() {
        SMELT_CACHE.clear();
    }

    public static ItemStack smeltResult(ServerLevel world, ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (input.has(DataComponents.CUSTOM_DATA) || input.has(DataComponents.BLOCK_ENTITY_DATA)) {
            return null;
        }

        Item item = input.getItem();
        ItemStack cached = SMELT_CACHE.get(item);
        if (cached != null) {
            return cached.isEmpty() ? null : scaledCopy(cached, input.getCount());
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(new ItemStack(item));
        Optional<? extends RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<SingleRecipeInput>>> match =
                world.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, world);

        if (match.isEmpty()) {
            SMELT_CACHE.put(item, ItemStack.EMPTY);
            return null;
        }

        ItemStack result = match.get().value().assemble(recipeInput);
        if (result == null || result.isEmpty()) {
            SMELT_CACHE.put(item, ItemStack.EMPTY);
            return null;
        }

        SMELT_CACHE.put(item, result.copy());
        return scaledCopy(result, input.getCount());
    }

    private static ItemStack scaledCopy(ItemStack baseResult, int inputCount) {
        ItemStack out = baseResult.copy();
        out.setCount(baseResult.getCount() * inputCount);
        return out;
    }
}

