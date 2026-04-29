package com.feel.gems.gametest;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.ModItems;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.screen.ModScreenHandlers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** An opt-in dedicated-server installation check for the Gems module alone. */
public final class StandaloneGemsServerGameTest {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 40)
    public void gemsLoadsWithoutCampaign(GameTestHelper context) {
        FabricLoader loader = FabricLoader.getInstance();
        require(loader.isModLoaded("gems"), "Gems is absent");
        require(!loader.isModLoaded("second_chance"), "Second Chance leaked into the standalone server");
        require(!loader.isModLoaded("terrablender"), "TerraBlender leaked into the standalone server");

        int abilities = 0;
        int passives = 0;
        for (GemId gem : GemId.values()) {
            GemDefinition definition = GemRegistry.definition(gem);
            Identifier itemId = BuiltInRegistries.ITEM.getKey(ModItems.gemItem(gem));
            require(Identifier.fromNamespaceAndPath("gems", gem.name().toLowerCase() + "_gem").equals(itemId),
                    "Gem item is not registered: " + gem);
            require(definition.availableAbilities(new GemEnergyState(1)).isEmpty(),
                    "Energy-1 ability unlock leaked: " + gem);
            require(definition.availableAbilities(new GemEnergyState(5)).equals(definition.abilities()),
                    "Energy-5 abilities did not unlock: " + gem);
            for (Identifier ability : definition.abilities()) {
                require(ModAbilities.get(ability) != null, "Missing registered ability: " + ability);
                abilities++;
            }
            for (Identifier passive : definition.passives()) {
                require(ModPassives.get(passive) != null, "Missing registered passive: " + passive);
                passives++;
            }
        }
        require(abilities > 0 && passives > 0, "Gem power definitions are empty");
        require(Identifier.fromNamespaceAndPath("gems", "gem_trader")
                        .equals(BuiltInRegistries.MENU.getKey(ModScreenHandlers.TRADER)),
                "Gems trader menu is not registered");
        require(Identifier.fromNamespaceAndPath("gems", "gem_seer")
                        .equals(BuiltInRegistries.MENU.getKey(ModScreenHandlers.GEM_SEER)),
                "Gems seer menu is not registered");

        System.out.println("[standalone-gems] PASS: " + GemId.values().length
                + " gems, " + abilities + " abilities, " + passives + " passives, and two menus");
        context.succeed();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
