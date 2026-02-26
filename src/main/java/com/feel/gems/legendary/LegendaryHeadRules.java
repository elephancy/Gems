package com.feel.gems.legendary;

import com.feel.gems.GemsMod;
import com.mojang.authlib.GameProfile;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class LegendaryHeadRules {
    private static final Set<Identifier> PLAYER_HEAD_RECIPES = Set.of(
            id("blood_oath_blade"),
            id("blood_oath_blade_discount"),
            id("tracker_compass"),
            id("tracker_compass_discount")
    );

    private LegendaryHeadRules() {
    }

    public static boolean canCraft(ServerPlayer player, CraftingContainer input, Identifier recipeId) {
        if (!PLAYER_HEAD_RECIPES.contains(recipeId)) {
            return true;
        }
        UUID playerId = player.getUUID();
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || !stack.is(Items.PLAYER_HEAD)) {
                continue;
            }
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);
            if (profile == null) {
                return false;
            }
            GameProfile gameProfile = profile.partialProfile();
            if (gameProfile == null || gameProfile.id() == null) {
                return false;
            }
            if (playerId.equals(gameProfile.id())) {
                return false;
            }
        }
        return true;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, path);
    }
}
