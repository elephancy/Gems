package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;




public final class LegendaryCrafting {
    private static final String STATE_KEY = "gems_legendary_crafts";
    private static final SavedDataType<State> STATE_TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_KEY), State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_CRAFTED = "crafted";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PENDING = "pending";

    private static final String KEY_ID = "id";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_START = "start";
    private static final String KEY_FINISH = "finish";
    private static final String KEY_DIM = "dim";
    private static final String KEY_POS = "pos";
    private static final String KEY_ITEMS = "items";

    private static final String KEY_BLOCKED_NOTIFY = "gemsLegendaryCraftBlockedUntil";

    private static final Map<String, net.minecraft.server.level.ServerBossEvent> ACTIVE_BARS = new HashMap<>();

    private LegendaryCrafting() {
    }

    public static boolean canStartCraft(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof LegendaryItem legendary)) {
            return true;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        String id = legendary.legendaryId();
        State state = state(server);
        if (!state.isAvailable(id)) {
            notifyBlocked(player, id);
            return false;
        }
        return true;
    }

    public static void onCrafted(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof LegendaryItem legendary)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        String id = legendary.legendaryId();
        State state = state(server);
        if (!state.isAvailable(id)) {
            notifyBlocked(player, id);
            removeCraftResult(player, stack.getItem());
            return;
        }

        long now = GemsTime.now(server);
        int craftTicks = GemsBalance.v().legendary().craftTicks();
        long finish = now + craftTicks;
        CraftLocation location = resolveCraftLocation(player);
        state.start(id, player.getUUID(), now, finish, location);
        state.setDirty();

        removeCraftResult(player, stack.getItem());
        announceCraftStart(server, player, stack.getItem(), craftTicks);
        player.sendSystemMessage(Component.translatable("gems.legendary.craft_started", seconds(craftTicks)), true);
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        updateBossBars(server, state, now);
        if (state.active.isEmpty()) {
            return;
        }
        List<ActiveCraft> finished = new ArrayList<>();
        for (ActiveCraft craft : state.active.values()) {
            if (craft.finishTick <= now) {
                finished.add(craft);
            }
        }
        if (finished.isEmpty()) {
            return;
        }
        for (ActiveCraft craft : finished) {
            state.active.remove(craft.key);
            state.addCrafted(craft.id);
            dropAtLocation(server, craft);
            removeBossBar(craft.key);
        }
        state.setDirty();
    }

    public static void deliverPending(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        State state = state(server);
        List<String> pending = state.pending.remove(player.getUUID());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (String id : pending) {
            deliver(player, id);
        }
        state.setDirty();
    }

    private static void deliver(ServerPlayer player, String id) {
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        applyDefaultEnchants(player.level().getServer(), stack);
        if (!player.addItem(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.sendSystemMessage(Component.translatable("gems.legendary.craft_complete", stack.getHoverName().getString()), false);
    }

    private static void dropAtLocation(MinecraftServer server, ActiveCraft craft) {
        Identifier itemId = Identifier.tryParse(craft.id);
        if (itemId == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null) {
            return;
        }
        net.minecraft.resources.ResourceKey<Level> worldKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, craft.dimension);
        net.minecraft.server.level.ServerLevel world = server.getLevel(worldKey);
        if (world == null) {
            world = server.overworld();
        }
        ItemStack stack = new ItemStack(item);
        applyDefaultEnchants(server, stack);
        BlockPos pos = craft.pos;
        world.getChunk(pos);
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, stack);
        world.addFreshEntity(entity);
        String itemName = stack.getHoverName().getString();
        Component text = Component.translatable("gems.legendary.craft_dropped", itemName,
                pos.getX(), pos.getY(), pos.getZ(), craft.dimension.toString());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(text, false);
        }
    }

    private static void removeCraftResult(ServerPlayer player, Item item) {
        if (player.containerMenu != null) {
            ItemStack cursor = player.containerMenu.getCarried();
            if (cursor.is(item)) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                return;
            }
        }
        var mainStacks = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.is(item)) {
                mainStacks.set(i, ItemStack.EMPTY);
                return;
            }
        }
        if (player.getOffhandItem().is(item)) {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            return;
        }
        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (var slot : armorSlots) {
            if (player.getItemBySlot(slot).is(item)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static void applyDefaultEnchants(MinecraftServer server, ItemStack stack) {
        if (server == null) {
            return;
        }
        if (stack.is(com.feel.gems.item.ModItems.EARTHSPLITTER_PICK)) {
            Holder<Enchantment> silk = resolveEnchantment(server, Enchantments.SILK_TOUCH);
            if (silk != null) {
                EnchantmentHelper.updateEnchantments(stack, builder -> builder.set(silk, 1));
            }
        }
    }

    private static Holder<Enchantment> resolveEnchantment(MinecraftServer server, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return server.registryAccess().get(key).orElse(null);
    }

    private static void announceCraftStart(MinecraftServer server, ServerPlayer player, Item item, int craftTicks) {
        CraftLocation location = resolveCraftLocation(player);
        BlockPos pos = location.pos;
        String dim = location.dimension.toString();
        String itemName = new ItemStack(item).getHoverName().getString();
        Component text = Component.translatable("gems.legendary.craft_announce",
                player.getName().getString(), itemName,
                pos.getX(), pos.getY(), pos.getZ(), dim);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            other.sendSystemMessage(text, false);
        }
    }

    private static void notifyBlocked(ServerPlayer player, String id) {
        long now = GemsTime.now(player);
        CompoundTag data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        long next = data.getLongOr(KEY_BLOCKED_NOTIFY, 0L);
        if (next > now) {
            return;
        }
        data.putLong(KEY_BLOCKED_NOTIFY, now + 40);
        player.sendSystemMessage(Component.translatable("gems.legendary.crafting_limit"), true);
    }

    private static int seconds(int ticks) {
        return Math.max(0, ticks / 20);
    }

    private static State state(MinecraftServer server) {
        SavedDataStorage mgr = server.overworld().getDataStorage();
        return mgr.computeIfAbsent(STATE_TYPE);
    }

    private record ActiveCraft(String key, String id, UUID owner, long startTick, long finishTick, Identifier dimension, BlockPos pos) {
        static final Codec<ActiveCraft> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf(KEY_ID).forGetter(ActiveCraft::id),
                UUIDUtil.AUTHLIB_CODEC.fieldOf(KEY_OWNER).forGetter(ActiveCraft::owner),
                Codec.LONG.fieldOf(KEY_START).forGetter(ActiveCraft::startTick),
                Codec.LONG.fieldOf(KEY_FINISH).forGetter(ActiveCraft::finishTick),
                Identifier.CODEC.fieldOf(KEY_DIM).forGetter(ActiveCraft::dimension),
                BlockPos.CODEC.fieldOf(KEY_POS).forGetter(ActiveCraft::pos)
        ).apply(instance, (id, owner, start, finish, dim, pos) -> new ActiveCraft(craftKey(id, owner, start), id, owner, start, finish, dim, pos)));
    }

    private static final class State extends SavedData {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf(KEY_CRAFTED, Map.of()).forGetter(state -> state.crafted),
                Codec.unboundedMap(Codec.STRING, ActiveCraft.CODEC).optionalFieldOf(KEY_ACTIVE, Map.of()).forGetter(state -> state.active),
                Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.STRING.listOf()).optionalFieldOf(KEY_PENDING, Map.of()).forGetter(state -> state.pending)
        ).apply(instance, State::new));

        private final Map<String, Integer> crafted;
        private final Map<String, ActiveCraft> active;
        private final Map<UUID, List<String>> pending;

        State() {
            this(Map.of(), Map.of(), Map.of());
        }

        State(Map<String, Integer> crafted, Map<String, ActiveCraft> active, Map<UUID, List<String>> pending) {
            this.crafted = new HashMap<>(crafted == null ? Map.of() : crafted);
            this.active = new HashMap<>(active == null ? Map.of() : active);
            this.pending = new HashMap<>();
            if (pending != null) {
                for (var entry : pending.entrySet()) {
                    List<String> items = entry.getValue() == null ? List.of() : entry.getValue();
                    this.pending.put(entry.getKey(), new ArrayList<>(items));
                }
            }
        }

        boolean isAvailable(String id) {
            int maxActive = GemsBalance.v().legendary().craftMaxActivePerItem();
            if (maxActive > 0 && activeCount(id) >= maxActive) {
                return false;
            }
            int maxPerItem = GemsBalance.v().legendary().craftMaxPerItem();
            if (maxPerItem <= 0) {
                return true;
            }
            return craftedCount(id) < maxPerItem;
        }

        void start(String id, UUID owner, long startTick, long finishTick, CraftLocation location) {
            String key = craftKey(id, owner, startTick);
            active.put(key, new ActiveCraft(key, id, owner, startTick, finishTick, location.dimension, location.pos));
        }

        void addCrafted(String id) {
            if (id == null || id.isEmpty()) {
                return;
            }
            crafted.put(id, crafted.getOrDefault(id, 0) + 1);
        }

        int craftedCount(String id) {
            if (id == null || id.isEmpty()) {
                return 0;
            }
            return crafted.getOrDefault(id, 0);
        }

        int activeCount(String id) {
            if (id == null || id.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ActiveCraft craft : active.values()) {
                if (id.equals(craft.id)) {
                    count++;
                }
            }
            return count;
        }

        void addPending(UUID owner, String id) {
            pending.computeIfAbsent(owner, key -> new ArrayList<>()).add(id);
        }
    }

    private record CraftLocation(Identifier dimension, BlockPos pos) {
    }

    private static CraftLocation resolveCraftLocation(ServerPlayer player) {
        if (player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu handler) {
            var context = ((com.feel.gems.mixin.accessor.CraftingScreenHandlerAccessor) handler).gems$getContext();
            if (context != null) {
                final BlockPos[] pos = new BlockPos[1];
                final Identifier[] dim = new Identifier[1];
                context.execute((world, blockPos) -> {
                    pos[0] = blockPos.immutable();
                    dim[0] = world.dimension().identifier();
                });
                if (pos[0] != null && dim[0] != null) {
                    return new CraftLocation(dim[0], pos[0]);
                }
            }
        }
        return new CraftLocation(player.level().dimension().identifier(), player.blockPosition());
    }

    private static void updateBossBars(MinecraftServer server, State state, long now) {
        if (state.active.isEmpty()) {
            clearBossBars();
            return;
        }
        for (ActiveCraft craft : state.active.values()) {
            net.minecraft.server.level.ServerBossEvent bar = ACTIVE_BARS.computeIfAbsent(craft.key, key -> new net.minecraft.server.level.ServerBossEvent(
                    java.util.UUID.randomUUID(),
                    Component.translatable("gems.legendary.craft_bossbar_title"),
                    net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
            ));
            long total = Math.max(1L, craft.finishTick - craft.startTick);
            float progress = Math.min(1.0F, Math.max(0.0F, (now - craft.startTick) / (float) total));
            bar.setProgress(progress);

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(craft.id));
            String itemName = item != null ? new ItemStack(item).getHoverName().getString() : craft.id;
            bar.setName(Component.translatable("gems.legendary.craft_bossbar", itemName,
                    craft.pos.getX(), craft.pos.getY(), craft.pos.getZ(), craft.dimension.toString()));

            bar.removeAllPlayers();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                bar.addPlayer(player);
            }
        }
        List<String> remove = new ArrayList<>();
        for (String key : ACTIVE_BARS.keySet()) {
            if (!state.active.containsKey(key)) {
                remove.add(key);
            }
        }
        for (String key : remove) {
            removeBossBar(key);
        }
    }

    private static void clearBossBars() {
        for (String key : new ArrayList<>(ACTIVE_BARS.keySet())) {
            removeBossBar(key);
        }
    }

    private static void removeBossBar(String key) {
        net.minecraft.server.level.ServerBossEvent bar = ACTIVE_BARS.remove(key);
        if (bar == null) {
            return;
        }
        bar.removeAllPlayers();
    }

    private static String craftKey(String id, UUID owner, long startTick) {
        return id + ":" + owner + ":" + startTick;
    }
}
