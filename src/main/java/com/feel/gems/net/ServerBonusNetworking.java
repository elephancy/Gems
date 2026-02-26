package com.feel.gems.net;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.net.GemStateSync;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side networking for bonus ability/passive selection.
 */
public final class ServerBonusNetworking {
    private static final int MAX_BONUS_ABILITIES = 2;
    private static final int MAX_BONUS_PASSIVES = 2;

    private ServerBonusNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BonusSelectionOpenRequestPayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> openEditor(context.player())));
        
        ServerPlayNetworking.registerGlobalReceiver(BonusSelectionClaimPayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> handleClaim(context.player(), payload)));
    }

    public static void openEditor(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        
        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendSystemMessage(Component.translatable("gems.bonus.need_energy_access"), true);
            return;
        }
        
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        
        BonusClaimsState claims = BonusClaimsState.get(server);
        UUID playerId = player.getUUID();
        
        Set<Identifier> playerAbilities = claims.getPlayerAbilities(playerId);
        Set<Identifier> playerPassives = claims.getPlayerPassives(playerId);
        
        // Build ability list
        List<BonusSelectionScreenPayload.BonusEntry> abilities = new ArrayList<>();
        for (Identifier id : BonusPoolRegistry.BONUS_ABILITIES) {
            GemAbility ability = ModAbilities.get(id);
            if (ability == null) continue;
            
            boolean claimed = playerAbilities.contains(id);
            boolean available = claimed || claims.isAbilityAvailable(id);
            
            abilities.add(new BonusSelectionScreenPayload.BonusEntry(
                    id,
                    ability.name(),
                    ability.description(),
                    available,
                    claimed
            ));
        }
        
        // Build passive list
        List<BonusSelectionScreenPayload.BonusEntry> passives = new ArrayList<>();
        for (Identifier id : BonusPoolRegistry.BONUS_PASSIVES) {
            GemPassive passive = ModPassives.get(id);
            if (passive == null) continue;
            
            boolean claimed = playerPassives.contains(id);
            boolean available = claimed || claims.isPassiveAvailable(id);
            
            passives.add(new BonusSelectionScreenPayload.BonusEntry(
                    id,
                    passive.name(),
                    passive.description(),
                    available,
                    claimed
            ));
        }
        
        ServerPlayNetworking.send(player, new BonusSelectionScreenPayload(
                abilities,
                passives,
                MAX_BONUS_ABILITIES,
                MAX_BONUS_PASSIVES
        ));
    }

    private static void handleClaim(ServerPlayer player, BonusSelectionClaimPayload payload) {
        GemPlayerState.initIfNeeded(player);
        
        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendSystemMessage(Component.translatable("gems.bonus.need_energy_claim"), true);
            return;
        }
        
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        
        BonusClaimsState claims = BonusClaimsState.get(server);
        UUID playerId = player.getUUID();
        
        Identifier powerId = payload.powerId();
        boolean isAbility = payload.isAbility();
        boolean claim = payload.claim();
        
        if (claim) {
            // Trying to claim
            boolean success;
            if (isAbility) {
                success = claims.claimAbility(playerId, powerId);
                if (success) {
                    GemAbility ability = ModAbilities.get(powerId);
                    String name = ability != null ? ability.name() : powerId.toString();
                    player.sendSystemMessage(Component.translatable("gems.bonus.claimed_ability", name), false);
                } else {
                    player.sendSystemMessage(Component.translatable("gems.bonus.cannot_claim_ability"), true);
                }
            } else {
                success = claims.claimPassive(playerId, powerId);
                if (success) {
                    GemPassive passive = ModPassives.get(powerId);
                    String name = passive != null ? passive.name() : powerId.toString();
                    player.sendSystemMessage(Component.translatable("gems.bonus.claimed_passive", name), false);
                    // Apply the passive immediately
                    passive.apply(player);
                } else {
                    player.sendSystemMessage(Component.translatable("gems.bonus.cannot_claim_passive"), true);
                }
            }
        } else {
            // Trying to unclaim
            if (isAbility) {
                Set<Identifier> current = claims.getPlayerAbilities(playerId);
                if (current.contains(powerId)) {
                    claims.releaseAbility(playerId, powerId);
                    GemAbility ability = ModAbilities.get(powerId);
                    String name = ability != null ? ability.name() : powerId.toString();
                    player.sendSystemMessage(Component.translatable("gems.bonus.released_ability", name), false);
                }
            } else {
                Set<Identifier> current = claims.getPlayerPassives(playerId);
                if (current.contains(powerId)) {
                    // Remove passive effect first
                    GemPassive passive = ModPassives.get(powerId);
                    if (passive != null) {
                        passive.remove(player);
                    }
                    claims.releasePassive(playerId, powerId);
                    String name = passive != null ? passive.name() : powerId.toString();
                    player.sendSystemMessage(Component.translatable("gems.bonus.released_passive", name), false);
                }
            }
        }
        
        GemCooldownSync.send(player);
        GemStateSync.sendBonusAbilitiesSync(player);

        // Refresh the screen
        openEditor(player);
    }
}
