package com.feel.gems.net;

import com.feel.gems.item.legendary.TrackerCompassItem;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerLegendaryNetworking {
    private ServerLegendaryNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TrackerCompassSelectPayload.ID, (payload, context) ->
                context.server().execute(() -> handleCompassSelection(context.player(), payload.target())));
    }

    private static void handleCompassSelection(net.minecraft.server.level.ServerPlayer player, Optional<UUID> target) {
        if (target.isEmpty()) {
            TrackerCompassItem.clearTarget(player);
            return;
        }
        TrackerCompassItem.setTarget(player, target.get());
    }
}
