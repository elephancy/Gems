package com.feel.gems.client;

import com.feel.gems.net.ChaosSlotPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Client-side state for Chaos gem's ability slots.
 * Slot count is dynamic and determined by server config.
 */
public final class ClientChaosState {
    
    public record SlotState(
        String abilityName,
        Identifier abilityId,
        String passiveName,
        Identifier passiveId,
        int remainingSeconds,
        int cooldownSeconds
    ) {
        public static SlotState inactive() {
            return new SlotState("", null, "", null, 0, 0);
        }
        
        public boolean isActive() {
            return abilityId != null && remainingSeconds > 0;
        }
    }
    
    private static final List<SlotState> slots = new ArrayList<>();
    private static int serverSlotCount = 6; // Default, updated by server payload
    
    static {
        for (int i = 0; i < 9; i++) { // Pre-allocate max possible slots
            slots.add(SlotState.inactive());
        }
    }

    private ClientChaosState() {
    }

    /**
     * Returns the current slot count as reported by the server.
     */
    public static int slotCount() {
        return serverSlotCount;
    }

    public static SlotState getSlot(int index) {
        if (index < 0 || index >= serverSlotCount) return SlotState.inactive();
        if (index >= slots.size()) return SlotState.inactive();
        return slots.get(index);
    }

    public static void update(ChaosSlotPayload payload) {
        List<ChaosSlotPayload.SlotData> data = payload.slots();
        serverSlotCount = data.size();
        
        // Ensure we have enough slots
        while (slots.size() < serverSlotCount) {
            slots.add(SlotState.inactive());
        }
        
        for (int i = 0; i < serverSlotCount; i++) {
            ChaosSlotPayload.SlotData d = data.get(i);
            Identifier abilityId = (d.abilityId() == null || d.abilityId().isEmpty()) 
                ? null : Identifier.tryParse(d.abilityId());
            Identifier passiveId = (d.passiveId() == null || d.passiveId().isEmpty())
                ? null : Identifier.tryParse(d.passiveId());
            slots.set(i, new SlotState(
                d.abilityName(),
                abilityId,
                d.passiveName(),
                passiveId,
                d.remainingSeconds(),
                d.cooldownSeconds()
            ));
        }
        
        // Clear any extra slots beyond server count
        for (int i = serverSlotCount; i < slots.size(); i++) {
            slots.set(i, SlotState.inactive());
        }
    }

    public static void reset() {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, SlotState.inactive());
        }
    }

    public static boolean hasAnyActiveSlot() {
        for (SlotState slot : slots) {
            if (slot.isActive()) return true;
        }
        return false;
    }
    
}
