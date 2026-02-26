package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: syncs the player's claimed bonus abilities to the client for HUD display.
 */
public record BonusAbilitiesSyncPayload(
        List<BonusAbilityInfo> abilities
) implements CustomPacketPayload {

    public record BonusAbilityInfo(
            Identifier id,
            String name,
            int remainingCooldownTicks
    ) {}

    public static final Type<BonusAbilitiesSyncPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bonus_abilities_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BonusAbilitiesSyncPayload> CODEC = StreamCodec.of(
            BonusAbilitiesSyncPayload::write,
            BonusAbilitiesSyncPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, BonusAbilitiesSyncPayload payload) {
        buf.writeVarInt(payload.abilities.size());
        for (BonusAbilityInfo info : payload.abilities) {
            buf.writeIdentifier(info.id());
            buf.writeUtf(info.name(), 128);
            buf.writeVarInt(info.remainingCooldownTicks());
        }
    }

    private static BonusAbilitiesSyncPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BonusAbilityInfo> abilities = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf(128);
            int cooldown = buf.readVarInt();
            abilities.add(new BonusAbilityInfo(id, name, cooldown));
        }
        return new BonusAbilitiesSyncPayload(abilities);
    }
}
