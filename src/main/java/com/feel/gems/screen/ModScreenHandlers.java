package com.feel.gems.screen;

import com.feel.gems.GemsMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;




public final class ModScreenHandlers {
    public static final MenuType<TraderScreenHandler> TRADER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gem_trader"),
            new MenuType<>(TraderScreenHandler::new, FeatureFlagSet.of())
    );

    public static final MenuType<GemSeerScreenHandler> GEM_SEER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gem_seer"),
            new MenuType<>(GemSeerScreenHandler::new, FeatureFlagSet.of())
    );

    private ModScreenHandlers() {
    }

    public static void init() {
        // Triggers static init.
    }
}
