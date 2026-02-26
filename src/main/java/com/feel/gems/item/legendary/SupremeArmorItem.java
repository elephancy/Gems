package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;




public final class SupremeArmorItem extends Item implements LegendaryItem {
    public enum Piece {
        HELMET("supreme_helmet"),
        CHESTPLATE("supreme_chestplate"),
        LEGGINGS("supreme_leggings"),
        BOOTS("supreme_boots");

        private final String id;

        Piece(String id) {
            this.id = id;
        }
    }

    private final Piece piece;

    public SupremeArmorItem(ArmorMaterial material, ArmorType type, Piece piece, Item.Properties settings) {
        super(settings.humanoidArmor(material, type).enchantable(15));
        this.piece = piece;
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, piece.id).toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("item.gems." + piece.id + ".desc"),
                Component.translatable("item.gems.supreme_set_bonus")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        applyTrimIfMissing(stack, world.registryAccess());
    }

    public static void applySupremeTrim(ItemStack stack, net.minecraft.core.HolderLookup.Provider lookup) {
        if (stack.has(DataComponents.TRIM)) {
            return;
        }
        if (lookup == null) {
            return;
        }
        ResourceKey<net.minecraft.world.item.equipment.trim.TrimMaterial> materialKey = TrimMaterials.NETHERITE;
        ResourceKey<net.minecraft.world.item.equipment.trim.TrimPattern> patternKey = TrimPatterns.SILENCE;
        var material = lookup.get(materialKey).orElse(null);
        var pattern = lookup.get(patternKey).orElse(null);
        if (material == null || pattern == null) {
            return;
        }
        stack.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
    }
	
    private static void applyTrimIfMissing(ItemStack stack, net.minecraft.core.RegistryAccess registries) {
        applySupremeTrim(stack, registries);
    }
}
