package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemMaintainedPassive;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;




public final class AutoEnchantPassive implements GemMaintainedPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final ResourceKey<Enchantment> enchantmentKey;
    private final int level;
    private final Predicate<ItemStack> predicate;

    public AutoEnchantPassive(
            Identifier id,
            String name,
            String description,
            ResourceKey<Enchantment> enchantmentKey,
            int level,
            Predicate<ItemStack> predicate
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enchantmentKey = enchantmentKey;
        this.level = level;
        this.predicate = predicate;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayer player) {
        Holder<Enchantment> enchantment = resolve(player);
        if (enchantment == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        ensureEnchanted(world, player.getMainHandItem(), enchantment);
        ensureEnchanted(world, player.getOffhandItem(), enchantment);
        ensureEnchanted(world, player.getItemBySlot(EquipmentSlot.HEAD), enchantment);
        ensureEnchanted(world, player.getItemBySlot(EquipmentSlot.CHEST), enchantment);
        ensureEnchanted(world, player.getItemBySlot(EquipmentSlot.LEGS), enchantment);
        ensureEnchanted(world, player.getItemBySlot(EquipmentSlot.FEET), enchantment);
        ensureEnchanted(world, player.getItemBySlot(EquipmentSlot.BODY), enchantment);
    }

    @Override
    public void remove(ServerPlayer player) {
        // Intentionally does not remove enchantments to avoid deleting player-owned enchants.
    }

    private void ensureEnchanted(ServerLevel world, ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        normalizeEnchantments(world, stack);
        if (!predicate.test(stack)) {
            return;
        }
        Enchantment enchantmentValue = enchantment.value();
        if (!enchantmentValue.canEnchant(stack)) {
            return;
        }
        if (!EnchantmentHelper.canStoreEnchantments(stack)) {
            return;
        }
        int current = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        if (current >= level) {
            return;
        }
        if (current <= 0) {
            var existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            for (Holder<Enchantment> other : existing.keySet()) {
                if (other == enchantment) {
                    continue;
                }
                if (!Enchantment.areCompatible(other, enchantment)) {
                    return;
                }
            }
        }

        EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(enchantment, level));
    }

    private void normalizeEnchantments(ServerLevel world, ItemStack stack) {
        var enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchants.isEmpty()) {
            return;
        }
        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        java.util.HashMap<Holder<Enchantment>, Integer> normalized = new java.util.HashMap<>();
        boolean changed = false;
        for (var entry : enchants.entrySet()) {
            Enchantment value = entry.getKey().value();
            var id = registry.getKey(value);
            if (id == null) {
                changed = true;
                continue;
            }
            var regEntry = registry.get(id);
            if (regEntry.isEmpty()) {
                changed = true;
                continue;
            }
            Holder<Enchantment> canonical = regEntry.get();
            int level = entry.getIntValue();
            normalized.merge(canonical, level, Math::max);
            if (canonical != entry.getKey()) {
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        EnchantmentHelper.setEnchantments(stack, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var entry : normalized.entrySet()) {
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(entry.getKey(), entry.getValue()));
        }
    }

    private Holder<Enchantment> resolve(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) {
            return null;
        }
        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var entry = registry.get(enchantmentKey.identifier());
        if (entry.isEmpty()) {
            return null;
        }
        return entry.get();
    }

    public static AutoEnchantPassive fireAspect(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.FIRE_ASPECT, level, AutoEnchantPredicates::isMeleeWeapon);
    }

    public static AutoEnchantPassive sharpness(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.SHARPNESS, level, AutoEnchantPredicates::isMeleeWeapon);
    }

    public static AutoEnchantPassive unbreaking(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.UNBREAKING, level, AutoEnchantPredicates::isGearOrTool);
    }

    public static AutoEnchantPassive power(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.POWER, level, AutoEnchantPredicates::isBow);
    }

    public static AutoEnchantPassive punch(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.PUNCH, level, AutoEnchantPredicates::isBow);
    }

    public static AutoEnchantPassive quickCharge(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.QUICK_CHARGE, level, AutoEnchantPredicates::isCrossbow);
    }

    public static AutoEnchantPassive mending(Identifier id, String name, String description) {
        return new AutoEnchantPassive(id, name, description, Enchantments.MENDING, 1, AutoEnchantPredicates::isGearOrTool);
    }

    public static AutoEnchantPassive fortune(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.FORTUNE, level, AutoEnchantPredicates::isTool);
    }

    public static AutoEnchantPassive looting(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.LOOTING, level, AutoEnchantPredicates::isMeleeWeapon);
    }
}
