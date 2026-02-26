package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.feel.gems.util.GemsTeleport;




public final class RecallRelicItem extends Item implements LegendaryItem {
    private static final String KEY_MARK_DIM = "legendaryRecallDim";
    private static final String KEY_MARK_POS = "legendaryRecallPos";

    public RecallRelicItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "recall_relic").toString();
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(held) && !GemsAdmin.noLegendaryCooldowns(player)) {
            return InteractionResult.SUCCESS;
        }
        if (hasMark(player)) {
            teleportToMark(player);
        } else {
            setMark(player);
        }
        int cooldown = GemsBalance.v().legendary().recallCooldownTicks();
        if (cooldown > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(held, cooldown);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.recall_relic.desc"));
    }

    public static void ensureForceload(ServerPlayer player) {
        if (!hasMark(player)) {
            return;
        }
        Mark mark = readMark(player);
        if (mark == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel world = server == null ? null : server.getLevel(mark.dimension());
        if (world == null) {
            return;
        }
        forceChunk(world, mark.pos(), true);
    }

    public static void clearIfMissingItem(ServerPlayer player) {
        if (!hasMark(player)) {
            return;
        }
        if (hasRelic(player)) {
            return;
        }
        clearMark(player);
    }

    private static boolean hasRelic(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof RecallRelicItem) {
                return true;
            }
        }
        if (player.getOffhandItem().getItem() instanceof RecallRelicItem) {
            return true;
        }
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof RecallRelicItem
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof RecallRelicItem
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).getItem() instanceof RecallRelicItem
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem() instanceof RecallRelicItem;
    }

    private static void setMark(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        ServerLevel world = player.level();
        data.putString(KEY_MARK_DIM, world.dimension().identifier().toString());
        writeBlockPos(data, KEY_MARK_POS, player.blockPosition());
        forceChunk(world, player.blockPosition(), true);
        player.sendSystemMessage(Component.translatable("gems.item.recall_relic.mark_set"), true);
    }

    private static void teleportToMark(ServerPlayer player) {
        Mark mark = readMark(player);
        if (mark == null) {
            clearMark(player);
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel targetWorld = server.getLevel(mark.dimension());
        if (targetWorld == null) {
            player.sendSystemMessage(Component.translatable("gems.item.recall_relic.failed"), true);
            clearMark(player);
            return;
        }
        BlockPos pos = mark.pos();
        GemsTeleport.teleport(player, targetWorld, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
        clearMark(player);
        player.sendSystemMessage(Component.translatable("gems.item.recall_relic.recalled"), true);
    }

    private static boolean hasMark(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.getString(KEY_MARK_DIM).isPresent() && data.getIntArray(KEY_MARK_POS).isPresent();
    }

    private static Mark readMark(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String dimRaw = data.getStringOr(KEY_MARK_DIM, "");
        if (dimRaw.isEmpty()) {
            return null;
        }
        Identifier dim = Identifier.tryParse(dimRaw);
        BlockPos pos = readBlockPos(data, KEY_MARK_POS);
        if (dim == null || pos == null) {
            return null;
        }
        return new Mark(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim), pos);
    }

    private static void clearMark(ServerPlayer player) {
        Mark mark = readMark(player);
        MinecraftServer server = player.level().getServer();
        if (mark != null && server != null) {
            ServerLevel world = server.getLevel(mark.dimension());
            if (world != null) {
                forceChunk(world, mark.pos(), false);
            }
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_MARK_DIM);
        data.remove(KEY_MARK_POS);
    }

    private static void forceChunk(ServerLevel world, BlockPos pos, boolean forced) {
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        world.setChunkForced(chunk.x(), chunk.z(), forced);
    }

    private static void writeBlockPos(CompoundTag nbt, String key, BlockPos pos) {
        nbt.put(key, new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
    }

    private static BlockPos readBlockPos(CompoundTag nbt, String key) {
        int[] values = nbt.getIntArray(key).orElse(null);
        if (values == null) {
            return null;
        }
        if (values.length != 3) {
            return null;
        }
        return new BlockPos(values[0], values[1], values[2]);
    }

    private record Mark(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
    }
}
