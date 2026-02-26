package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.loadout.GemLoadout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the loadout presets manager with current presets.
 */
public record LoadoutScreenPayload(
        GemId gem,
        int unlockEnergy,
        int maxPresets,
        int activeIndex,
        List<Identifier> abilityOrder,
        List<Preset> presets
) implements CustomPacketPayload {
    public static final Type<LoadoutScreenPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "loadout_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutScreenPayload> CODEC = StreamCodec.of(
            LoadoutScreenPayload::write,
            LoadoutScreenPayload::read
    );

    public record Preset(
            String name,
            boolean passivesEnabled,
            GemLoadout.HudPosition hudPosition,
            boolean showCooldowns,
            boolean showEnergy,
            boolean compactMode
    ) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, LoadoutScreenPayload payload) {
        buf.writeUtf(payload.gem().name(), 32);
        buf.writeVarInt(payload.unlockEnergy());
        buf.writeVarInt(payload.maxPresets());
        buf.writeVarInt(payload.activeIndex());
        buf.writeVarInt(payload.abilityOrder().size());
        for (Identifier id : payload.abilityOrder()) {
            buf.writeUtf(id.toString(), 128);
        }

        List<Preset> list = payload.presets();
        buf.writeVarInt(list.size());
        for (Preset preset : list) {
            buf.writeUtf(preset.name(), GemLoadout.MAX_NAME_LENGTH);
            buf.writeBoolean(preset.passivesEnabled());
            buf.writeVarInt(preset.hudPosition().ordinal());
            buf.writeBoolean(preset.showCooldowns());
            buf.writeBoolean(preset.showEnergy());
            buf.writeBoolean(preset.compactMode());
        }
    }

    private static LoadoutScreenPayload read(RegistryFriendlyByteBuf buf) {
        GemId gem = GemId.valueOf(buf.readUtf(32));
        int unlockEnergy = buf.readVarInt();
        int maxPresets = buf.readVarInt();
        int activeIndex = buf.readVarInt();
        int abilityCount = buf.readVarInt();
        List<Identifier> abilityOrder = new ArrayList<>(abilityCount);
        for (int i = 0; i < abilityCount; i++) {
            abilityOrder.add(Identifier.parse(buf.readUtf(128)));
        }

        int size = buf.readVarInt();
        List<Preset> presets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf(GemLoadout.MAX_NAME_LENGTH);
            boolean passivesEnabled = buf.readBoolean();
            int posOrdinal = buf.readVarInt();
            GemLoadout.HudPosition pos = GemLoadout.HudPosition.values()[
                    Math.min(posOrdinal, GemLoadout.HudPosition.values().length - 1)
            ];
            boolean showCooldowns = buf.readBoolean();
            boolean showEnergy = buf.readBoolean();
            boolean compactMode = buf.readBoolean();
            presets.add(new Preset(name, passivesEnabled, pos, showCooldowns, showEnergy, compactMode));
        }
        return new LoadoutScreenPayload(gem, unlockEnergy, maxPresets, activeIndex, abilityOrder, presets);
    }
}
