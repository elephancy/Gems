package com.feel.gems.client;

import com.feel.gems.net.BonusAbilitiesSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

/**
 * Client-side state for bonus abilities (HUD display and cooldown tracking).
 */
public final class ClientBonusState {
    private static final List<BonusAbilityEntry> ABILITIES = new ArrayList<>();
    private static final Map<Identifier, Long> COOLDOWN_END_TICKS = new HashMap<>();
    private static final Map<Identifier, Integer> LAST_COOLDOWN_TICKS = new HashMap<>();
    private static Identifier lastUsedBonus = null;

    private ClientBonusState() {
    }

    public record BonusAbilityEntry(Identifier id, String name) {}

    public static void reset() {
        ABILITIES.clear();
        COOLDOWN_END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedBonus = null;
    }

    public static void update(BonusAbilitiesSyncPayload payload) {
        ABILITIES.clear();
        COOLDOWN_END_TICKS.clear();
        
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        long now = world != null ? world.getGameTime() : 0;
        
        for (BonusAbilitiesSyncPayload.BonusAbilityInfo info : payload.abilities()) {
            ABILITIES.add(new BonusAbilityEntry(info.id(), info.name()));
            if (info.remainingCooldownTicks() > 0) {
                COOLDOWN_END_TICKS.put(info.id(), now + info.remainingCooldownTicks());
            }
        }
    }

    public static void setCooldown(int slotIndex, int cooldownTicks) {
        if (slotIndex < 0 || slotIndex >= ABILITIES.size()) {
            return;
        }
        Identifier id = ABILITIES.get(slotIndex).id();
        
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || cooldownTicks <= 0) {
            return;
        }
        long now = world.getGameTime();
        COOLDOWN_END_TICKS.put(id, now + cooldownTicks);
        LAST_COOLDOWN_TICKS.put(id, cooldownTicks);
        lastUsedBonus = id;
    }

    public static int lastCooldownTicks(Identifier abilityId) {
        return LAST_COOLDOWN_TICKS.getOrDefault(abilityId, 0);
    }

    public static List<BonusAbilityEntry> getAbilities() {
        return List.copyOf(ABILITIES);
    }

    public static int remainingTicks(Identifier abilityId) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            return 0;
        }
        Long end = COOLDOWN_END_TICKS.get(abilityId);
        if (end == null) {
            return 0;
        }
        long remaining = end - world.getGameTime();
        if (remaining <= 0) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    public static boolean isLastUsed(Identifier abilityId) {
        return abilityId != null && abilityId.equals(lastUsedBonus);
    }
}
