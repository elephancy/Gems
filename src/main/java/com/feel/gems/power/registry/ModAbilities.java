package com.feel.gems.power.registry;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.power.ability.air.*;
import com.feel.gems.power.ability.astra.*;
import com.feel.gems.power.ability.beacon.*;
import com.feel.gems.power.ability.bonus.*;
import com.feel.gems.power.ability.duelist.*;
import com.feel.gems.power.ability.fire.*;
import com.feel.gems.power.ability.flux.*;
import com.feel.gems.power.ability.hunter.*;
import com.feel.gems.power.ability.life.*;
import com.feel.gems.power.ability.pillager.*;
import com.feel.gems.power.ability.puff.*;
import com.feel.gems.power.ability.reaper.*;
import com.feel.gems.power.ability.sentinel.*;
import com.feel.gems.power.ability.space.*;
import com.feel.gems.power.ability.speed.*;
import com.feel.gems.power.ability.spy.*;
import com.feel.gems.power.ability.strength.*;
import com.feel.gems.power.ability.summoner.*;
import com.feel.gems.power.ability.terror.*;
import com.feel.gems.power.ability.trickster.*;
import com.feel.gems.power.ability.wealth.*;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;




public final class ModAbilities {
    private static final Map<Identifier, GemAbility> ABILITIES = new HashMap<>();

