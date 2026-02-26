package com.feel.gems.net;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.core.GemId;
import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.loadout.LoadoutManager;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;




public final class GemStateSync {
    private GemStateSync() {
    }

    public static void send(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        ServerPlayNetworking.send(player, new StateSyncPayload(
                active.ordinal(),
                GemPlayerState.getEnergy(player),
                GemPlayerState.getMaxHearts(player)
        ));

        sendAbilityOrderSync(player, active);
        sendHudLayoutSync(player);
        GemCooldownSync.send(player);
        GemExtraStateSync.send(player);
        StolenStateSync.send(player);
        sendBonusAbilitiesSync(player);
        
        // Send Prism abilities if the player has the Prism gem
        if (active == GemId.PRISM) {
            sendPrismAbilitiesSync(player);
        }
    }

    private static void sendAbilityOrderSync(ServerPlayer player, GemId active) {
        List<Identifier> order = LoadoutManager.getAbilityOrder(player, active);
        ServerPlayNetworking.send(player, new AbilityOrderSyncPayload(active.ordinal(), order));
    }

    private static void sendHudLayoutSync(ServerPlayer player) {
        GemLoadout.HudLayout layout = LoadoutManager.getHudLayout(player);
        ServerPlayNetworking.send(player, new HudLayoutPayload(
                layout.position(),
                layout.showCooldowns(),
                layout.showEnergy(),
                layout.compactMode()
        ));
    }

    public static void sendBonusAbilitiesSync(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        
        BonusClaimsState claims = BonusClaimsState.get(server);
        List<Identifier> orderedAbilities = claims.getPlayerAbilityOrder(player.getUUID());
        
        if (orderedAbilities.isEmpty()) {
            ServerPlayNetworking.send(player, new BonusAbilitiesSyncPayload(List.of()));
            return;
        }
        
        long now = GemsTime.now(player);
        List<BonusAbilitiesSyncPayload.BonusAbilityInfo> abilities = new ArrayList<>();
        
        for (Identifier id : orderedAbilities) {
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.getPath();
            
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, id);
            int remaining = nextAllowed > now ? (int) (nextAllowed - now) : 0;
            
            abilities.add(new BonusAbilitiesSyncPayload.BonusAbilityInfo(id, name, remaining));
        }
        
        ServerPlayNetworking.send(player, new BonusAbilitiesSyncPayload(abilities));
    }

    /**
     * Send Prism ability sync to client for HUD display.
     */
    public static void sendPrismAbilitiesSync(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        
        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUUID());
        List<Identifier> selectedAbilities = selection.gemAbilities();
        
        List<Identifier> selectedPassives = selection.allPassives();
        if (selectedAbilities.isEmpty() && selectedPassives.isEmpty()) {
            ServerPlayNetworking.send(player, new PrismAbilitiesSyncPayload(List.of(), List.of()));
            return;
        }

        long now = GemsTime.now(player);
        List<PrismAbilitiesSyncPayload.PrismAbilityInfo> abilities = new ArrayList<>();
        List<PrismAbilitiesSyncPayload.PrismPassiveInfo> passives = new ArrayList<>();

        for (Identifier id : selectedAbilities) {
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.getPath();

            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, id);
            int remaining = nextAllowed > now ? (int) (nextAllowed - now) : 0;

            abilities.add(new PrismAbilitiesSyncPayload.PrismAbilityInfo(id, name, remaining));
        }

        for (Identifier id : selectedPassives) {
            GemPassive passive = ModPassives.get(id);
            String name = passive != null ? passive.name() : id.getPath();
            passives.add(new PrismAbilitiesSyncPayload.PrismPassiveInfo(id, name));
        }

        ServerPlayNetworking.send(player, new PrismAbilitiesSyncPayload(abilities, passives));
    }
}
