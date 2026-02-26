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
 * C2S payload to save a new loadout preset.
 */
public record LoadoutSavePayload(
        GemId gem,
        String name,
        List<Identifier> abilityOrder,
        boolean passivesEnabled,
        GemLoadout.HudPosition hudPosition,
        boolean showCooldowns,
        boolean showEnergy,
        boolean compactMode
) implements CustomPacketPayload {
    public static final Type<LoadoutSavePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "loadout_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutSavePayload> CODEC = StreamCodec.of(
            LoadoutSavePayload::write,
            LoadoutSavePayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /**
     * Convert to a GemLoadout for saving.
     */
    public GemLoadout toLoadout() {
        return new GemLoadout(
                name,
                gem,
                abilityOrder,
                passivesEnabled,
                new GemLoadout.HudLayout(hudPosition, showCooldowns, showEnergy, compactMode)
        );
    }

    private static void write(RegistryFriendlyByteBuf buf, LoadoutSavePayload payload) {
        buf.writeUtf(payload.gem().name(), 32);
        buf.writeUtf(payload.name(), GemLoadout.MAX_NAME_LENGTH);
        buf.writeVarInt(payload.abilityOrder().size());
        for (Identifier id : payload.abilityOrder()) {
            buf.writeUtf(id.toString(), 128);
        }
        buf.writeBoolean(payload.passivesEnabled());
        buf.writeVarInt(payload.hudPosition().ordinal());
        buf.writeBoolean(payload.showCooldowns());
        buf.writeBoolean(payload.showEnergy());
        buf.writeBoolean(payload.compactMode());
    }

    private static LoadoutSavePayload read(RegistryFriendlyByteBuf buf) {
        GemId gem = GemId.valueOf(buf.readUtf(32));
        String name = buf.readUtf(GemLoadout.MAX_NAME_LENGTH);
        int abilityCount = buf.readVarInt();
        List<Identifier> abilities = new ArrayList<>(abilityCount);
        for (int i = 0; i < abilityCount; i++) {
            abilities.add(Identifier.parse(buf.readUtf(128)));
        }
        boolean passivesEnabled = buf.readBoolean();
        int posOrdinal = buf.readVarInt();
        GemLoadout.HudPosition pos = GemLoadout.HudPosition.values()[
                Math.min(posOrdinal, GemLoadout.HudPosition.values().length - 1)
        ];
        boolean showCooldowns = buf.readBoolean();
        boolean showEnergy = buf.readBoolean();
        boolean compactMode = buf.readBoolean();
        return new LoadoutSavePayload(gem, name, abilities, passivesEnabled, pos, showCooldowns, showEnergy, compactMode);
    }
}
