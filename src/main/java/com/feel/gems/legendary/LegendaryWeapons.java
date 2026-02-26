package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.util.GemsNbt;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;




public final class LegendaryWeapons {
    private static final String KEY_BLOOD_OATH_KILLS = "legendaryBloodOathKills";

    private LegendaryWeapons() {
    }

    public static void onPlayerKill(ServerPlayer killer, ServerPlayer victim) {
        if (!killer.getMainHandItem().is(ModItems.BLOOD_OATH_BLADE)) {
            return;
        }
        ItemStackHelper.applyUniqueKill(killer, victim);
    }

    private static final class ItemStackHelper {
        static void applyUniqueKill(ServerPlayer killer, ServerPlayer victim) {
            var stack = killer.getMainHandItem();
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag nbt = data != null ? data.copyTag() : new CompoundTag();
            ListTag list = nbt.getListOrEmpty(KEY_BLOOD_OATH_KILLS);
            UUID victimId = victim.getUUID();
            if (containsUuid(list, victimId)) {
                return;
            }
            list.add(GemsNbt.fromUuid(victimId));
            nbt.put(KEY_BLOOD_OATH_KILLS, list);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, out -> out.merge(nbt));

            Holder<Enchantment> sharpness = resolveSharpness(killer);
            if (sharpness == null) {
                return;
            }
            int current = EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack);
            int cap = GemsBalance.v().legendary().bloodOathSharpnessCap();
            int next = Math.min(cap, current + 1);
            if (next > current) {
                EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(sharpness, next));
            }
        }

        private static boolean containsUuid(ListTag list, UUID uuid) {
            for (int i = 0; i < list.size(); i++) {
                if (uuid.equals(GemsNbt.toUuid(list.get(i)))) {
                    return true;
                }
            }
            return false;
        }

        private static Holder<Enchantment> resolveSharpness(ServerPlayer killer) {
            return killer.level().registryAccess().get(Enchantments.SHARPNESS).orElse(null);
        }
    }
}
