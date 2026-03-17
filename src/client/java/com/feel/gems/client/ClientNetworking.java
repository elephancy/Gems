package com.feel.gems.client;

import com.feel.gems.bonus.BonusAbilityRuntime;
import com.feel.gems.client.screen.BonusSelectionScreen;
import com.feel.gems.client.screen.AugmentScreen;
import com.feel.gems.client.screen.InscriptionScreen;
import com.feel.gems.client.screen.PrismSelectionScreen;
import com.feel.gems.client.screen.SpyObservedSelectionScreen;
import com.feel.gems.client.screen.SummonerLoadoutScreen;
import com.feel.gems.client.screen.LoadoutPresetsScreen;
import com.feel.gems.client.screen.TrophyNecklaceScreen;
import com.feel.gems.client.screen.TrackerCompassScreen;
import com.feel.gems.client.screen.TitleSelectionScreen;
import com.feel.gems.client.screen.BountyBoardScreen;
import com.feel.gems.core.GemId;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.net.AbilityOrderSyncPayload;
import com.feel.gems.net.AugmentScreenPayload;
import com.feel.gems.net.InscriptionScreenPayload;
import com.feel.gems.net.BonusAbilitiesSyncPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.BountyBoardPayload;
import com.feel.gems.net.ChaosSlotPayload;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.net.ExtraStatePayload;
import com.feel.gems.net.PrismAbilitiesSyncPayload;
import com.feel.gems.net.PrismSelectionScreenPayload;
import com.feel.gems.net.SpyObservedScreenPayload;
import com.feel.gems.net.ServerDisablesPayload;
import com.feel.gems.net.StateSyncPayload;
import com.feel.gems.net.StolenStatePayload;
import com.feel.gems.net.SummonerLoadoutScreenPayload;
import com.feel.gems.net.LoadoutScreenPayload;
import com.feel.gems.net.HudLayoutPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.net.GemSeerOpeningPayload;
import com.feel.gems.screen.GemSeerScreenHandler;
import com.feel.gems.client.screen.GemSeerScreen;
import com.feel.gems.net.TitleSelectionScreenPayload;
import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.net.TricksterControlPayload;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.net.RivalrySyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;




