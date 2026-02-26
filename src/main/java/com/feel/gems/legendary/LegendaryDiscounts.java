package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;


public final class LegendaryDiscounts {
    private LegendaryDiscounts() {
    }

    public static boolean isDiscountRecipe(Identifier recipeId) {
        if (recipeId == null) {
            return false;
        }
        return GemsBalance.v().legendary().recipeGemRequirements().containsKey(recipeId);
    }

    public static boolean canUseDiscount(ServerPlayer player, Identifier recipeId) {
        GemId required = GemsBalance.v().legendary().recipeGemRequirements().get(recipeId);
        if (required == null) {
            return false;
        }
        GemPlayerState.initIfNeeded(player);
        return GemPlayerState.getActiveGem(player) == required;
    }
}
