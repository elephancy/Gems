package com.feel.gems.item;

import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;
import com.feel.gems.util.GemsNbt;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.AABB;




/**
 * Ownership tagging and validation for gem items.
 */
public final class GemOwnership {
    private static final String KEY_OWNER = "GemOwner";
    private static final String KEY_EPOCH = "GemOwnerEpoch";
    private static final String KEY_SKIP_HEART_DROP = "gemsSkipHeartDropOnce";

    private static final int PLAYER_PURGE_BUDGET = 8;
    private static final int CHUNK_PURGE_BUDGET = 6;
    private static final long PURGE_WINDOW_TICKS = 20L * 60L * 10L;
    private static final Deque<PurgeTask> PURGE_QUEUE = new ArrayDeque<>();
    private static final Set<UUID> PURGE_IN_FLIGHT = new HashSet<>();

    private static final String KEY_OFFLINE_PENALTIES = "gems_offline_gem_penalties";
    private static final String KEY_OWNER_EPOCHS = "gems_owner_epochs";
    private static final SavedDataType<OfflinePenaltyState> OFFLINE_PENALTY_STATE_TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(KEY_OFFLINE_PENALTIES), OfflinePenaltyState::new, OfflinePenaltyState.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    private static final SavedDataType<OwnerEpochState> OWNER_EPOCH_STATE_TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(KEY_OWNER_EPOCHS), OwnerEpochState::new, OwnerEpochState.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private GemOwnership() {
    }

