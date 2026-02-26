package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;




public final class GemsBalance {
    public static final int TICKS_PER_SECOND = 20;

    private static volatile Values VALUES = Values.defaults();

    private GemsBalance() {
    }

    public static Values v() {
        return VALUES;
    }

    public static void init() {
        GemsConfigManager.LoadResult load = GemsConfigManager.loadOrCreateWithFallback();
        apply(load.config());
    }

    static void apply(GemsBalanceConfig cfg) {
        Values next = Values.from(cfg);
        var warnings = GemsPerformanceBudget.validate(next);
        for (String warning : warnings) {
            GemsMod.LOGGER.warn("[balance] {}", warning);
        }
        VALUES = next;
    }

    public record ReloadResult(boolean applied, GemsConfigManager.LoadResult loadResult) {
    }

    public static ReloadResult reloadFromDisk() {
        GemsConfigManager.LoadResult load = GemsConfigManager.loadOrCreateStrict();
        if (load.status() == GemsConfigManager.LoadStatus.ERROR || load.config() == null) {
            return new ReloadResult(false, load);
        }
        apply(load.config());
        return new ReloadResult(true, load);
    }

    public static java.nio.file.Path dumpEffectiveBalance() {
        GemsBalanceConfig cfg = toConfig(VALUES);
        java.nio.file.Path out = GemsConfigManager.resolveInConfigDir("balance.effective.json");
        return GemsConfigManager.write(out, cfg) ? out : null;
    }

