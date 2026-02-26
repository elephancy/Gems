package com.feel.gems.legendary;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Persistent tracker for Gem Seer player gem data (including offline players).
 */
public final class GemSeerTracker {
    private static final String STATE_KEY = "gems_gem_seer_tracker";
    private static final SavedDataType<State> STATE_TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_KEY), State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_ACTIVE_GEM = "active_gem";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_OWNED = "owned";
    private static final String KEY_OWNED_COUNTS = "owned_counts";
    private static final String KEY_LAST_SEEN = "last_seen";

    private GemSeerTracker() {
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GemPlayerState.initIfNeeded(player);
            state.players.put(player.getUUID(), Snapshot.fromPlayer(player, now));
        }
        state.setDirty();
    }

    public static Snapshot snapshot(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            GemPlayerState.initIfNeeded(online);
            return Snapshot.fromPlayer(online, GemsTime.now(server));
        }
        return state(server).players.get(uuid);
    }

    public static List<Snapshot> knownSnapshots(MinecraftServer server) {
        return new ArrayList<>(state(server).players.values());
    }

    private static State state(MinecraftServer server) {
        SavedDataStorage mgr = server.overworld().getDataStorage();
        return mgr.computeIfAbsent(STATE_TYPE);
    }

    public record Snapshot(
            UUID uuid,
            String name,
            String activeGem,
            int energy,
            List<String> ownedGems,
            Map<String, Integer> ownedCounts,
            long lastSeenTick
    ) {
        static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.AUTHLIB_CODEC.fieldOf(KEY_UUID).forGetter(Snapshot::uuid),
                Codec.STRING.fieldOf(KEY_NAME).forGetter(Snapshot::name),
                Codec.STRING.fieldOf(KEY_ACTIVE_GEM).forGetter(Snapshot::activeGem),
                Codec.INT.fieldOf(KEY_ENERGY).forGetter(Snapshot::energy),
                Codec.STRING.listOf().optionalFieldOf(KEY_OWNED, List.of()).forGetter(Snapshot::ownedGems),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf(KEY_OWNED_COUNTS, Map.of()).forGetter(Snapshot::ownedCounts),
                Codec.LONG.fieldOf(KEY_LAST_SEEN).forGetter(Snapshot::lastSeenTick)
        ).apply(instance, Snapshot::new));

        static Snapshot fromPlayer(ServerPlayer player, long now) {
            GemId active = GemPlayerState.getActiveGem(player);
            int energy = GemPlayerState.getEnergy(player);
            List<String> owned = new ArrayList<>();
            Map<String, Integer> counts = new HashMap<>();
            for (GemId gem : GemPlayerState.getOwnedGems(player)) {
                owned.add(gem.name());
            }
            countGemItems(player, counts);
            return new Snapshot(
                    player.getUUID(),
                    player.getGameProfile().name(),
                    active.name(),
                    energy,
                    owned,
                    counts,
                    now
            );
        }
    }

    private static final class State extends SavedData {
        private final Map<UUID, Snapshot> players = new HashMap<>();

        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Snapshot.CODEC.listOf().optionalFieldOf(KEY_PLAYERS, List.of()).forGetter(state -> List.copyOf(state.players.values()))
        ).apply(instance, State::fromSnapshots));

        private static State fromSnapshots(List<Snapshot> snapshots) {
            State state = new State();
            if (snapshots != null) {
                for (Snapshot snapshot : snapshots) {
                    if (snapshot == null || snapshot.uuid() == null) {
                        continue;
                    }
                    state.players.put(snapshot.uuid(), snapshot);
                }
            }
            return state;
        }
    }

    private static void countGemItems(ServerPlayer player, Map<String, Integer> counts) {
        if (player == null || counts == null) {
            return;
        }
        for (var stack : player.getInventory().getNonEquipmentItems()) {
            addCount(counts, stack);
        }
        addCount(counts, player.getOffhandItem());
        addCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        addCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        addCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        addCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        var ender = player.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) {
            addCount(counts, ender.getItem(i));
        }
    }

    private static void addCount(Map<String, Integer> counts, net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof com.feel.gems.item.GemItem gem) {
            String key = gem.gemId().name();
            counts.merge(key, stack.getCount(), Integer::sum);
        }
    }
}
