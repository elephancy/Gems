package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: informs clients about server-side disables so UIs can hide disabled content.
 *
 * <p>Server remains authoritative; clients must never assume this is security-sensitive.</p>
 */
public record ServerDisablesPayload(
        List<Integer> disabledGemOrdinals,
        List<String> disabledAbilityIds,
        List<String> disabledPassiveIds,
        List<String> disabledBonusAbilityIds,
        List<String> disabledBonusPassiveIds
) implements CustomPacketPayload {
    public static final Type<ServerDisablesPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "server_disables"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerDisablesPayload> CODEC = StreamCodec.of(
            ServerDisablesPayload::write,
            ServerDisablesPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, ServerDisablesPayload payload) {
        List<Integer> gems = payload.disabledGemOrdinals == null ? List.of() : payload.disabledGemOrdinals;
        buf.writeVarInt(gems.size());
        for (int ordinal : gems) {
            buf.writeVarInt(ordinal);
        }

        writeStringList(buf, payload.disabledAbilityIds);
        writeStringList(buf, payload.disabledPassiveIds);
        writeStringList(buf, payload.disabledBonusAbilityIds);
        writeStringList(buf, payload.disabledBonusPassiveIds);
    }

    private static ServerDisablesPayload read(RegistryFriendlyByteBuf buf) {
        int gemCount = buf.readVarInt();
        List<Integer> disabledGems = new ArrayList<>(gemCount);
        for (int i = 0; i < gemCount; i++) {
            disabledGems.add(buf.readVarInt());
        }
        List<String> abilities = readStringList(buf);
        List<String> passives = readStringList(buf);
        List<String> bonusAbilities = readStringList(buf);
        List<String> bonusPassives = readStringList(buf);
        return new ServerDisablesPayload(disabledGems, abilities, passives, bonusAbilities, bonusPassives);
    }

    private static void writeStringList(RegistryFriendlyByteBuf buf, List<String> list) {
        List<String> safe = list == null ? List.of() : list;
        buf.writeVarInt(safe.size());
        for (String s : safe) {
            buf.writeUtf(s == null ? "" : s, 256);
        }
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(buf.readUtf(256));
        }
        return out;
    }
}

