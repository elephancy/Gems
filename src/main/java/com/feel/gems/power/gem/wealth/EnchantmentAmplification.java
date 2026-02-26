package com.feel.gems.power.gem.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;


public final class EnchantmentAmplification {
    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";
    private static final String CUSTOM_DATA_KEY_BONUS = "bonus";

    private EnchantmentAmplification() {
    }

    public static void apply(ServerPlayer player, int durationTicks) {
        long until = GemsTime.now(player) + durationTicks;

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        amplifyStack(world, player.getMainHandItem(), until);
        amplifyStack(world, player.getOffhandItem(), until);
        amplifyStack(world, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), until);
        amplifyStack(world, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), until);
        amplifyStack(world, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), until);
        amplifyStack(world, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), until);
        amplifyStack(world, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY), until);
    }

    private static void amplifyStack(ServerLevel world, ItemStack stack, long until) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.isEnchanted()) {
            return;
        }

        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && custom.copyTag().contains(CUSTOM_DATA_KEY_AMPLIFY)) {
            return;
        }

        ItemEnchantments enchants = stack.getEnchantments();
        if (enchants.isEmpty()) {
            return;
        }

        int bonusLevels = Math.max(1, GemsBalance.v().wealth().amplificationBonusLevels());
        ListTag originalList = new ListTag();
        CompoundTag bonus = new CompoundTag();
        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            if (level >= enchantment.value().getMaxLevel() && isOverMaxEligible(enchantment)) {
                bonus.putInt(enchantment.getRegisteredName(), bonusLevels);
            }

            CompoundTag e = new CompoundTag();
            e.putString("id", world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getKey(enchantment.value()).toString());
            e.putInt("lvl", level);
            originalList.add(e);
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            CompoundTag marker = new CompoundTag();
            marker.putLong("until", until);
            marker.put("enchants", originalList);
            if (!bonus.isEmpty()) {
                marker.put(CUSTOM_DATA_KEY_BONUS, bonus);
            }
            nbt.put(CUSTOM_DATA_KEY_AMPLIFY, marker);
        });

        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int current = entry.getIntValue();
            int boosted = Math.min(enchantment.value().getMaxLevel(), current + bonusLevels);
            if (boosted == current) {
                continue;
            }
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(enchantment, boosted));
        }
    }

    public static void restoreFromList(ServerLevel world, ItemStack stack, ListTag list) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        EnchantmentHelper.setEnchantments(stack, ItemEnchantments.EMPTY);
        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i).orElse(null);
            if (e == null) {
                continue;
            }
            String rawId = e.getStringOr("id", "");
            int lvl = e.getIntOr("lvl", 0);
            var id = net.minecraft.resources.Identifier.tryParse(rawId);
            if (id == null) {
                continue;
            }
            var entry = registry.get(id);
            if (entry.isEmpty()) {
                continue;
            }
            int clamped = Math.max(1, Math.min(entry.get().value().getMaxLevel(), lvl));
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(entry.get(), clamped));
        }
    }

    public static int getBonusLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        if (stack == null || stack.count() <= 0) {
            return 0;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return 0;
        }
        CompoundTag nbt = custom.copyTag();
        CompoundTag marker = nbt.getCompound(CUSTOM_DATA_KEY_AMPLIFY).orElse(null);
        if (marker == null || marker.isEmpty()) {
            return 0;
        }
        CompoundTag bonus = marker.getCompound(CUSTOM_DATA_KEY_BONUS).orElse(null);
        if (bonus == null || bonus.isEmpty()) {
            return 0;
        }
        String key = enchantment.getRegisteredName();
        if (key == null || key.isEmpty()) {
            return 0;
        }
        return bonus.getIntOr(key, 0);
    }

    private static boolean isOverMaxEligible(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.PROTECTION)
                || enchantment.is(Enchantments.MENDING)
                || enchantment.is(Enchantments.LOOTING)
                || enchantment.is(Enchantments.SHARPNESS)
                || enchantment.is(Enchantments.EFFICIENCY)
                || enchantment.is(Enchantments.FORTUNE);
    }
}
