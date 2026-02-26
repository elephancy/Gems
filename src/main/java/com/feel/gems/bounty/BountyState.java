package com.feel.gems.bounty;

import com.feel.gems.GemsMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

public final class BountyState extends SavedData {
    private static final String STATE_ID = GemsMod.MOD_ID + "_bounties";

    public record BountyEntry(String placerName, int hearts, int energy) {
        static final Codec<BountyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("placerName", "").forGetter(BountyEntry::placerName),
                Codec.INT.optionalFieldOf("hearts", 0).forGetter(BountyEntry::hearts),
                Codec.INT.optionalFieldOf("energy", 0).forGetter(BountyEntry::energy)
        ).apply(instance, BountyEntry::new));
    }

    public record TargetBounty(String targetName, Map<UUID, BountyEntry> placers) {
        static final Codec<TargetBounty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("targetName", "").forGetter(TargetBounty::targetName),
                Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, BountyEntry.CODEC)
                        .optionalFieldOf("placers", Map.of())
                        .forGetter(TargetBounty::placers)
        ).apply(instance, TargetBounty::new));
    }

    static final Codec<BountyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, TargetBounty.CODEC)
                    .optionalFieldOf("bounties", Map.of())
                    .forGetter(state -> state.bounties)
    ).apply(instance, BountyState::new));

    private static final SavedDataType<BountyState> TYPE =
            new SavedDataType<>(Identifier.withDefaultNamespace(STATE_ID), BountyState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private final Map<UUID, TargetBounty> bounties;

    public BountyState() {
        this(Map.of());
    }

    public BountyState(Map<UUID, TargetBounty> bounties) {
        this.bounties = new HashMap<>(bounties == null ? Map.of() : bounties);
    }

    public static BountyState get(MinecraftServer server) {
        SavedDataStorage manager = server.getLevel(Level.OVERWORLD).getDataStorage();
        return manager.computeIfAbsent(TYPE);
    }

    public Map<UUID, TargetBounty> getAll() {
        return bounties;
    }

    public TargetBounty getTarget(UUID targetId) {
        return bounties.get(targetId);
    }

    public BountyEntry getEntry(UUID targetId, UUID placerId) {
        TargetBounty target = bounties.get(targetId);
        if (target == null) {
            return null;
        }
        return target.placers().get(placerId);
    }

    public void putEntry(UUID targetId, String targetName, UUID placerId, String placerName, int hearts, int energy) {
        TargetBounty target = bounties.get(targetId);
        Map<UUID, BountyEntry> placers;
        if (target == null) {
            placers = new HashMap<>();
        } else {
            placers = new HashMap<>(target.placers());
        }
        placers.put(placerId, new BountyEntry(placerName, hearts, energy));
        bounties.put(targetId, new TargetBounty(targetName, placers));
        setDirty();
    }

    public TargetBounty removeTarget(UUID targetId) {
        TargetBounty removed = bounties.remove(targetId);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public BountyEntry removeEntry(UUID targetId, UUID placerId) {
        TargetBounty target = bounties.get(targetId);
        if (target == null) {
            return null;
        }
        Map<UUID, BountyEntry> placers = new HashMap<>(target.placers());
        BountyEntry removed = placers.remove(placerId);
        if (removed == null) {
            return null;
        }
        if (placers.isEmpty()) {
            bounties.remove(targetId);
        } else {
            bounties.put(targetId, new TargetBounty(target.targetName(), placers));
        }
        setDirty();
        return removed;
    }
}
