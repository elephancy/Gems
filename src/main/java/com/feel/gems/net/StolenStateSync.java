package com.feel.gems.net;

import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.power.gem.spy.SpySystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class StolenStateSync {
    private StolenStateSync() {
    }

    public static void send(ServerPlayer player) {
        List<Identifier> passives = new ArrayList<>(HuntersTrophyNecklaceItem.getStolenPassives(player));
        List<Identifier> abilities = SpySystem.getStolenAbilities(player);
        ServerPlayNetworking.send(player, new StolenStatePayload(passives, abilities));
    }
}
