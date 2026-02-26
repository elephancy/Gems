package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;




public final class TrackerCompassItem extends CompassItem implements LegendaryItem {
    private static final String KEY_TARGET = "legendaryTrackTarget";
    private static final String KEY_TARGET_NAME = "legendaryTrackTargetName";
    private static final String KEY_TARGET_DIM = "legendaryTrackDim";
    private static final String KEY_TARGET_POS = "legendaryTrackPos";
    private static final String KEY_TARGET_RESPAWN_DIM = "legendaryTrackRespawnDim";
    private static final String KEY_TARGET_RESPAWN_POS = "legendaryTrackRespawnPos";
    private static final String KEY_TARGET_LAST_SEEN = "legendaryTrackLastSeen";
    private static final String KEY_LAST_UPDATE = "legendaryTrackLastUpdate";

    public TrackerCompassItem(Properties settings) {
        super(settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        // `CompassItem` swaps the display name to "Lodestone Compass" when `LODESTONE_TRACKER` is set.
        // Tracker Compass uses that component for pointing, but should always keep its own name.
        return Component.translatable("item.gems.tracker_compass");
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "tracker_compass").toString();
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (!world.isClientSide() && user instanceof ServerPlayer player) {
            openSelectionScreen(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        UUID target = targetUuid(stack);
        if (target == null) {
            return;
        }
        long now = GemsTime.now(player);
        int refresh = Math.max(1, GemsBalance.v().legendary().trackerRefreshTicks());
        long last = lastUpdate(stack);
        if (now - last < refresh) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (SpySystem.hidesTracking(server, target)) {
            clearTarget(stack);
            player.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return;
        }
        updateTrackingData(stack, server, target, now);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.tracker_compass.desc"));
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            tooltip.accept(Component.translatable("gems.item.tracker_compass.tooltip.target_none"));
            return;
        }
        CompoundTag nbt = data.copyTag();
        if (nbt.getString(KEY_TARGET_NAME).isEmpty()) {
            tooltip.accept(Component.translatable("gems.item.tracker_compass.tooltip.target_none"));
            return;
        }
        String name = nbt.getStringOr(KEY_TARGET_NAME, "");
        tooltip.accept(Component.translatable("gems.item.tracker_compass.tooltip.target", name));
        BlockPos pos = readBlockPos(nbt, KEY_TARGET_POS);
        if (pos != null) {
            String dim = nbt.getStringOr(KEY_TARGET_DIM, "");
            tooltip.accept(Component.translatable(
                    "gems.item.tracker_compass.tooltip.last_seen",
                    String.valueOf(pos.getX()),
                    String.valueOf(pos.getY()),
                    String.valueOf(pos.getZ()),
                    dim
            ));
        }
        BlockPos respawn = readBlockPos(nbt, KEY_TARGET_RESPAWN_POS);
        if (respawn != null) {
            String dim = nbt.getStringOr(KEY_TARGET_RESPAWN_DIM, "");
            tooltip.accept(Component.translatable(
                    "gems.item.tracker_compass.tooltip.respawn",
                    String.valueOf(respawn.getX()),
                    String.valueOf(respawn.getY()),
                    String.valueOf(respawn.getZ()),
                    dim
            ));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static void setTarget(ServerPlayer player, String name) {
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.hold_to_set"), true);
            return;
        }
        if (name == null || name.isBlank()) {
            clearTarget(stack);
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.target_cleared"), true);
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        LegendaryPlayerTracker.Snapshot snapshot = null;
        for (LegendaryPlayerTracker.Snapshot entry : LegendaryPlayerTracker.knownSnapshots(server)) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name)) {
                snapshot = entry;
                break;
            }
        }
        if (snapshot == null) {
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.unknown_player", name), true);
            return;
        }
        if (SpySystem.hidesTracking(server, snapshot.uuid())) {
            player.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return;
        }
        applyTarget(player, stack, snapshot.uuid(), snapshot.name());
    }

