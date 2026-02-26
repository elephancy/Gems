package com.feel.gems.power.gem.summoner;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsBalanceConfig;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;


/**
 * Per-player Summoner loadouts. Server-authoritative, persisted in the player's
 * Gems persistent NBT and sanitized against the current balance config.
 */
public final class SummonerLoadouts {
    public static final int SLOT_COUNT = 5;
    private static final String KEY_LOADOUT = "summonerLoadout";

    private SummonerLoadouts() {
    }

    public record Entry(String entityId, int count) {
    }

    public record Loadout(
            List<Entry> slot1,
            List<Entry> slot2,
            List<Entry> slot3,
            List<Entry> slot4,
            List<Entry> slot5
    ) {
        public List<Entry> slot(int index) {
            return switch (index) {
                case 1 -> slot1;
                case 2 -> slot2;
                case 3 -> slot3;
                case 4 -> slot4;
                case 5 -> slot5;
                default -> Collections.emptyList();
            };
        }
    }

    public static Loadout resolve(ServerPlayer player, GemsBalance.Summoner cfg) {
        Loadout stored = load(player);
        if (stored == null) {
            return fromConfig(cfg);
        }
        return sanitize(stored, cfg);
    }

    public static Loadout sanitize(Loadout loadout, GemsBalance.Summoner cfg) {
        Objects.requireNonNull(loadout, "loadout");
        return new Loadout(
                sanitizeSlot(loadout.slot1(), cfg),
                sanitizeSlot(loadout.slot2(), cfg),
                sanitizeSlot(loadout.slot3(), cfg),
                sanitizeSlot(loadout.slot4(), cfg),
                sanitizeSlot(loadout.slot5(), cfg)
        );
    }

    public static Loadout fromConfig(GemsBalance.Summoner cfg) {
        return new Loadout(
                copy(cfg.slot1()),
                copy(cfg.slot2()),
                copy(cfg.slot3()),
                copy(cfg.slot4()),
                copy(cfg.slot5())
        );
    }

    public static Loadout fromEntries(
            List<Entry> slot1,
            List<Entry> slot2,
            List<Entry> slot3,
            List<Entry> slot4,
            List<Entry> slot5
    ) {
        return new Loadout(
                normalize(slot1),
                normalize(slot2),
                normalize(slot3),
                normalize(slot4),
                normalize(slot5)
        );
    }

    public static void save(ServerPlayer player, Loadout loadout) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag out = new CompoundTag();
        out.put("slot1", toNbtList(loadout.slot1()));
        out.put("slot2", toNbtList(loadout.slot2()));
        out.put("slot3", toNbtList(loadout.slot3()));
        out.put("slot4", toNbtList(loadout.slot4()));
        out.put("slot5", toNbtList(loadout.slot5()));
        root.put(KEY_LOADOUT, out);
    }

    public static Loadout load(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!root.contains(KEY_LOADOUT)) {
            return null;
        }
        CompoundTag in = root.getCompound(KEY_LOADOUT).orElse(null);
        if (in == null) {
            return null;
        }
        return new Loadout(
                fromNbtList(in.getList("slot1").orElse(null)),
                fromNbtList(in.getList("slot2").orElse(null)),
                fromNbtList(in.getList("slot3").orElse(null)),
                fromNbtList(in.getList("slot4").orElse(null)),
                fromNbtList(in.getList("slot5").orElse(null))
        );
    }

    private static List<Entry> sanitizeSlot(List<Entry> raw, GemsBalance.Summoner cfg) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>();
        for (Entry entry : raw) {
            if (entry == null || entry.entityId == null || entry.entityId.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(entry.entityId);
            if (id == null) {
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type == EntityTypes.WITHER || type == EntityTypes.ENDER_DRAGON) {
                continue;
            }
            int maxPerSlot = Math.max(0, cfg.maxPoints());
            int count = clamp(entry.count, 0, maxPerSlot);
            if (count <= 0) {
                continue;
            }
            out.add(new Entry(id.toString(), count));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<Entry> normalize(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>(entries.size());
        for (Entry e : entries) {
            if (e != null) {
                out.add(new Entry(e.entityId, e.count));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static List<Entry> copy(List<GemsBalanceConfig.Summoner.SummonSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>(specs.size());
        for (GemsBalanceConfig.Summoner.SummonSpec spec : specs) {
            if (spec == null || spec.entityId == null) {
                continue;
            }
            out.add(new Entry(spec.entityId, spec.count));
        }
        return Collections.unmodifiableList(out);
    }

    private static ListTag toNbtList(List<Entry> entries) {
        ListTag list = new ListTag();
        if (entries == null) {
            return list;
        }
        for (Entry entry : entries) {
            if (entry == null || entry.entityId == null || entry.entityId.isBlank() || entry.count <= 0) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entry.entityId);
            tag.putInt("count", entry.count);
            list.add(tag);
        }
        return list;
    }

    private static List<Entry> fromNbtList(ListTag list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Tag element = list.get(i);
            if (!(element instanceof CompoundTag tag)) {
                continue;
            }
            String id = tag.getStringOr("id", "");
            int count = tag.getIntOr("count", 0);
            if (id == null || id.isBlank() || count <= 0) {
                continue;
            }
            out.add(new Entry(id, count));
        }
        return Collections.unmodifiableList(out);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
