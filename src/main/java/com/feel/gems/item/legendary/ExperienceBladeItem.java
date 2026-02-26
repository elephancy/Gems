package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Experience Blade - consume XP to gain Sharpness.
 * 10 levels -> Sharpness II, 20 -> IV, 30 -> VI, max Sharpness XX at 100 levels.
 * Enchantment persists until death.
 */
public final class ExperienceBladeItem extends Item implements LegendaryItem {
    private static final String SHARPNESS_LEVEL_KEY = "experience_blade_sharpness";

    public ExperienceBladeItem(ToolMaterial material, Properties settings) {
        super(settings.sword(material, 3.0F, -2.4F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "experience_blade").toString();
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide() || !(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        int currentSharpness = getCurrentSharpness(player);

        int maxSharpness = GemsBalance.v().legendary().experienceBladeMaxSharpness();
        int sharpnessPerTier = GemsBalance.v().legendary().experienceBladeSharpnessPerTier();
        int xpLevelsPerTier = GemsBalance.v().legendary().experienceBladeXpLevelsPerTier();

        if (currentSharpness >= maxSharpness) {
            player.sendSystemMessage(Component.translatable("gems.item.experience_blade.max").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // Calculate XP needed for next tier
        int nextTier = (currentSharpness / sharpnessPerTier) + 1;
        int xpLevelsNeeded = nextTier * xpLevelsPerTier;

        if (player.experienceLevel < xpLevelsNeeded) {
            player.sendSystemMessage(Component.translatable("gems.item.experience_blade.need_xp", xpLevelsNeeded, player.experienceLevel).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // Consume XP and upgrade
        player.giveExperienceLevels(-xpLevelsNeeded);
        int newSharpness = Math.min(maxSharpness, currentSharpness + sharpnessPerTier);
        setCurrentSharpness(player, newSharpness);

        // Apply enchantment to the blade
        applySharpnessToStack(stack, newSharpness, player);

        player.sendSystemMessage(Component.translatable("gems.item.experience_blade.upgraded", toRoman(newSharpness)).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        // Sync sharpness level with player state
        int storedSharpness = getCurrentSharpness(player);
        if (storedSharpness > 0) {
            applySharpnessToStack(stack, storedSharpness, player);
        }
    }

    private void applySharpnessToStack(ItemStack stack, int level, ServerPlayer player) {
        Holder<Enchantment> sharpnessEntry = player.level().registryAccess()
                .get(Enchantments.SHARPNESS).orElse(null);
        if (sharpnessEntry == null) return;

        int current = EnchantmentHelper.getItemEnchantmentLevel(sharpnessEntry, stack);
        if (current < level) {
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(sharpnessEntry, level));
        }
    }

    public static int getCurrentSharpness(ServerPlayer player) {
        String levelStr = PlayerStateManager.getPersistent(player, SHARPNESS_LEVEL_KEY);
        return levelStr != null ? Integer.parseInt(levelStr) : 0;
    }

    public static void setCurrentSharpness(ServerPlayer player, int level) {
        PlayerStateManager.setPersistent(player, SHARPNESS_LEVEL_KEY, String.valueOf(level));
    }

    public static void clearSharpness(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, SHARPNESS_LEVEL_KEY);
    }

    public static void clearOnDeath(ServerPlayer player) {
        clearSharpness(player);
        Holder<Enchantment> sharpness = player.level().registryAccess()
                .get(Enchantments.SHARPNESS).orElse(null);
        if (sharpness == null) {
            return;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.is(com.feel.gems.item.ModItems.EXPERIENCE_BLADE)) {
                continue;
            }
            stripSharpness(stack, sharpness, player);
        }
    }

    private static void stripSharpness(ItemStack stack, Holder<Enchantment> sharpness, ServerPlayer player) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchants.isEmpty()) {
            return;
        }
        List<Holder<Enchantment>> keepKeys = new ArrayList<>();
        List<Integer> keepLevels = new ArrayList<>();
        for (var entry : enchants.entrySet()) {
            if (entry.getKey() == sharpness) {
                continue;
            }
            keepKeys.add(entry.getKey());
            keepLevels.add(entry.getIntValue());
        }
        EnchantmentHelper.setEnchantments(stack, ItemEnchantments.EMPTY);
        for (int i = 0; i < keepKeys.size(); i++) {
            Holder<Enchantment> key = keepKeys.get(i);
            int level = keepLevels.get(i);
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(key, level));
        }
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 2 -> "II";
            case 4 -> "IV";
            case 6 -> "VI";
            case 8 -> "VIII";
            case 10 -> "X";
            case 12 -> "XII";
            case 14 -> "XIV";
            case 16 -> "XVI";
            case 18 -> "XVIII";
            case 20 -> "XX";
            default -> String.valueOf(number);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.experience_blade.tooltip.1"),
                Component.translatable("gems.item.experience_blade.tooltip.2")
        );
    }
}