    private static GemsBalanceConfig toConfig(Values v) {
        GemsBalanceConfig cfg = new GemsBalanceConfig();

        cfg.visual.enableParticles = v.visual().enableParticles();
        cfg.visual.enableSounds = v.visual().enableSounds();
        cfg.visual.particleScalePercent = v.visual().particleScalePercent();
        cfg.visual.maxParticlesPerCall = v.visual().maxParticlesPerCall();
        cfg.visual.maxBeamSteps = v.visual().maxBeamSteps();
        cfg.visual.maxRingPoints = v.visual().maxRingPoints();
        cfg.systems.minMaxHearts = v.systems().minMaxHearts();
        cfg.systems.assassinTriggerHearts = v.systems().assassinTriggerHearts();
        cfg.systems.assassinMaxHearts = v.systems().assassinMaxHearts();
        cfg.systems.assassinEliminationHeartsThreshold = v.systems().assassinEliminationHeartsThreshold();
        cfg.systems.assassinVsAssassinVictimHeartsLoss = v.systems().assassinVsAssassinVictimHeartsLoss();
        cfg.systems.assassinVsAssassinKillerHeartsGain = v.systems().assassinVsAssassinKillerHeartsGain();
        cfg.systems.controlledFollowStartBlocks = v.systems().controlledFollowStartBlocks();
        cfg.systems.controlledFollowStopBlocks = v.systems().controlledFollowStopBlocks();
        cfg.systems.controlledFollowSpeed = v.systems().controlledFollowSpeed();

        cfg.astra.shadowAnchorWindowSeconds = ticksToSeconds(v.astra().shadowAnchorWindowTicks());
        cfg.astra.shadowAnchorPostCooldownSeconds = ticksToSeconds(v.astra().shadowAnchorPostCooldownTicks());
        cfg.astra.dimensionalVoidCooldownSeconds = ticksToSeconds(v.astra().dimensionalVoidCooldownTicks());
        cfg.astra.dimensionalVoidDurationSeconds = ticksToSeconds(v.astra().dimensionalVoidDurationTicks());
        cfg.astra.dimensionalVoidRadiusBlocks = v.astra().dimensionalVoidRadiusBlocks();
        cfg.astra.astralDaggersCooldownSeconds = ticksToSeconds(v.astra().astralDaggersCooldownTicks());
        cfg.astra.astralDaggersCount = v.astra().astralDaggersCount();
        cfg.astra.astralDaggersDamage = v.astra().astralDaggersDamage();
        cfg.astra.astralDaggersVelocity = v.astra().astralDaggersVelocity();
        cfg.astra.astralDaggersSpread = v.astra().astralDaggersSpread();
        cfg.astra.unboundedCooldownSeconds = ticksToSeconds(v.astra().unboundedCooldownTicks());
        cfg.astra.unboundedDurationSeconds = ticksToSeconds(v.astra().unboundedDurationTicks());
        cfg.astra.astralCameraCooldownSeconds = ticksToSeconds(v.astra().astralCameraCooldownTicks());
        cfg.astra.astralCameraDurationSeconds = ticksToSeconds(v.astra().astralCameraDurationTicks());
        cfg.astra.spookCooldownSeconds = ticksToSeconds(v.astra().spookCooldownTicks());
        cfg.astra.spookRadiusBlocks = v.astra().spookRadiusBlocks();
        cfg.astra.spookDurationSeconds = ticksToSeconds(v.astra().spookDurationTicks());
        cfg.astra.tagCooldownSeconds = ticksToSeconds(v.astra().tagCooldownTicks());
        cfg.astra.tagRangeBlocks = v.astra().tagRangeBlocks();
        cfg.astra.tagDurationSeconds = ticksToSeconds(v.astra().tagDurationTicks());
        cfg.astra.soulHealingHearts = v.astra().soulHealingHearts();
        cfg.astra.soulReleaseForwardBlocks = v.astra().soulReleaseForwardBlocks();
        cfg.astra.soulReleaseUpBlocks = v.astra().soulReleaseUpBlocks();

        cfg.fire.cosyCampfireCooldownSeconds = ticksToSeconds(v.fire().cosyCampfireCooldownTicks());
        cfg.fire.cosyCampfireDurationSeconds = ticksToSeconds(v.fire().cosyCampfireDurationTicks());
        cfg.fire.cosyCampfireRadiusBlocks = v.fire().cosyCampfireRadiusBlocks();
        cfg.fire.cosyCampfireRegenAmplifier = v.fire().cosyCampfireRegenAmplifier();
        cfg.fire.heatHazeCooldownSeconds = ticksToSeconds(v.fire().heatHazeCooldownTicks());
        cfg.fire.heatHazeDurationSeconds = ticksToSeconds(v.fire().heatHazeDurationTicks());
        cfg.fire.heatHazeRadiusBlocks = v.fire().heatHazeRadiusBlocks();
        cfg.fire.heatHazeEnemyMiningFatigueAmplifier = v.fire().heatHazeEnemyMiningFatigueAmplifier();
        cfg.fire.heatHazeEnemyWeaknessAmplifier = v.fire().heatHazeEnemyWeaknessAmplifier();
        cfg.fire.fireballChargeUpSeconds = ticksToSeconds(v.fire().fireballChargeUpTicks());
        cfg.fire.fireballChargeDownSeconds = ticksToSeconds(v.fire().fireballChargeDownTicks());
        cfg.fire.fireballInternalCooldownSeconds = ticksToSeconds(v.fire().fireballInternalCooldownTicks());
        cfg.fire.fireballMaxDistanceBlocks = v.fire().fireballMaxDistanceBlocks();
        cfg.fire.meteorShowerCooldownSeconds = ticksToSeconds(v.fire().meteorShowerCooldownTicks());
        cfg.fire.meteorShowerTargetRangeBlocks = v.fire().meteorShowerTargetRangeBlocks();
        cfg.fire.meteorShowerCount = v.fire().meteorShowerCount();
        cfg.fire.meteorShowerSpreadBlocks = v.fire().meteorShowerSpreadBlocks();
        cfg.fire.meteorShowerHeightBlocks = v.fire().meteorShowerHeightBlocks();
        cfg.fire.meteorShowerVelocity = v.fire().meteorShowerVelocity();
        cfg.fire.meteorShowerExplosionPower = v.fire().meteorShowerExplosionPower();

        cfg.flux.fluxBeamCooldownSeconds = ticksToSeconds(v.flux().fluxBeamCooldownTicks());
        cfg.flux.fluxBeamRangeBlocks = v.flux().fluxBeamRangeBlocks();
        cfg.flux.fluxBeamMinDamage = v.flux().fluxBeamMinDamage();
        cfg.flux.fluxBeamMaxDamageAt100 = v.flux().fluxBeamMaxDamageAt100();
        cfg.flux.fluxBeamMaxDamageAt200 = v.flux().fluxBeamMaxDamageAt200();
        cfg.flux.fluxBeamArmorDamageAt100 = v.flux().fluxBeamArmorDamageAt100();
        cfg.flux.fluxBeamArmorDamagePerPercent = v.flux().fluxBeamArmorDamagePerPercent();
        cfg.flux.staticBurstCooldownSeconds = ticksToSeconds(v.flux().staticBurstCooldownTicks());
        cfg.flux.staticBurstRadiusBlocks = v.flux().staticBurstRadiusBlocks();
        cfg.flux.staticBurstMaxDamage = v.flux().staticBurstMaxDamage();
        cfg.flux.staticBurstStoreWindowSeconds = ticksToSeconds(v.flux().staticBurstStoreWindowTicks());
        cfg.flux.chargeDiamondBlock = v.flux().chargeDiamondBlock();
        cfg.flux.chargeGoldBlock = v.flux().chargeGoldBlock();
        cfg.flux.chargeCopperBlock = v.flux().chargeCopperBlock();
        cfg.flux.chargeEmeraldBlock = v.flux().chargeEmeraldBlock();
        cfg.flux.chargeAmethystBlock = v.flux().chargeAmethystBlock();
        cfg.flux.chargeNetheriteScrap = v.flux().chargeNetheriteScrap();
        cfg.flux.chargeEnchantedDiamondItem = v.flux().chargeEnchantedDiamondItem();
        cfg.flux.overchargeDelaySeconds = ticksToSeconds(v.flux().overchargeDelayTicks());
        cfg.flux.overchargePerSecond = v.flux().overchargePerSecond();
        cfg.flux.overchargeSelfDamagePerSecond = v.flux().overchargeSelfDamagePerSecond();
        cfg.flux.fluxCapacitorChargeThreshold = v.flux().fluxCapacitorChargeThreshold();
        cfg.flux.fluxCapacitorAbsorptionAmplifier = v.flux().fluxCapacitorAbsorptionAmplifier();
        cfg.flux.fluxConductivityChargePerDamage = v.flux().fluxConductivityChargePerDamage();
        cfg.flux.fluxConductivityMaxChargePerHit = v.flux().fluxConductivityMaxChargePerHit();
        cfg.flux.fluxInsulationChargeThreshold = v.flux().fluxInsulationChargeThreshold();
        cfg.flux.fluxInsulationDamageMultiplier = v.flux().fluxInsulationDamageMultiplier();
        cfg.flux.fluxSurgeCooldownSeconds = ticksToSeconds(v.flux().fluxSurgeCooldownTicks());
        cfg.flux.fluxSurgeDurationSeconds = ticksToSeconds(v.flux().fluxSurgeDurationTicks());
        cfg.flux.fluxSurgeSpeedAmplifier = v.flux().fluxSurgeSpeedAmplifier();
        cfg.flux.fluxSurgeResistanceAmplifier = v.flux().fluxSurgeResistanceAmplifier();
        cfg.flux.fluxSurgeChargeCost = v.flux().fluxSurgeChargeCost();
        cfg.flux.fluxSurgeRadiusBlocks = v.flux().fluxSurgeRadiusBlocks();
        cfg.flux.fluxSurgeKnockback = v.flux().fluxSurgeKnockback();
        cfg.flux.fluxDischargeCooldownSeconds = ticksToSeconds(v.flux().fluxDischargeCooldownTicks());
        cfg.flux.fluxDischargeRadiusBlocks = v.flux().fluxDischargeRadiusBlocks();
        cfg.flux.fluxDischargeBaseDamage = v.flux().fluxDischargeBaseDamage();
        cfg.flux.fluxDischargeDamagePerCharge = v.flux().fluxDischargeDamagePerCharge();
        cfg.flux.fluxDischargeMaxDamage = v.flux().fluxDischargeMaxDamage();
        cfg.flux.fluxDischargeMinCharge = v.flux().fluxDischargeMinCharge();
        cfg.flux.fluxDischargeKnockback = v.flux().fluxDischargeKnockback();

        cfg.life.vitalityVortexCooldownSeconds = ticksToSeconds(v.life().vitalityVortexCooldownTicks());
        cfg.life.vitalityVortexRadiusBlocks = v.life().vitalityVortexRadiusBlocks();
        cfg.life.vitalityVortexDurationSeconds = ticksToSeconds(v.life().vitalityVortexDurationTicks());
        cfg.life.vitalityVortexScanRadiusBlocks = v.life().vitalityVortexScanRadiusBlocks();
        cfg.life.vitalityVortexVerdantThreshold = v.life().vitalityVortexVerdantThreshold();
        cfg.life.vitalityVortexAllyHeal = v.life().vitalityVortexAllyHeal();
        cfg.life.healthDrainCooldownSeconds = ticksToSeconds(v.life().healthDrainCooldownTicks());
        cfg.life.healthDrainRangeBlocks = v.life().healthDrainRangeBlocks();
        cfg.life.healthDrainAmount = v.life().healthDrainAmount();
        cfg.life.lifeSwapCooldownSeconds = ticksToSeconds(v.life().lifeSwapCooldownTicks());
        cfg.life.lifeSwapRangeBlocks = v.life().lifeSwapRangeBlocks();
        cfg.life.lifeSwapMinHearts = v.life().lifeSwapMinHearts();
        cfg.life.lifeCircleCooldownSeconds = ticksToSeconds(v.life().lifeCircleCooldownTicks());
        cfg.life.lifeCircleDurationSeconds = ticksToSeconds(v.life().lifeCircleDurationTicks());
        cfg.life.lifeCircleRadiusBlocks = v.life().lifeCircleRadiusBlocks();
        cfg.life.lifeCircleMaxHealthDelta = v.life().lifeCircleMaxHealthDelta();
        cfg.life.heartLockCooldownSeconds = ticksToSeconds(v.life().heartLockCooldownTicks());
        cfg.life.heartLockDurationSeconds = ticksToSeconds(v.life().heartLockDurationTicks());
        cfg.life.heartLockRangeBlocks = v.life().heartLockRangeBlocks();

        cfg.puff.doubleJumpCooldownSeconds = ticksToSeconds(v.puff().doubleJumpCooldownTicks());
        cfg.puff.doubleJumpVelocityY = v.puff().doubleJumpVelocityY();
        cfg.puff.dashCooldownSeconds = ticksToSeconds(v.puff().dashCooldownTicks());
        cfg.puff.dashVelocity = v.puff().dashVelocity();
        cfg.puff.dashDamage = v.puff().dashDamage();
        cfg.puff.dashHitRangeBlocks = v.puff().dashHitRangeBlocks();
        cfg.puff.breezyBashCooldownSeconds = ticksToSeconds(v.puff().breezyBashCooldownTicks());
        cfg.puff.breezyBashRangeBlocks = v.puff().breezyBashRangeBlocks();
        cfg.puff.breezyBashUpVelocityY = v.puff().breezyBashUpVelocityY();
        cfg.puff.breezyBashKnockback = v.puff().breezyBashKnockback();
        cfg.puff.breezyBashInitialDamage = v.puff().breezyBashInitialDamage();
        cfg.puff.breezyBashImpactDamage = v.puff().breezyBashImpactDamage();
        cfg.puff.breezyBashImpactWindowSeconds = ticksToSeconds(v.puff().breezyBashImpactWindowTicks());
        cfg.puff.groupBashCooldownSeconds = ticksToSeconds(v.puff().groupBashCooldownTicks());
        cfg.puff.groupBashRadiusBlocks = v.puff().groupBashRadiusBlocks();
        cfg.puff.groupBashKnockback = v.puff().groupBashKnockback();
        cfg.puff.groupBashUpVelocityY = v.puff().groupBashUpVelocityY();
        cfg.puff.gustCooldownSeconds = ticksToSeconds(v.puff().gustCooldownTicks());
        cfg.puff.gustRadiusBlocks = v.puff().gustRadiusBlocks();
        cfg.puff.gustUpVelocityY = v.puff().gustUpVelocityY();
        cfg.puff.gustKnockback = v.puff().gustKnockback();
        cfg.puff.gustSlownessDurationSeconds = ticksToSeconds(v.puff().gustSlownessDurationTicks());
        cfg.puff.gustSlownessAmplifier = v.puff().gustSlownessAmplifier();
        cfg.puff.gustSlowFallingDurationSeconds = ticksToSeconds(v.puff().gustSlowFallingDurationTicks());
        cfg.puff.windborneDurationSeconds = ticksToSeconds(v.puff().windborneDurationTicks());
        cfg.puff.windborneSlowFallingAmplifier = v.puff().windborneSlowFallingAmplifier();

        cfg.speed.momentumMinSpeed = v.speed().momentumMinSpeed();
        cfg.speed.momentumMaxSpeed = v.speed().momentumMaxSpeed();
        cfg.speed.momentumMinMultiplier = v.speed().momentumMinMultiplier();
        cfg.speed.momentumMaxMultiplier = v.speed().momentumMaxMultiplier();
        cfg.speed.frictionlessSpeedAmplifier = v.speed().frictionlessSpeedAmplifier();

        cfg.speed.arcShotCooldownSeconds = ticksToSeconds(v.speed().arcShotCooldownTicks());
        cfg.speed.arcShotRangeBlocks = v.speed().arcShotRangeBlocks();
        cfg.speed.arcShotRadiusBlocks = v.speed().arcShotRadiusBlocks();
        cfg.speed.arcShotMaxTargets = v.speed().arcShotMaxTargets();
        cfg.speed.arcShotDamage = v.speed().arcShotDamage();
        cfg.speed.speedStormCooldownSeconds = ticksToSeconds(v.speed().speedStormCooldownTicks());
        cfg.speed.speedStormDurationSeconds = ticksToSeconds(v.speed().speedStormDurationTicks());
        cfg.speed.speedStormRadiusBlocks = v.speed().speedStormRadiusBlocks();
        cfg.speed.speedStormAllySpeedAmplifier = v.speed().speedStormAllySpeedAmplifier();
        cfg.speed.speedStormAllyHasteAmplifier = v.speed().speedStormAllyHasteAmplifier();
        cfg.speed.speedStormEnemySlownessAmplifier = v.speed().speedStormEnemySlownessAmplifier();
        cfg.speed.speedStormEnemyMiningFatigueAmplifier = v.speed().speedStormEnemyMiningFatigueAmplifier();
        cfg.speed.terminalVelocityCooldownSeconds = ticksToSeconds(v.speed().terminalVelocityCooldownTicks());
        cfg.speed.terminalVelocityDurationSeconds = ticksToSeconds(v.speed().terminalVelocityDurationTicks());
        cfg.speed.terminalVelocitySpeedAmplifier = v.speed().terminalVelocitySpeedAmplifier();
        cfg.speed.terminalVelocityHasteAmplifier = v.speed().terminalVelocityHasteAmplifier();

        cfg.speed.slipstreamCooldownSeconds = ticksToSeconds(v.speed().slipstreamCooldownTicks());
        cfg.speed.slipstreamDurationSeconds = ticksToSeconds(v.speed().slipstreamDurationTicks());
        cfg.speed.slipstreamLengthBlocks = v.speed().slipstreamLengthBlocks();
        cfg.speed.slipstreamRadiusBlocks = v.speed().slipstreamRadiusBlocks();
        cfg.speed.slipstreamAllySpeedAmplifier = v.speed().slipstreamAllySpeedAmplifier();
        cfg.speed.slipstreamEnemySlownessAmplifier = v.speed().slipstreamEnemySlownessAmplifier();
        cfg.speed.slipstreamEnemyKnockback = v.speed().slipstreamEnemyKnockback();

        cfg.speed.afterimageCooldownSeconds = ticksToSeconds(v.speed().afterimageCooldownTicks());
        cfg.speed.afterimageDurationSeconds = ticksToSeconds(v.speed().afterimageDurationTicks());
        cfg.speed.afterimageSpeedAmplifier = v.speed().afterimageSpeedAmplifier();
        cfg.speed.tempoShiftCooldownSeconds = ticksToSeconds(v.speed().tempoShiftCooldownTicks());
        cfg.speed.tempoShiftDurationSeconds = ticksToSeconds(v.speed().tempoShiftDurationTicks());
        cfg.speed.tempoShiftRadiusBlocks = v.speed().tempoShiftRadiusBlocks();
        cfg.speed.tempoShiftAllyCooldownTicksPerSecond = v.speed().tempoShiftAllyCooldownTicksPerSecond();
        cfg.speed.tempoShiftEnemyCooldownTicksPerSecond = v.speed().tempoShiftEnemyCooldownTicksPerSecond();
        cfg.speed.autoStepCooldownSeconds = ticksToSeconds(v.speed().autoStepCooldownTicks());
        cfg.speed.autoStepHeightBonus = v.speed().autoStepHeightBonus();

        cfg.strength.nullifyCooldownSeconds = ticksToSeconds(v.strength().nullifyCooldownTicks());
        cfg.strength.nullifyRadiusBlocks = v.strength().nullifyRadiusBlocks();
        cfg.strength.frailerCooldownSeconds = ticksToSeconds(v.strength().frailerCooldownTicks());
        cfg.strength.frailerRangeBlocks = v.strength().frailerRangeBlocks();
        cfg.strength.frailerDurationSeconds = ticksToSeconds(v.strength().frailerDurationTicks());
        cfg.strength.bountyCooldownSeconds = ticksToSeconds(v.strength().bountyCooldownTicks());
        cfg.strength.bountyDurationSeconds = ticksToSeconds(v.strength().bountyDurationTicks());
        cfg.strength.chadCooldownSeconds = ticksToSeconds(v.strength().chadCooldownTicks());
        cfg.strength.chadDurationSeconds = ticksToSeconds(v.strength().chadDurationTicks());
        cfg.strength.chadEveryHits = v.strength().chadEveryHits();
        cfg.strength.chadBonusDamage = v.strength().chadBonusDamage();
        cfg.strength.adrenalineThresholdHearts = v.strength().adrenalineThresholdHearts();
        cfg.strength.adrenalineDurationSeconds = ticksToSeconds(v.strength().adrenalineDurationTicks());
        cfg.strength.adrenalineResistanceAmplifier = v.strength().adrenalineResistanceAmplifier();

        cfg.wealth.fumbleCooldownSeconds = ticksToSeconds(v.wealth().fumbleCooldownTicks());
        cfg.wealth.fumbleDurationSeconds = ticksToSeconds(v.wealth().fumbleDurationTicks());
        cfg.wealth.fumbleRadiusBlocks = v.wealth().fumbleRadiusBlocks();
        cfg.wealth.hotbarLockCooldownSeconds = ticksToSeconds(v.wealth().hotbarLockCooldownTicks());
        cfg.wealth.hotbarLockDurationSeconds = ticksToSeconds(v.wealth().hotbarLockDurationTicks());
        cfg.wealth.hotbarLockRangeBlocks = v.wealth().hotbarLockRangeBlocks();
        cfg.wealth.amplificationCooldownSeconds = ticksToSeconds(v.wealth().amplificationCooldownTicks());
        cfg.wealth.amplificationDurationSeconds = ticksToSeconds(v.wealth().amplificationDurationTicks());
        cfg.wealth.amplificationBonusLevels = v.wealth().amplificationBonusLevels();
        cfg.wealth.richRushCooldownSeconds = ticksToSeconds(v.wealth().richRushCooldownTicks());
        cfg.wealth.richRushDurationSeconds = ticksToSeconds(v.wealth().richRushDurationTicks());
        cfg.wealth.richRushLootRolls = v.wealth().richRushLootRolls();
        cfg.wealth.richRushWitherSkullGuarantee = v.wealth().richRushWitherSkullGuarantee();
        cfg.wealth.richRushShulkerShellGuarantee = v.wealth().richRushShulkerShellGuarantee();
        cfg.wealth.richRushNetherStarGuarantee = v.wealth().richRushNetherStarGuarantee();
        cfg.wealth.pocketsRows = v.wealth().pocketsRows();

        cfg.terror.dreadAuraRadiusBlocks = v.terror().dreadAuraRadiusBlocks();
        cfg.terror.dreadAuraAmplifier = v.terror().dreadAuraAmplifier();
        cfg.terror.bloodPriceDurationSeconds = ticksToSeconds(v.terror().bloodPriceDurationTicks());
        cfg.terror.bloodPriceStrengthAmplifier = v.terror().bloodPriceStrengthAmplifier();
        cfg.terror.bloodPriceResistanceAmplifier = v.terror().bloodPriceResistanceAmplifier();
        cfg.terror.terrorTradeCooldownSeconds = ticksToSeconds(v.terror().terrorTradeCooldownTicks());
        cfg.terror.terrorTradeRangeBlocks = v.terror().terrorTradeRangeBlocks();
        cfg.terror.terrorTradeMaxUses = v.terror().terrorTradeMaxUses();
        cfg.terror.terrorTradeHeartsCost = v.terror().terrorTradeHeartsCost();
        cfg.terror.terrorTradePermanentEnergyPenalty = v.terror().terrorTradePermanentEnergyPenalty();
        cfg.terror.terrorTradeNormalTargetHeartsPenalty = v.terror().terrorTradeNormalTargetHeartsPenalty();
        cfg.terror.terrorTradeNormalTargetEnergyPenalty = v.terror().terrorTradeNormalTargetEnergyPenalty();
        cfg.terror.panicRingCooldownSeconds = ticksToSeconds(v.terror().panicRingCooldownTicks());
        cfg.terror.panicRingTntCount = v.terror().panicRingTntCount();
        cfg.terror.panicRingFuseTicks = v.terror().panicRingFuseTicks();
        cfg.terror.panicRingRadiusBlocks = v.terror().panicRingRadiusBlocks();
        cfg.terror.rigCooldownSeconds = ticksToSeconds(v.terror().rigCooldownTicks());
        cfg.terror.rigRangeBlocks = v.terror().rigRangeBlocks();
        cfg.terror.rigDurationSeconds = ticksToSeconds(v.terror().rigDurationTicks());
        cfg.terror.rigFuseTicks = v.terror().rigFuseTicks();
        cfg.terror.rigTntCount = v.terror().rigTntCount();
        cfg.terror.remoteChargeArmWindowSeconds = ticksToSeconds(v.terror().remoteChargeArmWindowTicks());
        cfg.terror.remoteChargeDetonateWindowSeconds = ticksToSeconds(v.terror().remoteChargeDetonateWindowTicks());
        cfg.terror.remoteChargeFuseTicks = v.terror().remoteChargeFuseTicks();
        cfg.terror.remoteChargeCooldownSeconds = ticksToSeconds(v.terror().remoteChargeCooldownTicks());
        cfg.terror.breachChargeCooldownSeconds = ticksToSeconds(v.terror().breachChargeCooldownTicks());
        cfg.terror.breachChargeRangeBlocks = v.terror().breachChargeRangeBlocks();
        cfg.terror.breachChargeFuseTicks = v.terror().breachChargeFuseTicks();
        cfg.terror.breachChargeExplosionPower = v.terror().breachChargeExplosionPower();

        cfg.summoner.maxPoints = v.summoner().maxPoints();
        cfg.summoner.maxActiveSummons = v.summoner().maxActiveSummons();
        cfg.summoner.summonLifetimeSeconds = ticksToSeconds(v.summoner().summonLifetimeTicks());
        cfg.summoner.commandRangeBlocks = v.summoner().commandRangeBlocks();
        cfg.summoner.summonSlotCooldownSeconds = ticksToSeconds(v.summoner().summonSlotCooldownTicks());
        cfg.summoner.recallCooldownSeconds = ticksToSeconds(v.summoner().recallCooldownTicks());
        cfg.summoner.commandersMarkDurationSeconds = ticksToSeconds(v.summoner().commandersMarkDurationTicks());
        cfg.summoner.commandersMarkStrengthAmplifier = v.summoner().commandersMarkStrengthAmplifier();
        cfg.summoner.summonBonusHealth = v.summoner().summonBonusHealth();
        cfg.summoner.summonSpawnForwardBlocks = v.summoner().summonSpawnForwardBlocks();
        cfg.summoner.summonSpawnUpBlocks = v.summoner().summonSpawnUpBlocks();
        cfg.summoner.summonSpawnRingBaseBlocks = v.summoner().summonSpawnRingBaseBlocks();
        cfg.summoner.summonSpawnRingStepBlocks = v.summoner().summonSpawnRingStepBlocks();
        cfg.summoner.summonSpawnRingLayers = v.summoner().summonSpawnRingLayers();
        cfg.summoner.summonSpawnRingSegments = v.summoner().summonSpawnRingSegments();
        cfg.summoner.costs = v.summoner().costs();
        cfg.summoner.slot1 = v.summoner().slot1();
        cfg.summoner.slot2 = v.summoner().slot2();
        cfg.summoner.slot3 = v.summoner().slot3();
        cfg.summoner.slot4 = v.summoner().slot4();
        cfg.summoner.slot5 = v.summoner().slot5();

        cfg.space.lunarMinMultiplier = v.space().lunarMinMultiplier();
        cfg.space.lunarMaxMultiplier = v.space().lunarMaxMultiplier();
        cfg.space.starshieldProjectileDamageMultiplier = v.space().starshieldProjectileDamageMultiplier();
        cfg.space.orbitalLaserCooldownSeconds = ticksToSeconds(v.space().orbitalLaserCooldownTicks());
        cfg.space.orbitalLaserMiningCooldownSeconds = ticksToSeconds(v.space().orbitalLaserMiningCooldownTicks());
        cfg.space.orbitalLaserRangeBlocks = v.space().orbitalLaserRangeBlocks();
        cfg.space.orbitalLaserDelaySeconds = ticksToSeconds(v.space().orbitalLaserDelayTicks());
        cfg.space.orbitalLaserRadiusBlocks = v.space().orbitalLaserRadiusBlocks();
        cfg.space.orbitalLaserDamage = v.space().orbitalLaserDamage();
        cfg.space.orbitalLaserMiningRadiusBlocks = v.space().orbitalLaserMiningRadiusBlocks();
        cfg.space.orbitalLaserMiningHardnessCap = v.space().orbitalLaserMiningHardnessCap();
        cfg.space.orbitalLaserMiningMaxBlocks = v.space().orbitalLaserMiningMaxBlocks();
        cfg.space.gravityFieldCooldownSeconds = ticksToSeconds(v.space().gravityFieldCooldownTicks());
        cfg.space.gravityFieldDurationSeconds = ticksToSeconds(v.space().gravityFieldDurationTicks());
        cfg.space.gravityFieldRadiusBlocks = v.space().gravityFieldRadiusBlocks();
        cfg.space.gravityFieldAllyGravityMultiplier = v.space().gravityFieldAllyGravityMultiplier();
        cfg.space.gravityFieldEnemyGravityMultiplier = v.space().gravityFieldEnemyGravityMultiplier();
        cfg.space.blackHoleCooldownSeconds = ticksToSeconds(v.space().blackHoleCooldownTicks());
        cfg.space.blackHoleDurationSeconds = ticksToSeconds(v.space().blackHoleDurationTicks());
        cfg.space.blackHoleRadiusBlocks = v.space().blackHoleRadiusBlocks();
        cfg.space.blackHolePullStrength = v.space().blackHolePullStrength();
        cfg.space.blackHoleDamagePerSecond = v.space().blackHoleDamagePerSecond();
        cfg.space.whiteHoleCooldownSeconds = ticksToSeconds(v.space().whiteHoleCooldownTicks());
        cfg.space.whiteHoleDurationSeconds = ticksToSeconds(v.space().whiteHoleDurationTicks());
        cfg.space.whiteHoleRadiusBlocks = v.space().whiteHoleRadiusBlocks();
        cfg.space.whiteHolePushStrength = v.space().whiteHolePushStrength();
        cfg.space.whiteHoleDamagePerSecond = v.space().whiteHoleDamagePerSecond();

        cfg.reaper.undeadWardDamageMultiplier = v.reaper().undeadWardDamageMultiplier();
        cfg.reaper.harvestRegenDurationSeconds = ticksToSeconds(v.reaper().harvestRegenDurationTicks());
        cfg.reaper.harvestRegenAmplifier = v.reaper().harvestRegenAmplifier();
        cfg.reaper.graveSteedCooldownSeconds = ticksToSeconds(v.reaper().graveSteedCooldownTicks());
        cfg.reaper.graveSteedDurationSeconds = ticksToSeconds(v.reaper().graveSteedDurationTicks());
        cfg.reaper.graveSteedDecayDamagePerSecond = v.reaper().graveSteedDecayDamagePerSecond();
        cfg.reaper.witheringStrikesCooldownSeconds = ticksToSeconds(v.reaper().witheringStrikesCooldownTicks());
        cfg.reaper.witheringStrikesDurationSeconds = ticksToSeconds(v.reaper().witheringStrikesDurationTicks());
        cfg.reaper.witheringStrikesWitherDurationSeconds = ticksToSeconds(v.reaper().witheringStrikesWitherDurationTicks());
        cfg.reaper.witheringStrikesWitherAmplifier = v.reaper().witheringStrikesWitherAmplifier();
        cfg.reaper.deathOathCooldownSeconds = ticksToSeconds(v.reaper().deathOathCooldownTicks());
        cfg.reaper.deathOathDurationSeconds = ticksToSeconds(v.reaper().deathOathDurationTicks());
        cfg.reaper.deathOathRangeBlocks = v.reaper().deathOathRangeBlocks();
        cfg.reaper.deathOathSelfDamagePerSecond = v.reaper().deathOathSelfDamagePerSecond();
        cfg.reaper.deathOathBonusDamage = v.reaper().deathOathBonusDamage();
        cfg.reaper.retributionCooldownSeconds = ticksToSeconds(v.reaper().retributionCooldownTicks());
        cfg.reaper.retributionDurationSeconds = ticksToSeconds(v.reaper().retributionDurationTicks());
        cfg.reaper.retributionDamageMultiplier = v.reaper().retributionDamageMultiplier();
        cfg.reaper.scytheSweepCooldownSeconds = ticksToSeconds(v.reaper().scytheSweepCooldownTicks());
        cfg.reaper.scytheSweepRangeBlocks = v.reaper().scytheSweepRangeBlocks();
        cfg.reaper.scytheSweepArcDegrees = v.reaper().scytheSweepArcDegrees();
        cfg.reaper.scytheSweepDamage = v.reaper().scytheSweepDamage();
        cfg.reaper.scytheSweepKnockback = v.reaper().scytheSweepKnockback();
        cfg.reaper.bloodChargeCooldownSeconds = ticksToSeconds(v.reaper().bloodChargeCooldownTicks());
        cfg.reaper.bloodChargeMaxChargeSeconds = ticksToSeconds(v.reaper().bloodChargeMaxChargeTicks());
        cfg.reaper.bloodChargeSelfDamagePerSecond = v.reaper().bloodChargeSelfDamagePerSecond();
        cfg.reaper.bloodChargeMaxMultiplier = v.reaper().bloodChargeMaxMultiplier();
        cfg.reaper.bloodChargeBuffDurationSeconds = ticksToSeconds(v.reaper().bloodChargeBuffDurationTicks());
        cfg.reaper.shadowCloneCooldownSeconds = ticksToSeconds(v.reaper().shadowCloneCooldownTicks());
        cfg.reaper.shadowCloneDurationSeconds = ticksToSeconds(v.reaper().shadowCloneDurationTicks());
        cfg.reaper.shadowCloneMaxHealth = v.reaper().shadowCloneMaxHealth();
        cfg.reaper.shadowCloneCount = v.reaper().shadowCloneCount();
        cfg.reaper.shadowCloneEntityId = v.reaper().shadowCloneEntityId();

        cfg.pillager.raidersTrainingProjectileVelocityMultiplier = v.pillager().raidersTrainingProjectileVelocityMultiplier();
        cfg.pillager.shieldbreakerDisableCooldownTicks = v.pillager().shieldbreakerDisableCooldownTicks();
        cfg.pillager.illagerDisciplineThresholdHearts = v.pillager().illagerDisciplineThresholdHearts();
        cfg.pillager.illagerDisciplineResistanceDurationSeconds = ticksToSeconds(v.pillager().illagerDisciplineResistanceDurationTicks());
        cfg.pillager.illagerDisciplineResistanceAmplifier = v.pillager().illagerDisciplineResistanceAmplifier();
        cfg.pillager.illagerDisciplineCooldownSeconds = ticksToSeconds(v.pillager().illagerDisciplineCooldownTicks());
        cfg.pillager.fangsCooldownSeconds = ticksToSeconds(v.pillager().fangsCooldownTicks());
        cfg.pillager.fangsRangeBlocks = v.pillager().fangsRangeBlocks();
        cfg.pillager.fangsCount = v.pillager().fangsCount();
        cfg.pillager.fangsSpacingBlocks = v.pillager().fangsSpacingBlocks();
        cfg.pillager.fangsWarmupStepTicks = v.pillager().fangsWarmupStepTicks();
        cfg.pillager.ravageCooldownSeconds = ticksToSeconds(v.pillager().ravageCooldownTicks());
        cfg.pillager.ravageRangeBlocks = v.pillager().ravageRangeBlocks();
        cfg.pillager.ravageDamage = v.pillager().ravageDamage();
        cfg.pillager.ravageKnockback = v.pillager().ravageKnockback();
        cfg.pillager.vindicatorBreakCooldownSeconds = ticksToSeconds(v.pillager().vindicatorBreakCooldownTicks());
        cfg.pillager.vindicatorBreakDurationSeconds = ticksToSeconds(v.pillager().vindicatorBreakDurationTicks());
        cfg.pillager.vindicatorBreakStrengthAmplifier = v.pillager().vindicatorBreakStrengthAmplifier();
        cfg.pillager.vindicatorBreakShieldDisableCooldownTicks = v.pillager().vindicatorBreakShieldDisableCooldownTicks();
        cfg.pillager.volleyCooldownSeconds = ticksToSeconds(v.pillager().volleyCooldownTicks());
        cfg.pillager.volleyDurationSeconds = ticksToSeconds(v.pillager().volleyDurationTicks());
        cfg.pillager.volleyPeriodTicks = v.pillager().volleyPeriodTicks();
        cfg.pillager.volleyArrowsPerShot = v.pillager().volleyArrowsPerShot();
        cfg.pillager.volleyArrowDamage = v.pillager().volleyArrowDamage();
        cfg.pillager.volleyArrowVelocity = v.pillager().volleyArrowVelocity();
        cfg.pillager.volleyArrowInaccuracy = v.pillager().volleyArrowInaccuracy();
        cfg.pillager.warhornCooldownSeconds = ticksToSeconds(v.pillager().warhornCooldownTicks());
        cfg.pillager.warhornRadiusBlocks = v.pillager().warhornRadiusBlocks();
        cfg.pillager.warhornDurationSeconds = ticksToSeconds(v.pillager().warhornDurationTicks());
        cfg.pillager.warhornAllySpeedAmplifier = v.pillager().warhornAllySpeedAmplifier();
        cfg.pillager.warhornAllyResistanceAmplifier = v.pillager().warhornAllyResistanceAmplifier();
        cfg.pillager.warhornEnemySlownessAmplifier = v.pillager().warhornEnemySlownessAmplifier();
        cfg.pillager.warhornEnemyWeaknessAmplifier = v.pillager().warhornEnemyWeaknessAmplifier();
        cfg.pillager.snareCooldownSeconds = ticksToSeconds(v.pillager().snareCooldownTicks());
        cfg.pillager.snareRangeBlocks = v.pillager().snareRangeBlocks();
        cfg.pillager.snareDurationSeconds = ticksToSeconds(v.pillager().snareDurationTicks());
        cfg.pillager.snareSlownessAmplifier = v.pillager().snareSlownessAmplifier();

        cfg.spy.stillnessSeconds = ticksToSeconds(v.spy().stillnessTicks());
        cfg.spy.stillnessMoveEpsilonBlocks = v.spy().stillnessMoveEpsilonBlocks();
        cfg.spy.stillnessInvisRefreshSeconds = ticksToSeconds(v.spy().stillnessInvisRefreshTicks());
        cfg.spy.backstepCooldownSeconds = ticksToSeconds(v.spy().backstepCooldownTicks());
        cfg.spy.backstepVelocity = v.spy().backstepVelocity();
        cfg.spy.backstepUpVelocity = v.spy().backstepUpVelocity();
        cfg.spy.backstabBonusDamage = v.spy().backstabBonusDamage();
        cfg.spy.backstabAngleDegrees = v.spy().backstabAngleDegrees();
        cfg.spy.observeRangeBlocks = v.spy().observeRangeBlocks();
        cfg.spy.observeWindowSeconds = ticksToSeconds(v.spy().observeWindowTicks());
        cfg.spy.stealRequiredWitnessCount = v.spy().stealRequiredWitnessCount();
        cfg.spy.maxStolenAbilities = v.spy().maxStolenAbilities();
        cfg.spy.mimicFormCooldownSeconds = ticksToSeconds(v.spy().mimicFormCooldownTicks());
        cfg.spy.mimicFormDurationSeconds = ticksToSeconds(v.spy().mimicFormDurationTicks());
        cfg.spy.mimicFormBonusMaxHealth = v.spy().mimicFormBonusMaxHealth();
        cfg.spy.mimicFormSpeedMultiplier = v.spy().mimicFormSpeedMultiplier();
        cfg.spy.echoCooldownSeconds = ticksToSeconds(v.spy().echoCooldownTicks());
        cfg.spy.echoWindowSeconds = ticksToSeconds(v.spy().echoWindowTicks());
        cfg.spy.stealCooldownSeconds = ticksToSeconds(v.spy().stealCooldownTicks());
        cfg.spy.smokeBombCooldownSeconds = ticksToSeconds(v.spy().smokeBombCooldownTicks());
        cfg.spy.smokeBombRadiusBlocks = v.spy().smokeBombRadiusBlocks();
        cfg.spy.smokeBombDurationSeconds = ticksToSeconds(v.spy().smokeBombDurationTicks());
        cfg.spy.smokeBombBlindnessAmplifier = v.spy().smokeBombBlindnessAmplifier();
        cfg.spy.smokeBombSlownessAmplifier = v.spy().smokeBombSlownessAmplifier();
        cfg.spy.stolenCastCooldownSeconds = ticksToSeconds(v.spy().stolenCastCooldownTicks());
        cfg.spy.skinshiftCooldownSeconds = ticksToSeconds(v.spy().skinshiftCooldownTicks());
        cfg.spy.skinshiftDurationSeconds = ticksToSeconds(v.spy().skinshiftDurationTicks());
        cfg.spy.skinshiftRangeBlocks = v.spy().skinshiftRangeBlocks();

        cfg.beacon.coreRadiusBlocks = v.beacon().coreRadiusBlocks();
        cfg.beacon.corePulsePeriodSeconds = ticksToSeconds(v.beacon().corePulsePeriodTicks());
        cfg.beacon.coreRegenDurationSeconds = ticksToSeconds(v.beacon().coreRegenDurationTicks());
        cfg.beacon.coreRegenAmplifier = v.beacon().coreRegenAmplifier();
        cfg.beacon.stabilizeRadiusBlocks = v.beacon().stabilizeRadiusBlocks();
        cfg.beacon.stabilizeReduceTicksPerSecond = v.beacon().stabilizeReduceTicksPerSecond();
        cfg.beacon.rallyRadiusBlocks = v.beacon().rallyRadiusBlocks();
        cfg.beacon.rallyAbsorptionHearts = v.beacon().rallyAbsorptionHearts();
        cfg.beacon.rallyDurationSeconds = ticksToSeconds(v.beacon().rallyDurationTicks());
        cfg.beacon.auraCooldownSeconds = ticksToSeconds(v.beacon().auraCooldownTicks());
        cfg.beacon.auraDurationSeconds = ticksToSeconds(v.beacon().auraDurationTicks());
        cfg.beacon.auraRadiusBlocks = v.beacon().auraRadiusBlocks();
        cfg.beacon.auraRefreshSeconds = ticksToSeconds(v.beacon().auraRefreshTicks());
        cfg.beacon.auraSpeedAmplifier = v.beacon().auraSpeedAmplifier();
        cfg.beacon.auraHasteAmplifier = v.beacon().auraHasteAmplifier();
        cfg.beacon.auraResistanceAmplifier = v.beacon().auraResistanceAmplifier();
        cfg.beacon.auraJumpAmplifier = v.beacon().auraJumpAmplifier();
        cfg.beacon.auraStrengthAmplifier = v.beacon().auraStrengthAmplifier();
        cfg.beacon.auraRegenAmplifier = v.beacon().auraRegenAmplifier();

        cfg.air.aerialGuardFallDamageMultiplier = v.air().aerialGuardFallDamageMultiplier();
        cfg.air.aerialGuardDamageMultiplier = v.air().aerialGuardDamageMultiplier();
        cfg.air.aerialGuardKnockbackMultiplier = v.air().aerialGuardKnockbackMultiplier();
        cfg.air.windShearKnockback = v.air().windShearKnockback();
        cfg.air.windShearSlownessDurationSeconds = ticksToSeconds(v.air().windShearSlownessDurationTicks());
        cfg.air.windShearSlownessAmplifier = v.air().windShearSlownessAmplifier();
        cfg.air.windJumpCooldownSeconds = ticksToSeconds(v.air().windJumpCooldownTicks());
        cfg.air.windJumpVerticalVelocity = v.air().windJumpVerticalVelocity();
        cfg.air.windJumpForwardVelocity = v.air().windJumpForwardVelocity();
        cfg.air.galeSlamCooldownSeconds = ticksToSeconds(v.air().galeSlamCooldownTicks());
        cfg.air.galeSlamWindowSeconds = ticksToSeconds(v.air().galeSlamWindowTicks());
        cfg.air.galeSlamRadiusBlocks = v.air().galeSlamRadiusBlocks();
        cfg.air.galeSlamBonusDamage = v.air().galeSlamBonusDamage();
        cfg.air.galeSlamKnockback = v.air().galeSlamKnockback();
        cfg.air.crosswindCooldownSeconds = ticksToSeconds(v.air().crosswindCooldownTicks());
        cfg.air.crosswindRangeBlocks = v.air().crosswindRangeBlocks();
        cfg.air.crosswindRadiusBlocks = v.air().crosswindRadiusBlocks();
        cfg.air.crosswindDamage = v.air().crosswindDamage();
        cfg.air.crosswindKnockback = v.air().crosswindKnockback();
        cfg.air.crosswindSlownessDurationSeconds = ticksToSeconds(v.air().crosswindSlownessDurationTicks());
        cfg.air.crosswindSlownessAmplifier = v.air().crosswindSlownessAmplifier();
        cfg.air.dashCooldownSeconds = ticksToSeconds(v.air().dashCooldownTicks());
        cfg.air.dashVelocity = v.air().dashVelocity();
        cfg.air.dashUpVelocity = v.air().dashUpVelocity();
        cfg.air.dashIFrameDurationSeconds = ticksToSeconds(v.air().dashIFrameDurationTicks());
        cfg.air.dashIFrameResistanceAmplifier = v.air().dashIFrameResistanceAmplifier();
        cfg.air.airMaceBreachLevel = v.air().airMaceBreachLevel();
        cfg.air.airMaceWindBurstLevel = v.air().airMaceWindBurstLevel();
        cfg.air.airMaceMendingLevel = v.air().airMaceMendingLevel();
        cfg.air.airMaceUnbreakingLevel = v.air().airMaceUnbreakingLevel();
        cfg.air.airMaceFireAspectLevel = v.air().airMaceFireAspectLevel();

        cfg.voidGem.blockAllStatusEffects = v.voidGem().blockAllStatusEffects();
        cfg.chaos.slotDurationSeconds = ticksToSeconds(v.chaos().slotDurationTicks());
        cfg.chaos.slotAbilityCooldownSeconds = ticksToSeconds(v.chaos().slotAbilityCooldownTicks());
        cfg.chaos.slotCount = v.chaos().slotCount();
        cfg.prism.maxGemAbilities = v.prism().maxGemAbilities();
        cfg.prism.maxGemPassives = v.prism().maxGemPassives();

        cfg.legendary.craftSeconds = ticksToSeconds(v.legendary().craftTicks());
        cfg.legendary.craftMaxPerItem = v.legendary().craftMaxPerItem();
        cfg.legendary.craftMaxActivePerItem = v.legendary().craftMaxActivePerItem();
        cfg.legendary.trackerRefreshSeconds = ticksToSeconds(v.legendary().trackerRefreshTicks());
        cfg.legendary.trackerMaxDistanceBlocks = v.legendary().trackerMaxDistanceBlocks();
        cfg.legendary.recallCooldownSeconds = ticksToSeconds(v.legendary().recallCooldownTicks());
        cfg.legendary.chronoCharmCooldownMultiplier = v.legendary().chronoCharmCooldownMultiplier();
        cfg.legendary.hypnoHoldSeconds = ticksToSeconds(v.legendary().hypnoHoldTicks());
        cfg.legendary.hypnoRangeBlocks = v.legendary().hypnoRangeBlocks();
        cfg.legendary.hypnoViewRangeBlocks = v.legendary().hypnoViewRangeBlocks();
        cfg.legendary.hypnoHealHearts = v.legendary().hypnoHealHearts();
        cfg.legendary.hypnoMaxControlled = v.legendary().hypnoMaxControlled();
        cfg.legendary.hypnoDurationSeconds = ticksToSeconds(v.legendary().hypnoDurationTicks());
        cfg.legendary.earthsplitterRadiusBlocks = v.legendary().earthsplitterRadiusBlocks();
        cfg.legendary.earthsplitterTunnelLengthBlocks = v.legendary().earthsplitterTunnelLengthBlocks();
        cfg.legendary.bloodOathSharpnessCap = v.legendary().bloodOathSharpnessCap();
        cfg.legendary.demolitionCooldownSeconds = ticksToSeconds(v.legendary().demolitionCooldownTicks());
        cfg.legendary.demolitionCooldownScalePercent = v.legendary().demolitionCooldownScalePercent();
        cfg.legendary.demolitionFuseTicks = v.legendary().demolitionFuseTicks();
        cfg.legendary.demolitionRangeBlocks = v.legendary().demolitionRangeBlocks();
        cfg.legendary.demolitionExplosionPower = v.legendary().demolitionExplosionPower();
        cfg.legendary.demolitionTntCount = v.legendary().demolitionTntCount();
        cfg.legendary.hunterAimRangeBlocks = v.legendary().hunterAimRangeBlocks();
        cfg.legendary.hunterAimTimeoutSeconds = ticksToSeconds(v.legendary().hunterAimTimeoutTicks());
        cfg.legendary.hunterAimAssistStrength = v.legendary().hunterAimAssistStrength();
        cfg.legendary.thirdStrikeWindowSeconds = ticksToSeconds(v.legendary().thirdStrikeWindowTicks());
        cfg.legendary.thirdStrikeBonusDamage = v.legendary().thirdStrikeBonusDamage();
        cfg.legendary.vampiricHealAmount = v.legendary().vampiricHealAmount();
        cfg.legendary.duelistsRapierParryWindowTicks = v.legendary().duelistsRapierParryWindowTicks();
        cfg.legendary.duelistsRapierCooldownSeconds = ticksToSeconds(v.legendary().duelistsRapierCooldownTicks());
        cfg.legendary.duelistsRapierCritDamageMultiplier = v.legendary().duelistsRapierCritDamageMultiplier();
        cfg.legendary.challengersGauntletCooldownSeconds = ticksToSeconds(v.legendary().challengersGauntletCooldownTicks());
        cfg.legendary.challengersGauntletRangeBlocks = v.legendary().challengersGauntletRangeBlocks();
        cfg.legendary.reversalMirrorDurationSeconds = ticksToSeconds(v.legendary().reversalMirrorDurationTicks());
        cfg.legendary.reversalMirrorCooldownSeconds = ticksToSeconds(v.legendary().reversalMirrorCooldownTicks());
        cfg.legendary.gladiatorsMarkDurationSeconds = ticksToSeconds(v.legendary().gladiatorsMarkDurationTicks());
        cfg.legendary.gladiatorsMarkCooldownSeconds = ticksToSeconds(v.legendary().gladiatorsMarkCooldownTicks());
        cfg.legendary.gladiatorsMarkRangeBlocks = v.legendary().gladiatorsMarkRangeBlocks();
        cfg.legendary.gladiatorsMarkDamageMultiplier = v.legendary().gladiatorsMarkDamageMultiplier();
        cfg.legendary.soulShackleDurationSeconds = ticksToSeconds(v.legendary().soulShackleDurationTicks());
        cfg.legendary.soulShackleCooldownSeconds = ticksToSeconds(v.legendary().soulShackleCooldownTicks());
        cfg.legendary.soulShackleRangeBlocks = v.legendary().soulShackleRangeBlocks();
        cfg.legendary.soulShackleSplitRatio = v.legendary().soulShackleSplitRatio();
        cfg.legendary.experienceBladeMaxSharpness = v.legendary().experienceBladeMaxSharpness();
        cfg.legendary.experienceBladeSharpnessPerTier = v.legendary().experienceBladeSharpnessPerTier();
        cfg.legendary.experienceBladeXpLevelsPerTier = v.legendary().experienceBladeXpLevelsPerTier();
        cfg.legendary.trophyNecklaceMaxPassives = v.legendary().trophyNecklaceMaxPassives();
        cfg.legendary.supremeHelmetNightVisionAmplifier = v.legendary().supremeHelmetNightVisionAmplifier();
        cfg.legendary.supremeHelmetWaterBreathingAmplifier = v.legendary().supremeHelmetWaterBreathingAmplifier();
        cfg.legendary.supremeChestStrengthAmplifier = v.legendary().supremeChestStrengthAmplifier();
        cfg.legendary.supremeLeggingsFireResAmplifier = v.legendary().supremeLeggingsFireResAmplifier();
        cfg.legendary.supremeBootsSpeedAmplifier = v.legendary().supremeBootsSpeedAmplifier();
        cfg.legendary.supremeSetResistanceAmplifier = v.legendary().supremeSetResistanceAmplifier();
        cfg.legendary.recipeGemRequirements = new HashMap<>();
        for (var entry : v.legendary().recipeGemRequirements().entrySet()) {
            cfg.legendary.recipeGemRequirements.put(entry.getKey().toString(), entry.getValue().name().toLowerCase());
        }

        // Duelist
        cfg.duelist.riposteBonusDamageMultiplier = v.duelist().riposteBonusDamageMultiplier();
        cfg.duelist.riposteWindowSeconds = ticksToSeconds(v.duelist().riposteWindowTicks());
        cfg.duelist.focusBonusDamageMultiplier = v.duelist().focusBonusDamageMultiplier();
        cfg.duelist.focusRadiusBlocks = v.duelist().focusRadiusBlocks();
        cfg.duelist.combatStanceSpeedMultiplier = v.duelist().combatStanceSpeedMultiplier();
        cfg.duelist.lungeCooldownSeconds = ticksToSeconds(v.duelist().lungeCooldownTicks());
        cfg.duelist.lungeDistanceBlocks = v.duelist().lungeDistanceBlocks();
        cfg.duelist.lungeDamage = v.duelist().lungeDamage();
        cfg.duelist.parryCooldownSeconds = ticksToSeconds(v.duelist().parryCooldownTicks());
        cfg.duelist.parryWindowTicks = v.duelist().parryWindowTicks();
        cfg.duelist.parryStunSeconds = ticksToSeconds(v.duelist().parryStunTicks());
        cfg.duelist.rapidStrikeCooldownSeconds = ticksToSeconds(v.duelist().rapidStrikeCooldownTicks());
        cfg.duelist.rapidStrikeDurationSeconds = ticksToSeconds(v.duelist().rapidStrikeDurationTicks());
        cfg.duelist.flourishCooldownSeconds = ticksToSeconds(v.duelist().flourishCooldownTicks());
        cfg.duelist.flourishRadiusBlocks = v.duelist().flourishRadiusBlocks();
        cfg.duelist.flourishDamage = v.duelist().flourishDamage();
        cfg.duelist.mirrorMatchCooldownSeconds = ticksToSeconds(v.duelist().mirrorMatchCooldownTicks());
        cfg.duelist.mirrorMatchDurationSeconds = ticksToSeconds(v.duelist().mirrorMatchDurationTicks());
        cfg.duelist.mirrorMatchRangeBlocks = v.duelist().mirrorMatchRangeBlocks();
        cfg.duelist.bladeDanceCooldownSeconds = ticksToSeconds(v.duelist().bladeDanceCooldownTicks());
        cfg.duelist.bladeDanceDurationSeconds = ticksToSeconds(v.duelist().bladeDanceDurationTicks());
        cfg.duelist.bladeDanceStartingMultiplier = v.duelist().bladeDanceStartingMultiplier();
        cfg.duelist.bladeDanceIncreasePerHit = v.duelist().bladeDanceIncreasePerHit();
        cfg.duelist.bladeDanceMaxMultiplier = v.duelist().bladeDanceMaxMultiplier();
        cfg.duelist.bladeDanceResetSeconds = ticksToSeconds(v.duelist().bladeDanceResetTicks());

        // Hunter
        cfg.hunter.preyMarkBonusDamageMultiplier = v.hunter().preyMarkBonusDamageMultiplier();
        cfg.hunter.preyMarkDurationSeconds = ticksToSeconds(v.hunter().preyMarkDurationTicks());
        cfg.hunter.trackersEyeRangeBlocks = v.hunter().trackersEyeRangeBlocks();
        cfg.hunter.trophyHunterDurationSeconds = ticksToSeconds(v.hunter().trophyHunterDurationTicks());
        cfg.hunter.huntingTrapCooldownSeconds = ticksToSeconds(v.hunter().huntingTrapCooldownTicks());
        cfg.hunter.huntingTrapRootSeconds = ticksToSeconds(v.hunter().huntingTrapRootTicks());
        cfg.hunter.huntingTrapDamage = v.hunter().huntingTrapDamage();
        cfg.hunter.huntingTrapLifetimeSeconds = ticksToSeconds(v.hunter().huntingTrapLifetimeTicks());
        cfg.hunter.pounceCooldownSeconds = ticksToSeconds(v.hunter().pounceCooldownTicks());
        cfg.hunter.pounceRangeBlocks = v.hunter().pounceRangeBlocks();
        cfg.hunter.pounceDamage = v.hunter().pounceDamage();
        cfg.hunter.netShotCooldownSeconds = ticksToSeconds(v.hunter().netShotCooldownTicks());
        cfg.hunter.netShotSlowSeconds = ticksToSeconds(v.hunter().netShotSlowTicks());
        cfg.hunter.netShotRangeBlocks = v.hunter().netShotRangeBlocks();
        cfg.hunter.cripplingCooldownSeconds = ticksToSeconds(v.hunter().cripplingCooldownTicks());
        cfg.hunter.cripplingSlowMultiplier = v.hunter().cripplingSlowMultiplier();
        cfg.hunter.cripplingDurationSeconds = ticksToSeconds(v.hunter().cripplingDurationTicks());
        cfg.hunter.cripplingRangeBlocks = v.hunter().cripplingRangeBlocks();
        cfg.hunter.packTacticsCooldownSeconds = ticksToSeconds(v.hunter().packTacticsCooldownTicks());
        cfg.hunter.packTacticsBonusDamageMultiplier = v.hunter().packTacticsBonusDamageMultiplier();
        cfg.hunter.packTacticsDurationSeconds = ticksToSeconds(v.hunter().packTacticsDurationTicks());
        cfg.hunter.packTacticsRadiusBlocks = v.hunter().packTacticsRadiusBlocks();
        cfg.hunter.sixPackPainCooldownSeconds = ticksToSeconds(v.hunter().sixPackPainCooldownTicks());
        cfg.hunter.sixPackPainCloneCount = v.hunter().sixPackPainCloneCount();
        cfg.hunter.sixPackPainDurationSeconds = ticksToSeconds(v.hunter().sixPackPainDurationTicks());
        cfg.hunter.sixPackPainHealthPerClone = v.hunter().sixPackPainHealthPerClone();
        cfg.hunter.sixPackPainCloseTargetRangeBlocks = v.hunter().sixPackPainCloseTargetRangeBlocks();
        cfg.hunter.sixPackPainWideTargetRangeBlocks = v.hunter().sixPackPainWideTargetRangeBlocks();
        cfg.hunter.sixPackPainBuffDurationTicks = v.hunter().sixPackPainBuffDurationTicks();
        cfg.hunter.sixPackPainDebuffDurationTicks = v.hunter().sixPackPainDebuffDurationTicks();
        cfg.hunter.originTrackingCooldownSeconds = ticksToSeconds(v.hunter().originTrackingCooldownTicks());
        cfg.hunter.originTrackingDurationSeconds = ticksToSeconds(v.hunter().originTrackingDurationTicks());

        // Sentinel
        cfg.sentinel.guardianAuraDamageReduction = v.sentinel().guardianAuraDamageReduction();
        cfg.sentinel.guardianAuraRadiusBlocks = v.sentinel().guardianAuraRadiusBlocks();
        cfg.sentinel.fortressStandStillSeconds = ticksToSeconds(v.sentinel().fortressStandStillTicks());
        cfg.sentinel.fortressResistanceAmplifier = v.sentinel().fortressResistanceAmplifier();
        cfg.sentinel.retributionThornsDamageMultiplier = v.sentinel().retributionThornsDamageMultiplier();
        cfg.sentinel.shieldWallCooldownSeconds = ticksToSeconds(v.sentinel().shieldWallCooldownTicks());
        cfg.sentinel.shieldWallDurationSeconds = ticksToSeconds(v.sentinel().shieldWallDurationTicks());
        cfg.sentinel.shieldWallWidthBlocks = v.sentinel().shieldWallWidthBlocks();
        cfg.sentinel.shieldWallHeightBlocks = v.sentinel().shieldWallHeightBlocks();
        cfg.sentinel.tauntCooldownSeconds = ticksToSeconds(v.sentinel().tauntCooldownTicks());
        cfg.sentinel.tauntDurationSeconds = ticksToSeconds(v.sentinel().tauntDurationTicks());
        cfg.sentinel.tauntRadiusBlocks = v.sentinel().tauntRadiusBlocks();
        cfg.sentinel.tauntDamageReduction = v.sentinel().tauntDamageReduction();
        cfg.sentinel.interventionCooldownSeconds = ticksToSeconds(v.sentinel().interventionCooldownTicks());
        cfg.sentinel.interventionRangeBlocks = v.sentinel().interventionRangeBlocks();
        cfg.sentinel.rallyCryCooldownSeconds = ticksToSeconds(v.sentinel().rallyCryCooldownTicks());
        cfg.sentinel.rallyCryHealHearts = v.sentinel().rallyCryHealHearts();
        cfg.sentinel.rallyCryResistanceDurationSeconds = ticksToSeconds(v.sentinel().rallyCryResistanceDurationTicks());
        cfg.sentinel.rallyCryRadiusBlocks = v.sentinel().rallyCryRadiusBlocks();
        cfg.sentinel.lockdownCooldownSeconds = ticksToSeconds(v.sentinel().lockdownCooldownTicks());
        cfg.sentinel.lockdownDurationSeconds = ticksToSeconds(v.sentinel().lockdownDurationTicks());
        cfg.sentinel.lockdownRadiusBlocks = v.sentinel().lockdownRadiusBlocks();

        // Trickster
        cfg.trickster.sleightOfHandChance = v.trickster().sleightOfHandChance();
        cfg.trickster.slipperyChance = v.trickster().slipperyChance();
        cfg.trickster.shadowSwapCooldownSeconds = ticksToSeconds(v.trickster().shadowSwapCooldownTicks());
        cfg.trickster.shadowSwapRangeBlocks = v.trickster().shadowSwapRangeBlocks();
        cfg.trickster.mirageCooldownSeconds = ticksToSeconds(v.trickster().mirageCooldownTicks());
        cfg.trickster.mirageDurationSeconds = ticksToSeconds(v.trickster().mirageDurationTicks());
        cfg.trickster.mirageCloneCount = v.trickster().mirageCloneCount();
        cfg.trickster.mirageRangeBlocks = v.trickster().mirageRangeBlocks();
        cfg.trickster.mirageBuffDurationSeconds = ticksToSeconds(v.trickster().mirageBuffDurationTicks());
        cfg.trickster.mirageBuffAmplifier = v.trickster().mirageBuffAmplifier();
        cfg.trickster.glitchStepCooldownSeconds = ticksToSeconds(v.trickster().glitchStepCooldownTicks());
        cfg.trickster.glitchStepDistanceBlocks = v.trickster().glitchStepDistanceBlocks();
        cfg.trickster.glitchStepAfterimgDamage = v.trickster().glitchStepAfterimgDamage();
        cfg.trickster.glitchStepAfterimgRadiusBlocks = v.trickster().glitchStepAfterimgRadiusBlocks();
        cfg.trickster.puppetMasterCooldownSeconds = ticksToSeconds(v.trickster().puppetMasterCooldownTicks());
        cfg.trickster.puppetMasterDurationSeconds = ticksToSeconds(v.trickster().puppetMasterDurationTicks());
        cfg.trickster.puppetMasterRangeBlocks = v.trickster().puppetMasterRangeBlocks();
        cfg.trickster.mindGamesCooldownSeconds = ticksToSeconds(v.trickster().mindGamesCooldownTicks());
        cfg.trickster.mindGamesDurationSeconds = ticksToSeconds(v.trickster().mindGamesDurationTicks());
        cfg.trickster.mindGamesRangeBlocks = v.trickster().mindGamesRangeBlocks();

        // BonusPool
        cfg.bonusPool.maxBonusAbilities = v.bonusPool().maxBonusAbilities();
        cfg.bonusPool.maxBonusPassives = v.bonusPool().maxBonusPassives();
        cfg.bonusPool.showBonusesInHud = v.bonusPool().showBonusesInHud();
        cfg.bonusPool.bonusAbilityCooldownMultiplier = v.bonusPool().bonusAbilityCooldownMultiplier();
        cfg.bonusPool.bonusAbilityDamageMultiplier = v.bonusPool().bonusAbilityDamageMultiplier();
        cfg.bonusPool.bonusPassiveEffectMultiplier = v.bonusPool().bonusPassiveEffectMultiplier();

        // Mastery
        cfg.mastery.enabled = v.mastery().enabled();
        cfg.mastery.showAuraParticles = v.mastery().showAuraParticles();

        // Rivalry
        cfg.rivalry.enabled = v.rivalry().enabled();
        cfg.rivalry.damageMultiplier = v.rivalry().damageMultiplier();
        cfg.rivalry.showInHud = v.rivalry().showInHud();

        // Loadouts
        cfg.loadouts.enabled = v.loadouts().enabled();
        cfg.loadouts.unlockEnergy = v.loadouts().unlockEnergy();
        cfg.loadouts.maxPresetsPerGem = v.loadouts().maxPresetsPerGem();

        // Synergies
        cfg.synergies.enabled = v.synergies().enabled();
        cfg.synergies.windowSeconds = ticksToSeconds(v.synergies().windowTicks());
        cfg.synergies.cooldownSeconds = ticksToSeconds(v.synergies().cooldownTicks());
        cfg.synergies.showNotifications = v.synergies().showNotifications();
        java.util.List<GemsBalanceConfig.Synergies.SynergyEntry> defaults =
                GemsBalanceConfig.Synergies.defaultSynergyEntries();
        java.util.List<GemsBalanceConfig.Synergies.SynergyEntry> entries = new java.util.ArrayList<>(defaults.size());
        for (GemsBalanceConfig.Synergies.SynergyEntry def : defaults) {
            SynergyEntry override = v.synergies().entries().get(def.id);
            boolean enabled = override == null ? def.enabled : override.enabled();
            int windowSeconds = override == null ? def.windowSeconds : ticksToSeconds(override.windowTicks());
            int cooldownSeconds = override == null ? def.cooldownSeconds : ticksToSeconds(override.cooldownTicks());
            entries.add(new GemsBalanceConfig.Synergies.SynergyEntry(def.id, enabled, windowSeconds, cooldownSeconds));
        }
        cfg.synergies.entries = entries;

        // Augments
        cfg.augments.gemMaxSlots = v.augments().gemMaxSlots();
        cfg.augments.legendaryMaxSlots = v.augments().legendaryMaxSlots();
        cfg.augments.rarityCommonWeight = v.augments().rarityCommonWeight();
        cfg.augments.rarityRareWeight = v.augments().rarityRareWeight();
        cfg.augments.rarityEpicWeight = v.augments().rarityEpicWeight();
        cfg.augments.commonMagnitudeMin = v.augments().commonMagnitudeMin();
        cfg.augments.commonMagnitudeMax = v.augments().commonMagnitudeMax();
        cfg.augments.rareMagnitudeMin = v.augments().rareMagnitudeMin();
        cfg.augments.rareMagnitudeMax = v.augments().rareMagnitudeMax();
        cfg.augments.epicMagnitudeMin = v.augments().epicMagnitudeMin();
        cfg.augments.epicMagnitudeMax = v.augments().epicMagnitudeMax();

        cfg.mobBlacklist = v.mobBlacklist().stream().map(Identifier::toString).toList();

        return cfg;
    }

