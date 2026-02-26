package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Syncs all 4 chaos slot states to the client for HUD display.
 */
public record ChaosSlotPayload(List<SlotData> slots) implements CustomPacketPayload {
    public static final Type<ChaosSlotPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "chaos_slots"));
    
    public record SlotData(
        String abilityName,
        String abilityId,
        String passiveName,
        String passiveId,
        int remainingSeconds,
        int cooldownSeconds
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SlotData> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SlotData::abilityName,
            ByteBufCodecs.STRING_UTF8, SlotData::abilityId,
            ByteBufCodecs.STRING_UTF8, SlotData::passiveName,
            ByteBufCodecs.STRING_UTF8, SlotData::passiveId,
            ByteBufCodecs.VAR_INT, SlotData::remainingSeconds,
            ByteBufCodecs.VAR_INT, SlotData::cooldownSeconds,
            SlotData::new
        );
        
        public boolean isActive() {
            return !abilityName.isEmpty() && remainingSeconds > 0;
        }
    }
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ChaosSlotPayload> CODEC = StreamCodec.composite(
        SlotData.CODEC.apply(ByteBufCodecs.list()), ChaosSlotPayload::slots,
        ChaosSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
