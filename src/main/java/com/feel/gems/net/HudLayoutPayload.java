package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.loadout.GemLoadout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: syncs the current HUD layout settings.
 */
public record HudLayoutPayload(
        GemLoadout.HudPosition position,
        boolean showCooldowns,
        boolean showEnergy,
        boolean compactMode
) implements CustomPacketPayload {
    public static final Type<HudLayoutPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "hud_layout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HudLayoutPayload> CODEC = StreamCodec.of(
            HudLayoutPayload::write,
            HudLayoutPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, HudLayoutPayload payload) {
        buf.writeVarInt(payload.position().ordinal());
        buf.writeBoolean(payload.showCooldowns());
        buf.writeBoolean(payload.showEnergy());
        buf.writeBoolean(payload.compactMode());
    }

    private static HudLayoutPayload read(RegistryFriendlyByteBuf buf) {
        int posOrdinal = buf.readVarInt();
        GemLoadout.HudPosition pos = GemLoadout.HudPosition.values()[
                Math.min(posOrdinal, GemLoadout.HudPosition.values().length - 1)
        ];
        boolean showCooldowns = buf.readBoolean();
        boolean showEnergy = buf.readBoolean();
        boolean compactMode = buf.readBoolean();
        return new HudLayoutPayload(pos, showCooldowns, showEnergy, compactMode);
    }
}
