package com.feel.gems.augment;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.item.GemItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AugmentRuntime {
    private static final String KEY_GEM_AUGMENTS = "gemAugments";
    private static final String KEY_LEGENDARY_AUGMENTS = "legendaryAugments";
    private static final String KEY_AUGMENT_ID = "id";
    private static final String KEY_AUGMENT_RARITY = "rarity";
    private static final String KEY_AUGMENT_MAG = "mag";
    private static final String KEY_ACTIVE_GEM_AUGMENTS = "activeGemAugments";
    private static final String KEY_ACTIVE_GEM_ID = "activeGemAugmentsId";

    private static final Identifier MOD_LEGENDARY_DAMAGE = Identifier.fromNamespaceAndPath("gems", "legendary_damage");
    private static final Identifier MOD_LEGENDARY_ATTACK_SPEED = Identifier.fromNamespaceAndPath("gems", "legendary_attack_speed");
    private static final Identifier MOD_LEGENDARY_MOVE_SPEED = Identifier.fromNamespaceAndPath("gems", "legendary_move_speed");
    private static final Identifier MOD_LEGENDARY_ARMOR = Identifier.fromNamespaceAndPath("gems", "legendary_armor");

    private AugmentRuntime() {
    }

    public static AugmentInstance getInstance(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag nbt = custom.copyTag();
        String id = nbt.getStringOr(KEY_AUGMENT_ID, "");
        if (id.isEmpty()) {
            return null;
        }
        String rarityStr = nbt.getStringOr(KEY_AUGMENT_RARITY, "");
        float mag = nbt.getFloatOr(KEY_AUGMENT_MAG, 0.0F);
        AugmentRarity rarity;
        try {
            rarity = AugmentRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            rarity = AugmentRarity.COMMON;
        }
        return new AugmentInstance(id, rarity, mag);
    }

    public static void assignInstance(ItemStack stack, AugmentInstance instance) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.putString(KEY_AUGMENT_ID, instance.augmentId());
            nbt.putString(KEY_AUGMENT_RARITY, instance.rarity().name());
            nbt.putFloat(KEY_AUGMENT_MAG, instance.magnitude());
        });
    }

    public static AugmentInstance rollInstance(String augmentId) {
        AugmentDefinition def = AugmentRegistry.get(augmentId);
        if (def == null) {
            return null;
        }
        AugmentRarity rarity = rollRarity();
        float mag = rollMagnitude(rarity);
        return new AugmentInstance(augmentId, rarity, mag);
    }

    public static boolean applyGemAugment(ServerPlayer player, ItemStack target, AugmentInstance instance) {
        if (target == null || !(target.getItem() instanceof GemItem gemItem)) {
            return false;
        }
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null || def.target() != AugmentTarget.GEM) {
            return false;
        }

        List<AugmentInstance> current = getGemAugments(target);
        int maxSlots = GemsBalance.v().augments().gemMaxSlots();
        if (current.size() >= maxSlots) {
            player.sendSystemMessage(Component.translatable("gems.augment.no_slots").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (hasConflict(current, def)) {
            player.sendSystemMessage(Component.translatable("gems.augment.conflict").withStyle(ChatFormatting.RED), true);
            return false;
        }

        current.add(instance);
        saveGemAugments(target, current);
        if (GemPlayerState.getActiveGem(player) == gemItem.gemId()) {
            captureActiveGemAugments(player, gemItem.gemId(), target);
            com.feel.gems.net.GemCooldownSync.send(player);
        }
        player.sendSystemMessage(Component.translatable("gems.augment.applied_gem").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean applyLegendaryAugment(ServerPlayer player, ItemStack target, AugmentInstance instance) {
        if (!(target.getItem() instanceof LegendaryItem)) {
            return false;
        }
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null || def.target() != AugmentTarget.LEGENDARY) {
            return false;
        }
        List<AugmentInstance> current = getLegendaryAugments(target);
        int maxSlots = GemsBalance.v().augments().legendaryMaxSlots();
        if (current.size() >= maxSlots) {
            player.sendSystemMessage(Component.translatable("gems.augment.no_slots").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (hasConflict(current, def)) {
            player.sendSystemMessage(Component.translatable("gems.augment.conflict").withStyle(ChatFormatting.RED), true);
            return false;
        }
        current.add(instance);
        saveLegendaryAugments(target, current);
        applyLegendaryModifiers(player);
        player.sendSystemMessage(Component.translatable("gems.augment.applied_legendary").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static float cooldownMultiplier(ServerPlayer player, GemId gemId) {
        float reduction = 0.0f;
        for (AugmentInstance instance : getActiveGemAugments(player, gemId)) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            if (def == null) continue;
            for (AugmentModifier mod : def.modifiers()) {
                if (mod.type() == AugmentModifierType.COOLDOWN_MULTIPLIER) {
                    reduction += mod.baseMagnitude() * instance.magnitude();
                }
            }
        }
        reduction = Math.min(reduction, 0.6f); // cap 60% reduction
        return Math.max(0.1f, 1.0f - reduction);
    }

    public static float durationMultiplier(ServerPlayer player, GemId gemId) {
        float bonus = 0.0f;
        for (AugmentInstance instance : getActiveGemAugments(player, gemId)) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            if (def == null) {
                continue;
            }
            for (AugmentModifier mod : def.modifiers()) {
                if (mod.type() == AugmentModifierType.DURATION_MULTIPLIER) {
                    bonus += mod.baseMagnitude() * instance.magnitude();
                }
            }
        }
        bonus = Math.min(bonus, 1.0f); // cap 100% increase
        return Math.max(1.0f, 1.0f + bonus);
    }

    public static int applyDurationMultiplier(ServerPlayer player, GemId gemId, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = durationMultiplier(player, gemId);
        if (GemPlayerState.getActiveGem(player) == GemId.PRISM && gemId != GemId.PRISM) {
            mult *= durationMultiplier(player, GemId.PRISM);
        }
        int adjusted = Math.round(baseTicks * mult);
        return Math.max(1, adjusted);
    }

    public static int passiveAmplifierBonus(ServerPlayer player, Identifier passiveId) {
        Optional<GemId> gem = findGemForPassive(passiveId);
        if (gem.isEmpty()) {
            return 0;
        }
        if (GemPlayerState.getActiveGem(player) != gem.get()) {
            return 0;
        }
        int bonus = 0;
        for (AugmentInstance instance : getActiveGemAugments(player, gem.get())) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            if (def == null) continue;
            for (AugmentModifier mod : def.modifiers()) {
                if (mod.type() == AugmentModifierType.PASSIVE_AMP_BONUS) {
                    bonus += Math.round(mod.baseMagnitude() * instance.magnitude());
                }
            }
        }
        return Math.max(0, bonus);
    }

    public static void applyLegendaryModifiers(ServerPlayer player) {
        float damage = 0.0f;
        float attackSpeed = 0.0f;
        float moveSpeed = 0.0f;
        float armor = 0.0f;

        for (ItemStack stack : getLegendaryStacks(player)) {
            for (AugmentInstance instance : getLegendaryAugments(stack)) {
                AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
                if (def == null) continue;
                for (AugmentModifier mod : def.modifiers()) {
                    float value = mod.baseMagnitude() * instance.magnitude();
                    switch (mod.type()) {
                        case LEGENDARY_ATTACK_DAMAGE -> damage += value;
                        case LEGENDARY_ATTACK_SPEED -> attackSpeed += value;
                        case LEGENDARY_MOVE_SPEED -> moveSpeed += value;
                        case LEGENDARY_ARMOR -> armor += value;
                        default -> {
                        }
                    }
                }
            }
        }

        applyAttribute(player, Attributes.ATTACK_DAMAGE, MOD_LEGENDARY_DAMAGE, damage);
        applyAttribute(player, Attributes.ATTACK_SPEED, MOD_LEGENDARY_ATTACK_SPEED, attackSpeed);
        applyAttribute(player, Attributes.MOVEMENT_SPEED, MOD_LEGENDARY_MOVE_SPEED, moveSpeed);
        applyAttribute(player, Attributes.ARMOR, MOD_LEGENDARY_ARMOR, armor);
    }

    private static void applyAttribute(ServerPlayer player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, Identifier id, float value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (value > 0.0001f) {
            inst.addTransientModifier(new AttributeModifier(id, (double) value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static List<ItemStack> getLegendaryStacks(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        if (player.getMainHandItem().getItem() instanceof LegendaryItem) {
            stacks.add(player.getMainHandItem());
        }
        if (player.getOffhandItem().getItem() instanceof LegendaryItem) {
            stacks.add(player.getOffhandItem());
        }
        ItemStack head = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (head.getItem() instanceof LegendaryItem) {
            stacks.add(head);
        }
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.getItem() instanceof LegendaryItem) {
            stacks.add(chest);
        }
        ItemStack legs = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        if (legs.getItem() instanceof LegendaryItem) {
            stacks.add(legs);
        }
        ItemStack feet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
        if (feet.getItem() instanceof LegendaryItem) {
            stacks.add(feet);
        }
        return stacks;
    }

    private static boolean hasConflict(List<AugmentInstance> current, AugmentDefinition def) {
        Set<String> conflicts = def.conflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            return false;
        }
        String defId = def.id();
        for (AugmentInstance inst : current) {
            String id = inst.augmentId();
            if (id == null) {
                continue;
            }
            for (String conflict : conflicts) {
                if (conflict == null) {
                    continue;
                }
                // Allow stacking the same augment/inscription type.
                if (conflict.equals(defId) && id.equals(defId)) {
                    continue;
                }
                if (conflict.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<GemId> findGemForPassive(Identifier passiveId) {
        for (GemId gem : GemId.values()) {
            try {
                GemDefinition def = GemRegistry.definition(gem);
                if (def.passives().contains(passiveId)) {
                    return Optional.of(gem);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    private static CompoundTag persistentRoot(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    public static List<AugmentInstance> getGemAugments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ArrayList<>();
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return new ArrayList<>();
        }
        CompoundTag nbt = custom.copyTag();
        ListTag list = nbt.getList(KEY_GEM_AUGMENTS).orElse(new ListTag());
        return readList(list);
    }

    public static boolean removeGemAugment(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        List<AugmentInstance> current = getGemAugments(stack);
        if (index < 0 || index >= current.size()) {
            return false;
        }
        current.remove(index);
        saveGemAugments(stack, current);
        return true;
    }

    public static void clearGemAugments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        saveGemAugments(stack, List.of());
    }

    private static void saveGemAugments(ItemStack stack, List<AugmentInstance> list) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.put(KEY_GEM_AUGMENTS, writeList(list)));
    }

    public static void captureActiveGemAugments(ServerPlayer player, GemId gemId, ItemStack stack) {
        if (player == null || gemId == null || stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag root = persistentRoot(player);
        root.putString(KEY_ACTIVE_GEM_ID, gemId.name());
        root.put(KEY_ACTIVE_GEM_AUGMENTS, writeList(getGemAugments(stack)));
    }

    public static void clearActiveGemAugments(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag root = persistentRoot(player);
        root.remove(KEY_ACTIVE_GEM_ID);
        root.remove(KEY_ACTIVE_GEM_AUGMENTS);
    }

    private static List<AugmentInstance> getActiveGemAugments(ServerPlayer player, GemId gemId) {
        if (player == null || gemId == null) {
            return List.of();
        }
        CompoundTag root = persistentRoot(player);
        String active = root.getStringOr(KEY_ACTIVE_GEM_ID, "");
        if (!gemId.name().equals(active)) {
            return List.of();
        }
        ListTag list = root.getList(KEY_ACTIVE_GEM_AUGMENTS).orElse(new ListTag());
        return readList(list);
    }

    public static List<AugmentInstance> getLegendaryAugments(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return new ArrayList<>();
        }
        CompoundTag nbt = custom.copyTag();
        ListTag list = nbt.getList(KEY_LEGENDARY_AUGMENTS).orElse(new ListTag());
        return readList(list);
    }

    public static boolean removeLegendaryAugment(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        List<AugmentInstance> current = getLegendaryAugments(stack);
        if (index < 0 || index >= current.size()) {
            return false;
        }
        current.remove(index);
        saveLegendaryAugments(stack, current);
        return true;
    }

    public static void clearLegendaryAugments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        saveLegendaryAugments(stack, List.of());
    }

    private static void saveLegendaryAugments(ItemStack stack, List<AugmentInstance> list) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.put(KEY_LEGENDARY_AUGMENTS, writeList(list)));
    }

    private static List<AugmentInstance> readList(ListTag list) {
        List<AugmentInstance> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElse(null);
            if (entry == null) {
                continue;
            }
            String id = entry.getStringOr(KEY_AUGMENT_ID, "");
            String rarityStr = entry.getStringOr(KEY_AUGMENT_RARITY, "");
            float mag = entry.getFloatOr(KEY_AUGMENT_MAG, 0.0F);
            AugmentRarity rarity;
            try {
                rarity = AugmentRarity.valueOf(rarityStr);
            } catch (IllegalArgumentException e) {
                rarity = AugmentRarity.COMMON;
            }
            if (!id.isEmpty()) {
                out.add(new AugmentInstance(id, rarity, mag));
            }
        }
        return out;
    }

    private static ListTag writeList(List<AugmentInstance> list) {
        ListTag out = new ListTag();
        for (AugmentInstance inst : list) {
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_AUGMENT_ID, inst.augmentId());
            entry.putString(KEY_AUGMENT_RARITY, inst.rarity().name());
            entry.putFloat(KEY_AUGMENT_MAG, inst.magnitude());
            out.add(entry);
        }
        return out;
    }

    private static AugmentRarity rollRarity() {
        int common = Math.max(0, GemsBalance.v().augments().rarityCommonWeight());
        int rare = Math.max(0, GemsBalance.v().augments().rarityRareWeight());
        int epic = Math.max(0, GemsBalance.v().augments().rarityEpicWeight());
        int total = common + rare + epic;
        if (total <= 0) {
            return AugmentRarity.COMMON;
        }
        int roll = (int) (Math.random() * total);
        if (roll < common) {
            return AugmentRarity.COMMON;
        }
        roll -= common;
        if (roll < rare) {
            return AugmentRarity.RARE;
        }
        return AugmentRarity.EPIC;
    }

    private static float rollMagnitude(AugmentRarity rarity) {
        var cfg = GemsBalance.v().augments();
        return switch (rarity) {
            case COMMON -> randomRange(cfg.commonMagnitudeMin(), cfg.commonMagnitudeMax());
            case RARE -> randomRange(cfg.rareMagnitudeMin(), cfg.rareMagnitudeMax());
            case EPIC -> randomRange(cfg.epicMagnitudeMin(), cfg.epicMagnitudeMax());
        };
    }

    private static float randomRange(float min, float max) {
        if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        float range = Math.max(0.0f, max - min);
        return min + (float) Math.random() * range;
    }
}
