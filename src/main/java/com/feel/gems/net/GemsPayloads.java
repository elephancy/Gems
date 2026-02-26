package com.feel.gems.net;

import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;




public final class GemsPayloads {
    private static boolean registered = false;

    private GemsPayloads() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.serverboundPlay().register(ActivateAbilityPayload.ID, ActivateAbilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActivateBonusAbilityPayload.ID, ActivateBonusAbilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SoulReleasePayload.ID, SoulReleasePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FluxChargePayload.ID, FluxChargePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientPassiveTogglePayload.ID, ClientPassiveTogglePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoadoutSavePayload.ID, LoadoutSavePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoadoutLoadPayload.ID, LoadoutLoadPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoadoutDeletePayload.ID, LoadoutDeletePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoadoutOpenRequestPayload.ID, LoadoutOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SummonerLoadoutOpenRequestPayload.ID, SummonerLoadoutOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SummonerLoadoutSavePayload.ID, SummonerLoadoutSavePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrackerCompassSelectPayload.ID, TrackerCompassSelectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BonusSelectionOpenRequestPayload.ID, BonusSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BonusSelectionClaimPayload.ID, BonusSelectionClaimPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PrismSelectionOpenRequestPayload.ID, PrismSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PrismSelectionClaimPayload.ID, PrismSelectionClaimPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SpyObservedOpenRequestPayload.ID, SpyObservedOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SpyObservedSelectPayload.ID, SpyObservedSelectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrophyNecklaceOpenRequestPayload.ID, TrophyNecklaceOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrophyNecklaceClaimPayload.ID, TrophyNecklaceClaimPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AugmentOpenRequestPayload.ID, AugmentOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AugmentRemovePayload.ID, AugmentRemovePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InscriptionOpenRequestPayload.ID, InscriptionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InscriptionRemovePayload.ID, InscriptionRemovePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TitleSelectionOpenRequestPayload.ID, TitleSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TitleSelectionSelectPayload.ID, TitleSelectionSelectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BountyBoardOpenRequestPayload.ID, BountyBoardOpenRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BountyPlacePayload.ID, BountyPlacePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownSnapshotPayload.ID, CooldownSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AbilityCooldownPayload.ID, AbilityCooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ExtraStatePayload.ID, ExtraStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StolenStatePayload.ID, StolenStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AbilityOrderSyncPayload.ID, AbilityOrderSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HudLayoutPayload.ID, HudLayoutPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SummonerLoadoutScreenPayload.ID, SummonerLoadoutScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LoadoutScreenPayload.ID, LoadoutScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackerCompassScreenPayload.ID, TrackerCompassScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GemSeerOpeningPayload.ID, GemSeerOpeningPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SpySkinshiftPayload.ID, SpySkinshiftPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerDisablesPayload.ID, ServerDisablesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChaosSlotPayload.ID, ChaosSlotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BonusSelectionScreenPayload.ID, BonusSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BonusAbilitiesSyncPayload.ID, BonusAbilitiesSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PrismSelectionScreenPayload.ID, PrismSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PrismAbilitiesSyncPayload.ID, PrismAbilitiesSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SpyObservedScreenPayload.ID, SpyObservedScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrophyNecklaceScreenPayload.ID, TrophyNecklaceScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AugmentScreenPayload.ID, AugmentScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InscriptionScreenPayload.ID, InscriptionScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TitleSelectionScreenPayload.ID, TitleSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BountyBoardPayload.ID, BountyBoardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShadowCloneSyncPayload.ID, ShadowCloneSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TricksterControlPayload.ID, TricksterControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RivalrySyncPayload.ID, RivalrySyncPayload.CODEC);
        registered = true;
    }
}
