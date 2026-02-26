package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;




public final class EarthsplitterPickItem extends Item implements LegendaryItem {
    private static final TagKey<net.minecraft.world.level.block.Block> BLACKLIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "earthsplitter_blacklist")
    );
    private static final String KEY_MINING = "legendaryEarthsplitterMining";
    private static final String KEY_TUNNEL = "legendaryEarthsplitterTunnel";

    public EarthsplitterPickItem(ToolMaterial material, Properties settings) {
        super(settings.pickaxe(material, 1.0F, -2.8F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "earthsplitter_pick").toString();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, world, state, pos, miner);
        if (world.isClientSide()) {
            return result;
        }
        if (!(miner instanceof ServerPlayer player)) {
            return result;
        }
        if (isRecursiveBreak(stack)) {
            return result;
        }
        int horizontalRadius = 4;
        int verticalRadius = 1;
        markRecursiveBreak(stack, true);
        try {
            if (isTunnelMode(stack)) {
                breakTunnel(stack, player, pos, verticalRadius);
            } else {
                breakCube(stack, player, pos, horizontalRadius, verticalRadius);
            }
        } finally {
            markRecursiveBreak(stack, false);
        }
        return result;
    }

    @Override
    public InteractionResult use(Level world, net.minecraft.world.entity.player.Player user, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean tunnel = !isTunnelMode(stack);
        setTunnelMode(stack, tunnel);
        if (user instanceof ServerPlayer player) {
                int horizontalRadius = 4;
                int verticalRadius = 1;
                int cubeSize = horizontalRadius * 2 + 1;
                int height = verticalRadius * 2 + 1;
                int tunnelLength = 20;
                int tunnelWidth = 2;
            String mode = tunnel
                    ? "Tunnel " + tunnelLength + "x" + tunnelWidth + "x" + height
                    : "Volume " + cubeSize + "x" + cubeSize + "x" + height;
            player.sendSystemMessage(Component.translatable("gems.item.earthsplitter_pick.mode", mode), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Holder<Enchantment> silk = resolveSilkTouch(player);
        if (silk == null) {
            return;
        }
        int current = EnchantmentHelper.getItemEnchantmentLevel(silk, stack);
        if (current < 1) {
            EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(silk, 1));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.earthsplitter_pick.desc"));
    }

    private static Holder<Enchantment> resolveSilkTouch(ServerPlayer player) {
        return player.level().registryAccess().get(Enchantments.SILK_TOUCH).orElse(null);
    }

    private static boolean isRecursiveBreak(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyTag().getBooleanOr(KEY_MINING, false);
    }

    private static void markRecursiveBreak(ItemStack stack, boolean value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            if (value) {
                nbt.putBoolean(KEY_MINING, true);
            } else {
                nbt.remove(KEY_MINING);
            }
        });
    }

    private static boolean isTunnelMode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyTag().getBooleanOr(KEY_TUNNEL, false);
    }

    private static void setTunnelMode(ItemStack stack, boolean value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            if (value) {
                nbt.putBoolean(KEY_TUNNEL, true);
            } else {
                nbt.remove(KEY_TUNNEL);
            }
        });
    }

    private static void breakCube(ItemStack stack, ServerPlayer player, BlockPos pos, int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean stop = false;
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (stack.isEmpty()) {
                        stop = true;
                        break;
                    }
                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!canBreak(player, stack, mutable)) {
                        continue;
                    }
                    tryBreakBlockNoDurability(player, stack, mutable.immutable());
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
    }

    private static void breakTunnel(ItemStack stack, ServerPlayer player, BlockPos pos, int verticalRadius) {
        int length = 20;
        Direction facing = player.getDirection();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean stop = false;
        int width = 2;
        int widthMin = -(width - 1);
        int widthMax = 0;
        for (int step = 0; step < length; step++) {
            int baseX = pos.getX() + facing.getStepX() * step;
            int baseY = pos.getY();
            int baseZ = pos.getZ() + facing.getStepZ() * step;
            for (int dx = widthMin; dx <= widthMax; dx++) {
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    if (stack.isEmpty()) {
                        stop = true;
                        break;
                    }
                    if (step == 0 && dx == 0 && dy == 0) {
                        continue;
                    }
                    mutable.set(
                            baseX + right.getStepX() * dx,
                            baseY + dy,
                            baseZ + right.getStepZ() * dx
                    );
                    if (!canBreak(player, stack, mutable)) {
                        continue;
                    }
                    tryBreakBlockNoDurability(player, stack, mutable.immutable());
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
    }

    private static boolean canBreak(ServerPlayer player, ItemStack stack, BlockPos.MutableBlockPos pos) {
        BlockState target = player.level().getBlockState(pos);
        if (target.isAir() || target.is(BLACKLIST)) {
            return false;
        }
        if (target.getDestroySpeed(player.level(), pos) < 0.0F) {
            return false;
        }
        if (target.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(target)) {
            return false;
        }
        return true;
    }

    private static void tryBreakBlockNoDurability(ServerPlayer player, ItemStack stack, BlockPos pos) {
        int desiredDamage = stack.isDamageableItem() ? stack.getDamageValue() : 0;
        int maxDamage = stack.getMaxDamage();
        if (stack.isDamageableItem() && maxDamage > 0) {
            int safeDamage = desiredDamage;
            if (maxDamage > 1 && desiredDamage >= maxDamage - 1) {
                safeDamage = Math.max(0, maxDamage - 2);
            }
            if (safeDamage != desiredDamage) {
                stack.setDamageValue(safeDamage);
            }
        }

        player.gameMode.destroyBlock(pos);

        if (!stack.isEmpty() && stack.isDamageableItem()) {
            stack.setDamageValue(desiredDamage);
        }
    }
}
