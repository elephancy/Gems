package com.feel.gems.item;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.item.legendary.BloodOathBladeItem;
import com.feel.gems.item.legendary.ChallengersGauntletItem;
import com.feel.gems.item.legendary.ChronoCharmItem;
import com.feel.gems.item.legendary.DemolitionBladeItem;
import com.feel.gems.item.legendary.DuelistsRapierItem;
import com.feel.gems.item.legendary.EarthsplitterPickItem;
import com.feel.gems.item.legendary.ExperienceBladeItem;
import com.feel.gems.item.legendary.GemSeerItem;
import com.feel.gems.item.legendary.GladiatorsMarkItem;
import com.feel.gems.item.legendary.HunterSightBowItem;
import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.item.legendary.HypnoStaffItem;
import com.feel.gems.item.legendary.RecallRelicItem;
import com.feel.gems.item.legendary.ReversalMirrorItem;
import com.feel.gems.item.legendary.SoulShackleItem;
import com.feel.gems.item.legendary.SupremeArmorItem;
import com.feel.gems.item.legendary.ThirdStrikeBladeItem;
import com.feel.gems.item.legendary.TrackerCompassItem;
import com.feel.gems.item.legendary.VampiricEdgeItem;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;




public final class ModItems {
    public static final Item CREATIVE_TAB_ICON = register("creative_tab_icon", settings -> new Item(settings.stacksTo(1)));
    public static final Item HEART = register("heart", settings -> new HeartItem(settings.stacksTo(16)));
    public static final Item ENERGY_UPGRADE = register("energy_upgrade", settings -> new EnergyUpgradeItem(settings.stacksTo(16)));
    public static final Item GEM_TRADER = register("gem_trader", settings -> new TraderItem(settings.stacksTo(1)));
    public static final Item GEM_PURCHASE = register("gem_purchase", settings -> new GemPurchaseItem(settings.stacksTo(1)));
        public static final Item GEM_AUGMENT_FOCUS = register("gem_augment_focus", settings -> new AugmentItem("focus", settings));
        public static final Item GEM_AUGMENT_RESONANCE = register("gem_augment_resonance", settings -> new AugmentItem("resonance", settings));
        public static final Item GEM_AUGMENT_PERSISTENCE = register("gem_augment_persistence", settings -> new AugmentItem("persistence", settings));
        public static final Item LEGENDARY_INSCRIPTION_EDGE = register("legendary_inscription_edge", settings -> new AugmentItem("edge", settings));
        public static final Item LEGENDARY_INSCRIPTION_SWIFT = register("legendary_inscription_swift", settings -> new AugmentItem("swift", settings));
        public static final Item LEGENDARY_INSCRIPTION_WARD = register("legendary_inscription_ward", settings -> new AugmentItem("ward", settings));
    public static final Item TEST_DUMMY_SPAWN_EGG = register("test_dummy_spawn_egg",
            settings -> new SpawnEggItem(settings.spawnEgg(com.feel.gems.entity.ModEntities.TEST_DUMMY).stacksTo(64)));

