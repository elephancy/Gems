package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;




public final class LegendaryPlayerTracker {
    private static final String STATE_KEY = "gems_legendary_player_tracker";
    private static final SavedDataType<State> STATE_TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_KEY), State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_DIM = "dim";
    private static final String KEY_POS = "pos";
    private static final String KEY_RESPAWN_DIM = "respawn_dim";
    private static final String KEY_RESPAWN_POS = "respawn_pos";
    private static final String KEY_LAST_SEEN = "last_seen";

    private LegendaryPlayerTracker() {
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            state.players.put(player.getUUID(), Snapshot.fromPlayer(server, player, now));
        }
        state.setDirty();
    }

    public static Snapshot snapshot(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return Snapshot.fromPlayer(server, online, GemsTime.now(server));
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
            Identifier dimension,
            BlockPos pos,
            Identifier respawnDimension,
            BlockPos respawnPos,
            long lastSeenTick
    ) {
        static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.AUTHLIB_CODEC.fieldOf(KEY_UUID).forGetter(Snapshot::uuid),
                Codec.STRING.fieldOf(KEY_NAME).forGetter(Snapshot::name),
                Identifier.CODEC.fieldOf(KEY_DIM).forGetter(Snapshot::dimension),
                BlockPos.CODEC.fieldOf(KEY_POS).forGetter(Snapshot::pos),
                Identifier.CODEC.fieldOf(KEY_RESPAWN_DIM).forGetter(Snapshot::respawnDimension),
                BlockPos.CODEC.fieldOf(KEY_RESPAWN_POS).forGetter(Snapshot::respawnPos),
                Codec.LONG.fieldOf(KEY_LAST_SEEN).forGetter(Snapshot::lastSeenTick)
        ).apply(instance, Snapshot::new));

        static Snapshot fromPlayer(MinecraftServer server, ServerPlayer player, long now) {
            Identifier dim = player.level().dimension().identifier();
            BlockPos pos = player.blockPosition();

            var respawn = player.getRespawnConfig();
            var spawnPoint = respawn == null ? null : respawn.respawnData();
            ResourceKey<Level> respawnKey = spawnPoint == null ? null : spawnPoint.dimension();
            BlockPos respawnPos = spawnPoint == null ? null : spawnPoint.pos();
            if (respawnKey == null || respawnPos == null) {
                ServerLevel overworld = server.overworld();
                respawnKey = Level.OVERWORLD;
                respawnPos = overworld.getRespawnData().pos();
            }

            return new Snapshot(
                    player.getUUID(),
                    player.getGameProfile().name(),
                    dim,
                    pos,
                    respawnKey.identifier(),
                    respawnPos,
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
}