    public static void tagOwned(ItemStack stack, UUID owner, int epoch) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            GemsNbt.putUuid(nbt, KEY_OWNER, owner);
            nbt.putInt(KEY_EPOCH, epoch);
        });
    }

    public static void tagOwned(ItemStack stack, ServerPlayer owner) {
        if (!(stack.getItem() instanceof GemItem gem)) {
            return;
        }
        int epoch = currentEpochFor(owner, gem.gemId());
        tagOwned(stack, owner.getUUID(), epoch);
    }

    public static void ensureOwner(ItemStack stack, ServerPlayer owner) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            if (GemsNbt.containsUuid(nbt, KEY_OWNER)) {
                return;
            }
            GemsNbt.putUuid(nbt, KEY_OWNER, owner.getUUID());
            GemItem gem = (GemItem) stack.getItem();
            nbt.putInt(KEY_EPOCH, currentEpochFor(owner, gem.gemId()));
        });
    }

    public static UUID ownerUuid(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag nbt = custom.copyTag();
        return GemsNbt.getUuid(nbt, KEY_OWNER);
    }

    public static boolean isInvalidForEpoch(MinecraftServer server, ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem gem)) {
            return false;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        CompoundTag tag = custom.copyTag();
        UUID owner = GemsNbt.getUuid(tag, KEY_OWNER);
        if (owner == null) {
            return false;
        }
        int storedEpoch = tag.getIntOr(KEY_EPOCH, 0);
        int current = resolveEpoch(server, owner, gem.gemId());
        if (current < 0) {
            return false;
        }
        return storedEpoch < current;
    }

    public static boolean purgeIfInvalid(MinecraftServer server, ItemStack stack) {
        if (isInvalidForEpoch(server, stack)) {
            stack.setCount(0);
            return true;
        }
        return false;
    }

    public static void purgeInventory(MinecraftServer server, Container inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (purgeIfInvalid(server, stack)) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static void purgePlayerInventories(MinecraftServer server, Player player) {
        purgeInventory(server, player.getInventory());
        if (player instanceof ServerPlayer sp) {
            purgeInventory(server, sp.getEnderChestInventory());
        }
    }

    public static void purgePendingPlayerInventories(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgePlayerInventories(server, player);
        }
    }

    public static void purgeInventoryIfPending(MinecraftServer server, Container inv) {
        if (inv == null) {
            return;
        }
        purgeInventory(server, inv);
    }

    public static boolean isOwnedBy(ItemStack stack, UUID owner) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || !GemsNbt.containsUuid(custom.copyTag(), KEY_OWNER)) {
            return false;
        }
        UUID stored = GemsNbt.getUuid(custom.copyTag(), KEY_OWNER);
        return stored != null && stored.equals(owner);
    }

    public static int removeOwnedGemFromInventory(ServerPlayer holder, UUID owner, GemId gemId) {
        if (holder == null || owner == null || gemId == null) {
            return 0;
        }
        int removed = 0;
        var mainStacks = holder.getInventory().getNonEquipmentItems();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (isOwnedGem(stack, owner, gemId)) {
                mainStacks.set(i, ItemStack.EMPTY);
                removed++;
            }
        }
        ItemStack offhand = holder.getOffhandItem();
        if (isOwnedGem(offhand, owner, gemId)) {
            holder.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            removed++;
        }
        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (var slot : armorSlots) {
            ItemStack stack = holder.getItemBySlot(slot);
            if (isOwnedGem(stack, owner, gemId)) {
                holder.setItemSlot(slot, ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    public static int removeOwnedGemFromEnderChest(ServerPlayer holder, UUID owner, GemId gemId) {
        if (holder == null || owner == null || gemId == null) {
            return 0;
        }
        int removed = 0;
        Container inv = holder.getEnderChestInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isOwnedGem(stack, owner, gemId)) {
                inv.setItem(i, ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    private static boolean isOwnedGem(ItemStack stack, UUID owner, GemId gemId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof GemItem gem) || gem.gemId() != gemId) {
            return false;
        }
        return isOwnedBy(stack, owner);
    }

    public static void applyOwnerPenalty(ServerPlayer owner) {
        GemPlayerState.initIfNeeded(owner);
        GemPlayerState.addMaxHearts(owner, -2);
        GemPlayerState.applyMaxHearts(owner);
        ((com.feel.gems.state.GemsPersistentDataHolder) owner).gems$getPersistentData().putBoolean(KEY_SKIP_HEART_DROP, true);
        owner.hurtServer(owner.level(), owner.damageSources().magic(), Float.MAX_VALUE);
        owner.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.item.gem.penalty_applied"), false);
    }

    public static boolean consumeSkipHeartDrop(ServerPlayer player) {
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        boolean skip = data.getBoolean(KEY_SKIP_HEART_DROP).orElse(false);
        if (skip) {
            data.remove(KEY_SKIP_HEART_DROP);
        }
        return skip;
    }

    public static void markSkipHeartDropOnce(ServerPlayer player) {
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_SKIP_HEART_DROP, true);
    }

    private static final class OfflinePenaltyState extends SavedData {
        private static final Codec<OfflinePenaltyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.UUIDUtil.CODEC_SET.optionalFieldOf(KEY_OFFLINE_PENALTIES, Set.of()).forGetter(state -> state.queued)
        ).apply(instance, OfflinePenaltyState::new));

        private final Set<UUID> queued;

        OfflinePenaltyState() {
            this(Set.of());
        }

        OfflinePenaltyState(Set<UUID> queued) {
            this.queued = new HashSet<>(queued == null ? Set.of() : queued);
        }

        void queue(UUID owner) {
            queued.add(owner);
        }

        boolean consume(UUID owner) {
            return queued.remove(owner);
        }
    }

    public static void requestDeferredPurge(MinecraftServer server, UUID owner) {
        if (server == null || PURGE_IN_FLIGHT.contains(owner)) {
            return;
        }
        PURGE_IN_FLIGHT.add(owner);
        PURGE_QUEUE.add(buildTask(server, owner));
    }

    public static void queueOfflinePenalty(MinecraftServer server, UUID owner) {
        if (server == null || owner == null) {
            return;
        }
        OfflinePenaltyState state = penalties(server);
        state.queue(owner);
        state.setDirty();
    }

    public static void consumeOfflinePenalty(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        OfflinePenaltyState state = penalties(server);
        if (state.consume(player.getUUID())) {
            applyOwnerPenalty(player);
            state.setDirty();
        }
    }

    private static OfflinePenaltyState penalties(MinecraftServer server) {
        SavedDataStorage mgr = server.overworld().getDataStorage();
        return mgr.computeIfAbsent(OFFLINE_PENALTY_STATE_TYPE);
    }

    private static OwnerEpochState epochs(MinecraftServer server) {
        SavedDataStorage mgr = server.overworld().getDataStorage();
        return mgr.computeIfAbsent(OWNER_EPOCH_STATE_TYPE);
    }

    public static void recordOwnerEpoch(MinecraftServer server, UUID owner, int epoch) {
        if (server == null || owner == null) {
            return;
        }
        OwnerEpochState state = epochs(server);
        state.setBaseEpoch(owner, epoch);
        state.setDirty();
    }

    public static void setGemEpochOverride(MinecraftServer server, UUID owner, GemId gemId, int epoch) {
        if (server == null || owner == null || gemId == null) {
            return;
        }
        OwnerEpochState state = epochs(server);
        state.setGemEpoch(owner, gemId, epoch);
        state.markPending(owner, server.overworld().getGameTime() + PURGE_WINDOW_TICKS);
        state.setDirty();
    }

    public static int getGemEpochOverride(MinecraftServer server, UUID owner, GemId gemId) {
        if (server == null || owner == null || gemId == null) {
            return -1;
        }
        return epochs(server).getGemEpoch(owner, gemId);
    }

    public static int nextGemEpoch(MinecraftServer server, UUID owner, GemId gemId, int baseEpoch) {
        if (server == null || owner == null || gemId == null) {
            return baseEpoch + 1;
        }
        int currentOverride = getGemEpochOverride(server, owner, gemId);
        int max = Math.max(baseEpoch, currentOverride);
        return max + 1;
    }

    public static int currentEpochFor(ServerPlayer owner, GemId gemId) {
        if (owner == null || gemId == null) {
            return 0;
        }
        MinecraftServer server = owner.level().getServer();
        int base = GemPlayerState.getGemEpoch(owner);
        if (server != null) {
            OwnerEpochState state = epochs(server);
            state.setBaseEpoch(owner.getUUID(), base);
            int gemEpoch = state.getGemEpoch(owner.getUUID(), gemId);
            state.setDirty();
            if (gemEpoch >= 0) {
                return Math.max(base, gemEpoch);
            }
        }
        return base;
    }

    private static int resolveEpoch(MinecraftServer server, UUID owner, GemId gemId) {
        if (server == null || owner == null || gemId == null) {
            return -1;
        }
        OwnerEpochState state = epochs(server);
        int base;
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            base = GemPlayerState.getGemEpoch(player);
            state.setBaseEpoch(owner, base);
        } else {
            base = state.getBaseEpoch(owner);
        }
        int gemEpoch = state.getGemEpoch(owner, gemId);
        state.setDirty();
        if (gemEpoch < 0) {
            return base;
        }
        if (base < 0) {
            return gemEpoch;
        }
        return Math.max(base, gemEpoch);
    }

    private static boolean hasPendingPurge(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        return epochs(server).hasPending(server.overworld().getGameTime());
    }

    public static void tickPurgeQueue(MinecraftServer server) {
        PurgeTask task = PURGE_QUEUE.peek();
        if (task == null) {
            return;
        }
        if (task.process(server)) {
            PURGE_QUEUE.poll();
            PURGE_IN_FLIGHT.remove(task.owner());
        }
    }

    private static PurgeTask buildTask(MinecraftServer server, UUID owner) {
        Deque<ServerPlayer> players = new ArrayDeque<>();
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
        if (ownerPlayer != null) {
            players.add(ownerPlayer);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ownerPlayer != null && player == ownerPlayer) {
                continue;
            }
            players.add(player);
        }

        int view = server.getPlayerList().getViewDistance();
        return new PurgeTask(owner, players, view);
    }

    private static final class PurgeTask {
        private final UUID owner;
        private final Deque<ServerPlayer> players;
        private final int viewDistanceChunks;
        private java.util.Iterator<ItemEntity> scanningItems;
        private int scanBudgetRemaining;

        PurgeTask(UUID owner, Deque<ServerPlayer> players, int viewDistanceChunks) {
            this.owner = owner;
            this.players = players;
            this.viewDistanceChunks = viewDistanceChunks;
        }

        UUID owner() {
            return owner;
        }

        boolean process(MinecraftServer server) {
            int playerBudget = PLAYER_PURGE_BUDGET;
            while (playerBudget-- > 0 && !players.isEmpty()) {
                purgePlayerInventories(server, players.poll());
            }

            if (scanningItems == null) {
                while (!players.isEmpty() && scanningItems == null) {
                    ServerPlayer next = players.poll();
                    if (next == null || next.isRemoved() || next.level().getServer() != server) {
                        continue;
                    }
                    scanBudgetRemaining = CHUNK_PURGE_BUDGET * 64;
                    scanningItems = findNearbyItemEntities(next);
                }
            }

            if (scanningItems != null) {
                while (scanBudgetRemaining-- > 0 && scanningItems.hasNext()) {
                    ItemEntity itemEntity = scanningItems.next();
                    if (itemEntity == null) {
                        continue;
                    }
                    ItemStack stack = itemEntity.getItem();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (isOwnedBy(stack, owner) && isInvalidForEpoch(server, stack)) {
                        itemEntity.discard();
                    }
                }
                if (!scanningItems.hasNext()) {
                    scanningItems = null;
                }
            }

            return players.isEmpty() && scanningItems == null;
        }

        private java.util.Iterator<ItemEntity> findNearbyItemEntities(ServerPlayer aroundPlayer) {
            ServerLevel world = aroundPlayer.level();
            if (world == null) {
                return java.util.List.<ItemEntity>of().iterator();
            }
            int radiusBlocks = Math.max(0, viewDistanceChunks) * 16;
            if (radiusBlocks <= 0) {
                return java.util.List.<ItemEntity>of().iterator();
            }
            int minY = world.getMinY();
            int maxY = world.getMaxY();
            AABB box = new AABB(
                    aroundPlayer.getX() - radiusBlocks, minY, aroundPlayer.getZ() - radiusBlocks,
                    aroundPlayer.getX() + radiusBlocks, maxY, aroundPlayer.getZ() + radiusBlocks
            );
            return world.getEntitiesOfClass(ItemEntity.class, box, e -> true).iterator();
        }
    }

    private static final class OwnerEpochState extends SavedData {
        static final Codec<OwnerEpochState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.INT)
                        .optionalFieldOf("epochs", java.util.Map.of())
                        .forGetter(state -> state.baseEpochs),
                Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.unboundedMap(Codec.STRING, Codec.INT))
                        .optionalFieldOf("gemEpochs", java.util.Map.of())
                        .forGetter(state -> state.gemEpochsRaw),
                Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.LONG)
                        .optionalFieldOf("purgeUntil", java.util.Map.of())
                        .forGetter(state -> state.purgeUntil)
        ).apply(instance, OwnerEpochState::new));

        private final java.util.Map<UUID, Integer> baseEpochs;
        private final java.util.Map<UUID, java.util.Map<String, Integer>> gemEpochsRaw;
        private final java.util.Map<UUID, Long> purgeUntil;

        OwnerEpochState() {
            this(java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
        }

        OwnerEpochState(java.util.Map<UUID, Integer> baseEpochs, java.util.Map<UUID, java.util.Map<String, Integer>> gemEpochsRaw,
                        java.util.Map<UUID, Long> purgeUntil) {
            this.baseEpochs = new java.util.HashMap<>(baseEpochs == null ? java.util.Map.of() : baseEpochs);
            this.gemEpochsRaw = new java.util.HashMap<>(gemEpochsRaw == null ? java.util.Map.of() : gemEpochsRaw);
            this.purgeUntil = new java.util.HashMap<>(purgeUntil == null ? java.util.Map.of() : purgeUntil);
        }

        int getBaseEpoch(UUID owner) {
            return baseEpochs.getOrDefault(owner, -1);
        }

        void setBaseEpoch(UUID owner, int epoch) {
            baseEpochs.put(owner, epoch);
        }

        int getGemEpoch(UUID owner, GemId gemId) {
            java.util.Map<String, Integer> map = gemEpochsRaw.get(owner);
            if (map == null) {
                return -1;
            }
            return map.getOrDefault(gemId.name(), -1);
        }

        void setGemEpoch(UUID owner, GemId gemId, int epoch) {
            java.util.Map<String, Integer> map = gemEpochsRaw.computeIfAbsent(owner, key -> new java.util.HashMap<>());
            map.put(gemId.name(), epoch);
        }

        void markPending(UUID owner, long untilTick) {
            if (owner == null) {
                return;
            }
            long current = purgeUntil.getOrDefault(owner, 0L);
            if (untilTick > current) {
                purgeUntil.put(owner, untilTick);
            }
        }

        boolean hasPending(long now) {
            if (purgeUntil.isEmpty()) {
                return false;
            }
            boolean any = false;
            java.util.Iterator<java.util.Map.Entry<UUID, Long>> it = purgeUntil.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<UUID, Long> entry = it.next();
                if (entry.getValue() == null || entry.getValue() <= now) {
                    it.remove();
                    continue;
                }
                any = true;
            }
            return any;
        }
    }
}