    public static final Item TRACKER_COMPASS = register("tracker_compass", settings -> new TrackerCompassItem(settings.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final Item RECALL_RELIC = register("recall_relic", settings -> new RecallRelicItem(settings.stacksTo(1)));
    public static final Item HYPNO_STAFF = register("hypno_staff", settings -> new HypnoStaffItem(settings.stacksTo(1)));
    public static final Item EARTHSPLITTER_PICK = register("earthsplitter_pick",
            settings -> new EarthsplitterPickItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item SUPREME_HELMET = register("supreme_helmet",
            settings -> new SupremeArmorItem(ArmorMaterials.NETHERITE, ArmorType.HELMET, SupremeArmorItem.Piece.HELMET, settings.stacksTo(1)));
    public static final Item SUPREME_CHESTPLATE = register("supreme_chestplate",
            settings -> new SupremeArmorItem(ArmorMaterials.NETHERITE, ArmorType.CHESTPLATE, SupremeArmorItem.Piece.CHESTPLATE, settings.stacksTo(1)));
    public static final Item SUPREME_LEGGINGS = register("supreme_leggings",
            settings -> new SupremeArmorItem(ArmorMaterials.NETHERITE, ArmorType.LEGGINGS, SupremeArmorItem.Piece.LEGGINGS, settings.stacksTo(1)));
    public static final Item SUPREME_BOOTS = register("supreme_boots",
            settings -> new SupremeArmorItem(ArmorMaterials.NETHERITE, ArmorType.BOOTS, SupremeArmorItem.Piece.BOOTS, settings.stacksTo(1)));
    public static final Item BLOOD_OATH_BLADE = register("blood_oath_blade",
            settings -> new BloodOathBladeItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item DEMOLITION_BLADE = register("demolition_blade",
            settings -> new DemolitionBladeItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item HUNTERS_SIGHT_BOW = register("hunters_sight_bow",
            settings -> new HunterSightBowItem(settings.stacksTo(1).durability(600)));
    public static final Item THIRD_STRIKE_BLADE = register("third_strike_blade",
            settings -> new ThirdStrikeBladeItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item VAMPIRIC_EDGE = register("vampiric_edge",
            settings -> new VampiricEdgeItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));

    public static final Item ASTRA_GEM = registerGem(GemId.ASTRA);
    public static final Item FIRE_GEM = registerGem(GemId.FIRE);
    public static final Item FLUX_GEM = registerGem(GemId.FLUX);
    public static final Item LIFE_GEM = registerGem(GemId.LIFE);
    public static final Item PUFF_GEM = registerGem(GemId.PUFF);
    public static final Item SPEED_GEM = registerGem(GemId.SPEED);
    public static final Item STRENGTH_GEM = registerGem(GemId.STRENGTH);
    public static final Item WEALTH_GEM = registerGem(GemId.WEALTH);
    public static final Item TERROR_GEM = registerGem(GemId.TERROR);
    public static final Item SUMMONER_GEM = registerGem(GemId.SUMMONER);
    public static final Item SPACE_GEM = registerGem(GemId.SPACE);
    public static final Item REAPER_GEM = registerGem(GemId.REAPER);
    public static final Item PILLAGER_GEM = registerGem(GemId.PILLAGER);
    public static final Item SPY_GEM = registerGem(GemId.SPY);
    public static final Item BEACON_GEM = registerGem(GemId.BEACON);
    public static final Item AIR_GEM = registerGem(GemId.AIR);
    public static final Item VOID_GEM = registerGem(GemId.VOID);
    public static final Item CHAOS_GEM = registerGem(GemId.CHAOS);
    public static final Item PRISM_GEM = registerGem(GemId.PRISM);
    public static final Item DUELIST_GEM = registerGem(GemId.DUELIST);
    public static final Item HUNTER_GEM = registerGem(GemId.HUNTER);
    public static final Item SENTINEL_GEM = registerGem(GemId.SENTINEL);
    public static final Item TRICKSTER_GEM = registerGem(GemId.TRICKSTER);

    // Legendary Items
    public static final Item GEM_SEER = register("gem_seer", settings -> new GemSeerItem(settings.stacksTo(1)));

    // PvP Legendary Items
    public static final Item EXPERIENCE_BLADE = register("experience_blade",
            settings -> new ExperienceBladeItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item REVERSAL_MIRROR = register("reversal_mirror",
            settings -> new ReversalMirrorItem(settings.stacksTo(1)));
    public static final Item HUNTERS_TROPHY_NECKLACE = register("hunters_trophy_necklace",
            settings -> new HuntersTrophyNecklaceItem(settings.stacksTo(1)));
    public static final Item GLADIATORS_MARK = register("gladiators_mark",
            settings -> new GladiatorsMarkItem(settings.stacksTo(1)));
    public static final Item SOUL_SHACKLE = register("soul_shackle",
            settings -> new SoulShackleItem(settings.stacksTo(1)));
    public static final Item DUELISTS_RAPIER = register("duelists_rapier",
            settings -> new DuelistsRapierItem(ToolMaterial.NETHERITE, settings.stacksTo(1)));
    public static final Item CHALLENGERS_GAUNTLET = register("challengers_gauntlet",
            settings -> new ChallengersGauntletItem(settings.stacksTo(1)));
    public static final Item CHRONO_CHARM = register("chrono_charm",
            settings -> new ChronoCharmItem(settings.stacksTo(1)));

    private ModItems() {
    }

    public static void init() {
        // Triggers static initialization.
        ModItemGroups.init();
    }

    public static Item gemItem(GemId gemId) {
        return switch (gemId) {
            case ASTRA -> ASTRA_GEM;
            case FIRE -> FIRE_GEM;
            case FLUX -> FLUX_GEM;
            case LIFE -> LIFE_GEM;
            case PUFF -> PUFF_GEM;
            case SPEED -> SPEED_GEM;
            case STRENGTH -> STRENGTH_GEM;
            case WEALTH -> WEALTH_GEM;
            case TERROR -> TERROR_GEM;
            case SUMMONER -> SUMMONER_GEM;
            case SPACE -> SPACE_GEM;
            case REAPER -> REAPER_GEM;
            case PILLAGER -> PILLAGER_GEM;
            case SPY -> SPY_GEM;
            case BEACON -> BEACON_GEM;
            case AIR -> AIR_GEM;
            case VOID -> VOID_GEM;
            case CHAOS -> CHAOS_GEM;
            case PRISM -> PRISM_GEM;
            case DUELIST -> DUELIST_GEM;
            case HUNTER -> HUNTER_GEM;
            case SENTINEL -> SENTINEL_GEM;
            case TRICKSTER -> TRICKSTER_GEM;
        };
    }

    private static Item registerGem(GemId gemId) {
        return register(gemId.name().toLowerCase() + "_gem", settings -> new GemItem(gemId, settings.stacksTo(1)));
    }

    private static Item register(String path, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, path);
        Item.Properties settings = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
        Item item = factory.apply(settings);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }
}
