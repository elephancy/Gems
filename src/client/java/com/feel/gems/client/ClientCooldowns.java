package com.feel.gems.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;




public final class ClientCooldowns {
    private static final Map<Identifier, Long> END_TICKS = new HashMap<>();
    private static final Map<Identifier, Integer> LAST_COOLDOWN_TICKS = new HashMap<>();
    private static GemId activeGem = null;
    private static Identifier lastUsedAbility = null;

    private ClientCooldowns() {
    }

    public static void reset() {
        activeGem = null;
        END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedAbility = null;
    }

    public static void clearIfGemChanged(GemId gem) {
        if (activeGem == gem) {
            return;
        }
        activeGem = gem;
        END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedAbility = null;
    }

    public static void applySnapshot(GemId gem, List<Integer> remainingAbilityCooldownTicks, List<Integer> maxCooldownTicks) {
        clearIfGemChanged(gem);
        END_TICKS.clear();

        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            return;
        }
        long now = world.getGameTime();

        List<Identifier> abilities = orderedAbilities(gem);
        int n = Math.min(abilities.size(), remainingAbilityCooldownTicks.size());
        for (int i = 0; i < n; i++) {
            int remaining = remainingAbilityCooldownTicks.get(i);
            if (remaining <= 0) {
                continue;
            }
            END_TICKS.put(abilities.get(i), now + remaining);
        }

        if (maxCooldownTicks != null && !maxCooldownTicks.isEmpty()) {
            int m = Math.min(abilities.size(), maxCooldownTicks.size());
            for (int i = 0; i < m; i++) {
                int maxTicks = maxCooldownTicks.get(i);
                if (maxTicks > 0) {
                    LAST_COOLDOWN_TICKS.put(abilities.get(i), maxTicks);
                } else {
                    LAST_COOLDOWN_TICKS.remove(abilities.get(i));
                }
            }
        }
    }

    public static void setCooldown(GemId gem, int abilityIndex, int cooldownTicks) {
        clearIfGemChanged(gem);
        if (cooldownTicks <= 0) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            return;
        }
        long now = world.getGameTime();

        List<Identifier> abilities = orderedAbilities(gem);
        if (abilityIndex < 0 || abilityIndex >= abilities.size()) {
            return;
        }
        Identifier abilityId = abilities.get(abilityIndex);
        END_TICKS.put(abilityId, now + cooldownTicks);
        LAST_COOLDOWN_TICKS.put(abilityId, cooldownTicks);
        lastUsedAbility = abilityId;
    }

    public static int lastCooldownTicks(GemId gem, Identifier abilityId) {
        if (activeGem != gem) {
            return 0;
        }
        return LAST_COOLDOWN_TICKS.getOrDefault(abilityId, 0);
    }

    public static int remainingTicks(GemId gem, Identifier abilityId) {
        if (activeGem != gem) {
            return 0;
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            return 0;
        }
        Long end = END_TICKS.get(abilityId);
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

    public static boolean isLastUsed(GemId gem, Identifier abilityId) {
        return activeGem == gem && abilityId != null && abilityId.equals(lastUsedAbility);
    }

    private static List<Identifier> orderedAbilities(GemId gem) {
        List<Identifier> order = ClientAbilityOrder.getOrder(gem);
        if (!order.isEmpty()) {
            return order;
        }
        GemDefinition def = GemRegistry.definition(gem);
        return def.abilities();
    }
}