    static {
        // Astra
        register(new ShadowAnchorAbility());
        register(new DimensionalVoidAbility());
        register(new AstralDaggersAbility());
        register(new UnboundedAbility());
        register(new AstralCameraAbility());
        register(new SpookAbility());
        register(new TagAbility());

        // Fire
        register(new CosyCampfireAbility());
        register(new HeatHazeZoneAbility());
        register(new FireballAbility());
        register(new MeteorShowerAbility());

        // Flux
        register(new FluxBeamAbility());
        register(new StaticBurstAbility());
        register(new FluxSurgeAbility());
        register(new FluxDischargeAbility());

        // Life
        register(new VitalityVortexAbility());
        register(new HealthDrainAbility());
        register(new LifeSwapAbility());
        register(new LifeCircleAbility());
        register(new HeartLockAbility());

        // Puff
        register(new DoubleJumpAbility());
        register(new DashAbility());
        register(new BreezyBashAbility());
        register(new GroupBreezyBashAbility());
        register(new PuffGustAbility());

        // Speed
        register(new ArcShotAbility());
        register(new SpeedStormAbility());
        register(new TerminalVelocityAbility());
        register(new SpeedSlipstreamAbility());
        register(new SpeedAfterimageAbility());
        register(new SpeedTempoShiftAbility());
        register(new SpeedAutoStepAbility());

        // Strength
        register(new NullifyAbility());
        register(new FrailerAbility());
        register(new BountyHuntingAbility());
        register(new ChadStrengthAbility());

        // Wealth
        register(new PocketsAbility());
        register(new FumbleAbility());
        register(new HotbarLockAbility());
        register(new AmplificationAbility());
        register(new RichRushAbility());

        // Terror
        register(new TerrorTradeAbility());
        register(new PanicRingAbility());
        register(new TerrorRigAbility());
        register(new TerrorBreachChargeAbility());
        register(new TerrorRemoteChargeAbility());

        // Summoner
        register(new SummonSlotAbility(1));
        register(new SummonSlotAbility(2));
        register(new SummonSlotAbility(3));
        register(new SummonSlotAbility(4));
        register(new SummonSlotAbility(5));
        register(new SummonRecallAbility());

        // Space
        register(new SpaceOrbitalLaserAbility());
        register(new SpaceOrbitalLaserMiningAbility());
        register(new SpaceGravityFieldAbility());
        register(new SpaceBlackHoleAbility());
        register(new SpaceWhiteHoleAbility());

        // Reaper
        register(new ReaperGraveSteedAbility());
        register(new ReaperWitheringStrikesAbility());
        register(new ReaperDeathOathAbility());
        register(new ReaperRetributionAbility());
        register(new ReaperScytheSweepAbility());
        register(new ReaperBloodChargeAbility());
        register(new ReaperShadowCloneAbility());

        // Pillager
        register(new PillagerFangsAbility());
        register(new PillagerRavageAbility());
        register(new PillagerVindicatorBreakAbility());
        register(new PillagerVolleyAbility());
        register(new PillagerWarhornAbility());
        register(new PillagerSnareAbility());

        // Spy
        register(new SpyMimicFormAbility());
        register(new SpyEchoAbility());
        register(new SpyStealAbility());
        register(new SpySmokeBombAbility());
        register(new SpyStolenCastAbility());
        register(new SpySkinshiftAbility());

        // Beacon
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.SPEED));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.HASTE));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.RESISTANCE));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.JUMP));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.STRENGTH));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.REGEN));

        // Air
        register(new AirWindJumpAbility());
        register(new AirGaleSlamAbility());
        register(new AirCrosswindAbility());
        register(new AirDashAbility());

        // Duelist
        register(new DuelistLungeAbility());
        register(new DuelistParryAbility());
        register(new DuelistRapidStrikeAbility());
        register(new DuelistFlourishAbility());
        register(new DuelistMirrorMatchAbility());
        register(new DuelistBladeDanceAbility());

        // Hunter
        register(new HunterHuntingTrapAbility());
        register(new HunterPounceAbility());
        register(new HunterNetShotAbility());
        register(new HunterCripplingShotAbility());
        register(new HunterPackTacticsAbility());
        register(new HunterCallThePackAbility());
        register(new HunterOriginTrackingAbility());

        // Sentinel
        register(new SentinelShieldWallAbility());
        register(new SentinelTauntAbility());
        register(new SentinelInterventionAbility());
        register(new SentinelRallyCryAbility());
        register(new SentinelLockdownAbility());

        // Trickster
        register(new TricksterShadowSwapAbility());
        register(new TricksterMirageAbility());
        register(new TricksterGlitchStepAbility());
        register(new TricksterPuppetMasterAbility());
        register(new TricksterMindGamesAbility());

        // Bonus Pool Abilities (claimable at energy 10/10) - Original 20
        register(new BonusThunderstrikeAbility());
        register(new BonusFrostbiteAbility());
        register(new BonusEarthshatterAbility());
        register(new BonusShadowstepAbility());
        register(new BonusRadiantBurstAbility());
        register(new BonusVenomsprayAbility());
        register(new BonusTimewarpAbility());
        register(new BonusDecoyTrapAbility());
        register(new BonusGravityWellAbility());
        register(new BonusChainLightningAbility());
        register(new BonusMagmaPoolAbility());
        register(new BonusIceWallAbility());
        register(new BonusWindSlashAbility());
        register(new BonusCurseBoltAbility());
        register(new BonusBerserkerRageAbility());
        register(new BonusEtherealStepAbility());
        register(new BonusArcaneMissilesAbility());
        register(new BonusLifeTapAbility());
        register(new BonusDoomBoltAbility());
        register(new BonusSanctuaryAbility());
        
        // Bonus Pool Abilities - New 30
        register(new BonusSpectralChainsAbility());
        register(new BonusVoidRiftAbility());
        register(new BonusSoulLinkAbility());
        register(new BonusInfernoDashAbility());
        register(new BonusTidalWaveAbility());
        register(new BonusStarfallAbility());
        register(new BonusBloodlustAbility());
        register(new BonusCrystalCageAbility());
        register(new BonusPhantasmAbility());
        register(new BonusMirrorImageAbility());
        register(new BonusSonicBoomAbility());
        register(new BonusVampiricTouchAbility());
        register(new BonusBlindingFlashAbility());
        register(new BonusStormCallAbility());
        register(new BonusSmokeScreenAbility());
        register(new BonusThornsNovaAbility());
        register(new BonusQuicksandAbility());
        register(new BonusSearingLightAbility());
        register(new BonusSpectralBladeAbility());
        register(new BonusNetherPortalAbility());
        register(new BonusEntangleAbility());
        register(new BonusBlinkAbility());
        register(new BonusPurgeAbility());
        register(new BonusMindSpikeAbility());
        register(new BonusTremorAbility());
        register(new BonusIcicleBarrageAbility());
        register(new BonusBanishmentAbility());
        register(new BonusCorpseExplosionAbility());
        register(new BonusSoulSwapAbility());
        register(new BonusMarkOfDeathAbility());
        register(new BonusIronMaidenAbility());
        register(new BonusVulnerabilityAbility());
        register(new BonusReflectionWardAbility());
        register(new BonusWarpStrikeAbility());
        register(new BonusVortexStrikeAbility());
        register(new BonusPlagueCloudAbility());
        register(new BonusOverchargeAbility());
        register(new BonusGravityCrushAbility());
    }

    private ModAbilities() {
    }

    public static GemAbility get(Identifier id) {
        return ABILITIES.get(id);
    }

    public static Map<Identifier, GemAbility> all() {
        return Collections.unmodifiableMap(ABILITIES);
    }

    public static void override(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }

    /**
     * Find which gem an ability belongs to.
     * Returns null if the ability is not part of any gem (e.g., bonus pool abilities).
     */
    public static GemId findGemForAbility(Identifier abilityId) {
        // Check special gems first
        for (GemId specialGem : new GemId[]{GemId.VOID, GemId.CHAOS, GemId.PRISM}) {
            GemDefinition def = GemRegistry.definition(specialGem);
            if (def.abilities().contains(abilityId)) {
                return specialGem;
            }
        }

        // Check standard gems
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.VOID || gemId == GemId.CHAOS || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                if (def.abilities().contains(abilityId)) {
                    return gemId;
                }
            } catch (IllegalArgumentException e) {
                // Gem not registered, skip
            }
        }

        return null; // Not found (might be a bonus pool ability)
    }

    private static void register(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }
}