    private static int ticksToSeconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.round(ticks / (double) TICKS_PER_SECOND);
    }

    private static int secClampedOptional(int seconds, int minSeconds, int maxSeconds) {
        if (seconds <= 0) {
            return 0;
        }
        return secClamped(seconds, minSeconds, maxSeconds);
    }

    public record Values(
            Visual visual,
            Systems systems,
            Astra astra,
            Fire fire,
            Flux flux,
            Life life,
            Puff puff,
            Speed speed,
            Strength strength,
            Wealth wealth,
            Terror terror,
            Summoner summoner,
            Space space,
            Reaper reaper,
            Pillager pillager,
            Spy spy,
            Beacon beacon,
            Air air,
            VoidGem voidGem,
            Chaos chaos,
            Prism prism,
            Legendary legendary,
            Duelist duelist,
            Hunter hunter,
            Sentinel sentinel,
            Trickster trickster,
            BonusPool bonusPool,
            Mastery mastery,
            Rivalry rivalry,
            Loadouts loadouts,
            Synergies synergies,
                Augments augments,
            List<Identifier> mobBlacklist
    ) {
        public static Values defaults() {
            return from(new GemsBalanceConfig());
        }

        public static Values from(GemsBalanceConfig cfg) {
            return new Values(
                    Visual.from(cfg.visual != null ? cfg.visual : new GemsBalanceConfig.Visual()),
                    Systems.from(cfg.systems != null ? cfg.systems : new GemsBalanceConfig.Systems()),
                    Astra.from(cfg.astra != null ? cfg.astra : new GemsBalanceConfig.Astra()),
                    Fire.from(cfg.fire != null ? cfg.fire : new GemsBalanceConfig.Fire()),
                    Flux.from(cfg.flux != null ? cfg.flux : new GemsBalanceConfig.Flux()),
                    Life.from(cfg.life != null ? cfg.life : new GemsBalanceConfig.Life()),
                    Puff.from(cfg.puff != null ? cfg.puff : new GemsBalanceConfig.Puff()),
                    Speed.from(cfg.speed != null ? cfg.speed : new GemsBalanceConfig.Speed()),
                    Strength.from(cfg.strength != null ? cfg.strength : new GemsBalanceConfig.Strength()),
                    Wealth.from(cfg.wealth != null ? cfg.wealth : new GemsBalanceConfig.Wealth()),
                    Terror.from(cfg.terror != null ? cfg.terror : new GemsBalanceConfig.Terror()),
                    Summoner.from(cfg.summoner != null ? cfg.summoner : new GemsBalanceConfig.Summoner()),
                    Space.from(cfg.space != null ? cfg.space : new GemsBalanceConfig.Space()),
                    Reaper.from(cfg.reaper != null ? cfg.reaper : new GemsBalanceConfig.Reaper()),
                    Pillager.from(cfg.pillager != null ? cfg.pillager : new GemsBalanceConfig.Pillager()),
                    Spy.from(cfg.spy != null ? cfg.spy : new GemsBalanceConfig.Spy()),
                    Beacon.from(cfg.beacon != null ? cfg.beacon : new GemsBalanceConfig.Beacon()),
                    Air.from(cfg.air != null ? cfg.air : new GemsBalanceConfig.Air()),
                    VoidGem.from(cfg.voidGem != null ? cfg.voidGem : new GemsBalanceConfig.VoidGem()),
                    Chaos.from(cfg.chaos != null ? cfg.chaos : new GemsBalanceConfig.Chaos()),
                    Prism.from(cfg.prism != null ? cfg.prism : new GemsBalanceConfig.Prism()),
                    Legendary.from(cfg.legendary != null ? cfg.legendary : new GemsBalanceConfig.Legendary()),
                    Duelist.from(cfg.duelist != null ? cfg.duelist : new GemsBalanceConfig.Duelist()),
                    Hunter.from(cfg.hunter != null ? cfg.hunter : new GemsBalanceConfig.Hunter()),
                    Sentinel.from(cfg.sentinel != null ? cfg.sentinel : new GemsBalanceConfig.Sentinel()),
                    Trickster.from(cfg.trickster != null ? cfg.trickster : new GemsBalanceConfig.Trickster()),
                    BonusPool.from(cfg.bonusPool != null ? cfg.bonusPool : new GemsBalanceConfig.BonusPool()),
                    Mastery.from(cfg.mastery != null ? cfg.mastery : new GemsBalanceConfig.Mastery()),
                    Rivalry.from(cfg.rivalry != null ? cfg.rivalry : new GemsBalanceConfig.Rivalry()),
                    Loadouts.from(cfg.loadouts != null ? cfg.loadouts : new GemsBalanceConfig.Loadouts()),
                    Synergies.from(cfg.synergies != null ? cfg.synergies : new GemsBalanceConfig.Synergies()),
                    Augments.from(cfg.augments != null ? cfg.augments : new GemsBalanceConfig.Augments()),
                    parseIdentifierList(cfg.mobBlacklist)
            );
        }
    }

    public record Visual(
            boolean enableParticles,
            boolean enableSounds,
            int particleScalePercent,
            int maxParticlesPerCall,
            int maxBeamSteps,
            int maxRingPoints
    ) {
        static Visual from(GemsBalanceConfig.Visual cfg) {
            return new Visual(
                    cfg.enableParticles,
                    cfg.enableSounds,
                    clampInt(cfg.particleScalePercent, 0, 200),
                    clampInt(cfg.maxParticlesPerCall, 0, 2048),
                    clampInt(cfg.maxBeamSteps, 0, 2048),
                    clampInt(cfg.maxRingPoints, 0, 2048)
            );
        }
    }

    public record Systems(
            int minMaxHearts,
            int assassinTriggerHearts,
            int assassinMaxHearts,
            int assassinEliminationHeartsThreshold,
            int assassinVsAssassinVictimHeartsLoss,
            int assassinVsAssassinKillerHeartsGain,
            double controlledFollowStartBlocks,
            double controlledFollowStopBlocks,
            double controlledFollowSpeed
    ) {
        static Systems from(GemsBalanceConfig.Systems cfg) {
            double followStart = clampDouble(cfg.controlledFollowStartBlocks, 0.0D, 128.0D);
            double followStop = clampDouble(cfg.controlledFollowStopBlocks, 0.0D, 128.0D);
            if (followStop > followStart) {
                followStop = followStart;
            }
            double followSpeed = clampDouble(cfg.controlledFollowSpeed, 0.0D, 3.0D);
            int maxHearts = clampInt(cfg.assassinMaxHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS);
            int eliminationThreshold = clampInt(cfg.assassinEliminationHeartsThreshold, 0, maxHearts);
            return new Systems(
                    clampInt(cfg.minMaxHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS),
                    clampInt(cfg.assassinTriggerHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS),
                    maxHearts,
                    eliminationThreshold,
                    clampInt(cfg.assassinVsAssassinVictimHeartsLoss, 0, maxHearts),
                    clampInt(cfg.assassinVsAssassinKillerHeartsGain, 0, maxHearts),
                    followStart,
                    followStop,
                    followSpeed
            );
        }
    }

    public record Astra(
            int shadowAnchorWindowTicks,
            int shadowAnchorPostCooldownTicks,
            int dimensionalVoidCooldownTicks,
            int dimensionalVoidDurationTicks,
            int dimensionalVoidRadiusBlocks,
            int astralDaggersCooldownTicks,
            int astralDaggersCount,
            float astralDaggersDamage,
            float astralDaggersVelocity,
            float astralDaggersSpread,
            int unboundedCooldownTicks,
            int unboundedDurationTicks,
            int astralCameraCooldownTicks,
            int astralCameraDurationTicks,
            int spookCooldownTicks,
            int spookRadiusBlocks,
            int spookDurationTicks,
            int tagCooldownTicks,
            int tagRangeBlocks,
            int tagDurationTicks,
            float soulHealingHearts,
            double soulReleaseForwardBlocks,
            double soulReleaseUpBlocks
    ) {
        static Astra from(GemsBalanceConfig.Astra cfg) {
            return new Astra(
                    secClamped(cfg.shadowAnchorWindowSeconds, 1, 60),
                    secClamped(cfg.shadowAnchorPostCooldownSeconds, 0, 3600),
                    secClamped(cfg.dimensionalVoidCooldownSeconds, 0, 3600),
                    secClamped(cfg.dimensionalVoidDurationSeconds, 0, 60),
                    clampInt(cfg.dimensionalVoidRadiusBlocks, 0, 32),
                    secClamped(cfg.astralDaggersCooldownSeconds, 0, 3600),
                    clampInt(cfg.astralDaggersCount, 1, 30),
                    clampFloat(cfg.astralDaggersDamage, 0.0F, 40.0F),
                    clampFloat(cfg.astralDaggersVelocity, 0.1F, 8.0F),
                    clampFloat(cfg.astralDaggersSpread, 0.0F, 0.5F),
                    secClamped(cfg.unboundedCooldownSeconds, 0, 3600),
                    secClamped(cfg.unboundedDurationSeconds, 0, 10),
                    secClamped(cfg.astralCameraCooldownSeconds, 0, 3600),
                    secClamped(cfg.astralCameraDurationSeconds, 0, 60),
                    secClamped(cfg.spookCooldownSeconds, 0, 3600),
                    clampInt(cfg.spookRadiusBlocks, 0, 32),
                    secClamped(cfg.spookDurationSeconds, 0, 60),
                    secClamped(cfg.tagCooldownSeconds, 0, 3600),
                    clampInt(cfg.tagRangeBlocks, 0, 128),
                    secClamped(cfg.tagDurationSeconds, 0, 120),
                    clampFloat(cfg.soulHealingHearts, 0.0F, 40.0F),
                    clampDouble(cfg.soulReleaseForwardBlocks, 0.0D, 32.0D),
                    clampDouble(cfg.soulReleaseUpBlocks, 0.0D, 16.0D)
            );
        }
    }

    public record Fire(
            int cosyCampfireCooldownTicks,
            int cosyCampfireDurationTicks,
            int cosyCampfireRadiusBlocks,
            int cosyCampfireRegenAmplifier,
            int heatHazeCooldownTicks,
            int heatHazeDurationTicks,
            int heatHazeRadiusBlocks,
            int heatHazeEnemyMiningFatigueAmplifier,
            int heatHazeEnemyWeaknessAmplifier,
            int fireballChargeUpTicks,
            int fireballChargeDownTicks,
            int fireballInternalCooldownTicks,
            int fireballMaxDistanceBlocks,
            int meteorShowerCooldownTicks,
            int meteorShowerTargetRangeBlocks,
            int meteorShowerCount,
            int meteorShowerSpreadBlocks,
            int meteorShowerHeightBlocks,
            float meteorShowerVelocity,
            int meteorShowerExplosionPower
    ) {
        static Fire from(GemsBalanceConfig.Fire cfg) {
            return new Fire(
                    secClamped(cfg.cosyCampfireCooldownSeconds, 0, 3600),
                    secClamped(cfg.cosyCampfireDurationSeconds, 0, 120),
                    clampInt(cfg.cosyCampfireRadiusBlocks, 0, 32),
                    clampInt(cfg.cosyCampfireRegenAmplifier, 0, 10),
                    secClamped(cfg.heatHazeCooldownSeconds, 0, 3600),
                    secClamped(cfg.heatHazeDurationSeconds, 0, 120),
                    clampInt(cfg.heatHazeRadiusBlocks, 0, 32),
                    clampInt(cfg.heatHazeEnemyMiningFatigueAmplifier, 0, 10),
                    clampInt(cfg.heatHazeEnemyWeaknessAmplifier, 0, 10),
                    secClamped(cfg.fireballChargeUpSeconds, 0, 20),
                    secClamped(cfg.fireballChargeDownSeconds, 0, 20),
                    secClamped(cfg.fireballInternalCooldownSeconds, 0, 3600),
                    clampInt(cfg.fireballMaxDistanceBlocks, 10, 256),
                    secClamped(cfg.meteorShowerCooldownSeconds, 0, 3600),
                    clampInt(cfg.meteorShowerTargetRangeBlocks, 1, 128),
                    clampInt(cfg.meteorShowerCount, 0, 50),
                    clampInt(cfg.meteorShowerSpreadBlocks, 0, 48),
                    clampInt(cfg.meteorShowerHeightBlocks, 1, 256),
                    clampFloat(cfg.meteorShowerVelocity, 0.1F, 6.0F),
                    clampInt(cfg.meteorShowerExplosionPower, 1, 6)
            );
        }
    }

    public record Flux(
            int fluxBeamCooldownTicks,
            int fluxBeamRangeBlocks,
            float fluxBeamMinDamage,
            float fluxBeamMaxDamageAt100,
            float fluxBeamMaxDamageAt200,
            int fluxBeamArmorDamageAt100,
            int fluxBeamArmorDamagePerPercent,
            int staticBurstCooldownTicks,
            int staticBurstRadiusBlocks,
            float staticBurstMaxDamage,
            int staticBurstStoreWindowTicks,
            int chargeDiamondBlock,
            int chargeGoldBlock,
            int chargeCopperBlock,
            int chargeEmeraldBlock,
            int chargeAmethystBlock,
            int chargeNetheriteScrap,
            int chargeEnchantedDiamondItem,
            int overchargeDelayTicks,
            int overchargePerSecond,
            float overchargeSelfDamagePerSecond,
            int fluxCapacitorChargeThreshold,
            int fluxCapacitorAbsorptionAmplifier,
            int fluxConductivityChargePerDamage,
            int fluxConductivityMaxChargePerHit,
            int fluxInsulationChargeThreshold,
            float fluxInsulationDamageMultiplier,
            int fluxSurgeCooldownTicks,
            int fluxSurgeDurationTicks,
            int fluxSurgeSpeedAmplifier,
            int fluxSurgeResistanceAmplifier,
            int fluxSurgeChargeCost,
            int fluxSurgeRadiusBlocks,
            double fluxSurgeKnockback,
            int fluxDischargeCooldownTicks,
            int fluxDischargeRadiusBlocks,
            float fluxDischargeBaseDamage,
            float fluxDischargeDamagePerCharge,
            float fluxDischargeMaxDamage,
            int fluxDischargeMinCharge,
            double fluxDischargeKnockback
    ) {
        static Flux from(GemsBalanceConfig.Flux cfg) {
            return new Flux(
                    secClamped(cfg.fluxBeamCooldownSeconds, 0, 3600),
                    clampInt(cfg.fluxBeamRangeBlocks, 1, 256),
                    clampFloat(cfg.fluxBeamMinDamage, 0.0F, 40.0F),
                    clampFloat(cfg.fluxBeamMaxDamageAt100, 0.0F, 80.0F),
                    clampFloat(cfg.fluxBeamMaxDamageAt200, 0.0F, 120.0F),
                    clampInt(cfg.fluxBeamArmorDamageAt100, 0, 2000),
                    clampInt(cfg.fluxBeamArmorDamagePerPercent, 0, 100),
                    secClamped(cfg.staticBurstCooldownSeconds, 0, 3600),
                    clampInt(cfg.staticBurstRadiusBlocks, 0, 32),
                    clampFloat(cfg.staticBurstMaxDamage, 0.0F, 80.0F),
                    secClamped(cfg.staticBurstStoreWindowSeconds, 0, 600),
                    clampInt(cfg.chargeDiamondBlock, 0, 200),
                    clampInt(cfg.chargeGoldBlock, 0, 200),
                    clampInt(cfg.chargeCopperBlock, 0, 200),
                    clampInt(cfg.chargeEmeraldBlock, 0, 200),
                    clampInt(cfg.chargeAmethystBlock, 0, 200),
                    clampInt(cfg.chargeNetheriteScrap, 0, 200),
                    clampInt(cfg.chargeEnchantedDiamondItem, 0, 200),
                    secClamped(cfg.overchargeDelaySeconds, 0, 60),
                    clampInt(cfg.overchargePerSecond, 0, 100),
                    clampFloat(cfg.overchargeSelfDamagePerSecond, 0.0F, 20.0F),
                    clampInt(cfg.fluxCapacitorChargeThreshold, 0, 200),
                    clampInt(cfg.fluxCapacitorAbsorptionAmplifier, 0, 10),
                    clampInt(cfg.fluxConductivityChargePerDamage, 0, 50),
                    clampInt(cfg.fluxConductivityMaxChargePerHit, 0, 200),
                    clampInt(cfg.fluxInsulationChargeThreshold, 0, 200),
                    clampFloat(cfg.fluxInsulationDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.fluxSurgeCooldownSeconds, 0, 3600),
                    secClamped(cfg.fluxSurgeDurationSeconds, 0, 120),
                    clampInt(cfg.fluxSurgeSpeedAmplifier, 0, 10),
                    clampInt(cfg.fluxSurgeResistanceAmplifier, 0, 10),
                    clampInt(cfg.fluxSurgeChargeCost, 0, 200),
                    clampInt(cfg.fluxSurgeRadiusBlocks, 0, 32),
                    clampDouble(cfg.fluxSurgeKnockback, 0.0D, 5.0D),
                    secClamped(cfg.fluxDischargeCooldownSeconds, 0, 3600),
                    clampInt(cfg.fluxDischargeRadiusBlocks, 0, 32),
                    clampFloat(cfg.fluxDischargeBaseDamage, 0.0F, 120.0F),
                    clampFloat(cfg.fluxDischargeDamagePerCharge, 0.0F, 10.0F),
                    clampFloat(cfg.fluxDischargeMaxDamage, 0.0F, 120.0F),
                    clampInt(cfg.fluxDischargeMinCharge, 0, 200),
                    clampDouble(cfg.fluxDischargeKnockback, 0.0D, 5.0D)
            );
        }
    }

    public record Life(
            int vitalityVortexCooldownTicks,
            int vitalityVortexRadiusBlocks,
            int vitalityVortexDurationTicks,
            int vitalityVortexScanRadiusBlocks,
            int vitalityVortexVerdantThreshold,
            float vitalityVortexAllyHeal,
            int healthDrainCooldownTicks,
            int healthDrainRangeBlocks,
            float healthDrainAmount,
            int lifeSwapCooldownTicks,
            int lifeSwapRangeBlocks,
            float lifeSwapMinHearts,
            int lifeSwapReswapTicks,
            int lifeCircleCooldownTicks,
            int lifeCircleDurationTicks,
            int lifeCircleRadiusBlocks,
            double lifeCircleMaxHealthDelta,
            int heartLockCooldownTicks,
            int heartLockDurationTicks,
            int heartLockRangeBlocks
    ) {
        static Life from(GemsBalanceConfig.Life cfg) {
            return new Life(
                    secClamped(cfg.vitalityVortexCooldownSeconds, 0, 3600),
                    clampInt(cfg.vitalityVortexRadiusBlocks, 0, 32),
                    secClamped(cfg.vitalityVortexDurationSeconds, 0, 120),
                    clampInt(cfg.vitalityVortexScanRadiusBlocks, 1, 6),
                    clampInt(cfg.vitalityVortexVerdantThreshold, 0, 64),
                    clampFloat(cfg.vitalityVortexAllyHeal, 0.0F, 40.0F),
                    secClamped(cfg.healthDrainCooldownSeconds, 0, 3600),
                    clampInt(cfg.healthDrainRangeBlocks, 0, 128),
                    clampFloat(cfg.healthDrainAmount, 0.0F, 40.0F),
                    secClamped(cfg.lifeSwapCooldownSeconds, 0, 3600),
                    clampInt(cfg.lifeSwapRangeBlocks, 0, 128),
                    clampFloat(cfg.lifeSwapMinHearts, 1.0F, 20.0F),
                    secClamped(cfg.lifeSwapReswapSeconds, 0, 3600),
                    secClamped(cfg.lifeCircleCooldownSeconds, 0, 3600),
                    secClamped(cfg.lifeCircleDurationSeconds, 0, 120),
                    clampInt(cfg.lifeCircleRadiusBlocks, 0, 32),
                    clampDouble(cfg.lifeCircleMaxHealthDelta, 0.0D, 40.0D),
                    secClamped(cfg.heartLockCooldownSeconds, 0, 3600),
                    secClamped(cfg.heartLockDurationSeconds, 0, 60),
                    clampInt(cfg.heartLockRangeBlocks, 0, 128)
            );
        }
    }

    public record Puff(
            int doubleJumpCooldownTicks,
            double doubleJumpVelocityY,
            int dashCooldownTicks,
            double dashVelocity,
            float dashDamage,
            double dashHitRangeBlocks,
            int breezyBashCooldownTicks,
            int breezyBashRangeBlocks,
            double breezyBashUpVelocityY,
            double breezyBashKnockback,
            float breezyBashInitialDamage,
            float breezyBashImpactDamage,
            int breezyBashImpactWindowTicks,
            int groupBashCooldownTicks,
            int groupBashRadiusBlocks,
            double groupBashKnockback,
            double groupBashUpVelocityY,
            int gustCooldownTicks,
            int gustRadiusBlocks,
            double gustUpVelocityY,
            double gustKnockback,
            int gustSlownessDurationTicks,
            int gustSlownessAmplifier,
            int gustSlowFallingDurationTicks,
            int windborneDurationTicks,
            int windborneSlowFallingAmplifier
    ) {
        static Puff from(GemsBalanceConfig.Puff cfg) {
            return new Puff(
                    secClamped(cfg.doubleJumpCooldownSeconds, 0, 3600),
                    clampDouble(cfg.doubleJumpVelocityY, 0.0D, 3.0D),
                    secClamped(cfg.dashCooldownSeconds, 0, 3600),
                    clampDouble(cfg.dashVelocity, 0.0D, 5.0D),
                    clampFloat(cfg.dashDamage, 0.0F, 40.0F),
                    clampDouble(cfg.dashHitRangeBlocks, 0.5D, 16.0D),
                    secClamped(cfg.breezyBashCooldownSeconds, 0, 3600),
                    clampInt(cfg.breezyBashRangeBlocks, 0, 64),
                    clampDouble(cfg.breezyBashUpVelocityY, 0.0D, 5.0D),
                    clampDouble(cfg.breezyBashKnockback, 0.0D, 5.0D),
                    clampFloat(cfg.breezyBashInitialDamage, 0.0F, 40.0F),
                    clampFloat(cfg.breezyBashImpactDamage, 0.0F, 80.0F),
                    secClamped(cfg.breezyBashImpactWindowSeconds, 0, 60),
                    secClamped(cfg.groupBashCooldownSeconds, 0, 3600),
                    clampInt(cfg.groupBashRadiusBlocks, 0, 64),
                    clampDouble(cfg.groupBashKnockback, 0.0D, 5.0D),
                    clampDouble(cfg.groupBashUpVelocityY, 0.0D, 5.0D),
                    secClamped(cfg.gustCooldownSeconds, 0, 3600),
                    clampInt(cfg.gustRadiusBlocks, 0, 64),
                    clampDouble(cfg.gustUpVelocityY, 0.0D, 5.0D),
                    clampDouble(cfg.gustKnockback, 0.0D, 5.0D),
                    secClamped(cfg.gustSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.gustSlownessAmplifier, 0, 10),
                    secClamped(cfg.gustSlowFallingDurationSeconds, 0, 120),
                    secClamped(cfg.windborneDurationSeconds, 0, 60),
                    clampInt(cfg.windborneSlowFallingAmplifier, 0, 10)
            );
        }
    }

    public record Speed(
            double momentumMinSpeed,
            double momentumMaxSpeed,
            float momentumMinMultiplier,
            float momentumMaxMultiplier,
            int frictionlessSpeedAmplifier,
            int arcShotCooldownTicks,
            int arcShotRangeBlocks,
            double arcShotRadiusBlocks,
            int arcShotMaxTargets,
            float arcShotDamage,
            int speedStormCooldownTicks,
            int speedStormDurationTicks,
            int speedStormRadiusBlocks,
            int speedStormAllySpeedAmplifier,
            int speedStormAllyHasteAmplifier,
            int speedStormEnemySlownessAmplifier,
            int speedStormEnemyMiningFatigueAmplifier,
            int terminalVelocityCooldownTicks,
            int terminalVelocityDurationTicks,
            int terminalVelocitySpeedAmplifier,
            int terminalVelocityHasteAmplifier,
            int slipstreamCooldownTicks,
            int slipstreamDurationTicks,
            int slipstreamLengthBlocks,
            int slipstreamRadiusBlocks,
            int slipstreamAllySpeedAmplifier,
            int slipstreamEnemySlownessAmplifier,
            double slipstreamEnemyKnockback,
            int afterimageCooldownTicks,
            int afterimageDurationTicks,
            int afterimageSpeedAmplifier,
            int tempoShiftCooldownTicks,
            int tempoShiftDurationTicks,
            int tempoShiftRadiusBlocks,
            int tempoShiftAllyCooldownTicksPerSecond,
            int tempoShiftEnemyCooldownTicksPerSecond,
            int autoStepCooldownTicks,
            double autoStepHeightBonus
    ) {
        static Speed from(GemsBalanceConfig.Speed cfg) {
            return new Speed(
                    clampDouble(cfg.momentumMinSpeed, 0.0D, 2.0D),
                    clampDouble(cfg.momentumMaxSpeed, 0.0D, 2.0D),
                    clampFloat(cfg.momentumMinMultiplier, 0.1F, 3.0F),
                    clampFloat(cfg.momentumMaxMultiplier, 0.1F, 3.0F),
                    clampInt(cfg.frictionlessSpeedAmplifier, 0, 5),
                    secClamped(cfg.arcShotCooldownSeconds, 0, 3600),
                    clampInt(cfg.arcShotRangeBlocks, 0, 256),
                    clampDouble(cfg.arcShotRadiusBlocks, 0.0D, 16.0D),
                    clampInt(cfg.arcShotMaxTargets, 1, 10),
                    clampFloat(cfg.arcShotDamage, 0.0F, 40.0F),
                    secClamped(cfg.speedStormCooldownSeconds, 0, 3600),
                    secClamped(cfg.speedStormDurationSeconds, 0, 120),
                    clampInt(cfg.speedStormRadiusBlocks, 0, 32),
                    clampInt(cfg.speedStormAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.speedStormAllyHasteAmplifier, 0, 10),
                    clampInt(cfg.speedStormEnemySlownessAmplifier, 0, 10),
                    clampInt(cfg.speedStormEnemyMiningFatigueAmplifier, 0, 10),
                    secClamped(cfg.terminalVelocityCooldownSeconds, 0, 3600),
                    secClamped(cfg.terminalVelocityDurationSeconds, 0, 120),
                    clampInt(cfg.terminalVelocitySpeedAmplifier, 0, 10),
                    clampInt(cfg.terminalVelocityHasteAmplifier, 0, 10),
                    secClamped(cfg.slipstreamCooldownSeconds, 0, 3600),
                    secClamped(cfg.slipstreamDurationSeconds, 0, 120),
                    clampInt(cfg.slipstreamLengthBlocks, 0, 128),
                    clampInt(cfg.slipstreamRadiusBlocks, 0, 32),
                    clampInt(cfg.slipstreamAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.slipstreamEnemySlownessAmplifier, 0, 10),
                    clampDouble(cfg.slipstreamEnemyKnockback, 0.0D, 3.0D),
                    secClamped(cfg.afterimageCooldownSeconds, 0, 3600),
                    secClamped(cfg.afterimageDurationSeconds, 0, 120),
                    clampInt(cfg.afterimageSpeedAmplifier, 0, 10),
                    secClamped(cfg.tempoShiftCooldownSeconds, 0, 3600),
                    secClamped(cfg.tempoShiftDurationSeconds, 0, 3600),
                    clampInt(cfg.tempoShiftRadiusBlocks, 0, 48),
                    clampInt(cfg.tempoShiftAllyCooldownTicksPerSecond, 0, 200),
                    clampInt(cfg.tempoShiftEnemyCooldownTicksPerSecond, 0, 200),
                    secClamped(cfg.autoStepCooldownSeconds, 0, 60),
                    clampDouble(cfg.autoStepHeightBonus, 0.0D, 1.0D)
            );
        }
    }

    public record Strength(
            int nullifyCooldownTicks,
            int nullifyRadiusBlocks,
            int frailerCooldownTicks,
            int frailerRangeBlocks,
            int frailerDurationTicks,
            int bountyCooldownTicks,
            int bountyDurationTicks,
            int chadCooldownTicks,
            int chadDurationTicks,
            int chadEveryHits,
            float chadBonusDamage,
            float adrenalineThresholdHearts,
            int adrenalineDurationTicks,
            int adrenalineResistanceAmplifier
    ) {
        static Strength from(GemsBalanceConfig.Strength cfg) {
            return new Strength(
                    secClamped(cfg.nullifyCooldownSeconds, 0, 3600),
                    clampInt(cfg.nullifyRadiusBlocks, 0, 32),
                    secClamped(cfg.frailerCooldownSeconds, 0, 3600),
                    clampInt(cfg.frailerRangeBlocks, 0, 128),
                    secClamped(cfg.frailerDurationSeconds, 0, 120),
                    secClamped(cfg.bountyCooldownSeconds, 0, 3600),
                    secClamped(cfg.bountyDurationSeconds, 0, 3600),
                    secClamped(cfg.chadCooldownSeconds, 0, 3600),
                    secClamped(cfg.chadDurationSeconds, 0, 3600),
                    clampInt(cfg.chadEveryHits, 1, 20),
                    clampFloat(cfg.chadBonusDamage, 0.0F, 80.0F),
                    clampFloat(cfg.adrenalineThresholdHearts, 0.0F, 20.0F),
                    secClamped(cfg.adrenalineDurationSeconds, 0, 120),
                    clampInt(cfg.adrenalineResistanceAmplifier, 0, 10)
            );
        }
    }

    public record Wealth(
            int fumbleCooldownTicks,
            int fumbleDurationTicks,
            int fumbleRadiusBlocks,
            int hotbarLockCooldownTicks,
            int hotbarLockDurationTicks,
            int hotbarLockRangeBlocks,
            int amplificationCooldownTicks,
            int amplificationDurationTicks,
            int amplificationBonusLevels,
            int richRushCooldownTicks,
            int richRushDurationTicks,
            int richRushLootRolls,
            int richRushWitherSkullGuarantee,
            int richRushShulkerShellGuarantee,
            int richRushNetherStarGuarantee,
            int pocketsRows
    ) {
        static Wealth from(GemsBalanceConfig.Wealth cfg) {
            return new Wealth(
                    secClamped(cfg.fumbleCooldownSeconds, 0, 3600),
                    secClamped(cfg.fumbleDurationSeconds, 0, 120),
                    clampInt(cfg.fumbleRadiusBlocks, 0, 32),
                    secClamped(cfg.hotbarLockCooldownSeconds, 0, 3600),
                    secClamped(cfg.hotbarLockDurationSeconds, 0, 120),
                    clampInt(cfg.hotbarLockRangeBlocks, 0, 128),
                    secClamped(cfg.amplificationCooldownSeconds, 0, 3600),
                    secClamped(cfg.amplificationDurationSeconds, 0, 600),
                    clampInt(cfg.amplificationBonusLevels, 1, 5),
                    secClamped(cfg.richRushCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.richRushDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.richRushLootRolls, 1, 20),
                    clampInt(cfg.richRushWitherSkullGuarantee, 0, 64),
                    clampInt(cfg.richRushShulkerShellGuarantee, 0, 64),
                    clampInt(cfg.richRushNetherStarGuarantee, 0, 64),
                    clampInt(cfg.pocketsRows, 1, 6)
            );
        }
    }

    public record Terror(
            int dreadAuraRadiusBlocks,
            int dreadAuraAmplifier,
            int bloodPriceDurationTicks,
            int bloodPriceStrengthAmplifier,
            int bloodPriceResistanceAmplifier,
            int terrorTradeCooldownTicks,
            int terrorTradeRangeBlocks,
            int terrorTradeMaxUses,
            int terrorTradeHeartsCost,
            int terrorTradePermanentEnergyPenalty,
            int terrorTradeNormalTargetHeartsPenalty,
            int terrorTradeNormalTargetEnergyPenalty,
            int panicRingCooldownTicks,
            int panicRingTntCount,
            int panicRingFuseTicks,
            double panicRingRadiusBlocks,
            int rigCooldownTicks,
            int rigRangeBlocks,
            int rigDurationTicks,
            int rigFuseTicks,
            int rigTntCount,
            int remoteChargeArmWindowTicks,
            int remoteChargeDetonateWindowTicks,
            int remoteChargeFuseTicks,
            int remoteChargeCooldownTicks,
            int breachChargeCooldownTicks,
            int breachChargeRangeBlocks,
            int breachChargeFuseTicks,
            float breachChargeExplosionPower
    ) {
        static Terror from(GemsBalanceConfig.Terror cfg) {
            return new Terror(
                    clampInt(cfg.dreadAuraRadiusBlocks, 0, 32),
                    clampInt(cfg.dreadAuraAmplifier, 0, 10),
                    secClamped(cfg.bloodPriceDurationSeconds, 0, 60),
                    clampInt(cfg.bloodPriceStrengthAmplifier, 0, 10),
                    clampInt(cfg.bloodPriceResistanceAmplifier, 0, 10),
                    secClamped(cfg.terrorTradeCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.terrorTradeRangeBlocks, 0, 128),
                    clampInt(cfg.terrorTradeMaxUses, 0, 10),
                    clampInt(cfg.terrorTradeHeartsCost, 0, 10),
                    clampInt(cfg.terrorTradePermanentEnergyPenalty, 0, 10),
                    clampInt(cfg.terrorTradeNormalTargetHeartsPenalty, 0, 20),
                    clampInt(cfg.terrorTradeNormalTargetEnergyPenalty, 0, 10),
                    secClamped(cfg.panicRingCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.panicRingTntCount, 0, 40),
                    clampInt(cfg.panicRingFuseTicks, 1, 200),
                    clampDouble(cfg.panicRingRadiusBlocks, 0.0D, 8.0D),
                    secClamped(cfg.rigCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.rigRangeBlocks, 0, 128),
                    secClamped(cfg.rigDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.rigFuseTicks, 1, 200),
                    clampInt(cfg.rigTntCount, 0, 50),
                    secClamped(cfg.remoteChargeArmWindowSeconds, 0, 60),
                    secClamped(cfg.remoteChargeDetonateWindowSeconds, 0, 3600),
                    clampInt(cfg.remoteChargeFuseTicks, 1, 200),
                    secClamped(cfg.remoteChargeCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.breachChargeCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.breachChargeRangeBlocks, 0, 128),
                    clampInt(cfg.breachChargeFuseTicks, 1, 200),
                    clampFloat(cfg.breachChargeExplosionPower, 0.0F, 12.0F)
            );
        }
    }

    public record Summoner(
            int maxPoints,
            int maxActiveSummons,
            int summonLifetimeTicks,
            int commandRangeBlocks,
            int summonSlotCooldownTicks,
            int recallCooldownTicks,
            int commandersMarkDurationTicks,
            int commandersMarkStrengthAmplifier,
            float summonBonusHealth,
            double summonSpawnForwardBlocks,
            double summonSpawnUpBlocks,
            double summonSpawnRingBaseBlocks,
            double summonSpawnRingStepBlocks,
            int summonSpawnRingLayers,
            int summonSpawnRingSegments,
            java.util.Map<String, Integer> costs,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot1,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot2,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot3,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot4,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot5
    ) {
        static Summoner from(GemsBalanceConfig.Summoner cfg) {
            return new Summoner(
                    clampInt(cfg.maxPoints, 0, 500),
                    clampInt(cfg.maxActiveSummons, 0, 200),
                    secClamped(cfg.summonLifetimeSeconds, 0, 24 * 3600),
                    clampInt(cfg.commandRangeBlocks, 0, 128),
                    secClamped(cfg.summonSlotCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.recallCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.commandersMarkDurationSeconds, 0, 60),
                    clampInt(cfg.commandersMarkStrengthAmplifier, 0, 10),
                    clampFloat(cfg.summonBonusHealth, 0.0F, 40.0F),
                    clampDouble(cfg.summonSpawnForwardBlocks, 0.0D, 16.0D),
                    clampDouble(cfg.summonSpawnUpBlocks, 0.0D, 8.0D),
                    clampDouble(cfg.summonSpawnRingBaseBlocks, 0.0D, 4.0D),
                    clampDouble(cfg.summonSpawnRingStepBlocks, 0.0D, 4.0D),
                    clampInt(cfg.summonSpawnRingLayers, 1, 8),
                    clampInt(cfg.summonSpawnRingSegments, 1, 16),
                    sanitizeCosts(cfg.costs),
                    sanitizeSpecs(cfg.slot1),
                    sanitizeSpecs(cfg.slot2),
                    sanitizeSpecs(cfg.slot3),
                    sanitizeSpecs(cfg.slot4),
                    sanitizeSpecs(cfg.slot5)
            );
        }

        private static java.util.Map<String, Integer> sanitizeCosts(java.util.Map<String, Integer> costs) {
            if (costs == null || costs.isEmpty()) {
                return java.util.Map.of();
            }
            java.util.Map<String, Integer> out = new java.util.HashMap<>();
            for (var e : costs.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) {
                    continue;
                }
                int v = e.getValue() != null ? e.getValue() : 0;
                out.put(e.getKey(), clampInt(v, 0, 500));
            }
            return java.util.Collections.unmodifiableMap(out);
        }

        private static java.util.List<GemsBalanceConfig.Summoner.SummonSpec> sanitizeSpecs(java.util.List<GemsBalanceConfig.Summoner.SummonSpec> list) {
            if (list == null || list.isEmpty()) {
                return java.util.List.of();
            }
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> out = new java.util.ArrayList<>();
            for (GemsBalanceConfig.Summoner.SummonSpec spec : list) {
                if (spec == null || spec.entityId == null || spec.entityId.isBlank()) {
                    continue;
                }
                int count = clampInt(spec.count, 0, 64);
                if (count <= 0) {
                    continue;
                }
                out.add(new GemsBalanceConfig.Summoner.SummonSpec(spec.entityId, count));
            }
            return java.util.Collections.unmodifiableList(out);
        }
    }

    public record Space(
            float lunarMinMultiplier,
            float lunarMaxMultiplier,
            float starshieldProjectileDamageMultiplier,
            int orbitalLaserCooldownTicks,
            int orbitalLaserMiningCooldownTicks,
            int orbitalLaserRangeBlocks,
            int orbitalLaserDelayTicks,
            int orbitalLaserRadiusBlocks,
            float orbitalLaserDamage,
            int orbitalLaserMiningRadiusBlocks,
            float orbitalLaserMiningHardnessCap,
            int orbitalLaserMiningMaxBlocks,
            int gravityFieldCooldownTicks,
            int gravityFieldDurationTicks,
            int gravityFieldRadiusBlocks,
            float gravityFieldAllyGravityMultiplier,
            float gravityFieldEnemyGravityMultiplier,
            int blackHoleCooldownTicks,
            int blackHoleDurationTicks,
            int blackHoleRadiusBlocks,
            float blackHolePullStrength,
            float blackHoleDamagePerSecond,
            int whiteHoleCooldownTicks,
            int whiteHoleDurationTicks,
            int whiteHoleRadiusBlocks,
            float whiteHolePushStrength,
            float whiteHoleDamagePerSecond
    ) {
        static Space from(GemsBalanceConfig.Space cfg) {
            float lunarMin = clampFloat(cfg.lunarMinMultiplier, 0.1F, 5.0F);
            float lunarMax = clampFloat(cfg.lunarMaxMultiplier, lunarMin, 5.0F);
            return new Space(
                    lunarMin,
                    lunarMax,
                    clampFloat(cfg.starshieldProjectileDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.orbitalLaserCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.orbitalLaserMiningCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.orbitalLaserRangeBlocks, 0, 256),
                    secClamped(cfg.orbitalLaserDelaySeconds, 0, 10),
                    clampInt(cfg.orbitalLaserRadiusBlocks, 0, 32),
                    clampFloat(cfg.orbitalLaserDamage, 0.0F, 200.0F),
                    clampInt(cfg.orbitalLaserMiningRadiusBlocks, 0, 16),
                    clampFloat(cfg.orbitalLaserMiningHardnessCap, 0.0F, 2000.0F),
                    clampInt(cfg.orbitalLaserMiningMaxBlocks, 0, 2048),
                    secClamped(cfg.gravityFieldCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.gravityFieldDurationSeconds, 0, 120),
                    clampInt(cfg.gravityFieldRadiusBlocks, 0, 64),
                    clampFloat(cfg.gravityFieldAllyGravityMultiplier, 0.1F, 5.0F),
                    clampFloat(cfg.gravityFieldEnemyGravityMultiplier, 0.1F, 5.0F),
                    secClamped(cfg.blackHoleCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.blackHoleDurationSeconds, 0, 60),
                    clampInt(cfg.blackHoleRadiusBlocks, 0, 64),
                    clampFloat(cfg.blackHolePullStrength, 0.0F, 5.0F),
                    clampFloat(cfg.blackHoleDamagePerSecond, 0.0F, 200.0F),
                    secClamped(cfg.whiteHoleCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.whiteHoleDurationSeconds, 0, 60),
                    clampInt(cfg.whiteHoleRadiusBlocks, 0, 64),
                    clampFloat(cfg.whiteHolePushStrength, 0.0F, 5.0F),
                    clampFloat(cfg.whiteHoleDamagePerSecond, 0.0F, 200.0F)
            );
        }
    }

    public record Reaper(
            float undeadWardDamageMultiplier,
            int harvestRegenDurationTicks,
            int harvestRegenAmplifier,
            int graveSteedCooldownTicks,
            int graveSteedDurationTicks,
            float graveSteedDecayDamagePerSecond,
            int witheringStrikesCooldownTicks,
            int witheringStrikesDurationTicks,
            int witheringStrikesWitherDurationTicks,
            int witheringStrikesWitherAmplifier,
            int deathOathCooldownTicks,
            int deathOathDurationTicks,
            int deathOathRangeBlocks,
            float deathOathSelfDamagePerSecond,
            float deathOathBonusDamage,
            int retributionCooldownTicks,
            int retributionDurationTicks,
            float retributionDamageMultiplier,
            int scytheSweepCooldownTicks,
            int scytheSweepRangeBlocks,
            int scytheSweepArcDegrees,
            float scytheSweepDamage,
            double scytheSweepKnockback,
            int bloodChargeCooldownTicks,
            int bloodChargeMaxChargeTicks,
            float bloodChargeSelfDamagePerSecond,
            float bloodChargeMaxMultiplier,
            int bloodChargeBuffDurationTicks,
            int shadowCloneCooldownTicks,
            int shadowCloneDurationTicks,
            float shadowCloneMaxHealth,
            int shadowCloneCount,
            String shadowCloneEntityId
    ) {
        static Reaper from(GemsBalanceConfig.Reaper cfg) {
            return new Reaper(
                    clampFloat(cfg.undeadWardDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.harvestRegenDurationSeconds, 0, 60),
                    clampInt(cfg.harvestRegenAmplifier, 0, 10),
                    secClamped(cfg.graveSteedCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.graveSteedDurationSeconds, 0, 24 * 3600),
                    clampFloat(cfg.graveSteedDecayDamagePerSecond, 0.0F, 200.0F),
                    secClamped(cfg.witheringStrikesCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.witheringStrikesDurationSeconds, 0, 120),
                    secClamped(cfg.witheringStrikesWitherDurationSeconds, 0, 60),
                    clampInt(cfg.witheringStrikesWitherAmplifier, 0, 10),
                    secClamped(cfg.deathOathCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.deathOathDurationSeconds, 0, 300),
                    clampInt(cfg.deathOathRangeBlocks, 0, 256),
                    clampFloat(cfg.deathOathSelfDamagePerSecond, 0.0F, 200.0F),
                    clampFloat(cfg.deathOathBonusDamage, 0.0F, 200.0F),
                    secClamped(cfg.retributionCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.retributionDurationSeconds, 0, 300),
                    clampFloat(cfg.retributionDamageMultiplier, 0.0F, 10.0F),
                    secClamped(cfg.scytheSweepCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.scytheSweepRangeBlocks, 0, 16),
                    clampInt(cfg.scytheSweepArcDegrees, 10, 180),
                    clampFloat(cfg.scytheSweepDamage, 0.0F, 200.0F),
                    clampDouble(cfg.scytheSweepKnockback, 0.0D, 5.0D),
                    secClamped(cfg.bloodChargeCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.bloodChargeMaxChargeSeconds, 0, 60),
                    clampFloat(cfg.bloodChargeSelfDamagePerSecond, 0.0F, 200.0F),
                    clampFloat(cfg.bloodChargeMaxMultiplier, 1.0F, 10.0F),
                    secClamped(cfg.bloodChargeBuffDurationSeconds, 0, 300),
                    secClamped(cfg.shadowCloneCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.shadowCloneDurationSeconds, 0, 120),
                    clampFloat(cfg.shadowCloneMaxHealth, 1.0F, 200.0F),
                    clampInt(cfg.shadowCloneCount, 1, 50),
                    cfg.shadowCloneEntityId
            );
        }
    }

    public record Pillager(
            float raidersTrainingProjectileVelocityMultiplier,
            int shieldbreakerDisableCooldownTicks,
            float illagerDisciplineThresholdHearts,
            int illagerDisciplineResistanceDurationTicks,
            int illagerDisciplineResistanceAmplifier,
            int illagerDisciplineCooldownTicks,
            int fangsCooldownTicks,
            int fangsRangeBlocks,
            int fangsCount,
            float fangsSpacingBlocks,
            int fangsWarmupStepTicks,
            int ravageCooldownTicks,
            int ravageRangeBlocks,
            float ravageDamage,
            double ravageKnockback,
            int vindicatorBreakCooldownTicks,
            int vindicatorBreakDurationTicks,
            int vindicatorBreakStrengthAmplifier,
            int vindicatorBreakShieldDisableCooldownTicks,
            int volleyCooldownTicks,
            int volleyDurationTicks,
            int volleyPeriodTicks,
            int volleyArrowsPerShot,
            float volleyArrowDamage,
            float volleyArrowVelocity,
            float volleyArrowInaccuracy,
            int warhornCooldownTicks,
            int warhornRadiusBlocks,
            int warhornDurationTicks,
            int warhornAllySpeedAmplifier,
            int warhornAllyResistanceAmplifier,
            int warhornEnemySlownessAmplifier,
            int warhornEnemyWeaknessAmplifier,
            int snareCooldownTicks,
            int snareRangeBlocks,
            int snareDurationTicks,
            int snareSlownessAmplifier
    ) {
        static Pillager from(GemsBalanceConfig.Pillager cfg) {
            return new Pillager(
                    clampFloat(cfg.raidersTrainingProjectileVelocityMultiplier, 1.0F, 3.0F),
                    clampInt(cfg.shieldbreakerDisableCooldownTicks, 0, 20 * 60),
                    clampFloat(cfg.illagerDisciplineThresholdHearts, 1.0F, 20.0F),
                    secClamped(cfg.illagerDisciplineResistanceDurationSeconds, 0, 60),
                    clampInt(cfg.illagerDisciplineResistanceAmplifier, 0, 10),
                    secClamped(cfg.illagerDisciplineCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.fangsCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.fangsRangeBlocks, 0, 128),
                    clampInt(cfg.fangsCount, 0, 32),
                    clampFloat(cfg.fangsSpacingBlocks, 0.1F, 6.0F),
                    clampInt(cfg.fangsWarmupStepTicks, 0, 40),
                    secClamped(cfg.ravageCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.ravageRangeBlocks, 0, 32),
                    clampFloat(cfg.ravageDamage, 0.0F, 200.0F),
                    clampDouble(cfg.ravageKnockback, 0.0D, 5.0D),
                    secClamped(cfg.vindicatorBreakCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.vindicatorBreakDurationSeconds, 0, 120),
                    clampInt(cfg.vindicatorBreakStrengthAmplifier, 0, 10),
                    clampInt(cfg.vindicatorBreakShieldDisableCooldownTicks, 0, 20 * 60),
                    secClamped(cfg.volleyCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.volleyDurationSeconds, 0, 60),
                    clampInt(cfg.volleyPeriodTicks, 1, 40),
                    clampInt(cfg.volleyArrowsPerShot, 0, 8),
                    clampFloat(cfg.volleyArrowDamage, 0.0F, 200.0F),
                    clampFloat(cfg.volleyArrowVelocity, 0.1F, 10.0F),
                    clampFloat(cfg.volleyArrowInaccuracy, 0.0F, 10.0F),
                    secClamped(cfg.warhornCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.warhornRadiusBlocks, 0, 64),
                    secClamped(cfg.warhornDurationSeconds, 0, 60),
                    clampInt(cfg.warhornAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.warhornAllyResistanceAmplifier, 0, 10),
                    clampInt(cfg.warhornEnemySlownessAmplifier, 0, 10),
                    clampInt(cfg.warhornEnemyWeaknessAmplifier, 0, 10),
                    secClamped(cfg.snareCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.snareRangeBlocks, 0, 128),
                    secClamped(cfg.snareDurationSeconds, 0, 60),
                    clampInt(cfg.snareSlownessAmplifier, 0, 10)
            );
        }
    }

    public record Spy(
            int stillnessTicks,
            float stillnessMoveEpsilonBlocks,
            int stillnessInvisRefreshTicks,
            int backstepCooldownTicks,
            double backstepVelocity,
            double backstepUpVelocity,
            float backstabBonusDamage,
            int backstabAngleDegrees,
            int observeRangeBlocks,
            int observeWindowTicks,
            int stealRequiredWitnessCount,
            int maxStolenAbilities,
            int mimicFormCooldownTicks,
            int mimicFormDurationTicks,
            float mimicFormBonusMaxHealth,
            float mimicFormSpeedMultiplier,
            int echoCooldownTicks,
            int echoWindowTicks,
            int stealCooldownTicks,
            int smokeBombCooldownTicks,
            int smokeBombRadiusBlocks,
            int smokeBombDurationTicks,
            int smokeBombBlindnessAmplifier,
            int smokeBombSlownessAmplifier,
            int stolenCastCooldownTicks,
            int skinshiftCooldownTicks,
            int skinshiftDurationTicks,
            int skinshiftRangeBlocks
    ) {
        static Spy from(GemsBalanceConfig.Spy cfg) {
            return new Spy(
                    secClamped(cfg.stillnessSeconds, 0, 60),
                    clampFloat(cfg.stillnessMoveEpsilonBlocks, 0.0F, 2.0F),
                    secClamped(cfg.stillnessInvisRefreshSeconds, 0, 60),
                    secClamped(cfg.backstepCooldownSeconds, 0, 3600),
                    clampDouble(cfg.backstepVelocity, 0.0D, 3.0D),
                    clampDouble(cfg.backstepUpVelocity, 0.0D, 3.0D),
                    clampFloat(cfg.backstabBonusDamage, 0.0F, 200.0F),
                    clampInt(cfg.backstabAngleDegrees, 10, 180),
                    clampInt(cfg.observeRangeBlocks, 0, 128),
                    secClamped(cfg.observeWindowSeconds, 0, 24 * 3600),
                    clampInt(cfg.stealRequiredWitnessCount, 1, 64),
                    clampInt(cfg.maxStolenAbilities, 0, 10),
                    secClamped(cfg.mimicFormCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.mimicFormDurationSeconds, 0, 300),
                    clampFloat(cfg.mimicFormBonusMaxHealth, 0.0F, 200.0F),
                    clampFloat(cfg.mimicFormSpeedMultiplier, 0.1F, 3.0F),
                    secClamped(cfg.echoCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.echoWindowSeconds, 0, 120),
                    secClamped(cfg.stealCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.smokeBombCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.smokeBombRadiusBlocks, 0, 64),
                    secClamped(cfg.smokeBombDurationSeconds, 0, 60),
                    clampInt(cfg.smokeBombBlindnessAmplifier, 0, 10),
                    clampInt(cfg.smokeBombSlownessAmplifier, 0, 10),
                    secClamped(cfg.stolenCastCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.skinshiftCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.skinshiftDurationSeconds, 0, 300),
                    clampInt(cfg.skinshiftRangeBlocks, 0, 128)
            );
        }
    }

    public record Beacon(
            int coreRadiusBlocks,
            int corePulsePeriodTicks,
            int coreRegenDurationTicks,
            int coreRegenAmplifier,
            int stabilizeRadiusBlocks,
            int stabilizeReduceTicksPerSecond,
            int rallyRadiusBlocks,
            int rallyAbsorptionHearts,
            int rallyDurationTicks,
            int auraCooldownTicks,
            int auraDurationTicks,
            int auraRadiusBlocks,
            int auraRefreshTicks,
            int auraSpeedAmplifier,
            int auraHasteAmplifier,
            int auraResistanceAmplifier,
            int auraJumpAmplifier,
            int auraStrengthAmplifier,
            int auraRegenAmplifier
    ) {
        static Beacon from(GemsBalanceConfig.Beacon cfg) {
            return new Beacon(
                    clampInt(cfg.coreRadiusBlocks, 0, 64),
                    secClamped(cfg.corePulsePeriodSeconds, 1, 60),
                    secClamped(cfg.coreRegenDurationSeconds, 1, 60),
                    clampInt(cfg.coreRegenAmplifier, 0, 10),
                    clampInt(cfg.stabilizeRadiusBlocks, 0, 64),
                    clampInt(cfg.stabilizeReduceTicksPerSecond, 0, 200),
                    clampInt(cfg.rallyRadiusBlocks, 0, 64),
                    clampInt(cfg.rallyAbsorptionHearts, 0, 40),
                    secClamped(cfg.rallyDurationSeconds, 0, 120),
                    secClamped(cfg.auraCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.auraDurationSeconds, 0, 600),
                    clampInt(cfg.auraRadiusBlocks, 0, 128),
                    secClamped(cfg.auraRefreshSeconds, 1, 60),
                    clampInt(cfg.auraSpeedAmplifier, 0, 10),
                    clampInt(cfg.auraHasteAmplifier, 0, 10),
                    clampInt(cfg.auraResistanceAmplifier, 0, 10),
                    clampInt(cfg.auraJumpAmplifier, 0, 10),
                    clampInt(cfg.auraStrengthAmplifier, 0, 10),
                    clampInt(cfg.auraRegenAmplifier, 0, 10)
            );
        }
    }

    public record Air(
            float aerialGuardFallDamageMultiplier,
            float aerialGuardDamageMultiplier,
            float aerialGuardKnockbackMultiplier,
            double windShearKnockback,
            int windShearSlownessDurationTicks,
            int windShearSlownessAmplifier,
            int windJumpCooldownTicks,
            double windJumpVerticalVelocity,
            double windJumpForwardVelocity,
            int galeSlamCooldownTicks,
            int galeSlamWindowTicks,
            int galeSlamRadiusBlocks,
            float galeSlamBonusDamage,
            double galeSlamKnockback,
            int crosswindCooldownTicks,
            int crosswindRangeBlocks,
            int crosswindRadiusBlocks,
            float crosswindDamage,
            double crosswindKnockback,
            int crosswindSlownessDurationTicks,
            int crosswindSlownessAmplifier,
            int dashCooldownTicks,
            double dashVelocity,
            double dashUpVelocity,
            int dashIFrameDurationTicks,
            int dashIFrameResistanceAmplifier,
            int airMaceBreachLevel,
            int airMaceWindBurstLevel,
            int airMaceMendingLevel,
            int airMaceUnbreakingLevel,
            int airMaceFireAspectLevel
    ) {
        static Air from(GemsBalanceConfig.Air cfg) {
            return new Air(
                    clampFloat(cfg.aerialGuardFallDamageMultiplier, 0.0F, 1.0F),
                    clampFloat(cfg.aerialGuardDamageMultiplier, 0.0F, 1.0F),
                    clampFloat(cfg.aerialGuardKnockbackMultiplier, 0.0F, 1.0F),
                    clampDouble(cfg.windShearKnockback, 0.0D, 5.0D),
                    secClamped(cfg.windShearSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.windShearSlownessAmplifier, 0, 10),
                    secClamped(cfg.windJumpCooldownSeconds, 0, 24 * 3600),
                    clampDouble(cfg.windJumpVerticalVelocity, 0.0D, 5.0D),
                    clampDouble(cfg.windJumpForwardVelocity, 0.0D, 5.0D),
                    secClamped(cfg.galeSlamCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.galeSlamWindowSeconds, 0, 60),
                    clampInt(cfg.galeSlamRadiusBlocks, 0, 32),
                    clampFloat(cfg.galeSlamBonusDamage, 0.0F, 200.0F),
                    clampDouble(cfg.galeSlamKnockback, 0.0D, 5.0D),
                    secClamped(cfg.crosswindCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.crosswindRangeBlocks, 1, 128),
                    clampInt(cfg.crosswindRadiusBlocks, 0, 32),
                    clampFloat(cfg.crosswindDamage, 0.0F, 200.0F),
                    clampDouble(cfg.crosswindKnockback, 0.0D, 5.0D),
                    secClamped(cfg.crosswindSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.crosswindSlownessAmplifier, 0, 10),
                    secClamped(cfg.dashCooldownSeconds, 0, 24 * 3600),
                    clampDouble(cfg.dashVelocity, 0.0D, 5.0D),
                    clampDouble(cfg.dashUpVelocity, -1.0D, 5.0D),
                    secClamped(cfg.dashIFrameDurationSeconds, 0, 10),
                    clampInt(cfg.dashIFrameResistanceAmplifier, 0, 10),
                    clampInt(cfg.airMaceBreachLevel, 0, 10),
                    clampInt(cfg.airMaceWindBurstLevel, 0, 10),
                    clampInt(cfg.airMaceMendingLevel, 0, 1),
                    clampInt(cfg.airMaceUnbreakingLevel, 0, 10),
                    clampInt(cfg.airMaceFireAspectLevel, 0, 10)
            );
        }
    }

    public record Legendary(
            int craftTicks,
            int craftMaxPerItem,
            int craftMaxActivePerItem,
            int trackerRefreshTicks,
            int trackerMaxDistanceBlocks,
            int recallCooldownTicks,
            float chronoCharmCooldownMultiplier,
            int hypnoHoldTicks,
            int hypnoRangeBlocks,
            int hypnoViewRangeBlocks,
            float hypnoHealHearts,
            int hypnoMaxControlled,
            int hypnoDurationTicks,
            int earthsplitterRadiusBlocks,
            int earthsplitterTunnelLengthBlocks,
            int bloodOathSharpnessCap,
            int demolitionCooldownTicks,
            int demolitionCooldownScalePercent,
            int demolitionFuseTicks,
            int demolitionRangeBlocks,
            float demolitionExplosionPower,
            int demolitionTntCount,
            int hunterAimRangeBlocks,
            int hunterAimTimeoutTicks,
            float hunterAimAssistStrength,
            int thirdStrikeWindowTicks,
            float thirdStrikeBonusDamage,
            float vampiricHealAmount,
            int duelistsRapierParryWindowTicks,
            int duelistsRapierCooldownTicks,
            float duelistsRapierCritDamageMultiplier,
            int challengersGauntletCooldownTicks,
            int challengersGauntletRangeBlocks,
            int reversalMirrorDurationTicks,
            int reversalMirrorCooldownTicks,
            int gladiatorsMarkDurationTicks,
            int gladiatorsMarkCooldownTicks,
            int gladiatorsMarkRangeBlocks,
            float gladiatorsMarkDamageMultiplier,
            int soulShackleDurationTicks,
            int soulShackleCooldownTicks,
            int soulShackleRangeBlocks,
            float soulShackleSplitRatio,
            int experienceBladeMaxSharpness,
            int experienceBladeSharpnessPerTier,
            int experienceBladeXpLevelsPerTier,
            int trophyNecklaceMaxPassives,
            int supremeHelmetNightVisionAmplifier,
            int supremeHelmetWaterBreathingAmplifier,
            int supremeChestStrengthAmplifier,
            int supremeLeggingsFireResAmplifier,
            int supremeBootsSpeedAmplifier,
            int supremeSetResistanceAmplifier,
            Map<Identifier, GemId> recipeGemRequirements
    ) {
        static Legendary from(GemsBalanceConfig.Legendary cfg) {
            return new Legendary(
                    secClamped(cfg.craftSeconds, 1, 24 * 3600),
                    clampInt(cfg.craftMaxPerItem, 0, 100),
                    clampInt(cfg.craftMaxActivePerItem, 0, 100),
                    secClamped(cfg.trackerRefreshSeconds, 1, 60),
                    clampInt(cfg.trackerMaxDistanceBlocks, 0, 200000),
                    secClamped(cfg.recallCooldownSeconds, 0, 3600),
                    clampFloat(cfg.chronoCharmCooldownMultiplier, 0.05F, 1.0F),
                    secClamped(cfg.hypnoHoldSeconds, 1, 30),
                    clampInt(cfg.hypnoRangeBlocks, 1, 64),
                    clampInt(cfg.hypnoViewRangeBlocks, 0, 32),
                    clampFloat(cfg.hypnoHealHearts, 0.0F, 200.0F),
                    clampInt(cfg.hypnoMaxControlled, 1, 64),
                    secClamped(cfg.hypnoDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.earthsplitterRadiusBlocks, 0, 2),
                    clampInt(cfg.earthsplitterTunnelLengthBlocks, 1, 32),
                    clampInt(cfg.bloodOathSharpnessCap, 1, 10),
                    secClamped(cfg.demolitionCooldownSeconds, 0, 3600),
                    clampInt(cfg.demolitionCooldownScalePercent, 0, 200),
                    clampInt(cfg.demolitionFuseTicks, 1, 200),
                    clampInt(cfg.demolitionRangeBlocks, 1, 128),
                    clampFloat(cfg.demolitionExplosionPower, 0.0F, 12.0F),
                    clampInt(cfg.demolitionTntCount, 0, 50),
                    clampInt(cfg.hunterAimRangeBlocks, 1, 256),
                    secClamped(cfg.hunterAimTimeoutSeconds, 0, 3600),
                    clampFloat(cfg.hunterAimAssistStrength, 0.0F, 1.0F),
                    secClamped(cfg.thirdStrikeWindowSeconds, 1, 60),
                    clampFloat(cfg.thirdStrikeBonusDamage, 0.0F, 40.0F),
                    clampFloat(cfg.vampiricHealAmount, 0.0F, 40.0F),
                    clampInt(cfg.duelistsRapierParryWindowTicks, 0, 60),
                    secClamped(cfg.duelistsRapierCooldownSeconds, 0, 3600),
                    clampFloat(cfg.duelistsRapierCritDamageMultiplier, 0.0F, 10.0F),
                    secClamped(cfg.challengersGauntletCooldownSeconds, 0, 3600),
                    clampInt(cfg.challengersGauntletRangeBlocks, 1, 64),
                    secClamped(cfg.reversalMirrorDurationSeconds, 0, 60),
                    secClamped(cfg.reversalMirrorCooldownSeconds, 0, 3600),
                    secClamped(cfg.gladiatorsMarkDurationSeconds, 0, 3600),
                    secClamped(cfg.gladiatorsMarkCooldownSeconds, 0, 3600),
                    clampInt(cfg.gladiatorsMarkRangeBlocks, 1, 64),
                    clampFloat(cfg.gladiatorsMarkDamageMultiplier, 0.0F, 10.0F),
                    secClamped(cfg.soulShackleDurationSeconds, 0, 3600),
                    secClamped(cfg.soulShackleCooldownSeconds, 0, 3600),
                    clampInt(cfg.soulShackleRangeBlocks, 1, 64),
                    clampFloat(cfg.soulShackleSplitRatio, 0.0F, 1.0F),
                    clampInt(cfg.experienceBladeMaxSharpness, 0, 255),
                    clampInt(cfg.experienceBladeSharpnessPerTier, 1, 50),
                    clampInt(cfg.experienceBladeXpLevelsPerTier, 1, 100),
                    clampInt(cfg.trophyNecklaceMaxPassives, 0, 64),
                    clampInt(cfg.supremeHelmetNightVisionAmplifier, 0, 10),
                    clampInt(cfg.supremeHelmetWaterBreathingAmplifier, 0, 10),
                    clampInt(cfg.supremeChestStrengthAmplifier, 0, 10),
                    clampInt(cfg.supremeLeggingsFireResAmplifier, 0, 10),
                    clampInt(cfg.supremeBootsSpeedAmplifier, 0, 10),
                    clampInt(cfg.supremeSetResistanceAmplifier, 0, 10),
                    parseRecipeGemRequirements(cfg.recipeGemRequirements)
            );
        }
    }

    public record VoidGem(boolean blockAllStatusEffects) {
        static VoidGem from(GemsBalanceConfig.VoidGem cfg) {
            return new VoidGem(cfg.blockAllStatusEffects);
        }
    }

    public record Chaos(
            int slotDurationTicks,
            int slotAbilityCooldownTicks,
            int slotCount
    ) {
        static Chaos from(GemsBalanceConfig.Chaos cfg) {
            return new Chaos(
                    secClamped(cfg.slotDurationSeconds, 1, 24 * 3600),
                    secClamped(cfg.slotAbilityCooldownSeconds, 0, 3600),
                    clampInt(cfg.slotCount, 1, 9)
            );
        }
    }

    public record Prism(int maxGemAbilities, int maxGemPassives) {
        static Prism from(GemsBalanceConfig.Prism cfg) {
            return new Prism(
                    clampInt(cfg.maxGemAbilities, 0, 10),
                    clampInt(cfg.maxGemPassives, 0, 10)
            );
        }
    }

    public record Duelist(
            // Passives
            float riposteBonusDamageMultiplier,
            int riposteWindowTicks,
            float focusBonusDamageMultiplier,
            int focusRadiusBlocks,
            float combatStanceSpeedMultiplier,
            // Abilities
            int lungeCooldownTicks,
            int lungeDistanceBlocks,
            float lungeDamage,
            int parryCooldownTicks,
            int parryWindowTicks,
            int parryStunTicks,
            int rapidStrikeCooldownTicks,
            int rapidStrikeDurationTicks,
            int flourishCooldownTicks,
            int flourishRadiusBlocks,
            float flourishDamage,
            int mirrorMatchCooldownTicks,
            int mirrorMatchDurationTicks,
            int mirrorMatchRangeBlocks,
            int bladeDanceCooldownTicks,
            int bladeDanceDurationTicks,
            float bladeDanceStartingMultiplier,
            float bladeDanceIncreasePerHit,
            float bladeDanceMaxMultiplier,
            int bladeDanceResetTicks
    ) {
        static Duelist from(GemsBalanceConfig.Duelist cfg) {
            return new Duelist(
                    clampFloat(cfg.riposteBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.riposteWindowSeconds, 0, 60),
                    clampFloat(cfg.focusBonusDamageMultiplier, 1.0F, 5.0F),
                    clampInt(cfg.focusRadiusBlocks, 0, 64),
                    clampFloat(cfg.combatStanceSpeedMultiplier, 1.0F, 2.0F),
                    secClamped(cfg.lungeCooldownSeconds, 0, 3600),
                    clampInt(cfg.lungeDistanceBlocks, 1, 32),
                    clampFloat(cfg.lungeDamage, 0.0F, 200.0F),
                    secClamped(cfg.parryCooldownSeconds, 0, 3600),
                    clampInt(cfg.parryWindowTicks, 1, 100),
                    secClamped(cfg.parryStunSeconds, 0, 60),
                    secClamped(cfg.rapidStrikeCooldownSeconds, 0, 3600),
                    secClamped(cfg.rapidStrikeDurationSeconds, 0, 60),
                    secClamped(cfg.flourishCooldownSeconds, 0, 3600),
                    clampInt(cfg.flourishRadiusBlocks, 0, 32),
                    clampFloat(cfg.flourishDamage, 0.0F, 200.0F),
                    secClamped(cfg.mirrorMatchCooldownSeconds, 0, 3600),
                    secClamped(cfg.mirrorMatchDurationSeconds, 0, 120),
                    clampInt(cfg.mirrorMatchRangeBlocks, 0, 64),
                    secClamped(cfg.bladeDanceCooldownSeconds, 0, 3600),
                    secClamped(cfg.bladeDanceDurationSeconds, 0, 120),
                    clampFloat(cfg.bladeDanceStartingMultiplier, 0.0F, 5.0F),
                    clampFloat(cfg.bladeDanceIncreasePerHit, 0.0F, 1.0F),
                    clampFloat(cfg.bladeDanceMaxMultiplier, 1.0F, 10.0F),
                    secClamped(cfg.bladeDanceResetSeconds, 0, 60)
            );
        }
    }

    public record Hunter(
            // Passives
            float preyMarkBonusDamageMultiplier,
            int preyMarkDurationTicks,
            int trackersEyeRangeBlocks,
            int trophyHunterDurationTicks,
            // Abilities
            int huntingTrapCooldownTicks,
            int huntingTrapRootTicks,
            float huntingTrapDamage,
            int huntingTrapLifetimeTicks,
            int pounceCooldownTicks,
            int pounceRangeBlocks,
            float pounceDamage,
            int netShotCooldownTicks,
            int netShotSlowTicks,
            int netShotRangeBlocks,
            int cripplingCooldownTicks,
            float cripplingSlowMultiplier,
            int cripplingDurationTicks,
            int cripplingRangeBlocks,
            int packTacticsCooldownTicks,
            float packTacticsBonusDamageMultiplier,
            int packTacticsDurationTicks,
            int packTacticsRadiusBlocks,
            // Six-Pack Pain
            int sixPackPainCooldownTicks,
            int sixPackPainCloneCount,
            int sixPackPainDurationTicks,
            float sixPackPainHealthPerClone,
            int sixPackPainCloseTargetRangeBlocks,
            int sixPackPainWideTargetRangeBlocks,
            int sixPackPainBuffDurationTicks,
            int sixPackPainDebuffDurationTicks,
            // Origin Tracking
            int originTrackingCooldownTicks,
            int originTrackingDurationTicks
    ) {
        static Hunter from(GemsBalanceConfig.Hunter cfg) {
            return new Hunter(
                    clampFloat(cfg.preyMarkBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.preyMarkDurationSeconds, 0, 600),
                    clampInt(cfg.trackersEyeRangeBlocks, 0, 128),
                    secClamped(cfg.trophyHunterDurationSeconds, 0, 600),
                    secClamped(cfg.huntingTrapCooldownSeconds, 0, 3600),
                    secClamped(cfg.huntingTrapRootSeconds, 0, 60),
                    clampFloat(cfg.huntingTrapDamage, 0.0F, 200.0F),
                    secClamped(cfg.huntingTrapLifetimeSeconds, 0, 600),
                    secClamped(cfg.pounceCooldownSeconds, 0, 3600),
                    clampInt(cfg.pounceRangeBlocks, 0, 64),
                    clampFloat(cfg.pounceDamage, 0.0F, 200.0F),
                    secClamped(cfg.netShotCooldownSeconds, 0, 3600),
                    secClamped(cfg.netShotSlowSeconds, 0, 60),
                    clampInt(cfg.netShotRangeBlocks, 0, 64),
                    secClamped(cfg.cripplingCooldownSeconds, 0, 3600),
                    clampFloat(cfg.cripplingSlowMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.cripplingDurationSeconds, 0, 60),
                    clampInt(cfg.cripplingRangeBlocks, 0, 64),
                    secClamped(cfg.packTacticsCooldownSeconds, 0, 3600),
                    clampFloat(cfg.packTacticsBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.packTacticsDurationSeconds, 0, 120),
                    clampInt(cfg.packTacticsRadiusBlocks, 0, 64),
                    secClamped(cfg.sixPackPainCooldownSeconds, 0, 3600),
                    clampInt(cfg.sixPackPainCloneCount, 1, 10),
                    secClamped(cfg.sixPackPainDurationSeconds, 0, 600),
                    clampFloat(cfg.sixPackPainHealthPerClone, 1.0F, 100.0F),
                    clampInt(cfg.sixPackPainCloseTargetRangeBlocks, 1, 64),
                    clampInt(cfg.sixPackPainWideTargetRangeBlocks, 1, 128),
                    clampInt(cfg.sixPackPainBuffDurationTicks, 0, 6000),
                    clampInt(cfg.sixPackPainDebuffDurationTicks, 0, 6000),
                    secClamped(cfg.originTrackingCooldownSeconds, 0, 3600),
                    secClamped(cfg.originTrackingDurationSeconds, 0, 600)
            );
        }
    }

    public record Sentinel(
            // Passives
            float guardianAuraDamageReduction,
            int guardianAuraRadiusBlocks,
            int fortressStandStillTicks,
            int fortressResistanceAmplifier,
            float retributionThornsDamageMultiplier,
            // Abilities
            int shieldWallCooldownTicks,
            int shieldWallDurationTicks,
            int shieldWallWidthBlocks,
            int shieldWallHeightBlocks,
            int tauntCooldownTicks,
            int tauntDurationTicks,
            int tauntRadiusBlocks,
            float tauntDamageReduction,
            int interventionCooldownTicks,
            int interventionRangeBlocks,
            int rallyCryCooldownTicks,
            float rallyCryHealHearts,
            int rallyCryResistanceDurationTicks,
            int rallyCryRadiusBlocks,
            int lockdownCooldownTicks,
            int lockdownDurationTicks,
            int lockdownRadiusBlocks
    ) {
        static Sentinel from(GemsBalanceConfig.Sentinel cfg) {
            return new Sentinel(
                    clampFloat(cfg.guardianAuraDamageReduction, 0.0F, 1.0F),
                    clampInt(cfg.guardianAuraRadiusBlocks, 0, 64),
                    secClamped(cfg.fortressStandStillSeconds, 0, 60),
                    clampInt(cfg.fortressResistanceAmplifier, 0, 10),
                    clampFloat(cfg.retributionThornsDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.shieldWallCooldownSeconds, 0, 3600),
                    secClamped(cfg.shieldWallDurationSeconds, 0, 120),
                    clampInt(cfg.shieldWallWidthBlocks, 1, 16),
                    clampInt(cfg.shieldWallHeightBlocks, 1, 16),
                    secClamped(cfg.tauntCooldownSeconds, 0, 3600),
                    secClamped(cfg.tauntDurationSeconds, 0, 60),
                    clampInt(cfg.tauntRadiusBlocks, 0, 64),
                    clampFloat(cfg.tauntDamageReduction, 0.0F, 1.0F),
                    secClamped(cfg.interventionCooldownSeconds, 0, 3600),
                    clampInt(cfg.interventionRangeBlocks, 0, 64),
                    secClamped(cfg.rallyCryCooldownSeconds, 0, 3600),
                    clampFloat(cfg.rallyCryHealHearts, 0.0F, 200.0F),
                    secClamped(cfg.rallyCryResistanceDurationSeconds, 0, 120),
                    clampInt(cfg.rallyCryRadiusBlocks, 0, 64),
                    secClamped(cfg.lockdownCooldownSeconds, 0, 3600),
                    secClamped(cfg.lockdownDurationSeconds, 0, 120),
                    clampInt(cfg.lockdownRadiusBlocks, 0, 64)
            );
        }
    }

    public record Trickster(
            // Passives
            float sleightOfHandChance,
            float slipperyChance,
            // Abilities
            int shadowSwapCooldownTicks,
            int shadowSwapRangeBlocks,
            int mirageCooldownTicks,
            int mirageDurationTicks,
            int mirageCloneCount,
            int mirageRangeBlocks,
            int mirageBuffDurationTicks,
            int mirageBuffAmplifier,
            int glitchStepCooldownTicks,
            int glitchStepDistanceBlocks,
            float glitchStepAfterimgDamage,
            int glitchStepAfterimgRadiusBlocks,
            int puppetMasterCooldownTicks,
            int puppetMasterDurationTicks,
            int puppetMasterRangeBlocks,
            int mindGamesCooldownTicks,
            int mindGamesDurationTicks,
            int mindGamesRangeBlocks
    ) {
        static Trickster from(GemsBalanceConfig.Trickster cfg) {
            return new Trickster(
                    clampFloat(cfg.sleightOfHandChance, 0.0F, 1.0F),
                    clampFloat(cfg.slipperyChance, 0.0F, 1.0F),
                    secClamped(cfg.shadowSwapCooldownSeconds, 0, 3600),
                    clampInt(cfg.shadowSwapRangeBlocks, 1, 128),
                    secClamped(cfg.mirageCooldownSeconds, 0, 3600),
                    secClamped(cfg.mirageDurationSeconds, 0, 120),
                    clampInt(cfg.mirageCloneCount, 0, 16),
                    clampInt(cfg.mirageRangeBlocks, 0, 128),
                    secClamped(cfg.mirageBuffDurationSeconds, 0, 120),
                    clampInt(cfg.mirageBuffAmplifier, 0, 4),
                    secClamped(cfg.glitchStepCooldownSeconds, 0, 3600),
                    clampInt(cfg.glitchStepDistanceBlocks, 0, 32),
                    clampFloat(cfg.glitchStepAfterimgDamage, 0.0F, 200.0F),
                    clampInt(cfg.glitchStepAfterimgRadiusBlocks, 0, 16),
                    secClamped(cfg.puppetMasterCooldownSeconds, 0, 3600),
                    secClamped(cfg.puppetMasterDurationSeconds, 0, 60),
                    clampInt(cfg.puppetMasterRangeBlocks, 0, 64),
                    secClamped(cfg.mindGamesCooldownSeconds, 0, 3600),
                    secClamped(cfg.mindGamesDurationSeconds, 0, 60),
                    clampInt(cfg.mindGamesRangeBlocks, 0, 64)
            );
        }
    }

    public static final class BonusPool {
        public final int maxBonusAbilities;
        public final int maxBonusPassives;
        public final boolean showBonusesInHud;
        public final float bonusAbilityCooldownMultiplier;
        public final float bonusAbilityDamageMultiplier;
        public final float bonusPassiveEffectMultiplier;

        public final int thunderstrikeCooldownSeconds;
        public final float thunderstrikeDamage;
        public final int thunderstrikeRangeBlocks;
        public final int frostbiteCooldownSeconds;
        public final float frostbiteDamage;
        public final int frostbiteFreezeSeconds;
        public final int frostbiteRangeBlocks;
        public final int earthshatterCooldownSeconds;
        public final float earthshatterDamage;
        public final int earthshatterRadiusBlocks;
        public final int shadowstepCooldownSeconds;
        public final int shadowstepDistanceBlocks;
        public final int radiantBurstCooldownSeconds;
        public final float radiantBurstDamage;
        public final int radiantBurstRadiusBlocks;
        public final int radiantBurstBlindSeconds;
        public final int venomsprayCooldownSeconds;
        public final int venomsprayPoisonSeconds;
        public final int venomsprayConeAngleDegrees;
        public final int venomsprayRangeBlocks;
        public final int timewarpCooldownSeconds;
        public final int timewarpDurationSeconds;
        public final int timewarpRadiusBlocks;
        public final int timewarpSlownessAmplifier;
        public final int decoyTrapCooldownSeconds;
        public final float decoyTrapExplosionDamage;
        public final int decoyTrapArmTimeSeconds;
        public final int decoyTrapMaxActive;
        public final int decoyTrapDespawnSeconds;
        public final int gravityWellCooldownSeconds;
        public final int gravityWellDurationSeconds;
        public final int gravityWellRadiusBlocks;
        public final float gravityWellPullStrength;
        public final int chainLightningCooldownSeconds;
        public final float chainLightningDamage;
        public final int chainLightningMaxBounces;
        public final int chainLightningBounceRangeBlocks;
        public final int magmaPoolCooldownSeconds;
        public final float magmaPoolDamagePerSecond;
        public final int magmaPoolDurationSeconds;
        public final int magmaPoolRadiusBlocks;
        public final int iceWallCooldownSeconds;
        public final int iceWallDurationSeconds;
        public final int iceWallWidthBlocks;
        public final int iceWallHeightBlocks;
        public final int windSlashCooldownSeconds;
        public final float windSlashDamage;
        public final int windSlashRangeBlocks;
        public final int curseBoltCooldownSeconds;
        public final float curseBoltDamage;
        public final int curseBoltEffectDurationSeconds;
        public final int berserkerRageCooldownSeconds;
        public final int berserkerRageDurationSeconds;
        public final float berserkerRageDamageBoostPercent;
        public final float berserkerRageDamageTakenBoostPercent;
        public final int etherealStepCooldownSeconds;
        public final int etherealStepDistanceBlocks;
        public final int arcaneMissilesCooldownSeconds;
        public final float arcaneMissilesDamagePerMissile;
        public final int arcaneMissilesCount;
        public final int lifeTapCooldownSeconds;
        public final float lifeTapHealthCost;
        public final float lifeTapCooldownReductionPercent;
        public final int doomBoltCooldownSeconds;
        public final float doomBoltDamage;
        public final float doomBoltVelocity;
        public final int sanctuaryCooldownSeconds;
        public final int sanctuaryDurationSeconds;
        public final int sanctuaryRadiusBlocks;
        public final int spectralChainsCooldownSeconds;
        public final int spectralChainsDurationSeconds;
        public final int spectralChainsRangeBlocks;
        public final int voidRiftCooldownSeconds;
        public final float voidRiftDamagePerSecond;
        public final int voidRiftDurationSeconds;
        public final int voidRiftRadiusBlocks;
        public final int infernoDashCooldownSeconds;
        public final float infernoDashDamage;
        public final int infernoDashDistanceBlocks;
        public final int infernoDashFireDurationSeconds;
        public final int tidalWaveCooldownSeconds;
        public final float tidalWaveDamage;
        public final int tidalWaveRangeBlocks;
        public final int tidalWaveSlowSeconds;
        public final int starfallCooldownSeconds;
        public final float starfallDamagePerHit;
        public final int starfallMeteorCount;
        public final int starfallRadiusBlocks;
        public final int bloodlustCooldownSeconds;
        public final int bloodlustDurationSeconds;
        public final float bloodlustAttackSpeedPerKill;
        public final int bloodlustMaxStacks;
        public final int crystalCageCooldownSeconds;
        public final int crystalCageDurationSeconds;
        public final int crystalCageRangeBlocks;
        public final int phantasmCooldownSeconds;
        public final int phantasmDurationSeconds;
        public final float phantasmExplosionDamage;
        public final int sonicBoomCooldownSeconds;
        public final float sonicBoomDamage;
        public final int sonicBoomRadiusBlocks;
        public final float sonicBoomKnockbackStrength;
        public final int vampiricTouchCooldownSeconds;
        public final float vampiricTouchDamage;
        public final float vampiricTouchHealPercent;
        public final int blindingFlashCooldownSeconds;
        public final int blindingFlashBlindSeconds;
        public final int blindingFlashRadiusBlocks;
        public final int stormCallCooldownSeconds;
        public final float stormCallDamagePerStrike;
        public final int stormCallDurationSeconds;
        public final int stormCallRadiusBlocks;
        public final int stormCallStrikesPerSecond;
        public final int quicksandCooldownSeconds;
        public final int quicksandDurationSeconds;
        public final int quicksandRadiusBlocks;
        public final int quicksandSlownessAmplifier;
        public final int searingLightCooldownSeconds;
        public final float searingLightDamage;
        public final float searingLightUndeadBonusDamage;
        public final int searingLightRangeBlocks;
        public final int spectralBladeCooldownSeconds;
        public final float spectralBladeDamage;
        public final int spectralBladeDurationSeconds;
        public final int netherPortalCooldownSeconds;
        public final int netherPortalDistanceBlocks;
        public final int entangleCooldownSeconds;
        public final int entangleDurationSeconds;
        public final int entangleRadiusBlocks;
        public final int mindSpikeCooldownSeconds;
        public final float mindSpikeDamagePerSecond;
        public final int mindSpikeDurationSeconds;
        public final int mindSpikeRangeBlocks;
        public final int seismicSlamCooldownSeconds;
        public final float seismicSlamDamage;
        public final int seismicSlamRadiusBlocks;
        public final float seismicSlamKnockupStrength;
        public final int icicleBarrageCooldownSeconds;
        public final float icicleBarrageDamagePerIcicle;
        public final int icicleBarrageCount;
        public final int icicleBarrageRangeBlocks;
        public final int banishmentCooldownSeconds;
        public final int banishmentDistanceBlocks;
        public final int banishmentRangeBlocks;
        public final int corpseExplosionCooldownSeconds;
        public final float corpseExplosionDamage;
        public final int corpseExplosionRadiusBlocks;
        public final int corpseExplosionCorpseRangeBlocks;
        public final int corpseExplosionMarkDurationSeconds;
        public final int soulSwapCooldownSeconds;
        public final int soulSwapRangeBlocks;
        public final int markOfDeathCooldownSeconds;
        public final int markOfDeathDurationSeconds;
        public final float markOfDeathBonusDamagePercent;
        public final int markOfDeathRangeBlocks;
        public final int ironMaidenCooldownSeconds;
        public final int ironMaidenDurationSeconds;
        public final float ironMaidenReflectPercent;
        public final int warpStrikeCooldownSeconds;
        public final float warpStrikeDamage;
        public final int warpStrikeRangeBlocks;
        public final int vortexStrikeCooldownSeconds;
        public final float vortexStrikeDamage;
        public final int vortexStrikeRadiusBlocks;
        public final float vortexStrikePullStrength;
        public final int plagueCloudCooldownSeconds;
        public final int plagueCloudDurationSeconds;
        public final int plagueCloudPoisonAmplifier;
        public final int plagueCloudWeaknessAmplifier;
        public final int overchargeCooldownSeconds;
        public final float overchargeDamageMultiplier;
        public final float overchargeHealthCost;
        public final int overchargeDurationSeconds;
        public final int gravityCrushCooldownSeconds;
        public final float gravityCrushDamage;
        public final int gravityCrushRootDurationSeconds;
        public final int gravityCrushRangeBlocks;

        public final float thornsAuraDamagePercent;
        public final float lifestealPercent;
        public final float dodgeChancePercent;
        public final float criticalStrikeBonusDamagePercent;
        public final float criticalStrikeChanceBonus;
        public final float manaShieldXpPerDamage;
        public final int regenerationBoostAmplifier;
        public final float damageReductionPercent;
        public final float attackSpeedBoostPercent;
        public final float reachExtendBlocks;
        public final float impactAbsorbPercent;
        public final int impactAbsorbMaxAbsorption;
        public final int adrenalineSurgeDurationSeconds;
        public final int adrenalineSurgeCooldownSeconds;
        public final float intimidateDamageReductionPercent;
        public final int intimidateRadiusBlocks;
        public final int evasiveRollCooldownSeconds;
        public final int evasiveRollDistanceBlocks;
        public final float combatMeditateHealPerSecond;
        public final int combatMeditateDelaySeconds;
        public final float weaponMasteryBonusDamage;
        public final float cullingBladeThresholdPercent;
        public final float thickSkinProjectileReductionPercent;
        public final float xpBoostPercent;
        public final float hungerResistReductionPercent;
        public final boolean poisonImmunityFull;
        public final int secondWindCooldownSeconds;
        public final float echoStrikeChancePercent;
        public final float chainBreakerDurationReductionPercent;
        public final float stoneSkinFlatReduction;
        public final int arcaneBarrierCooldownSeconds;
        public final float arcaneBarrierAbsorbAmount;
        public final float predatorSenseThresholdPercent;
        public final int predatorSenseRangeBlocks;
        public final float battleMedicHealPerSecond;
        public final int battleMedicRadiusBlocks;
        public final float lastStandThresholdPercent;
        public final float lastStandDamageBoostPercent;
        public final float executionerThresholdPercent;
        public final float executionerBonusDamagePercent;
        public final float bloodthirstHealOnKill;
        public final boolean steelResolveFullKnockbackImmunity;
        public final float elementalHarmonyReductionPercent;
        public final float treasureHunterDropBoostPercent;
        public final float counterStrikeDamageMultiplier;
        public final int counterStrikeWindowSeconds;
        public final float bulwarkBlockEffectivenessBoostPercent;
        public final float quickRecoveryDebuffReductionPercent;
        public final int overfowingVitalityBonusHearts;
        public final float magneticPullRangeMultiplier;
        public final float vengeanceBuffDurationSeconds;
        public final float vengeanceDamageBoostPercent;
        public final float nemesisBonusDamagePercent;
        public final float huntersInstinctCritBoostPercent;
        public final float berserkerBloodMaxAttackSpeedBoost;
        public final float opportunistBackstabBonusPercent;
        public final float ironcladArmorBoostPercent;
        public final float mistFormPhaseChancePercent;
        public final int warCryRadiusBlocks;
        public final int warCryStrengthDurationSeconds;
        public final int siphonSoulRegenDurationSeconds;
        public final float unbreakableDurabilityReductionPercent;
        public final float focusedMindCooldownReductionPercent;
        public final int sixthSenseWarningRangeBlocks;

        private BonusPool(
                int maxBonusAbilities,
                int maxBonusPassives,
                boolean showBonusesInHud,
                float bonusAbilityCooldownMultiplier,
                float bonusAbilityDamageMultiplier,
                float bonusPassiveEffectMultiplier,
                int thunderstrikeCooldownSeconds,
                float thunderstrikeDamage,
                int thunderstrikeRangeBlocks,
                int frostbiteCooldownSeconds,
                float frostbiteDamage,
                int frostbiteFreezeSeconds,
                int frostbiteRangeBlocks,
                int earthshatterCooldownSeconds,
                float earthshatterDamage,
                int earthshatterRadiusBlocks,
                int shadowstepCooldownSeconds,
                int shadowstepDistanceBlocks,
                int radiantBurstCooldownSeconds,
                float radiantBurstDamage,
                int radiantBurstRadiusBlocks,
                int radiantBurstBlindSeconds,
                int venomsprayCooldownSeconds,
                int venomsprayPoisonSeconds,
                int venomsprayConeAngleDegrees,
                int venomsprayRangeBlocks,
                int timewarpCooldownSeconds,
                int timewarpDurationSeconds,
                int timewarpRadiusBlocks,
                int timewarpSlownessAmplifier,
                int decoyTrapCooldownSeconds,
                float decoyTrapExplosionDamage,
                int decoyTrapArmTimeSeconds,
                int decoyTrapMaxActive,
                int decoyTrapDespawnSeconds,
                int gravityWellCooldownSeconds,
                int gravityWellDurationSeconds,
                int gravityWellRadiusBlocks,
                float gravityWellPullStrength,
                int chainLightningCooldownSeconds,
                float chainLightningDamage,
                int chainLightningMaxBounces,
                int chainLightningBounceRangeBlocks,
                int magmaPoolCooldownSeconds,
                float magmaPoolDamagePerSecond,
                int magmaPoolDurationSeconds,
                int magmaPoolRadiusBlocks,
                int iceWallCooldownSeconds,
                int iceWallDurationSeconds,
                int iceWallWidthBlocks,
                int iceWallHeightBlocks,
                int windSlashCooldownSeconds,
                float windSlashDamage,
                int windSlashRangeBlocks,
                int curseBoltCooldownSeconds,
                float curseBoltDamage,
                int curseBoltEffectDurationSeconds,
                int berserkerRageCooldownSeconds,
                int berserkerRageDurationSeconds,
                float berserkerRageDamageBoostPercent,
                float berserkerRageDamageTakenBoostPercent,
                int etherealStepCooldownSeconds,
                int etherealStepDistanceBlocks,
                int arcaneMissilesCooldownSeconds,
                float arcaneMissilesDamagePerMissile,
                int arcaneMissilesCount,
                int lifeTapCooldownSeconds,
                float lifeTapHealthCost,
                float lifeTapCooldownReductionPercent,
                int doomBoltCooldownSeconds,
                float doomBoltDamage,
                float doomBoltVelocity,
                int sanctuaryCooldownSeconds,
                int sanctuaryDurationSeconds,
                int sanctuaryRadiusBlocks,
                int spectralChainsCooldownSeconds,
                int spectralChainsDurationSeconds,
                int spectralChainsRangeBlocks,
                int voidRiftCooldownSeconds,
                float voidRiftDamagePerSecond,
                int voidRiftDurationSeconds,
                int voidRiftRadiusBlocks,
                int infernoDashCooldownSeconds,
                float infernoDashDamage,
                int infernoDashDistanceBlocks,
                int infernoDashFireDurationSeconds,
                int tidalWaveCooldownSeconds,
                float tidalWaveDamage,
                int tidalWaveRangeBlocks,
                int tidalWaveSlowSeconds,
                int starfallCooldownSeconds,
                float starfallDamagePerHit,
                int starfallMeteorCount,
                int starfallRadiusBlocks,
                int bloodlustCooldownSeconds,
                int bloodlustDurationSeconds,
                float bloodlustAttackSpeedPerKill,
                int bloodlustMaxStacks,
                int crystalCageCooldownSeconds,
                int crystalCageDurationSeconds,
                int crystalCageRangeBlocks,
                int phantasmCooldownSeconds,
                int phantasmDurationSeconds,
                float phantasmExplosionDamage,
                int sonicBoomCooldownSeconds,
                float sonicBoomDamage,
                int sonicBoomRadiusBlocks,
                float sonicBoomKnockbackStrength,
                int vampiricTouchCooldownSeconds,
                float vampiricTouchDamage,
                float vampiricTouchHealPercent,
                int blindingFlashCooldownSeconds,
                int blindingFlashBlindSeconds,
                int blindingFlashRadiusBlocks,
                int stormCallCooldownSeconds,
                float stormCallDamagePerStrike,
                int stormCallDurationSeconds,
                int stormCallRadiusBlocks,
                int stormCallStrikesPerSecond,
                int quicksandCooldownSeconds,
                int quicksandDurationSeconds,
                int quicksandRadiusBlocks,
                int quicksandSlownessAmplifier,
                int searingLightCooldownSeconds,
                float searingLightDamage,
                float searingLightUndeadBonusDamage,
                int searingLightRangeBlocks,
                int spectralBladeCooldownSeconds,
                float spectralBladeDamage,
                int spectralBladeDurationSeconds,
                int netherPortalCooldownSeconds,
                int netherPortalDistanceBlocks,
                int entangleCooldownSeconds,
                int entangleDurationSeconds,
                int entangleRadiusBlocks,
                int mindSpikeCooldownSeconds,
                float mindSpikeDamagePerSecond,
                int mindSpikeDurationSeconds,
                int mindSpikeRangeBlocks,
                int seismicSlamCooldownSeconds,
                float seismicSlamDamage,
                int seismicSlamRadiusBlocks,
                float seismicSlamKnockupStrength,
                int icicleBarrageCooldownSeconds,
                float icicleBarrageDamagePerIcicle,
                int icicleBarrageCount,
                int icicleBarrageRangeBlocks,
                int banishmentCooldownSeconds,
                int banishmentDistanceBlocks,
                int banishmentRangeBlocks,
                int corpseExplosionCooldownSeconds,
                float corpseExplosionDamage,
                int corpseExplosionRadiusBlocks,
                int corpseExplosionCorpseRangeBlocks,
                int corpseExplosionMarkDurationSeconds,
                int soulSwapCooldownSeconds,
                int soulSwapRangeBlocks,
                int markOfDeathCooldownSeconds,
                int markOfDeathDurationSeconds,
                float markOfDeathBonusDamagePercent,
                int markOfDeathRangeBlocks,
                int ironMaidenCooldownSeconds,
                int ironMaidenDurationSeconds,
                float ironMaidenReflectPercent,
                int warpStrikeCooldownSeconds,
                float warpStrikeDamage,
                int warpStrikeRangeBlocks,
                int vortexStrikeCooldownSeconds,
                float vortexStrikeDamage,
                int vortexStrikeRadiusBlocks,
                float vortexStrikePullStrength,
                int plagueCloudCooldownSeconds,
                int plagueCloudDurationSeconds,
                int plagueCloudPoisonAmplifier,
                int plagueCloudWeaknessAmplifier,
                int overchargeCooldownSeconds,
                float overchargeDamageMultiplier,
                float overchargeHealthCost,
                int overchargeDurationSeconds,
                int gravityCrushCooldownSeconds,
                float gravityCrushDamage,
                int gravityCrushRootDurationSeconds,
                int gravityCrushRangeBlocks,
                float thornsAuraDamagePercent,
                float lifestealPercent,
                float dodgeChancePercent,
                float criticalStrikeBonusDamagePercent,
                float criticalStrikeChanceBonus,
                float manaShieldXpPerDamage,
                int regenerationBoostAmplifier,
                float damageReductionPercent,
                float attackSpeedBoostPercent,
                float reachExtendBlocks,
                float impactAbsorbPercent,
                int impactAbsorbMaxAbsorption,
                int adrenalineSurgeDurationSeconds,
                int adrenalineSurgeCooldownSeconds,
                float intimidateDamageReductionPercent,
                int intimidateRadiusBlocks,
                int evasiveRollCooldownSeconds,
                int evasiveRollDistanceBlocks,
                float combatMeditateHealPerSecond,
                int combatMeditateDelaySeconds,
                float weaponMasteryBonusDamage,
                float cullingBladeThresholdPercent,
                float thickSkinProjectileReductionPercent,
                float xpBoostPercent,
                float hungerResistReductionPercent,
                boolean poisonImmunityFull,
                int secondWindCooldownSeconds,
                float echoStrikeChancePercent,
                float chainBreakerDurationReductionPercent,
                float stoneSkinFlatReduction,
                int arcaneBarrierCooldownSeconds,
                float arcaneBarrierAbsorbAmount,
                float predatorSenseThresholdPercent,
                int predatorSenseRangeBlocks,
                float battleMedicHealPerSecond,
                int battleMedicRadiusBlocks,
                float lastStandThresholdPercent,
                float lastStandDamageBoostPercent,
                float executionerThresholdPercent,
                float executionerBonusDamagePercent,
                float bloodthirstHealOnKill,
                boolean steelResolveFullKnockbackImmunity,
                float elementalHarmonyReductionPercent,
                float treasureHunterDropBoostPercent,
                float counterStrikeDamageMultiplier,
                int counterStrikeWindowSeconds,
                float bulwarkBlockEffectivenessBoostPercent,
                float quickRecoveryDebuffReductionPercent,
                int overfowingVitalityBonusHearts,
                float magneticPullRangeMultiplier,
                float vengeanceBuffDurationSeconds,
                float vengeanceDamageBoostPercent,
                float nemesisBonusDamagePercent,
                float huntersInstinctCritBoostPercent,
                float berserkerBloodMaxAttackSpeedBoost,
                float opportunistBackstabBonusPercent,
                float ironcladArmorBoostPercent,
                float mistFormPhaseChancePercent,
                int warCryRadiusBlocks,
                int warCryStrengthDurationSeconds,
                int siphonSoulRegenDurationSeconds,
                float unbreakableDurabilityReductionPercent,
                float focusedMindCooldownReductionPercent,
                int sixthSenseWarningRangeBlocks) {
            this.maxBonusAbilities = maxBonusAbilities;
            this.maxBonusPassives = maxBonusPassives;
            this.showBonusesInHud = showBonusesInHud;
            this.bonusAbilityCooldownMultiplier = bonusAbilityCooldownMultiplier;
            this.bonusAbilityDamageMultiplier = bonusAbilityDamageMultiplier;
            this.bonusPassiveEffectMultiplier = bonusPassiveEffectMultiplier;
            this.thunderstrikeCooldownSeconds = thunderstrikeCooldownSeconds;
            this.thunderstrikeDamage = thunderstrikeDamage;
            this.thunderstrikeRangeBlocks = thunderstrikeRangeBlocks;
            this.frostbiteCooldownSeconds = frostbiteCooldownSeconds;
            this.frostbiteDamage = frostbiteDamage;
            this.frostbiteFreezeSeconds = frostbiteFreezeSeconds;
            this.frostbiteRangeBlocks = frostbiteRangeBlocks;
            this.earthshatterCooldownSeconds = earthshatterCooldownSeconds;
            this.earthshatterDamage = earthshatterDamage;
            this.earthshatterRadiusBlocks = earthshatterRadiusBlocks;
            this.shadowstepCooldownSeconds = shadowstepCooldownSeconds;
            this.shadowstepDistanceBlocks = shadowstepDistanceBlocks;
            this.radiantBurstCooldownSeconds = radiantBurstCooldownSeconds;
            this.radiantBurstDamage = radiantBurstDamage;
            this.radiantBurstRadiusBlocks = radiantBurstRadiusBlocks;
            this.radiantBurstBlindSeconds = radiantBurstBlindSeconds;
            this.venomsprayCooldownSeconds = venomsprayCooldownSeconds;
            this.venomsprayPoisonSeconds = venomsprayPoisonSeconds;
            this.venomsprayConeAngleDegrees = venomsprayConeAngleDegrees;
            this.venomsprayRangeBlocks = venomsprayRangeBlocks;
            this.timewarpCooldownSeconds = timewarpCooldownSeconds;
            this.timewarpDurationSeconds = timewarpDurationSeconds;
            this.timewarpRadiusBlocks = timewarpRadiusBlocks;
            this.timewarpSlownessAmplifier = timewarpSlownessAmplifier;
            this.decoyTrapCooldownSeconds = decoyTrapCooldownSeconds;
            this.decoyTrapExplosionDamage = decoyTrapExplosionDamage;
            this.decoyTrapArmTimeSeconds = decoyTrapArmTimeSeconds;
            this.decoyTrapMaxActive = decoyTrapMaxActive;
            this.decoyTrapDespawnSeconds = decoyTrapDespawnSeconds;
            this.gravityWellCooldownSeconds = gravityWellCooldownSeconds;
            this.gravityWellDurationSeconds = gravityWellDurationSeconds;
            this.gravityWellRadiusBlocks = gravityWellRadiusBlocks;
            this.gravityWellPullStrength = gravityWellPullStrength;
            this.chainLightningCooldownSeconds = chainLightningCooldownSeconds;
            this.chainLightningDamage = chainLightningDamage;
            this.chainLightningMaxBounces = chainLightningMaxBounces;
            this.chainLightningBounceRangeBlocks = chainLightningBounceRangeBlocks;
            this.magmaPoolCooldownSeconds = magmaPoolCooldownSeconds;
            this.magmaPoolDamagePerSecond = magmaPoolDamagePerSecond;
            this.magmaPoolDurationSeconds = magmaPoolDurationSeconds;
            this.magmaPoolRadiusBlocks = magmaPoolRadiusBlocks;
            this.iceWallCooldownSeconds = iceWallCooldownSeconds;
            this.iceWallDurationSeconds = iceWallDurationSeconds;
            this.iceWallWidthBlocks = iceWallWidthBlocks;
            this.iceWallHeightBlocks = iceWallHeightBlocks;
            this.windSlashCooldownSeconds = windSlashCooldownSeconds;
            this.windSlashDamage = windSlashDamage;
            this.windSlashRangeBlocks = windSlashRangeBlocks;
            this.curseBoltCooldownSeconds = curseBoltCooldownSeconds;
            this.curseBoltDamage = curseBoltDamage;
            this.curseBoltEffectDurationSeconds = curseBoltEffectDurationSeconds;
            this.berserkerRageCooldownSeconds = berserkerRageCooldownSeconds;
            this.berserkerRageDurationSeconds = berserkerRageDurationSeconds;
            this.berserkerRageDamageBoostPercent = berserkerRageDamageBoostPercent;
            this.berserkerRageDamageTakenBoostPercent = berserkerRageDamageTakenBoostPercent;
            this.etherealStepCooldownSeconds = etherealStepCooldownSeconds;
            this.etherealStepDistanceBlocks = etherealStepDistanceBlocks;
            this.arcaneMissilesCooldownSeconds = arcaneMissilesCooldownSeconds;
            this.arcaneMissilesDamagePerMissile = arcaneMissilesDamagePerMissile;
            this.arcaneMissilesCount = arcaneMissilesCount;
            this.lifeTapCooldownSeconds = lifeTapCooldownSeconds;
            this.lifeTapHealthCost = lifeTapHealthCost;
            this.lifeTapCooldownReductionPercent = lifeTapCooldownReductionPercent;
            this.doomBoltCooldownSeconds = doomBoltCooldownSeconds;
            this.doomBoltDamage = doomBoltDamage;
            this.doomBoltVelocity = doomBoltVelocity;
            this.sanctuaryCooldownSeconds = sanctuaryCooldownSeconds;
            this.sanctuaryDurationSeconds = sanctuaryDurationSeconds;
            this.sanctuaryRadiusBlocks = sanctuaryRadiusBlocks;
            this.spectralChainsCooldownSeconds = spectralChainsCooldownSeconds;
            this.spectralChainsDurationSeconds = spectralChainsDurationSeconds;
            this.spectralChainsRangeBlocks = spectralChainsRangeBlocks;
            this.voidRiftCooldownSeconds = voidRiftCooldownSeconds;
            this.voidRiftDamagePerSecond = voidRiftDamagePerSecond;
            this.voidRiftDurationSeconds = voidRiftDurationSeconds;
            this.voidRiftRadiusBlocks = voidRiftRadiusBlocks;
            this.infernoDashCooldownSeconds = infernoDashCooldownSeconds;
            this.infernoDashDamage = infernoDashDamage;
            this.infernoDashDistanceBlocks = infernoDashDistanceBlocks;
            this.infernoDashFireDurationSeconds = infernoDashFireDurationSeconds;
            this.tidalWaveCooldownSeconds = tidalWaveCooldownSeconds;
            this.tidalWaveDamage = tidalWaveDamage;
            this.tidalWaveRangeBlocks = tidalWaveRangeBlocks;
            this.tidalWaveSlowSeconds = tidalWaveSlowSeconds;
            this.starfallCooldownSeconds = starfallCooldownSeconds;
            this.starfallDamagePerHit = starfallDamagePerHit;
            this.starfallMeteorCount = starfallMeteorCount;
            this.starfallRadiusBlocks = starfallRadiusBlocks;
            this.bloodlustCooldownSeconds = bloodlustCooldownSeconds;
            this.bloodlustDurationSeconds = bloodlustDurationSeconds;
            this.bloodlustAttackSpeedPerKill = bloodlustAttackSpeedPerKill;
            this.bloodlustMaxStacks = bloodlustMaxStacks;
            this.crystalCageCooldownSeconds = crystalCageCooldownSeconds;
            this.crystalCageDurationSeconds = crystalCageDurationSeconds;
            this.crystalCageRangeBlocks = crystalCageRangeBlocks;
            this.phantasmCooldownSeconds = phantasmCooldownSeconds;
            this.phantasmDurationSeconds = phantasmDurationSeconds;
            this.phantasmExplosionDamage = phantasmExplosionDamage;
            this.sonicBoomCooldownSeconds = sonicBoomCooldownSeconds;
            this.sonicBoomDamage = sonicBoomDamage;
            this.sonicBoomRadiusBlocks = sonicBoomRadiusBlocks;
            this.sonicBoomKnockbackStrength = sonicBoomKnockbackStrength;
            this.vampiricTouchCooldownSeconds = vampiricTouchCooldownSeconds;
            this.vampiricTouchDamage = vampiricTouchDamage;
            this.vampiricTouchHealPercent = vampiricTouchHealPercent;
            this.blindingFlashCooldownSeconds = blindingFlashCooldownSeconds;
            this.blindingFlashBlindSeconds = blindingFlashBlindSeconds;
            this.blindingFlashRadiusBlocks = blindingFlashRadiusBlocks;
            this.stormCallCooldownSeconds = stormCallCooldownSeconds;
            this.stormCallDamagePerStrike = stormCallDamagePerStrike;
            this.stormCallDurationSeconds = stormCallDurationSeconds;
            this.stormCallRadiusBlocks = stormCallRadiusBlocks;
            this.stormCallStrikesPerSecond = stormCallStrikesPerSecond;
            this.quicksandCooldownSeconds = quicksandCooldownSeconds;
            this.quicksandDurationSeconds = quicksandDurationSeconds;
            this.quicksandRadiusBlocks = quicksandRadiusBlocks;
            this.quicksandSlownessAmplifier = quicksandSlownessAmplifier;
            this.searingLightCooldownSeconds = searingLightCooldownSeconds;
            this.searingLightDamage = searingLightDamage;
            this.searingLightUndeadBonusDamage = searingLightUndeadBonusDamage;
            this.searingLightRangeBlocks = searingLightRangeBlocks;
            this.spectralBladeCooldownSeconds = spectralBladeCooldownSeconds;
            this.spectralBladeDamage = spectralBladeDamage;
            this.spectralBladeDurationSeconds = spectralBladeDurationSeconds;
            this.netherPortalCooldownSeconds = netherPortalCooldownSeconds;
            this.netherPortalDistanceBlocks = netherPortalDistanceBlocks;
            this.entangleCooldownSeconds = entangleCooldownSeconds;
            this.entangleDurationSeconds = entangleDurationSeconds;
            this.entangleRadiusBlocks = entangleRadiusBlocks;
            this.mindSpikeCooldownSeconds = mindSpikeCooldownSeconds;
            this.mindSpikeDamagePerSecond = mindSpikeDamagePerSecond;
            this.mindSpikeDurationSeconds = mindSpikeDurationSeconds;
            this.mindSpikeRangeBlocks = mindSpikeRangeBlocks;
            this.seismicSlamCooldownSeconds = seismicSlamCooldownSeconds;
            this.seismicSlamDamage = seismicSlamDamage;
            this.seismicSlamRadiusBlocks = seismicSlamRadiusBlocks;
            this.seismicSlamKnockupStrength = seismicSlamKnockupStrength;
            this.icicleBarrageCooldownSeconds = icicleBarrageCooldownSeconds;
            this.icicleBarrageDamagePerIcicle = icicleBarrageDamagePerIcicle;
            this.icicleBarrageCount = icicleBarrageCount;
            this.icicleBarrageRangeBlocks = icicleBarrageRangeBlocks;
            this.banishmentCooldownSeconds = banishmentCooldownSeconds;
            this.banishmentDistanceBlocks = banishmentDistanceBlocks;
            this.banishmentRangeBlocks = banishmentRangeBlocks;
            this.corpseExplosionCooldownSeconds = corpseExplosionCooldownSeconds;
            this.corpseExplosionDamage = corpseExplosionDamage;
            this.corpseExplosionRadiusBlocks = corpseExplosionRadiusBlocks;
            this.corpseExplosionCorpseRangeBlocks = corpseExplosionCorpseRangeBlocks;
            this.corpseExplosionMarkDurationSeconds = corpseExplosionMarkDurationSeconds;
            this.soulSwapCooldownSeconds = soulSwapCooldownSeconds;
            this.soulSwapRangeBlocks = soulSwapRangeBlocks;
            this.markOfDeathCooldownSeconds = markOfDeathCooldownSeconds;
            this.markOfDeathDurationSeconds = markOfDeathDurationSeconds;
            this.markOfDeathBonusDamagePercent = markOfDeathBonusDamagePercent;
            this.markOfDeathRangeBlocks = markOfDeathRangeBlocks;
            this.ironMaidenCooldownSeconds = ironMaidenCooldownSeconds;
            this.ironMaidenDurationSeconds = ironMaidenDurationSeconds;
            this.ironMaidenReflectPercent = ironMaidenReflectPercent;
            this.warpStrikeCooldownSeconds = warpStrikeCooldownSeconds;
            this.warpStrikeDamage = warpStrikeDamage;
            this.warpStrikeRangeBlocks = warpStrikeRangeBlocks;
            this.vortexStrikeCooldownSeconds = vortexStrikeCooldownSeconds;
            this.vortexStrikeDamage = vortexStrikeDamage;
            this.vortexStrikeRadiusBlocks = vortexStrikeRadiusBlocks;
            this.vortexStrikePullStrength = vortexStrikePullStrength;
            this.plagueCloudCooldownSeconds = plagueCloudCooldownSeconds;
            this.plagueCloudDurationSeconds = plagueCloudDurationSeconds;
            this.plagueCloudPoisonAmplifier = plagueCloudPoisonAmplifier;
            this.plagueCloudWeaknessAmplifier = plagueCloudWeaknessAmplifier;
            this.overchargeCooldownSeconds = overchargeCooldownSeconds;
            this.overchargeDamageMultiplier = overchargeDamageMultiplier;
            this.overchargeHealthCost = overchargeHealthCost;
            this.overchargeDurationSeconds = overchargeDurationSeconds;
            this.gravityCrushCooldownSeconds = gravityCrushCooldownSeconds;
            this.gravityCrushDamage = gravityCrushDamage;
            this.gravityCrushRootDurationSeconds = gravityCrushRootDurationSeconds;
            this.gravityCrushRangeBlocks = gravityCrushRangeBlocks;
            this.thornsAuraDamagePercent = thornsAuraDamagePercent;
            this.lifestealPercent = lifestealPercent;
            this.dodgeChancePercent = dodgeChancePercent;
            this.criticalStrikeBonusDamagePercent = criticalStrikeBonusDamagePercent;
            this.criticalStrikeChanceBonus = criticalStrikeChanceBonus;
            this.manaShieldXpPerDamage = manaShieldXpPerDamage;
            this.regenerationBoostAmplifier = regenerationBoostAmplifier;
            this.damageReductionPercent = damageReductionPercent;
            this.attackSpeedBoostPercent = attackSpeedBoostPercent;
            this.reachExtendBlocks = reachExtendBlocks;
            this.impactAbsorbPercent = impactAbsorbPercent;
            this.impactAbsorbMaxAbsorption = impactAbsorbMaxAbsorption;
            this.adrenalineSurgeDurationSeconds = adrenalineSurgeDurationSeconds;
            this.adrenalineSurgeCooldownSeconds = adrenalineSurgeCooldownSeconds;
            this.intimidateDamageReductionPercent = intimidateDamageReductionPercent;
            this.intimidateRadiusBlocks = intimidateRadiusBlocks;
            this.evasiveRollCooldownSeconds = evasiveRollCooldownSeconds;
            this.evasiveRollDistanceBlocks = evasiveRollDistanceBlocks;
            this.combatMeditateHealPerSecond = combatMeditateHealPerSecond;
            this.combatMeditateDelaySeconds = combatMeditateDelaySeconds;
            this.weaponMasteryBonusDamage = weaponMasteryBonusDamage;
            this.cullingBladeThresholdPercent = cullingBladeThresholdPercent;
            this.thickSkinProjectileReductionPercent = thickSkinProjectileReductionPercent;
            this.xpBoostPercent = xpBoostPercent;
            this.hungerResistReductionPercent = hungerResistReductionPercent;
            this.poisonImmunityFull = poisonImmunityFull;
            this.secondWindCooldownSeconds = secondWindCooldownSeconds;
            this.echoStrikeChancePercent = echoStrikeChancePercent;
            this.chainBreakerDurationReductionPercent = chainBreakerDurationReductionPercent;
            this.stoneSkinFlatReduction = stoneSkinFlatReduction;
            this.arcaneBarrierCooldownSeconds = arcaneBarrierCooldownSeconds;
            this.arcaneBarrierAbsorbAmount = arcaneBarrierAbsorbAmount;
            this.predatorSenseThresholdPercent = predatorSenseThresholdPercent;
            this.predatorSenseRangeBlocks = predatorSenseRangeBlocks;
            this.battleMedicHealPerSecond = battleMedicHealPerSecond;
            this.battleMedicRadiusBlocks = battleMedicRadiusBlocks;
            this.lastStandThresholdPercent = lastStandThresholdPercent;
            this.lastStandDamageBoostPercent = lastStandDamageBoostPercent;
            this.executionerThresholdPercent = executionerThresholdPercent;
            this.executionerBonusDamagePercent = executionerBonusDamagePercent;
            this.bloodthirstHealOnKill = bloodthirstHealOnKill;
            this.steelResolveFullKnockbackImmunity = steelResolveFullKnockbackImmunity;
            this.elementalHarmonyReductionPercent = elementalHarmonyReductionPercent;
            this.treasureHunterDropBoostPercent = treasureHunterDropBoostPercent;
            this.counterStrikeDamageMultiplier = counterStrikeDamageMultiplier;
            this.counterStrikeWindowSeconds = counterStrikeWindowSeconds;
            this.bulwarkBlockEffectivenessBoostPercent = bulwarkBlockEffectivenessBoostPercent;
            this.quickRecoveryDebuffReductionPercent = quickRecoveryDebuffReductionPercent;
            this.overfowingVitalityBonusHearts = overfowingVitalityBonusHearts;
            this.magneticPullRangeMultiplier = magneticPullRangeMultiplier;
            this.vengeanceBuffDurationSeconds = vengeanceBuffDurationSeconds;
            this.vengeanceDamageBoostPercent = vengeanceDamageBoostPercent;
            this.nemesisBonusDamagePercent = nemesisBonusDamagePercent;
            this.huntersInstinctCritBoostPercent = huntersInstinctCritBoostPercent;
            this.berserkerBloodMaxAttackSpeedBoost = berserkerBloodMaxAttackSpeedBoost;
            this.opportunistBackstabBonusPercent = opportunistBackstabBonusPercent;
            this.ironcladArmorBoostPercent = ironcladArmorBoostPercent;
            this.mistFormPhaseChancePercent = mistFormPhaseChancePercent;
            this.warCryRadiusBlocks = warCryRadiusBlocks;
            this.warCryStrengthDurationSeconds = warCryStrengthDurationSeconds;
            this.siphonSoulRegenDurationSeconds = siphonSoulRegenDurationSeconds;
            this.unbreakableDurabilityReductionPercent = unbreakableDurabilityReductionPercent;
            this.focusedMindCooldownReductionPercent = focusedMindCooldownReductionPercent;
            this.sixthSenseWarningRangeBlocks = sixthSenseWarningRangeBlocks;
        }

        static BonusPool from(GemsBalanceConfig.BonusPool cfg) {
            return new BonusPool(
                    clampInt(cfg.maxBonusAbilities, 0, 10),
                    clampInt(cfg.maxBonusPassives, 0, 10),
                    cfg.showBonusesInHud,
                    clampFloat(cfg.bonusAbilityCooldownMultiplier, 0.0F, 10.0F),
                    clampFloat(cfg.bonusAbilityDamageMultiplier, 0.0F, 10.0F),
                    clampFloat(cfg.bonusPassiveEffectMultiplier, 0.0F, 10.0F),
                    cfg.thunderstrikeCooldownSeconds,
                    cfg.thunderstrikeDamage,
                    cfg.thunderstrikeRangeBlocks,
                    cfg.frostbiteCooldownSeconds,
                    cfg.frostbiteDamage,
                    cfg.frostbiteFreezeSeconds,
                    cfg.frostbiteRangeBlocks,
                    cfg.earthshatterCooldownSeconds,
                    cfg.earthshatterDamage,
                    cfg.earthshatterRadiusBlocks,
                    cfg.shadowstepCooldownSeconds,
                    cfg.shadowstepDistanceBlocks,
                    cfg.radiantBurstCooldownSeconds,
                    cfg.radiantBurstDamage,
                    cfg.radiantBurstRadiusBlocks,
                    cfg.radiantBurstBlindSeconds,
                    cfg.venomsprayCooldownSeconds,
                    cfg.venomsprayPoisonSeconds,
                    cfg.venomsprayConeAngleDegrees,
                    cfg.venomsprayRangeBlocks,
                    cfg.timewarpCooldownSeconds,
                    cfg.timewarpDurationSeconds,
                    cfg.timewarpRadiusBlocks,
                    cfg.timewarpSlownessAmplifier,
                    cfg.decoyTrapCooldownSeconds,
                    cfg.decoyTrapExplosionDamage,
                    cfg.decoyTrapArmTimeSeconds,
                    clampInt(cfg.decoyTrapMaxActive, 0, 200),
                    secClamped(cfg.decoyTrapDespawnSeconds, 0, 3600),
                    cfg.gravityWellCooldownSeconds,
                    cfg.gravityWellDurationSeconds,
                    cfg.gravityWellRadiusBlocks,
                    cfg.gravityWellPullStrength,
                    cfg.chainLightningCooldownSeconds,
                    cfg.chainLightningDamage,
                    cfg.chainLightningMaxBounces,
                    cfg.chainLightningBounceRangeBlocks,
                    cfg.magmaPoolCooldownSeconds,
                    cfg.magmaPoolDamagePerSecond,
                    cfg.magmaPoolDurationSeconds,
                    cfg.magmaPoolRadiusBlocks,
                    cfg.iceWallCooldownSeconds,
                    cfg.iceWallDurationSeconds,
                    cfg.iceWallWidthBlocks,
                    cfg.iceWallHeightBlocks,
                    cfg.windSlashCooldownSeconds,
                    cfg.windSlashDamage,
                    cfg.windSlashRangeBlocks,
                    cfg.curseBoltCooldownSeconds,
                    cfg.curseBoltDamage,
                    cfg.curseBoltEffectDurationSeconds,
                    cfg.berserkerRageCooldownSeconds,
                    cfg.berserkerRageDurationSeconds,
                    cfg.berserkerRageDamageBoostPercent,
                    cfg.berserkerRageDamageTakenBoostPercent,
                    cfg.etherealStepCooldownSeconds,
                    cfg.etherealStepDistanceBlocks,
                    cfg.arcaneMissilesCooldownSeconds,
                    cfg.arcaneMissilesDamagePerMissile,
                    cfg.arcaneMissilesCount,
                    cfg.lifeTapCooldownSeconds,
                    cfg.lifeTapHealthCost,
                    cfg.lifeTapCooldownReductionPercent,
                    cfg.doomBoltCooldownSeconds,
                    cfg.doomBoltDamage,
                    cfg.doomBoltVelocity,
                    cfg.sanctuaryCooldownSeconds,
                    cfg.sanctuaryDurationSeconds,
                    cfg.sanctuaryRadiusBlocks,
                    cfg.spectralChainsCooldownSeconds,
                    cfg.spectralChainsDurationSeconds,
                    cfg.spectralChainsRangeBlocks,
                    cfg.voidRiftCooldownSeconds,
                    cfg.voidRiftDamagePerSecond,
                    cfg.voidRiftDurationSeconds,
                    cfg.voidRiftRadiusBlocks,
                    cfg.infernoDashCooldownSeconds,
                    cfg.infernoDashDamage,
                    cfg.infernoDashDistanceBlocks,
                    cfg.infernoDashFireDurationSeconds,
                    cfg.tidalWaveCooldownSeconds,
                    cfg.tidalWaveDamage,
                    cfg.tidalWaveRangeBlocks,
                    cfg.tidalWaveSlowSeconds,
                    cfg.starfallCooldownSeconds,
                    cfg.starfallDamagePerHit,
                    cfg.starfallMeteorCount,
                    cfg.starfallRadiusBlocks,
                    cfg.bloodlustCooldownSeconds,
                    cfg.bloodlustDurationSeconds,
                    cfg.bloodlustAttackSpeedPerKill,
                    cfg.bloodlustMaxStacks,
                    cfg.crystalCageCooldownSeconds,
                    cfg.crystalCageDurationSeconds,
                    cfg.crystalCageRangeBlocks,
                    cfg.phantasmCooldownSeconds,
                    cfg.phantasmDurationSeconds,
                    cfg.phantasmExplosionDamage,
                    cfg.sonicBoomCooldownSeconds,
                    cfg.sonicBoomDamage,
                    cfg.sonicBoomRadiusBlocks,
                    cfg.sonicBoomKnockbackStrength,
                    cfg.vampiricTouchCooldownSeconds,
                    cfg.vampiricTouchDamage,
                    cfg.vampiricTouchHealPercent,
                    cfg.blindingFlashCooldownSeconds,
                    cfg.blindingFlashBlindSeconds,
                    cfg.blindingFlashRadiusBlocks,
                    cfg.stormCallCooldownSeconds,
                    cfg.stormCallDamagePerStrike,
                    cfg.stormCallDurationSeconds,
                    cfg.stormCallRadiusBlocks,
                    cfg.stormCallStrikesPerSecond,
                    cfg.quicksandCooldownSeconds,
                    cfg.quicksandDurationSeconds,
                    cfg.quicksandRadiusBlocks,
                    cfg.quicksandSlownessAmplifier,
                    cfg.searingLightCooldownSeconds,
                    cfg.searingLightDamage,
                    cfg.searingLightUndeadBonusDamage,
                    cfg.searingLightRangeBlocks,
                    cfg.spectralBladeCooldownSeconds,
                    cfg.spectralBladeDamage,
                    cfg.spectralBladeDurationSeconds,
                    cfg.netherPortalCooldownSeconds,
                    cfg.netherPortalDistanceBlocks,
                    cfg.entangleCooldownSeconds,
                    cfg.entangleDurationSeconds,
                    cfg.entangleRadiusBlocks,
                    cfg.mindSpikeCooldownSeconds,
                    cfg.mindSpikeDamagePerSecond,
                    cfg.mindSpikeDurationSeconds,
                    cfg.mindSpikeRangeBlocks,
                    cfg.seismicSlamCooldownSeconds,
                    cfg.seismicSlamDamage,
                    cfg.seismicSlamRadiusBlocks,
                    cfg.seismicSlamKnockupStrength,
                    cfg.icicleBarrageCooldownSeconds,
                    cfg.icicleBarrageDamagePerIcicle,
                    cfg.icicleBarrageCount,
                    cfg.icicleBarrageRangeBlocks,
                    cfg.banishmentCooldownSeconds,
                    cfg.banishmentDistanceBlocks,
                    cfg.banishmentRangeBlocks,
                    cfg.corpseExplosionCooldownSeconds,
                    cfg.corpseExplosionDamage,
                    cfg.corpseExplosionRadiusBlocks,
                    cfg.corpseExplosionCorpseRangeBlocks,
                    cfg.corpseExplosionMarkDurationSeconds,
                    cfg.soulSwapCooldownSeconds,
                    cfg.soulSwapRangeBlocks,
                    cfg.markOfDeathCooldownSeconds,
                    cfg.markOfDeathDurationSeconds,
                    cfg.markOfDeathBonusDamagePercent,
                    cfg.markOfDeathRangeBlocks,
                    cfg.ironMaidenCooldownSeconds,
                    cfg.ironMaidenDurationSeconds,
                    cfg.ironMaidenReflectPercent,
                    cfg.warpStrikeCooldownSeconds,
                    cfg.warpStrikeDamage,
                    cfg.warpStrikeRangeBlocks,
                    cfg.vortexStrikeCooldownSeconds,
                    cfg.vortexStrikeDamage,
                    cfg.vortexStrikeRadiusBlocks,
                    cfg.vortexStrikePullStrength,
                    cfg.plagueCloudCooldownSeconds,
                    cfg.plagueCloudDurationSeconds,
                    cfg.plagueCloudPoisonAmplifier,
                    cfg.plagueCloudWeaknessAmplifier,
                    cfg.overchargeCooldownSeconds,
                    cfg.overchargeDamageMultiplier,
                    cfg.overchargeHealthCost,
                    cfg.overchargeDurationSeconds,
                    cfg.gravityCrushCooldownSeconds,
                    cfg.gravityCrushDamage,
                    cfg.gravityCrushRootDurationSeconds,
                    cfg.gravityCrushRangeBlocks,
                    cfg.thornsAuraDamagePercent,
                    cfg.lifestealPercent,
                    cfg.dodgeChancePercent,
                    cfg.criticalStrikeBonusDamagePercent,
                    cfg.criticalStrikeChanceBonus,
                    cfg.manaShieldXpPerDamage,
                    cfg.regenerationBoostAmplifier,
                    cfg.damageReductionPercent,
                    cfg.attackSpeedBoostPercent,
                    cfg.reachExtendBlocks,
                    cfg.impactAbsorbPercent,
                    cfg.impactAbsorbMaxAbsorption,
                    cfg.adrenalineSurgeDurationSeconds,
                    cfg.adrenalineSurgeCooldownSeconds,
                    cfg.intimidateDamageReductionPercent,
                    cfg.intimidateRadiusBlocks,
                    cfg.evasiveRollCooldownSeconds,
                    cfg.evasiveRollDistanceBlocks,
                    cfg.combatMeditateHealPerSecond,
                    cfg.combatMeditateDelaySeconds,
                    cfg.weaponMasteryBonusDamage,
                    cfg.cullingBladeThresholdPercent,
                    cfg.thickSkinProjectileReductionPercent,
                    cfg.xpBoostPercent,
                    cfg.hungerResistReductionPercent,
                    cfg.poisonImmunityFull,
                    cfg.secondWindCooldownSeconds,
                    cfg.echoStrikeChancePercent,
                    cfg.chainBreakerDurationReductionPercent,
                    cfg.stoneSkinFlatReduction,
                    cfg.arcaneBarrierCooldownSeconds,
                    cfg.arcaneBarrierAbsorbAmount,
                    cfg.predatorSenseThresholdPercent,
                    cfg.predatorSenseRangeBlocks,
                    cfg.battleMedicHealPerSecond,
                    cfg.battleMedicRadiusBlocks,
                    cfg.lastStandThresholdPercent,
                    cfg.lastStandDamageBoostPercent,
                    cfg.executionerThresholdPercent,
                    cfg.executionerBonusDamagePercent,
                    cfg.bloodthirstHealOnKill,
                    cfg.steelResolveFullKnockbackImmunity,
                    cfg.elementalHarmonyReductionPercent,
                    cfg.treasureHunterDropBoostPercent,
                    cfg.counterStrikeDamageMultiplier,
                    cfg.counterStrikeWindowSeconds,
                    cfg.bulwarkBlockEffectivenessBoostPercent,
                    cfg.quickRecoveryDebuffReductionPercent,
                    cfg.overfowingVitalityBonusHearts,
                    cfg.magneticPullRangeMultiplier,
                    cfg.vengeanceBuffDurationSeconds,
                    cfg.vengeanceDamageBoostPercent,
                    cfg.nemesisBonusDamagePercent,
                    cfg.huntersInstinctCritBoostPercent,
                    cfg.berserkerBloodMaxAttackSpeedBoost,
                    cfg.opportunistBackstabBonusPercent,
                    cfg.ironcladArmorBoostPercent,
                    cfg.mistFormPhaseChancePercent,
                    cfg.warCryRadiusBlocks,
                    cfg.warCryStrengthDurationSeconds,
                    cfg.siphonSoulRegenDurationSeconds,
                    cfg.unbreakableDurabilityReductionPercent,
                    cfg.focusedMindCooldownReductionPercent,
                    cfg.sixthSenseWarningRangeBlocks
            );
        }

        public int maxBonusAbilities() {
            return maxBonusAbilities;
        }

        public int maxBonusPassives() {
            return maxBonusPassives;
        }

        public boolean showBonusesInHud() {
            return showBonusesInHud;
        }

        public float bonusAbilityCooldownMultiplier() {
            return bonusAbilityCooldownMultiplier;
        }

        public float bonusAbilityDamageMultiplier() {
            return bonusAbilityDamageMultiplier;
        }

        public float bonusPassiveEffectMultiplier() {
            return bonusPassiveEffectMultiplier;
        }

        public float thickSkinProjectileReductionPercent() {
            return thickSkinProjectileReductionPercent;
        }
    }

    private static List<Identifier> parseIdentifierList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Identifier> out = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(entry);
            if (id != null) {
                out.add(id);
            }
        }
        return List.copyOf(out);
    }

    private static Map<Identifier, GemId> parseRecipeGemRequirements(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, GemId> out = new HashMap<>();
        for (var entry : raw.entrySet()) {
            if (entry == null) {
                continue;
            }
            Identifier recipeId = Identifier.tryParse(entry.getKey());
            if (recipeId == null) {
                continue;
            }
            String gemRaw = entry.getValue();
            if (gemRaw == null || gemRaw.isBlank()) {
                continue;
            }
            try {
                GemId gem = GemId.valueOf(gemRaw.trim().toUpperCase());
                out.put(recipeId, gem);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Map.copyOf(out);
    }

    private static int secClamped(int seconds, int minSeconds, int maxSeconds) {
        if (seconds <= 0) {
            return 0;
        }
        return sec(clampInt(seconds, minSeconds, maxSeconds));
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int sec(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.multiplyExact(seconds, TICKS_PER_SECOND);
    }

    public record Mastery(
            boolean enabled,
            boolean showAuraParticles
    ) {
        static Mastery from(GemsBalanceConfig.Mastery cfg) {
            return new Mastery(
                    cfg.enabled,
                    cfg.showAuraParticles
            );
        }
    }

    public record Rivalry(
            boolean enabled,
            double damageMultiplier,
            boolean showInHud
    ) {
        static Rivalry from(GemsBalanceConfig.Rivalry cfg) {
            return new Rivalry(
                    cfg.enabled,
                    clampDouble(cfg.damageMultiplier, 1.0, 5.0),
                    cfg.showInHud
            );
        }
    }

    public record Loadouts(
            boolean enabled,
            int unlockEnergy,
            int maxPresetsPerGem
    ) {
        static Loadouts from(GemsBalanceConfig.Loadouts cfg) {
            return new Loadouts(
                    cfg.enabled,
                    clampInt(cfg.unlockEnergy, 0, 10),
                    clampInt(cfg.maxPresetsPerGem, 1, 10)
            );
        }
    }

    public record Synergies(
            boolean enabled,
            int windowTicks,
            int cooldownTicks,
            boolean showNotifications,
            java.util.Map<String, SynergyEntry> entries
    ) {
        static Synergies from(GemsBalanceConfig.Synergies cfg) {
            java.util.Map<String, SynergyEntry> entries = new java.util.HashMap<>();
            if (cfg.entries != null) {
                for (GemsBalanceConfig.Synergies.SynergyEntry entry : cfg.entries) {
                    if (entry == null || entry.id == null || entry.id.isBlank()) {
                        continue;
                    }
                    entries.put(entry.id, new SynergyEntry(
                            entry.enabled,
                            secClampedOptional(entry.windowSeconds, 1, 10),
                            secClampedOptional(entry.cooldownSeconds, 5, 120)
                    ));
                }
            }
            return new Synergies(
                    cfg.enabled,
                    secClamped(cfg.windowSeconds, 1, 10),
                    secClamped(cfg.cooldownSeconds, 5, 120),
                    cfg.showNotifications,
                    entries
            );
        }

        public boolean isSynergyEnabled(String id) {
            if (!enabled || id == null) {
                return false;
            }
            SynergyEntry entry = entries.get(id);
            return entry == null || entry.enabled();
        }

        public int windowTicksFor(String id, int fallbackTicks) {
            SynergyEntry entry = entries.get(id);
            if (entry != null && entry.windowTicks() > 0) {
                return entry.windowTicks();
            }
            if (fallbackTicks > 0) {
                return fallbackTicks;
            }
            return windowTicks;
        }

        public int cooldownTicksFor(String id, int fallbackTicks) {
            SynergyEntry entry = entries.get(id);
            if (entry != null && entry.cooldownTicks() > 0) {
                return entry.cooldownTicks();
            }
            if (fallbackTicks > 0) {
                return fallbackTicks;
            }
            return cooldownTicks;
        }

        public int maxWindowTicks() {
            int max = windowTicks;
            for (SynergyEntry entry : entries.values()) {
                if (entry.windowTicks() > max) {
                    max = entry.windowTicks();
                }
            }
            return max;
        }
    }

    public record SynergyEntry(
            boolean enabled,
            int windowTicks,
            int cooldownTicks
    ) {
    }

    public record Augments(
            int gemMaxSlots,
            int legendaryMaxSlots,
            int rarityCommonWeight,
            int rarityRareWeight,
            int rarityEpicWeight,
            float commonMagnitudeMin,
            float commonMagnitudeMax,
            float rareMagnitudeMin,
            float rareMagnitudeMax,
            float epicMagnitudeMin,
            float epicMagnitudeMax
    ) {
        static Augments from(GemsBalanceConfig.Augments cfg) {
            return new Augments(
                    clampInt(cfg.gemMaxSlots, 0, 6),
                    clampInt(cfg.legendaryMaxSlots, 0, 4),
                    clampInt(cfg.rarityCommonWeight, 0, 1000),
                    clampInt(cfg.rarityRareWeight, 0, 1000),
                    clampInt(cfg.rarityEpicWeight, 0, 1000),
                    clampFloat(cfg.commonMagnitudeMin, 0.1f, 2.0f),
                    clampFloat(cfg.commonMagnitudeMax, 0.1f, 3.0f),
                    clampFloat(cfg.rareMagnitudeMin, 0.1f, 3.0f),
                    clampFloat(cfg.rareMagnitudeMax, 0.1f, 4.0f),
                    clampFloat(cfg.epicMagnitudeMin, 0.1f, 4.0f),
                    clampFloat(cfg.epicMagnitudeMax, 0.1f, 5.0f)
            );
        }
    }
}
