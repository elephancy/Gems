package com.feel.gems.item;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.item.legendary.SupremeArmorItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;




public final class ModItemGroups {
    private static boolean initialized = false;
    private static final List<Supplier<ItemStack>> ADDON_ITEMS = new ArrayList<>();

    public static CreativeModeTab GEMS;

    private ModItemGroups() {
    }

    /** Addon items appear after Gems basics when that addon is installed. */
    public static void registerAddonItem(Supplier<ItemStack> stackFactory) {
        ADDON_ITEMS.add(Objects.requireNonNull(stackFactory));
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GEMS = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gems"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.gems"))
                        .icon(() -> new ItemStack(ModItems.CREATIVE_TAB_ICON))
                        .displayItems((displayContext, entries) -> {
                            HolderLookup.Provider lookup = displayContext.holders();
                            
                            // === GEMS (first) ===
                            for (GemId gemId : GemId.values()) {
                                entries.accept(ModItems.gemItem(gemId));
                            }
                            
                            // === NORMAL ITEMS ===
                            entries.accept(ModItems.HEART);
                            entries.accept(ModItems.ENERGY_UPGRADE);
                            entries.accept(ModItems.GEM_TRADER);
                            entries.accept(ModItems.GEM_PURCHASE);
                            entries.accept(ModItems.GEM_AUGMENT_FOCUS);
                            entries.accept(ModItems.GEM_AUGMENT_RESONANCE);
                            entries.accept(ModItems.GEM_AUGMENT_PERSISTENCE);
                            entries.accept(ModItems.LEGENDARY_INSCRIPTION_EDGE);
                            entries.accept(ModItems.LEGENDARY_INSCRIPTION_SWIFT);
                            entries.accept(ModItems.LEGENDARY_INSCRIPTION_WARD);
                            entries.accept(ModItems.TEST_DUMMY_SPAWN_EGG);
                            for (Supplier<ItemStack> factory : ADDON_ITEMS) {
                                ItemStack addon = factory.get();
                                if (!addon.isEmpty()) entries.accept(addon);
                            }
                            
                            // === LEGENDARY ITEMS ===
                            // Utility
                            entries.accept(ModItems.TRACKER_COMPASS);
                            entries.accept(ModItems.RECALL_RELIC);
                            entries.accept(ModItems.GEM_SEER);
                            entries.accept(ModItems.HYPNO_STAFF);
                            entries.accept(ModItems.EARTHSPLITTER_PICK);
                            
                            // Armor (with trims)
                            entries.accept(trimmedSupreme(ModItems.SUPREME_HELMET, lookup));
                            entries.accept(trimmedSupreme(ModItems.SUPREME_CHESTPLATE, lookup));
                            entries.accept(trimmedSupreme(ModItems.SUPREME_LEGGINGS, lookup));
                            entries.accept(trimmedSupreme(ModItems.SUPREME_BOOTS, lookup));
                            
                            // Weapons
                            entries.accept(ModItems.BLOOD_OATH_BLADE);
                            entries.accept(ModItems.DEMOLITION_BLADE);
                            entries.accept(ModItems.THIRD_STRIKE_BLADE);
                            entries.accept(ModItems.VAMPIRIC_EDGE);
                            entries.accept(ModItems.EXPERIENCE_BLADE);
                            entries.accept(ModItems.DUELISTS_RAPIER);
                            entries.accept(ModItems.HUNTERS_SIGHT_BOW);
                            
                            // PvP Items
                            entries.accept(ModItems.REVERSAL_MIRROR);
                            entries.accept(ModItems.HUNTERS_TROPHY_NECKLACE);
                            entries.accept(ModItems.GLADIATORS_MARK);
                            entries.accept(ModItems.SOUL_SHACKLE);
                            entries.accept(ModItems.CHALLENGERS_GAUNTLET);
                            entries.accept(ModItems.CHRONO_CHARM);
                        })
                        .build()
        );
    }

    private static ItemStack trimmedSupreme(Item item, HolderLookup.Provider lookup) {
        ItemStack stack = new ItemStack(item);
        SupremeArmorItem.applySupremeTrim(stack, lookup);
        return stack;
    }
}
