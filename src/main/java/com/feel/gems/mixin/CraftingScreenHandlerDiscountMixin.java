package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryDiscounts;
import com.feel.gems.legendary.LegendaryHeadRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CraftingMenu.class)
public abstract class CraftingScreenHandlerDiscountMixin {
    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void gems$gateDiscounts(AbstractContainerMenu handler, ServerLevel world, Player player, CraftingContainer input, ResultContainer result, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (recipe == null) {
            return;
        }

        net.minecraft.resources.Identifier id = recipe.id().identifier();
        if (!isAllowed(serverPlayer, input, id)) {
            result.setItem(0, ItemStack.EMPTY);
            result.setChanged();
            handler.broadcastChanges();
            return;
        }
    }

    private static boolean isAllowed(ServerPlayer player, CraftingContainer input, net.minecraft.resources.Identifier id) {
        if (!LegendaryHeadRules.canCraft(player, input, id)) {
            return false;
        }
        if (LegendaryDiscounts.isDiscountRecipe(id) && !LegendaryDiscounts.canUseDiscount(player, id)) {
            return false;
        }
        return true;
    }
}
