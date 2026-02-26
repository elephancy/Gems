package com.feel.gems.net;

import com.feel.gems.core.GemId;
import com.feel.gems.loadout.LoadoutManager;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;




public final class GemCooldownSync {
    private GemCooldownSync() {
    }

    public static void send(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        List<Identifier> abilities = LoadoutManager.getAbilityOrder(player, active);

        long now = GemsTime.now(player);

        List<Integer> remaining = new ArrayList<>(abilities.size());
        List<Integer> maxCooldowns = new ArrayList<>(abilities.size());
        for (int i = 0; i < abilities.size(); i++) {
            Identifier id = abilities.get(i);
            int rawRemaining = GemAbilityCooldowns.remainingTicks(player, id, now);
            remaining.add(rawRemaining);

            GemAbility ability = ModAbilities.get(id);
            int baseCooldown = ability != null ? Math.max(0, ability.cooldownTicks()) : 0;
            int adjusted = GemAbilities.adjustedCooldownTicks(player, active, baseCooldown);
            maxCooldowns.add(adjusted);
        }

        ServerPlayNetworking.send(player, new CooldownSnapshotPayload(active.ordinal(), remaining, maxCooldowns));
    }
}
