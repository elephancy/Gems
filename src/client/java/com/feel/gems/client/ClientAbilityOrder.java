package com.feel.gems.client;

import com.feel.gems.core.GemId;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ClientAbilityOrder {
    private static final EnumMap<GemId, List<Identifier>> ORDERS = new EnumMap<>(GemId.class);

    private ClientAbilityOrder() {
    }

    public static void setOrder(GemId gem, List<Identifier> order) {
        if (gem == null) {
            return;
        }
        ORDERS.put(gem, order == null ? List.of() : List.copyOf(order));
    }

    public static List<Identifier> getOrder(GemId gem) {
        if (gem == null) {
            return List.of();
        }
        return ORDERS.getOrDefault(gem, List.of());
    }

    public static void reset() {
        ORDERS.clear();
    }
}