public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientGemState.reset();
            ClientCooldowns.reset();
            ClientExtraState.reset();
            ClientAbilitySelection.reset();
            ClientAbilityOrder.reset();
            ClientHudLayout.reset();
            ClientChaosState.reset();
            ClientDisguiseState.reset();
            ClientDisables.reset();
            ClientBonusState.reset();
            ClientShadowCloneState.reset();
            ClientTricksterState.reset();
            ClientStolenState.reset();
            ClientRivalryState.clear();
        }));

        ClientPlayNetworking.registerGlobalReceiver(StateSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    GemId gem = safeGemId(payload.activeGemOrdinal());
                    ClientGemState.update(gem, payload.energy(), payload.maxHearts());
                    ClientCooldowns.clearIfGemChanged(gem);
                }));

        ClientPlayNetworking.registerGlobalReceiver(CooldownSnapshotPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientCooldowns.applySnapshot(
                        safeGemId(payload.activeGemOrdinal()),
                        payload.remainingAbilityCooldownTicks(),
                        payload.maxAbilityCooldownTicks()
                )));

        ClientPlayNetworking.registerGlobalReceiver(AbilityCooldownPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    int abilityIndex = payload.abilityIndex();
                    // Check if this is a bonus ability cooldown (offset by 100)
                    if (abilityIndex >= BonusAbilityRuntime.BONUS_ABILITY_INDEX_OFFSET) {
                        int bonusSlot = abilityIndex - BonusAbilityRuntime.BONUS_ABILITY_INDEX_OFFSET;
                        ClientBonusState.setCooldown(bonusSlot, payload.cooldownTicks());
                    } else {
                        GemId gem = safeGemId(payload.activeGemOrdinal());
                        if (gem == GemId.PRISM) {
                            ClientPrismState.setCooldown(abilityIndex, payload.cooldownTicks());
                        } else {
                            ClientCooldowns.setCooldown(gem, abilityIndex, payload.cooldownTicks());
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ExtraStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientExtraState.update(
                        payload.fluxChargePercent(),
                        payload.hasSoul(),
                        payload.soulTypeId()
                )));

        ClientPlayNetworking.registerGlobalReceiver(StolenStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientStolenState.update(
                        payload.stolenPassives(),
                        payload.stolenAbilities()
                )));

        ClientPlayNetworking.registerGlobalReceiver(AbilityOrderSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    GemId gem = safeGemId(payload.activeGemOrdinal());
                    ClientAbilityOrder.setOrder(gem, payload.abilityOrder());
                }));

        ClientPlayNetworking.registerGlobalReceiver(HudLayoutPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientHudLayout.update(
                        new com.feel.gems.loadout.GemLoadout.HudLayout(
                                payload.position(),
                                payload.showCooldowns(),
                                payload.showEnergy(),
                                payload.compactMode()
                        )
                )));

        ClientPlayNetworking.registerGlobalReceiver(SummonerLoadoutScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new SummonerLoadoutScreen(payload));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(LoadoutScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new LoadoutPresetsScreen(payload));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(GemSeerOpeningPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client.player != null
                            && client.player.containerMenu instanceof GemSeerScreenHandler menu
                            && menu.containerId == payload.menuId()) {
                        menu.setOpeningData(payload.data());
                        if (client.gui.screen() instanceof GemSeerScreen screen) {
                            screen.refreshPlayers();
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(TrackerCompassScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new TrackerCompassScreen(payload));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(SpySkinshiftPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientDisguiseState.update(payload.player(), payload.target()))
        );
        ClientPlayNetworking.registerGlobalReceiver(TricksterControlPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientTricksterState.update(payload.puppeted(), payload.mindGames()))
        );

        ClientPlayNetworking.registerGlobalReceiver(ServerDisablesPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientDisables.update(payload))
        );

        // Chaos slots
        ClientPlayNetworking.registerGlobalReceiver(ChaosSlotPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientChaosState.update(payload))
        );

        // Bonus selection screen
        ClientPlayNetworking.registerGlobalReceiver(BonusSelectionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new BonusSelectionScreen(payload));
                    }
                }));

        // Bonus abilities sync (for HUD)
        ClientPlayNetworking.registerGlobalReceiver(BonusAbilitiesSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientBonusState.update(payload))
        );

        // Trophy Necklace screen
        ClientPlayNetworking.registerGlobalReceiver(TrophyNecklaceScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new TrophyNecklaceScreen(payload));
                    }
                }));

        // Augment management screen
        ClientPlayNetworking.registerGlobalReceiver(AugmentScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new AugmentScreen(payload));
                    }
                }));

        // Inscription management screen
        ClientPlayNetworking.registerGlobalReceiver(InscriptionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new InscriptionScreen(payload));
                    }
                }));

        // Prism abilities sync (for HUD)
        ClientPlayNetworking.registerGlobalReceiver(PrismAbilitiesSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientPrismState.update(payload))
        );

        // Shadow clone owner sync (for skin rendering)
        ClientPlayNetworking.registerGlobalReceiver(ShadowCloneSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientShadowCloneState.setOwner(payload.entityId(), payload.ownerUuid()))
        );

        // Prism selection screen
        ClientPlayNetworking.registerGlobalReceiver(PrismSelectionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new PrismSelectionScreen(payload));
                    }
                }));

        // Spy observed abilities screen
        ClientPlayNetworking.registerGlobalReceiver(SpyObservedScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new SpyObservedSelectionScreen(payload));
                    }
                }));

        // Title selection screen
        ClientPlayNetworking.registerGlobalReceiver(TitleSelectionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new TitleSelectionScreen(payload));
                    }
                }));

        // Bounty board screen
        ClientPlayNetworking.registerGlobalReceiver(BountyBoardPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (client != null) {
                        client.gui.setScreen(new BountyBoardScreen(payload));
                    }
                }));

        // Rivalry target sync (for HUD)
        ClientPlayNetworking.registerGlobalReceiver(RivalrySyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientRivalryState.setRivalTarget(payload.targetName()))
        );
    }

    private static GemId safeGemId(int ordinal) {
        GemId[] values = GemId.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GemId.ASTRA;
        }
        return values[ordinal];
    }
}
