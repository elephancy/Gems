package com.feel.gems.mixin;

import com.feel.gems.power.gem.fire.AutoSmeltCache;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Block.class)
public abstract class BlockAutoSmeltMixin {
    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void gems$autoSmelt(
            BlockState state,
            ServerLevel world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemInstance tool,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        boolean autoSmelt = GemPowers.isPassiveActive(player, PowerIds.AUTO_SMELT);
        boolean richRush = AbilityRuntime.isRichRushActive(player);
        if (!autoSmelt && !richRush) {
            return;
        }

        List<ItemStack> original = cir.getReturnValue();
        if (original.isEmpty()) {
            return;
        }

        List<ItemStack> out = new ArrayList<>(original.size() + 2);
        boolean changed = false;
        for (ItemStack stack : original) {
            ItemStack next = stack;

            if (autoSmelt) {
                ItemStack smelted = AutoSmeltCache.smeltResult(world, stack);
                if (smelted != null) {
                    next = smelted;
                    changed = true;
                }
            }

            if (richRush && isOre(state) && !hasSilkTouch(world, tool)) {
                int rolls = Math.max(1, GemsBalance.v().wealth().richRushLootRolls());
                ItemStack doubled = next.copy();
                doubled.setCount(next.getCount() * rolls);
                next = doubled;
                changed = true;
            }

            if (next == stack) {
                out.add(stack);
            } else {
                splitAndAdd(out, next);
            }
        }

        if (changed) {
            cir.setReturnValue(out);
        }
    }

    private static boolean isOre(BlockState state) {
        return (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE))
                || state.is(BlockTags.COPPER_ORES)
                || (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))
                || (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE))
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE))
                || (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE));
    }

    private static boolean hasSilkTouch(ServerLevel world, ItemInstance tool) {
        if (tool == null || tool.count() <= 0 || world == null) {
            return false;
        }
        var entry = world.registryAccess()
                .get(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)
                .orElse(null);
        if (entry == null) {
            return false;
        }
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(entry, tool) > 0;
    }

    private static void splitAndAdd(List<ItemStack> out, ItemStack stack) {
        int remaining = stack.getCount();
        int max = stack.getMaxStackSize();
        while (remaining > 0) {
            int part = Math.min(max, remaining);
            ItemStack copy = stack.copy();
            copy.setCount(part);
            out.add(copy);
            remaining -= part;
        }
    }
}