    public static void setTarget(ServerPlayer player, UUID uuid) {
        if (uuid == null) {
            clearTarget(player);
            return;
        }
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.hold_to_set"), true);
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, uuid);
        if (snapshot == null) {
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.unknown_player_track"), true);
            return;
        }
        if (SpySystem.hidesTracking(server, uuid)) {
            player.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return;
        }
        applyTarget(player, stack, uuid, snapshot.name());
    }

    public static void clearTarget(ServerPlayer player) {
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.hold_to_clear"), true);
            return;
        }
        clearTarget(stack);
        player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.target_cleared"), true);
    }

    public static void openSelectionScreen(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        List<LegendaryPlayerTracker.Snapshot> profiles = new ArrayList<>(LegendaryPlayerTracker.knownSnapshots(server));
        profiles.sort(java.util.Comparator.comparing(p -> p.name().toLowerCase(java.util.Locale.ROOT)));
        List<TrackerCompassScreenPayload.Entry> entries = new ArrayList<>(profiles.size());
        for (LegendaryPlayerTracker.Snapshot profile : profiles) {
            if (SpySystem.hidesTracking(server, profile.uuid())) {
                continue;
            }
            boolean online = server.getPlayerList().getPlayer(profile.uuid()) != null;
            entries.add(new TrackerCompassScreenPayload.Entry(profile.uuid(), profile.name(), online));
        }
        ServerPlayNetworking.send(player, new TrackerCompassScreenPayload(entries));
    }

    private static void applyTarget(ServerPlayer player, ItemStack stack, UUID uuid, String display) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.put(KEY_TARGET, GemsNbt.fromUuid(uuid));
            nbt.putString(KEY_TARGET_NAME, display);
            nbt.putLong(KEY_LAST_UPDATE, 0L);
        });
        updateTrackingData(stack, server, uuid, GemsTime.now(server));
        player.sendSystemMessage(Component.translatable("gems.item.tracker_compass.now_tracking", display), true);
        sendTargetInfo(player, server, uuid);
    }

    private static void sendTargetInfo(ServerPlayer player, MinecraftServer server, UUID target) {
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, target);
        if (snapshot == null) {
            return;
        }
        BlockPos pos = snapshot.pos();
        BlockPos respawn = snapshot.respawnPos();
        player.sendSystemMessage(Component.translatable(
                "gems.item.tracker_compass.current_pos",
                String.valueOf(pos.getX()),
                String.valueOf(pos.getY()),
                String.valueOf(pos.getZ()),
                snapshot.dimension().toString()
        ), true);
        player.sendSystemMessage(Component.translatable(
                "gems.item.tracker_compass.respawn_pos",
                String.valueOf(respawn.getX()),
                String.valueOf(respawn.getY()),
                String.valueOf(respawn.getZ()),
                snapshot.respawnDimension().toString()
        ), true);
    }

    private static void updateTrackingData(ItemStack stack, MinecraftServer server, UUID target, long now) {
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, target);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.putLong(KEY_LAST_UPDATE, now);
            if (snapshot == null) {
                nbt.putString(KEY_TARGET_DIM, "");
                nbt.remove(KEY_TARGET_POS);
                nbt.putString(KEY_TARGET_RESPAWN_DIM, "");
                nbt.remove(KEY_TARGET_RESPAWN_POS);
                nbt.remove(KEY_TARGET_LAST_SEEN);
                return;
            }
            nbt.putString(KEY_TARGET_NAME, snapshot.name());
            nbt.putString(KEY_TARGET_DIM, snapshot.dimension().toString());
            writeBlockPos(nbt, KEY_TARGET_POS, snapshot.pos());
            nbt.putString(KEY_TARGET_RESPAWN_DIM, snapshot.respawnDimension().toString());
            writeBlockPos(nbt, KEY_TARGET_RESPAWN_POS, snapshot.respawnPos());
            nbt.putLong(KEY_TARGET_LAST_SEEN, snapshot.lastSeenTick());
        });

        if (snapshot != null) {
            GlobalPos global = GlobalPos.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, snapshot.dimension()), snapshot.pos());
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(global), true));
        } else {
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
        }
    }

    private static UUID targetUuid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag nbt = data.copyTag();
        if (!nbt.contains(KEY_TARGET)) {
            return null;
        }
        return GemsNbt.toUuid(nbt.get(KEY_TARGET));
    }

    private static long lastUpdate(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        return data.copyTag().getLongOr(KEY_LAST_UPDATE, 0L);
    }

    private static void clearTarget(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove(KEY_TARGET);
            nbt.remove(KEY_TARGET_NAME);
            nbt.remove(KEY_TARGET_DIM);
            nbt.remove(KEY_TARGET_POS);
            nbt.remove(KEY_TARGET_RESPAWN_DIM);
            nbt.remove(KEY_TARGET_RESPAWN_POS);
            nbt.remove(KEY_TARGET_LAST_SEEN);
            nbt.remove(KEY_LAST_UPDATE);
        });
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
    }

    private static ItemStack findHeldCompass(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TrackerCompassItem)) {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof TrackerCompassItem)) {
            return ItemStack.EMPTY;
        }
        return stack;
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
}
