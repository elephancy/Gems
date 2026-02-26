package com.feel.gems.config;




/**
 * User-editable balancing config. Units:
 * - durations/cooldowns are in seconds unless explicitly noted
 * - radii/ranges are in blocks
 * - damage is in half-hearts (Minecraft health points) where 1.0 = half-heart, 2.0 = 1 heart
 */
public final class GemsBalanceConfig {
    public Visual visual = new Visual();
    public Systems systems = new Systems();

    public Astra astra = new Astra();
    public Fire fire = new Fire();
    public Flux flux = new Flux();
    public Life life = new Life();
    public Puff puff = new Puff();
    public Speed speed = new Speed();
    public Strength strength = new Strength();
    public Wealth wealth = new Wealth();
    public Terror terror = new Terror();
    public Summoner summoner = new Summoner();
    public Space space = new Space();
    public Reaper reaper = new Reaper();
    public Pillager pillager = new Pillager();
    public Spy spy = new Spy();
    public Beacon beacon = new Beacon();
    public Air air = new Air();
    public VoidGem voidGem = new VoidGem();
    public Chaos chaos = new Chaos();
    public Prism prism = new Prism();
    public Duelist duelist = new Duelist();
    public Hunter hunter = new Hunter();
    public Sentinel sentinel = new Sentinel();
    public Trickster trickster = new Trickster();
    public Legendary legendary = new Legendary();
    public BonusPool bonusPool = new BonusPool();
    public Mastery mastery = new Mastery();
    public Rivalry rivalry = new Rivalry();
    public Loadouts loadouts = new Loadouts();
    public Synergies synergies = new Synergies();
    public Augments augments = new Augments();

    /**
     * Universal mob blacklist for Hypno Staff, Summoner summons, and Astra soul captures.
     */
    public java.util.List<String> mobBlacklist = java.util.List.of(
            "minecraft:ender_dragon",
            "minecraft:wither"
    );

    public static final class Visual {
        public boolean enableParticles = true;
        public boolean enableSounds = true;

        /**
         * Scales particle counts for all abilities (0 = off, 100 = default, 200 = double).
         * This is a global safety knob for large servers.
         */
        public int particleScalePercent = 100;

        /**
         * Hard cap for a single server particle packet.
         */
        public int maxParticlesPerCall = 128;

        /**
         * Hard cap for beam particle steps (used by Flux Beam and similar effects).
         */
        public int maxBeamSteps = 256;

        /**
         * Hard cap for ring/aura particle points (used by campfire/zone-style abilities).
         */
        public int maxRingPoints = 128;
    }

    public static final class Systems {
        public int minMaxHearts = 5;
        public int assassinTriggerHearts = 5;
        public int assassinMaxHearts = 10;
        public int assassinEliminationHeartsThreshold = 0;
        public int assassinVsAssassinVictimHeartsLoss = 2;
        public int assassinVsAssassinKillerHeartsGain = 2;
        public double controlledFollowStartBlocks = 6.0D;
        public double controlledFollowStopBlocks = 3.0D;
        public double controlledFollowSpeed = 1.1D;
    }

    /**
     * Special gem: Void. Its primary gameplay is immunity to debuffs/negative effects.
     */
    public static final class VoidGem {
        /**
         * When true, Void players are immune to all status effects.
         *
         * <p>This is intentionally strong and will also block vanilla effects (potions, beacons, etc.).</p>
         */
        public boolean blockAllStatusEffects = true;
    }

    /**
     * Special gem: Chaos. Provides independent randomized ability/passive slots.
     */
    public static final class Chaos {
        /** Duration a Chaos slot stays active after rolling (seconds). */
        public int slotDurationSeconds = 300;
        /** Cooldown between uses of a rolled Chaos slot ability (seconds). */
        public int slotAbilityCooldownSeconds = 10;
        /** Number of Chaos slots available (1-9). */
        public int slotCount = 6;
    }

    /**
     * Special gem: Prism. Players select which gem abilities/passives they want to slot.
     */
    public static final class Prism {
        /** Max number of selected "gem abilities" Prism can slot. */
        public int maxGemAbilities = 3;
        /** Max number of selected "gem passives" Prism can slot. */
        public int maxGemPassives = 3;
    }

    public static final class Astra {
        public int shadowAnchorWindowSeconds = 10;
        public int shadowAnchorPostCooldownSeconds = 10;

        public int dimensionalVoidCooldownSeconds = 60;
        public int dimensionalVoidDurationSeconds = 8;
        public int dimensionalVoidRadiusBlocks = 10;

        public int astralDaggersCooldownSeconds = 8;
        public int astralDaggersCount = 5;
        public float astralDaggersDamage = 4.0F;
        public float astralDaggersVelocity = 3.5F;
        public float astralDaggersSpread = 0.05F;

        public int unboundedCooldownSeconds = 60;
        public int unboundedDurationSeconds = 3;

        public int astralCameraCooldownSeconds = 60;
        public int astralCameraDurationSeconds = 8;

        public int spookCooldownSeconds = 30;
        public int spookRadiusBlocks = 10;
        public int spookDurationSeconds = 6;

        public int tagCooldownSeconds = 20;
        public int tagRangeBlocks = 30;
        public int tagDurationSeconds = 12;

        public float soulHealingHearts = 2.0F;
        public double soulReleaseForwardBlocks = 2.0D;
        public double soulReleaseUpBlocks = 1.0D;
    }

    public static final class Fire {
        public int cosyCampfireCooldownSeconds = 45;
        public int cosyCampfireDurationSeconds = 10;
        public int cosyCampfireRadiusBlocks = 8;
        public int cosyCampfireRegenAmplifier = 3; // Regen IV = amplifier 3

        public int heatHazeCooldownSeconds = 90;
        public int heatHazeDurationSeconds = 10;
        public int heatHazeRadiusBlocks = 10;
        public int heatHazeEnemyMiningFatigueAmplifier = 0;
        public int heatHazeEnemyWeaknessAmplifier = 0;

        public int fireballChargeUpSeconds = 3;
        public int fireballChargeDownSeconds = 3;
        public int fireballInternalCooldownSeconds = 4;
        public int fireballMaxDistanceBlocks = 60;

        public int meteorShowerCooldownSeconds = 120;
        public int meteorShowerTargetRangeBlocks = 60;
        public int meteorShowerCount = 10;
        public int meteorShowerSpreadBlocks = 10;
        public int meteorShowerHeightBlocks = 25;
        public float meteorShowerVelocity = 1.5F;
        public int meteorShowerExplosionPower = 2;
    }

    public static final class Flux {
        public int fluxBeamCooldownSeconds = 4;
        public int fluxBeamRangeBlocks = 60;
        public float fluxBeamMinDamage = 6.0F;
        public float fluxBeamMaxDamageAt100 = 12.0F;
        public float fluxBeamMaxDamageAt200 = 24.0F;
        public int fluxBeamArmorDamageAt100 = 200;
        public int fluxBeamArmorDamagePerPercent = 2;

        public int staticBurstCooldownSeconds = 30;
        public int staticBurstRadiusBlocks = 8;
        public float staticBurstMaxDamage = 20.0F;
        public int staticBurstStoreWindowSeconds = 120;

        public int chargeDiamondBlock = 25;
        public int chargeGoldBlock = 15;
        public int chargeCopperBlock = 5;
        public int chargeEmeraldBlock = 20;
        public int chargeAmethystBlock = 10;
        public int chargeNetheriteScrap = 35;
        public int chargeEnchantedDiamondItem = 20;

        public int overchargeDelaySeconds = 5;
        public int overchargePerSecond = 5;
        public float overchargeSelfDamagePerSecond = 1.0F;

        public int fluxCapacitorChargeThreshold = 140;
        public int fluxCapacitorAbsorptionAmplifier = 0;
        public int fluxConductivityChargePerDamage = 2;
        public int fluxConductivityMaxChargePerHit = 12;
        public int fluxInsulationChargeThreshold = 150;
        public float fluxInsulationDamageMultiplier = 0.75F;

        public int fluxSurgeCooldownSeconds = 35;
        public int fluxSurgeDurationSeconds = 6;
        public int fluxSurgeSpeedAmplifier = 1;
        public int fluxSurgeResistanceAmplifier = 0;
        public int fluxSurgeChargeCost = 30;
        public int fluxSurgeRadiusBlocks = 5;
        public double fluxSurgeKnockback = 0.7D;

        public int fluxDischargeCooldownSeconds = 40;
        public int fluxDischargeRadiusBlocks = 8;
        public float fluxDischargeBaseDamage = 4.0F;
        public float fluxDischargeDamagePerCharge = 0.2F;
        public float fluxDischargeMaxDamage = 20.0F;
        public int fluxDischargeMinCharge = 25;
        public double fluxDischargeKnockback = 0.8D;
    }

    public static final class Life {
        public int vitalityVortexCooldownSeconds = 30;
        public int vitalityVortexRadiusBlocks = 8;
        public int vitalityVortexDurationSeconds = 8;
        public int vitalityVortexScanRadiusBlocks = 3;
        public int vitalityVortexVerdantThreshold = 10;
        public float vitalityVortexAllyHeal = 2.0F;

        public int healthDrainCooldownSeconds = 45;
        public int healthDrainRangeBlocks = 20;
        public float healthDrainAmount = 6.0F;

        public int lifeSwapCooldownSeconds = 45;
        public int lifeSwapRangeBlocks = 24;
        public float lifeSwapMinHearts = 3.0F;
        public int lifeSwapReswapSeconds = 15;

        public int lifeCircleCooldownSeconds = 60;
        public int lifeCircleDurationSeconds = 12;
        public int lifeCircleRadiusBlocks = 8;
        public double lifeCircleMaxHealthDelta = 8.0D;

        public int heartLockCooldownSeconds = 45;
        public int heartLockDurationSeconds = 4;
        public int heartLockRangeBlocks = 20;
    }

    public static final class Puff {
        public int doubleJumpCooldownSeconds = 2;
        public double doubleJumpVelocityY = 0.85D;

        public int dashCooldownSeconds = 6;
        public double dashVelocity = 1.8D;
        public float dashDamage = 6.0F;
        public double dashHitRangeBlocks = 4.0D;

        public int breezyBashCooldownSeconds = 20;
        public int breezyBashRangeBlocks = 10;
        public double breezyBashUpVelocityY = 1.2D;
        public double breezyBashKnockback = 0.6D;
        public float breezyBashInitialDamage = 4.0F;
        public float breezyBashImpactDamage = 6.0F;
        public int breezyBashImpactWindowSeconds = 6;

        public int groupBashCooldownSeconds = 45;
        public int groupBashRadiusBlocks = 10;
        public double groupBashKnockback = 1.2D;
        public double groupBashUpVelocityY = 0.8D;

        public int gustCooldownSeconds = 30;
        public int gustRadiusBlocks = 8;
        public double gustUpVelocityY = 0.7D;
        public double gustKnockback = 1.0D;
        public int gustSlownessDurationSeconds = 5;
        public int gustSlownessAmplifier = 1;
        public int gustSlowFallingDurationSeconds = 4;

        public int windborneDurationSeconds = 2;
        public int windborneSlowFallingAmplifier = 0;
    }

    public static final class Speed {
        public double momentumMinSpeed = 0.05D;
        public double momentumMaxSpeed = 0.45D;
        public float momentumMinMultiplier = 1.00F;
        public float momentumMaxMultiplier = 2.00F;
        public int frictionlessSpeedAmplifier = 1;

        public int arcShotCooldownSeconds = 20;
        public int arcShotRangeBlocks = 40;
        public double arcShotRadiusBlocks = 2.0D;
        public int arcShotMaxTargets = 3;
        public float arcShotDamage = 5.0F;

        public int speedStormCooldownSeconds = 60;
        public int speedStormDurationSeconds = 8;
        public int speedStormRadiusBlocks = 10;
        public int speedStormAllySpeedAmplifier = 1;
        public int speedStormAllyHasteAmplifier = 1;
        public int speedStormEnemySlownessAmplifier = 6;
        public int speedStormEnemyMiningFatigueAmplifier = 2;

        public int terminalVelocityCooldownSeconds = 30;
        public int terminalVelocityDurationSeconds = 10;
        public int terminalVelocitySpeedAmplifier = 4; // Speed V
        public int terminalVelocityHasteAmplifier = 2; // Haste III

        public int slipstreamCooldownSeconds = 45;
        public int slipstreamDurationSeconds = 8;
        public int slipstreamLengthBlocks = 18;
        public int slipstreamRadiusBlocks = 3;
        public int slipstreamAllySpeedAmplifier = 1;
        public int slipstreamEnemySlownessAmplifier = 1;
        public double slipstreamEnemyKnockback = 0.35D;

        public int afterimageCooldownSeconds = 30;
        public int afterimageDurationSeconds = 6;
        public int afterimageSpeedAmplifier = 1;

        public int tempoShiftCooldownSeconds = 60;
        public int tempoShiftDurationSeconds = 10;
        public int tempoShiftRadiusBlocks = 12;
        public int tempoShiftAllyCooldownTicksPerSecond = 10;
        public int tempoShiftEnemyCooldownTicksPerSecond = 10;

        public int autoStepCooldownSeconds = 5;
        public double autoStepHeightBonus = 0.4D;
    }

    public static final class Strength {
        public int nullifyCooldownSeconds = 20;
        public int nullifyRadiusBlocks = 10;

        public int frailerCooldownSeconds = 20;
        public int frailerRangeBlocks = 20;
        public int frailerDurationSeconds = 8;

        public int bountyCooldownSeconds = 60;
        public int bountyDurationSeconds = 60;

        public int chadCooldownSeconds = 90;
        public int chadDurationSeconds = 45;
        public int chadEveryHits = 4;
        public float chadBonusDamage = 7.0F;

        public float adrenalineThresholdHearts = 4.0F;
        public int adrenalineDurationSeconds = 4;
        public int adrenalineResistanceAmplifier = 0;
    }

    public static final class Wealth {
        public int fumbleCooldownSeconds = 30;
        public int fumbleDurationSeconds = 8;
        public int fumbleRadiusBlocks = 10;

        public int hotbarLockCooldownSeconds = 30;
        public int hotbarLockDurationSeconds = 6;
        public int hotbarLockRangeBlocks = 20;

        public int amplificationCooldownSeconds = 180;
        public int amplificationDurationSeconds = 45;
        public int amplificationBonusLevels = 2;

        public int richRushCooldownSeconds = 9 * 60;
        public int richRushDurationSeconds = 3 * 60;
        public int richRushLootRolls = 3;
        public int richRushWitherSkullGuarantee = 3;
        public int richRushShulkerShellGuarantee = 3;
        public int richRushNetherStarGuarantee = 3;

        public int pocketsRows = 3;
    }

    public static final class Terror {
        // Passives
        public int dreadAuraRadiusBlocks = 10;
        public int dreadAuraAmplifier = 0;
        public int bloodPriceDurationSeconds = 6;
        public int bloodPriceStrengthAmplifier = 1;
        public int bloodPriceResistanceAmplifier = 0;

        // Abilities
        public int terrorTradeCooldownSeconds = 180;
        public int terrorTradeRangeBlocks = 30;
        public int terrorTradeMaxUses = 3;
        public int terrorTradeHeartsCost = 2;
        public int terrorTradePermanentEnergyPenalty = 2;
        public int terrorTradeNormalTargetHeartsPenalty = 2;
        public int terrorTradeNormalTargetEnergyPenalty = 1;

        public int panicRingCooldownSeconds = 60;
        public int panicRingTntCount = 5;
        public int panicRingFuseTicks = 50;
        public double panicRingRadiusBlocks = 1.6D;

        public int rigCooldownSeconds = 45;
        public int rigRangeBlocks = 12;
        public int rigDurationSeconds = 90;
        public int rigFuseTicks = 20;
        public int rigTntCount = 5;

        public int remoteChargeArmWindowSeconds = 10;
        public int remoteChargeDetonateWindowSeconds = 60;
        public int remoteChargeFuseTicks = 20;
        public int remoteChargeCooldownSeconds = 45;

        public int breachChargeCooldownSeconds = 75;
        public int breachChargeRangeBlocks = 18;
        public int breachChargeFuseTicks = 40;
        public float breachChargeExplosionPower = 3.0F;
    }

    public static final class Summoner {
        public int maxPoints = 50;
        public int maxActiveSummons = 20;
        public int summonLifetimeSeconds = 120;
        public int commandRangeBlocks = 32;

        public int summonSlotCooldownSeconds = 30;
        public int recallCooldownSeconds = 20;

        public int commandersMarkDurationSeconds = 3;
        public int commandersMarkStrengthAmplifier = 0;

        public float summonBonusHealth = 4.0F;
        public double summonSpawnForwardBlocks = 2.0D;
        public double summonSpawnUpBlocks = 0.1D;
        public double summonSpawnRingBaseBlocks = 0.4D;
        public double summonSpawnRingStepBlocks = 0.25D;
        public int summonSpawnRingLayers = 3;
        public int summonSpawnRingSegments = 8;

        /**
         * Entity cost map, keyed by entity id string (e.g. "minecraft:zombie").
         */
        public java.util.Map<String, Integer> costs = defaultCosts();

        /**
         * Slot loadouts; each slot is a list of entity specs to spawn when the corresponding ability is used.
         */
        public java.util.List<SummonSpec> slot1 = java.util.List.of(new SummonSpec("minecraft:zombie", 2));
        public java.util.List<SummonSpec> slot2 = java.util.List.of(new SummonSpec("minecraft:skeleton", 2));
        public java.util.List<SummonSpec> slot3 = java.util.List.of(new SummonSpec("minecraft:creeper", 1));
        public java.util.List<SummonSpec> slot4 = java.util.List.of();
        public java.util.List<SummonSpec> slot5 = java.util.List.of();

        public static final class SummonSpec {
            public String entityId;
            public int count;

            public SummonSpec() {
            }

            public SummonSpec(String entityId, int count) {
                this.entityId = entityId;
                this.count = count;
            }
        }

        private static java.util.Map<String, Integer> defaultCosts() {
            java.util.Map<String, Integer> m = new java.util.HashMap<>();
            m.put("minecraft:zombie", 5);
            m.put("minecraft:skeleton", 5);
            m.put("minecraft:creeper", 10);
            m.put("minecraft:spider", 6);
            m.put("minecraft:cave_spider", 8);
            m.put("minecraft:husk", 6);
            m.put("minecraft:drowned", 8);
            m.put("minecraft:stray", 6);
            m.put("minecraft:piglin", 8);
            m.put("minecraft:piglin_brute", 25);
            m.put("minecraft:enderman", 20);
            m.put("minecraft:wolf", 8);
            m.put("minecraft:iron_golem", 40);
            m.put("minecraft:vindicator", 18);
            m.put("minecraft:pillager", 12);
            m.put("minecraft:ravager", 45);
            m.put("minecraft:witch", 14);
            m.put("minecraft:blaze", 14);
            m.put("minecraft:wither_skeleton", 18);
            m.put("minecraft:guardian", 18);
            m.put("minecraft:hoglin", 16);
            m.put("minecraft:zoglin", 22);
            m.put("minecraft:slime", 8);
            m.put("minecraft:magma_cube", 10);
            m.put("minecraft:snow_golem", 10);
            return m;
        }
    }

    public static final class Space {
        // Passives
        public float lunarMinMultiplier = 0.85F;
        public float lunarMaxMultiplier = 1.20F;
        public float starshieldProjectileDamageMultiplier = 0.80F;

        // Abilities
        public int orbitalLaserCooldownSeconds = 60;
        public int orbitalLaserMiningCooldownSeconds = 60;
        public int orbitalLaserRangeBlocks = 64;
        public int orbitalLaserDelaySeconds = 1;
        public int orbitalLaserRadiusBlocks = 4;
        public float orbitalLaserDamage = 10.0F;

        public int orbitalLaserMiningRadiusBlocks = 2;
        public float orbitalLaserMiningHardnessCap = 60.0F;
        public int orbitalLaserMiningMaxBlocks = 64;

        public int gravityFieldCooldownSeconds = 45;
        public int gravityFieldDurationSeconds = 10;
        public int gravityFieldRadiusBlocks = 10;
        public float gravityFieldAllyGravityMultiplier = 0.75F;
        public float gravityFieldEnemyGravityMultiplier = 1.25F;

        public int blackHoleCooldownSeconds = 60;
        public int blackHoleDurationSeconds = 6;
        public int blackHoleRadiusBlocks = 10;
        public float blackHolePullStrength = 1.0F;
        public float blackHoleDamagePerSecond = 12.0F;

        public int whiteHoleCooldownSeconds = 60;
        public int whiteHoleDurationSeconds = 6;
        public int whiteHoleRadiusBlocks = 10;
        public float whiteHolePushStrength = 1.0F;
        public float whiteHoleDamagePerSecond = 9.0F;
    }

    public static final class Reaper {
        // Passives
        public float undeadWardDamageMultiplier = 0.80F;
        public int harvestRegenDurationSeconds = 4;
        public int harvestRegenAmplifier = 0;

        // Abilities
        public int graveSteedCooldownSeconds = 60;
        public int graveSteedDurationSeconds = 30;
        public float graveSteedDecayDamagePerSecond = 1.0F;

        public int witheringStrikesCooldownSeconds = 45;
        public int witheringStrikesDurationSeconds = 10;
        public int witheringStrikesWitherDurationSeconds = 4;
        public int witheringStrikesWitherAmplifier = 0;

        public int deathOathCooldownSeconds = 60;
        public int deathOathDurationSeconds = 12;
        public int deathOathRangeBlocks = 48;
        public float deathOathSelfDamagePerSecond = 1.0F;
        public float deathOathBonusDamage = 4.0F;

        public int retributionCooldownSeconds = 90;
        public int retributionDurationSeconds = 6;
        public float retributionDamageMultiplier = 1.0F;

        public int scytheSweepCooldownSeconds = 20;
        public int scytheSweepRangeBlocks = 5;
        public int scytheSweepArcDegrees = 110;
        public float scytheSweepDamage = 7.0F;
        public double scytheSweepKnockback = 0.55D;

        public int bloodChargeCooldownSeconds = 60;
        public int bloodChargeMaxChargeSeconds = 8;
        public float bloodChargeSelfDamagePerSecond = 1.0F;
        public float bloodChargeMaxMultiplier = 1.60F;
        public int bloodChargeBuffDurationSeconds = 8;

        public int shadowCloneCooldownSeconds = 90;
        public int shadowCloneDurationSeconds = 10;
        public float shadowCloneMaxHealth = 20.0F;
        public int shadowCloneCount = 10;
        public String shadowCloneEntityId = "minecraft:zombie";
    }

    public static final class Pillager {
        // Passives
        public float raidersTrainingProjectileVelocityMultiplier = 1.15F;
        public int shieldbreakerDisableCooldownTicks = 80;
        public float illagerDisciplineThresholdHearts = 4.0F;
        public int illagerDisciplineResistanceDurationSeconds = 4;
        public int illagerDisciplineResistanceAmplifier = 0;
        public int illagerDisciplineCooldownSeconds = 45;

        // Abilities
        public int fangsCooldownSeconds = 25;
        public int fangsRangeBlocks = 24;
        public int fangsCount = 16;
        public float fangsSpacingBlocks = 1.25F;
        public int fangsWarmupStepTicks = 2;

        public int ravageCooldownSeconds = 20;
        public int ravageRangeBlocks = 6;
        public float ravageDamage = 6.0F;
        public double ravageKnockback = 1.25D;

        public int vindicatorBreakCooldownSeconds = 35;
        public int vindicatorBreakDurationSeconds = 8;
        public int vindicatorBreakStrengthAmplifier = 0;
        public int vindicatorBreakShieldDisableCooldownTicks = 100;

        public int volleyCooldownSeconds = 45;
        public int volleyDurationSeconds = 3;
        public int volleyPeriodTicks = 10;
        public int volleyArrowsPerShot = 1;
        public float volleyArrowDamage = 4.0F;
        public float volleyArrowVelocity = 3.0F;
        public float volleyArrowInaccuracy = 1.0F;

        public int warhornCooldownSeconds = 60;
        public int warhornDurationSeconds = 8;
        public int warhornRadiusBlocks = 10;
        public int warhornAllySpeedAmplifier = 1;
        public int warhornAllyResistanceAmplifier = 0;
        public int warhornEnemySlownessAmplifier = 1;
        public int warhornEnemyWeaknessAmplifier = 0;

        public int snareCooldownSeconds = 30;
        public int snareDurationSeconds = 6;
        public int snareRangeBlocks = 24;
        public int snareSlownessAmplifier = 2;
    }

    public static final class Spy {
        // Passives
        public int stillnessSeconds = 5;
        public float stillnessMoveEpsilonBlocks = 0.05F;
        public int stillnessInvisRefreshSeconds = 2;
        public int backstepCooldownSeconds = 12;
        public double backstepVelocity = 0.7D;
        public double backstepUpVelocity = 0.15D;
        public float backstabBonusDamage = 3.0F;
        public int backstabAngleDegrees = 60;

        // Observation
        public int observeRangeBlocks = 24;
        public int observeWindowSeconds = 10 * 60;
        public int stealRequiredWitnessCount = 4;
        public int maxStolenAbilities = 3;

        // Abilities
        public int mimicFormCooldownSeconds = 30;
        public int mimicFormDurationSeconds = 10;
        public float mimicFormBonusMaxHealth = 4.0F;
        public float mimicFormSpeedMultiplier = 1.10F;

        public int echoCooldownSeconds = 25;
        public int echoWindowSeconds = 8;

        public int stealCooldownSeconds = 60;

        public int smokeBombCooldownSeconds = 30;
        public int smokeBombRadiusBlocks = 8;
        public int smokeBombDurationSeconds = 6;
        public int smokeBombBlindnessAmplifier = 0;
        public int smokeBombSlownessAmplifier = 0;

        public int stolenCastCooldownSeconds = 20;

        public int skinshiftCooldownSeconds = 90;
        public int skinshiftDurationSeconds = 20;
        public int skinshiftRangeBlocks = 24;
    }

    public static final class Beacon {
        // Passives
        public int coreRadiusBlocks = 8;
        public int corePulsePeriodSeconds = 2;
        public int coreRegenDurationSeconds = 3;
        public int coreRegenAmplifier = 0;

        public int stabilizeRadiusBlocks = 8;
        public int stabilizeReduceTicksPerSecond = 20;

        public int rallyRadiusBlocks = 10;
        public int rallyAbsorptionHearts = 4;
        public int rallyDurationSeconds = 8;

        // Abilities (auras)
        public int auraCooldownSeconds = 30;
        public int auraDurationSeconds = 12;
        public int auraRadiusBlocks = 10;
        public int auraRefreshSeconds = 2;
        public int auraSpeedAmplifier = 1;
        public int auraHasteAmplifier = 1;
        public int auraResistanceAmplifier = 0;
        public int auraJumpAmplifier = 1;
        public int auraStrengthAmplifier = 0;
        public int auraRegenAmplifier = 0;
    }

    public static final class Air {
        // Passives
        public float aerialGuardFallDamageMultiplier = 0.50F;
        public float aerialGuardDamageMultiplier = 0.85F;
        public float aerialGuardKnockbackMultiplier = 0.60F;
        public double windShearKnockback = 0.6D;
        public int windShearSlownessDurationSeconds = 3;
        public int windShearSlownessAmplifier = 0;

        // Abilities
        public int windJumpCooldownSeconds = 8;
        public double windJumpVerticalVelocity = 1.0D;
        public double windJumpForwardVelocity = 0.2D;

        public int galeSlamCooldownSeconds = 30;
        public int galeSlamWindowSeconds = 8;
        public int galeSlamRadiusBlocks = 4;
        public float galeSlamBonusDamage = 6.0F;
        public double galeSlamKnockback = 1.0D;

        public int crosswindCooldownSeconds = 30;
        public int crosswindRangeBlocks = 18;
        public int crosswindRadiusBlocks = 3;
        public float crosswindDamage = 4.0F;
        public double crosswindKnockback = 0.8D;
        public int crosswindSlownessDurationSeconds = 4;
        public int crosswindSlownessAmplifier = 1;

        public int dashCooldownSeconds = 6;
        public double dashVelocity = 1.6D;
        public double dashUpVelocity = 0.1D;
        public int dashIFrameDurationSeconds = 1;
        public int dashIFrameResistanceAmplifier = 4;

        public int airMaceBreachLevel = 4;
        public int airMaceWindBurstLevel = 3;
        public int airMaceMendingLevel = 1;
        public int airMaceUnbreakingLevel = 3;
        public int airMaceFireAspectLevel = 2;
    }

    public static final class Duelist {
        // Passives
        public float riposteBonusDamageMultiplier = 1.50F;
        public int riposteWindowSeconds = 2;
        public float focusBonusDamageMultiplier = 1.25F;
        public int focusRadiusBlocks = 15;
        public float combatStanceSpeedMultiplier = 1.10F;

        // Abilities
        public int lungeCooldownSeconds = 8;
        public int lungeDistanceBlocks = 8;
        public float lungeDamage = 6.0F;

        public int parryCooldownSeconds = 12;
        public int parryWindowTicks = 10;
        public int parryStunSeconds = 2;

        public int rapidStrikeCooldownSeconds = 45;
        public int rapidStrikeDurationSeconds = 5;

        public int flourishCooldownSeconds = 15;
        public int flourishRadiusBlocks = 3;
        public float flourishDamage = 5.0F;

        public int mirrorMatchCooldownSeconds = 90;
        public int mirrorMatchDurationSeconds = 15;
        public int mirrorMatchRangeBlocks = 20;

        public int bladeDanceCooldownSeconds = 30;
        public int bladeDanceDurationSeconds = 10;
        public float bladeDanceStartingMultiplier = 1.0F;
        public float bladeDanceIncreasePerHit = 0.10F;
        public float bladeDanceMaxMultiplier = 2.0F;
        public int bladeDanceResetSeconds = 3;
    }

    public static final class Hunter {
        // Passives
        public float preyMarkBonusDamageMultiplier = 1.15F;
        public int preyMarkDurationSeconds = 30;
        public int trackersEyeRangeBlocks = 30;
        public int trophyHunterDurationSeconds = 60;

        // Abilities
        public int huntingTrapCooldownSeconds = 20;
        public int huntingTrapRootSeconds = 3;
        public float huntingTrapDamage = 4.0F;
        public int huntingTrapLifetimeSeconds = 60;

        public int pounceCooldownSeconds = 15;
        public int pounceRangeBlocks = 20;
        public float pounceDamage = 6.0F;

        public int netShotCooldownSeconds = 25;
        public int netShotSlowSeconds = 5;
        public int netShotRangeBlocks = 30;

        public int cripplingCooldownSeconds = 20;
        public float cripplingSlowMultiplier = 0.50F;
        public int cripplingDurationSeconds = 8;
        public int cripplingRangeBlocks = 30;

        public int packTacticsCooldownSeconds = 45;
        public float packTacticsBonusDamageMultiplier = 1.20F;
        public int packTacticsDurationSeconds = 10;
        public int packTacticsRadiusBlocks = 10;

        // Six-Pack Pain
        public int sixPackPainCooldownSeconds = 120;
        public int sixPackPainCloneCount = 6;
        public int sixPackPainDurationSeconds = 10;
        public float sixPackPainHealthPerClone = 20.0F; // Total shared = 120 HP
        public int sixPackPainCloseTargetRangeBlocks = 5;
        public int sixPackPainWideTargetRangeBlocks = 30;
        public int sixPackPainBuffDurationTicks = 200; // 10 seconds
        public int sixPackPainDebuffDurationTicks = 100; // 5 seconds

        // Origin Tracking
        public int originTrackingCooldownSeconds = 30;
        public int originTrackingDurationSeconds = 60;
    }

    public static final class Sentinel {
        // Passives
        public float guardianAuraDamageReduction = 0.15F;
        public int guardianAuraRadiusBlocks = 8;
        public int fortressStandStillSeconds = 2;
        public int fortressResistanceAmplifier = 1;
        public float retributionThornsDamageMultiplier = 0.20F;

        // Abilities
        public int shieldWallCooldownSeconds = 30;
        public int shieldWallDurationSeconds = 8;
        public int shieldWallWidthBlocks = 5;
        public int shieldWallHeightBlocks = 3;

        public int tauntCooldownSeconds = 45;
        public int tauntDurationSeconds = 5;
        public int tauntRadiusBlocks = 10;
        public float tauntDamageReduction = 0.30F;

        public int interventionCooldownSeconds = 60;
        public int interventionRangeBlocks = 30;

        public int rallyCryCooldownSeconds = 60;
        public float rallyCryHealHearts = 4.0F;
        public int rallyCryResistanceDurationSeconds = 8;
        public int rallyCryRadiusBlocks = 12;

        public int lockdownCooldownSeconds = 90;
        public int lockdownDurationSeconds = 10;
        public int lockdownRadiusBlocks = 8;
    }

    public static final class Trickster {
        // Passives
        public float sleightOfHandChance = 0.20F;
        public float slipperyChance = 0.25F;

        // Abilities
        public int shadowSwapCooldownSeconds = 20;
        public int shadowSwapRangeBlocks = 32;

        public int mirageCooldownSeconds = 30;
        public int mirageDurationSeconds = 10;
        public int mirageCloneCount = 1;
        public int mirageRangeBlocks = 64;
        public int mirageBuffDurationSeconds = 6;
        public int mirageBuffAmplifier = 0;

        public int glitchStepCooldownSeconds = 10;
        public int glitchStepDistanceBlocks = 8;
        public float glitchStepAfterimgDamage = 4.0F;
        public int glitchStepAfterimgRadiusBlocks = 2;

        public int puppetMasterCooldownSeconds = 60;
        public int puppetMasterDurationSeconds = 3;
        public int puppetMasterRangeBlocks = 15;

        public int mindGamesCooldownSeconds = 45;
        public int mindGamesDurationSeconds = 5;
        public int mindGamesRangeBlocks = 20;
    }

    public static final class Legendary {
        public int craftSeconds = 600;
        public int craftMaxPerItem = 1;
        public int craftMaxActivePerItem = 1;
        public int trackerRefreshSeconds = 2;
        public int trackerMaxDistanceBlocks = 0; // 0 = unlimited

        public int recallCooldownSeconds = 60;

        public float chronoCharmCooldownMultiplier = 0.5F;

        public int hypnoHoldSeconds = 3;
        public int hypnoRangeBlocks = 40;
        public int hypnoViewRangeBlocks = 10;
        public float hypnoHealHearts = 10.0F;
        public int hypnoMaxControlled = 10;
        public int hypnoDurationSeconds = 0; // 0 = infinite while online

        public int earthsplitterRadiusBlocks = 4;
        public int earthsplitterTunnelLengthBlocks = 20;

        public int bloodOathSharpnessCap = 10;
        public int demolitionCooldownSeconds = 5;
        public int demolitionCooldownScalePercent = 50;
        public int demolitionFuseTicks = 80;
        public int demolitionRangeBlocks = 12;
        public float demolitionExplosionPower = 3.0F;
        public int demolitionTntCount = 3;
        public int hunterAimRangeBlocks = 50;
        public int hunterAimTimeoutSeconds = 15;
        public float hunterAimAssistStrength = 1.0F;
        public int thirdStrikeWindowSeconds = 5;
        public float thirdStrikeBonusDamage = 4.0F;
        public float vampiricHealAmount = 2.0F;

        public int duelistsRapierParryWindowTicks = 10;
        public int duelistsRapierCooldownSeconds = 8;
        public float duelistsRapierCritDamageMultiplier = 1.5F;

        public int challengersGauntletCooldownSeconds = 300;
        public int challengersGauntletRangeBlocks = 10;

        public int reversalMirrorDurationSeconds = 5;
        public int reversalMirrorCooldownSeconds = 60;

        public int gladiatorsMarkDurationSeconds = 60;
        public int gladiatorsMarkCooldownSeconds = 120;
        public int gladiatorsMarkRangeBlocks = 20;
        public float gladiatorsMarkDamageMultiplier = 1.5F;

        public int soulShackleDurationSeconds = 10;
        public int soulShackleCooldownSeconds = 90;
        public int soulShackleRangeBlocks = 15;
        public float soulShackleSplitRatio = 0.5F;

        public int experienceBladeMaxSharpness = 20;
        public int experienceBladeSharpnessPerTier = 2;
        public int experienceBladeXpLevelsPerTier = 10;

        public int trophyNecklaceMaxPassives = 10;

        public int supremeHelmetNightVisionAmplifier = 0;
        public int supremeHelmetWaterBreathingAmplifier = 0;
        public int supremeChestStrengthAmplifier = 0;
        public int supremeLeggingsFireResAmplifier = 0;
        public int supremeBootsSpeedAmplifier = 0;
        public int supremeSetResistanceAmplifier = 2;

        /**
         * Map of discount recipe ids to gem ids. Any listed recipe is only craftable while
         * the matching gem is active (multiple recipes can point at the same output item).
         */
        public java.util.Map<String, String> recipeGemRequirements = defaultRecipeGemRequirements();

        private static java.util.Map<String, String> defaultRecipeGemRequirements() {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("gems:tracker_compass_discount", "spy");
            map.put("gems:recall_relic_discount", "space");
            map.put("gems:hypno_staff_discount", "summoner");
            map.put("gems:earthsplitter_pick_discount", "wealth");
            map.put("gems:supreme_helmet_discount", "beacon");
            map.put("gems:supreme_chestplate_discount", "beacon");
            map.put("gems:supreme_leggings_discount", "beacon");
            map.put("gems:supreme_boots_discount", "beacon");
            map.put("gems:blood_oath_blade_discount", "reaper");
            map.put("gems:demolition_blade_discount", "terror");
            map.put("gems:hunters_sight_bow_discount", "puff");
            map.put("gems:third_strike_blade_discount", "strength");
            map.put("gems:vampiric_edge_discount", "life");
            map.put("gems:duelists_rapier_discount", "duelist");
            map.put("gems:challengers_gauntlet_discount", "duelist");
            map.put("gems:experience_blade_discount", "wealth");
            map.put("gems:reversal_mirror_discount", "sentinel");
            map.put("gems:hunters_trophy_necklace_discount", "hunter");
            map.put("gems:gladiators_mark_discount", "duelist");
            map.put("gems:soul_shackle_discount", "reaper");
            map.put("gems:chrono_charm_discount", "astra");
            return map;
        }
    }

    /**
     * Balance settings for bonus abilities and passives claimed at max energy.
     */
    public static final class BonusPool {
        /** Number of bonus abilities a player can claim at max energy */
        public int maxBonusAbilities = 1;
        /** Number of bonus passives a player can claim at max energy */
        public int maxBonusPassives = 1;
        /** Whether to show claimed bonuses in the HUD */
        public boolean showBonusesInHud = true;
        /** Global cooldown multiplier for bonus abilities (1.0 = normal) */
        public float bonusAbilityCooldownMultiplier = 1.0F;
        /** Global damage multiplier for bonus abilities (1.0 = normal) */
        public float bonusAbilityDamageMultiplier = 1.0F;
        /** Global effect duration multiplier for bonus passives (1.0 = normal) */
        public float bonusPassiveEffectMultiplier = 1.0F;

        // ================= BONUS ABILITY CONFIGS =================
        // Thunderstrike
        public int thunderstrikeCooldownSeconds = 15;
        public float thunderstrikeDamage = 8.0F;
        public int thunderstrikeRangeBlocks = 30;

        // Frostbite
        public int frostbiteCooldownSeconds = 12;
        public float frostbiteDamage = 4.0F;
        public int frostbiteFreezeSeconds = 3;
        public int frostbiteRangeBlocks = 20;

        // Earthshatter
        public int earthshatterCooldownSeconds = 20;
        public float earthshatterDamage = 6.0F;
        public int earthshatterRadiusBlocks = 5;

        // Shadowstep
        public int shadowstepCooldownSeconds = 8;
        public int shadowstepDistanceBlocks = 10;

        // Radiant Burst
        public int radiantBurstCooldownSeconds = 25;
        public float radiantBurstDamage = 5.0F;
        public int radiantBurstRadiusBlocks = 6;
        public int radiantBurstBlindSeconds = 3;

        // Venomspray
        public int venomsprayCooldownSeconds = 15;
        public int venomsprayPoisonSeconds = 8;
        public int venomsprayConeAngleDegrees = 45;
        public int venomsprayRangeBlocks = 8;

        // Timewarp
        public int timewarpCooldownSeconds = 45;
        public int timewarpDurationSeconds = 4;
        public int timewarpRadiusBlocks = 10;
        public int timewarpSlownessAmplifier = 3;

        // Decoy Trap (was Mirror Image)
        public int decoyTrapCooldownSeconds = 30;
        public float decoyTrapExplosionDamage = 6.0F;
        public int decoyTrapArmTimeSeconds = 2;
        public int decoyTrapMaxActive = 20;
        public int decoyTrapDespawnSeconds = 60;

        // Gravity Well
        public int gravityWellCooldownSeconds = 30;
        public int gravityWellDurationSeconds = 5;
        public int gravityWellRadiusBlocks = 6;
        public float gravityWellPullStrength = 0.5F;

        // Chain Lightning
        public int chainLightningCooldownSeconds = 12;
        public float chainLightningDamage = 4.0F;
        public int chainLightningMaxBounces = 5;
        public int chainLightningBounceRangeBlocks = 8;

        // Magma Pool
        public int magmaPoolCooldownSeconds = 25;
        public float magmaPoolDamagePerSecond = 2.0F;
        public int magmaPoolDurationSeconds = 8;
        public int magmaPoolRadiusBlocks = 4;

        // Ice Wall
        public int iceWallCooldownSeconds = 20;
        public int iceWallDurationSeconds = 10;
        public int iceWallWidthBlocks = 5;
        public int iceWallHeightBlocks = 3;

        // Wind Slash
        public int windSlashCooldownSeconds = 6;
        public float windSlashDamage = 5.0F;
        public int windSlashRangeBlocks = 15;

        // Curse Bolt (was Soul Drain)
        public int curseBoltCooldownSeconds = 18;
        public float curseBoltDamage = 3.0F;
        public int curseBoltEffectDurationSeconds = 6;

        // Berserker Rage
        public int berserkerRageCooldownSeconds = 60;
        public int berserkerRageDurationSeconds = 10;
        public float berserkerRageDamageBoostPercent = 200.0F;
        public float berserkerRageDamageTakenBoostPercent = 25.0F;

        // Ethereal Step (was Phase Shift)
        public int etherealStepCooldownSeconds = 10;
        public int etherealStepDistanceBlocks = 8;

        // Arcane Missiles
        public int arcaneMissilesCooldownSeconds = 10;
        public float arcaneMissilesDamagePerMissile = 2.0F;
        public int arcaneMissilesCount = 5;

        // Life Tap
        public int lifeTapCooldownSeconds = 45;
        public float lifeTapHealthCost = 4.0F;
        public float lifeTapCooldownReductionPercent = 50.0F;

        // Doom Bolt
        public int doomBoltCooldownSeconds = 20;
        public float doomBoltDamage = 12.0F;
        public float doomBoltVelocity = 0.5F;

        // Sanctuary
        public int sanctuaryCooldownSeconds = 60;
        public int sanctuaryDurationSeconds = 6;
        public int sanctuaryRadiusBlocks = 4;

        // Spectral Chains
        public int spectralChainsCooldownSeconds = 18;
        public int spectralChainsDurationSeconds = 4;
        public int spectralChainsRangeBlocks = 15;

        // Void Rift
        public int voidRiftCooldownSeconds = 30;
        public float voidRiftDamagePerSecond = 3.0F;
        public int voidRiftDurationSeconds = 5;
        public int voidRiftRadiusBlocks = 4;

        // Inferno Dash
        public int infernoDashCooldownSeconds = 12;
        public float infernoDashDamage = 3.0F;
        public int infernoDashDistanceBlocks = 10;
        public int infernoDashFireDurationSeconds = 4;

        // Tidal Wave
        public int tidalWaveCooldownSeconds = 25;
        public float tidalWaveDamage = 4.0F;
        public int tidalWaveRangeBlocks = 12;
        public int tidalWaveSlowSeconds = 3;

        // Starfall
        public int starfallCooldownSeconds = 35;
        public float starfallDamagePerHit = 3.0F;
        public int starfallMeteorCount = 8;
        public int starfallRadiusBlocks = 8;

        // Bloodlust
        public int bloodlustCooldownSeconds = 40;
        public int bloodlustDurationSeconds = 12;
        public float bloodlustAttackSpeedPerKill = 0.1F;
        public int bloodlustMaxStacks = 5;

        // Crystal Cage
        public int crystalCageCooldownSeconds = 35;
        public int crystalCageDurationSeconds = 3;
        public int crystalCageRangeBlocks = 20;

        // Phantasm
        public int phantasmCooldownSeconds = 25;
        public int phantasmDurationSeconds = 5;
        public float phantasmExplosionDamage = 6.0F;

        // Sonic Boom
        public int sonicBoomCooldownSeconds = 20;
        public float sonicBoomDamage = 4.0F;
        public int sonicBoomRadiusBlocks = 8;
        public float sonicBoomKnockbackStrength = 2.0F;

        // Vampiric Touch
        public int vampiricTouchCooldownSeconds = 15;
        public float vampiricTouchDamage = 6.0F;
        public float vampiricTouchHealPercent = 50.0F;

        // Blinding Flash
        public int blindingFlashCooldownSeconds = 25;
        public int blindingFlashBlindSeconds = 4;
        public int blindingFlashRadiusBlocks = 10;

        // Storm Call
        public int stormCallCooldownSeconds = 45;
        public float stormCallDamagePerStrike = 4.0F;
        public int stormCallDurationSeconds = 6;
        public int stormCallRadiusBlocks = 12;
        public int stormCallStrikesPerSecond = 2;

        // Quicksand
        public int quicksandCooldownSeconds = 25;
        public int quicksandDurationSeconds = 8;
        public int quicksandRadiusBlocks = 5;
        public int quicksandSlownessAmplifier = 3;

        // Searing Light
        public int searingLightCooldownSeconds = 14;
        public float searingLightDamage = 5.0F;
        public float searingLightUndeadBonusDamage = 4.0F;
        public int searingLightRangeBlocks = 20;

        // Spectral Blade (was Shadow Clone)
        public int spectralBladeCooldownSeconds = 20;
        public float spectralBladeDamage = 3.0F;
        public int spectralBladeDurationSeconds = 8;

        // Nether Portal
        public int netherPortalCooldownSeconds = 15;
        public int netherPortalDistanceBlocks = 15;

        // Entangle
        public int entangleCooldownSeconds = 20;
        public int entangleDurationSeconds = 4;
        public int entangleRadiusBlocks = 6;

        // Mind Spike
        public int mindSpikeCooldownSeconds = 16;
        public float mindSpikeDamagePerSecond = 2.0F;
        public int mindSpikeDurationSeconds = 5;
        public int mindSpikeRangeBlocks = 25;

        // Seismic Slam
        public int seismicSlamCooldownSeconds = 18;
        public float seismicSlamDamage = 6.0F;
        public int seismicSlamRadiusBlocks = 6;
        public float seismicSlamKnockupStrength = 0.8F;

        // Icicle Barrage
        public int icicleBarrageCooldownSeconds = 12;
        public float icicleBarrageDamagePerIcicle = 2.0F;
        public int icicleBarrageCount = 8;
        public int icicleBarrageRangeBlocks = 20;

        // Banishment
        public int banishmentCooldownSeconds = 60;
        public int banishmentDistanceBlocks = 50;
        public int banishmentRangeBlocks = 20;

        // Corpse Explosion
        public int corpseExplosionCooldownSeconds = 25;
        public float corpseExplosionDamage = 5.0F;
        public int corpseExplosionRadiusBlocks = 6;
        public int corpseExplosionCorpseRangeBlocks = 10;
        public int corpseExplosionMarkDurationSeconds = 5;

        // Soul Swap
        public int soulSwapCooldownSeconds = 30;
        public int soulSwapRangeBlocks = 25;

        // Mark of Death
        public int markOfDeathCooldownSeconds = 35;
        public int markOfDeathDurationSeconds = 8;
        public float markOfDeathBonusDamagePercent = 30.0F;
        public int markOfDeathRangeBlocks = 30;

        // Iron Maiden
        public int ironMaidenCooldownSeconds = 40;
        public int ironMaidenDurationSeconds = 6;
        public float ironMaidenReflectPercent = 50.0F;

        // Warp Strike (was Spirit Walk)
        public int warpStrikeCooldownSeconds = 14;
        public float warpStrikeDamage = 5.0F;
        public int warpStrikeRangeBlocks = 15;

        // Vortex Strike
        public int vortexStrikeCooldownSeconds = 18;
        public float vortexStrikeDamage = 4.0F;
        public int vortexStrikeRadiusBlocks = 5;
        public float vortexStrikePullStrength = 1.0F;

        // Plague Cloud
        public int plagueCloudCooldownSeconds = 30;
        public int plagueCloudDurationSeconds = 10;
        public int plagueCloudPoisonAmplifier = 1;
        public int plagueCloudWeaknessAmplifier = 0;

        // Overcharge
        public int overchargeCooldownSeconds = 45;
        public float overchargeDamageMultiplier = 2.0F;
        public float overchargeHealthCost = 4.0F;
        public int overchargeDurationSeconds = 10;

        // Gravity Crush (was Temporal Anchor)
        public int gravityCrushCooldownSeconds = 22;
        public float gravityCrushDamage = 5.0F;
        public int gravityCrushRootDurationSeconds = 2;
        public int gravityCrushRangeBlocks = 20;

        // ================= BONUS PASSIVE CONFIGS =================
        // Thorns Aura
        public float thornsAuraDamagePercent = 25.0F;

        // Lifesteal
        public float lifestealPercent = 10.0F;

        // Dodge Chance
        public float dodgeChancePercent = 10.0F;

        // Critical Strike
        public float criticalStrikeBonusDamagePercent = 50.0F;
        public float criticalStrikeChanceBonus = 15.0F;

        // Mana Shield
        public float manaShieldXpPerDamage = 2.0F;

        // Regeneration Boost
        public int regenerationBoostAmplifier = 0;

        // Damage Reduction
        public float damageReductionPercent = 10.0F;

        // Attack Speed
        public float attackSpeedBoostPercent = 15.0F;

        // Reach Extend
        public float reachExtendBlocks = 1.5F;

        // Impact Absorb (was Night Vision)
        public float impactAbsorbPercent = 20.0F;
        public int impactAbsorbMaxAbsorption = 4;

        // Adrenaline Surge (was Water Breathing)
        public int adrenalineSurgeDurationSeconds = 3;
        public int adrenalineSurgeCooldownSeconds = 10;

        // Intimidate (was Fire Walker)
        public float intimidateDamageReductionPercent = 10.0F;
        public int intimidateRadiusBlocks = 8;

        // Evasive Roll (was Feather Fall)
        public int evasiveRollCooldownSeconds = 8;
        public int evasiveRollDistanceBlocks = 4;

        // Combat Meditate (was Swift Sneak)
        public float combatMeditateHealPerSecond = 1.0F;
        public int combatMeditateDelaySeconds = 2;

        // Weapon Mastery (was Soul Speed)
        public float weaponMasteryBonusDamage = 1.0F;

        // Culling Blade (was Depth Strider)
        public float cullingBladeThresholdPercent = 10.0F;

        // Thick Skin (was Frost Walker)
        public float thickSkinProjectileReductionPercent = 25.0F;

        // XP Boost
        public float xpBoostPercent = 25.0F;

        // Hunger Resist
        public float hungerResistReductionPercent = 50.0F;

        // Poison Immunity
        public boolean poisonImmunityFull = true;

        // Second Wind
        public int secondWindCooldownSeconds = 180;

        // Echo Strike
        public float echoStrikeChancePercent = 15.0F;

        // Chain Breaker (was Momentum)
        public float chainBreakerDurationReductionPercent = 50.0F;

        // Stone Skin
        public float stoneSkinFlatReduction = 1.0F;

        // Arcane Barrier
        public int arcaneBarrierCooldownSeconds = 30;
        public float arcaneBarrierAbsorbAmount = 4.0F;

        // Predator Sense
        public float predatorSenseThresholdPercent = 30.0F;
        public int predatorSenseRangeBlocks = 20;

        // Battle Medic
        public float battleMedicHealPerSecond = 0.5F;
        public int battleMedicRadiusBlocks = 8;

        // Last Stand
        public float lastStandThresholdPercent = 25.0F;
        public float lastStandDamageBoostPercent = 50.0F;

        // Executioner
        public float executionerThresholdPercent = 25.0F;
        public float executionerBonusDamagePercent = 30.0F;

        // Bloodthirst
        public float bloodthirstHealOnKill = 4.0F;

        // Steel Resolve
        public boolean steelResolveFullKnockbackImmunity = true;

        // Elemental Harmony
        public float elementalHarmonyReductionPercent = 25.0F;

        // Treasure Hunter
        public float treasureHunterDropBoostPercent = 20.0F;

        // Counter Strike (was Shadowmeld)
        public float counterStrikeDamageMultiplier = 2.0F;
        public int counterStrikeWindowSeconds = 3;

        // Bulwark
        public float bulwarkBlockEffectivenessBoostPercent = 50.0F;

        // Quick Recovery
        public float quickRecoveryDebuffReductionPercent = 30.0F;

        // Overflowing Vitality
        public int overfowingVitalityBonusHearts = 4;

        // Magnetic Pull
        public float magneticPullRangeMultiplier = 2.0F;

        // Vengeance
        public float vengeanceBuffDurationSeconds = 5.0F;
        public float vengeanceDamageBoostPercent = 50.0F;

        // Nemesis (was Phoenix Blessing)
        public float nemesisBonusDamagePercent = 25.0F;

        // Hunter's Instinct (was Spectral Sight)
        public float huntersInstinctCritBoostPercent = 20.0F;

        // Berserker Blood
        public float berserkerBloodMaxAttackSpeedBoost = 50.0F;

        // Opportunist
        public float opportunistBackstabBonusPercent = 25.0F;

        // Ironclad
        public float ironcladArmorBoostPercent = 25.0F;

        // Mist Form
        public float mistFormPhaseChancePercent = 10.0F;

        // War Cry
        public int warCryRadiusBlocks = 10;
        public int warCryStrengthDurationSeconds = 5;

        // Siphon Soul
        public int siphonSoulRegenDurationSeconds = 4;

        // Unbreakable
        public float unbreakableDurabilityReductionPercent = 50.0F;

        // Focused Mind
        public float focusedMindCooldownReductionPercent = 15.0F;

        // Sixth Sense
        public int sixthSenseWarningRangeBlocks = 15;
    }

    /**
     * Mastery system settings (cosmetic progression).
     */
    public static final class Mastery {
        /**
         * Whether the mastery system is enabled.
         */
        public boolean enabled = true;

        /**
         * Whether to show aura particles for players with selected auras.
         */
        public boolean showAuraParticles = true;
    }

    /**
     * Rivalry system settings.
     */
    public static final class Rivalry {
        /**
         * Whether the rivalry system is enabled.
         */
        public boolean enabled = true;

        /**
         * Damage multiplier when attacking your rivalry target.
         * 1.25 = 25% bonus damage.
         */
        public double damageMultiplier = 1.25;

        /**
         * Whether to show the rivalry target in the HUD.
         */
        public boolean showInHud = true;
    }

    /**
     * Loadout presets system settings.
     */
    public static final class Loadouts {
        /**
         * Whether the loadout presets system is enabled.
         */
        public boolean enabled = true;

        /**
         * Minimum energy level required to unlock loadout presets.
         * Players must have this energy level or higher to save/load presets.
         */
        public int unlockEnergy = 6;

        /**
         * Maximum number of preset slots per gem.
         */
        public int maxPresetsPerGem = 5;
    }

    /**
     * Team synergies system settings.
     * Synergies trigger when different gem abilities are cast within a short window.
     */
    public static final class Synergies {
        /**
         * Whether the team synergies system is enabled.
         */
        public boolean enabled = true;

        /**
         * Time window in seconds for abilities to count as a synergy combo.
         * Both abilities must be cast within this window.
         */
        public int windowSeconds = 3;

        /**
         * Cooldown in seconds before the same synergy can trigger again.
         * This applies per-group (participants who triggered together).
         */
        public int cooldownSeconds = 30;

        /**
         * Whether to show synergy trigger notifications in chat/action bar.
         */
        public boolean showNotifications = true;

        /**
         * Per-synergy overrides (window/cooldown/enable). Leave empty to use defaults.
         */
        public java.util.List<SynergyEntry> entries = defaultSynergyEntries();

        public static final class SynergyEntry {
            public String id = "";
            public boolean enabled = true;
            public int windowSeconds = 0;
            public int cooldownSeconds = 0;

            public SynergyEntry() {
            }

            public SynergyEntry(String id, boolean enabled, int windowSeconds, int cooldownSeconds) {
                this.id = id;
                this.enabled = enabled;
                this.windowSeconds = windowSeconds;
                this.cooldownSeconds = cooldownSeconds;
            }
        }

        public static java.util.List<SynergyEntry> defaultSynergyEntries() {
            java.util.List<SynergyEntry> list = new java.util.ArrayList<>();
            list.add(new SynergyEntry("alpha_swarm", true, 3, 45));
            list.add(new SynergyEntry("blood_convergence", true, 3, 60));
            list.add(new SynergyEntry("bulwark_riposte", true, 3, 45));
            list.add(new SynergyEntry("celestial_stride", true, 3, 45));
            list.add(new SynergyEntry("cyclone", true, 3, 30));
            list.add(new SynergyEntry("ember_chain", true, 3, 45));
            list.add(new SynergyEntry("ghostline", true, 3, 45));
            list.add(new SynergyEntry("gravity_weave", true, 3, 45));
            list.add(new SynergyEntry("guardians_challenge", true, 3, 30));
            list.add(new SynergyEntry("harvest_tide", true, 3, 60));
            list.add(new SynergyEntry("ion_burst", true, 3, 45));
            list.add(new SynergyEntry("iron_chorus", true, 3, 60));
            list.add(new SynergyEntry("judgment_field", true, 3, 60));
            list.add(new SynergyEntry("mirror_heist", true, 3, 45));
            list.add(new SynergyEntry("overclock", true, 3, 60));
            list.add(new SynergyEntry("pack_alpha", true, 3, 30));
            list.add(new SynergyEntry("phantom_strike", true, 3, 30));
            list.add(new SynergyEntry("plasma_lance", true, 3, 45));
            list.add(new SynergyEntry("plunder", true, 3, 30));
            list.add(new SynergyEntry("predator_lunge", true, 3, 45));
            list.add(new SynergyEntry("radiant_restoration", true, 3, 30));
            list.add(new SynergyEntry("reality_warp", true, 3, 30));
            list.add(new SynergyEntry("reaping_snare", true, 3, 45));
            list.add(new SynergyEntry("sanctuary_pulse", true, 3, 60));
            list.add(new SynergyEntry("shadow_barrage", true, 3, 45));
            list.add(new SynergyEntry("shadow_relay", true, 3, 45));
            list.add(new SynergyEntry("siegebreak", true, 3, 45));
            list.add(new SynergyEntry("singularity_ignition", true, 3, 60));
            list.add(new SynergyEntry("sonic_uppercut", true, 3, 45));
            list.add(new SynergyEntry("soul_exchange", true, 3, 30));
            list.add(new SynergyEntry("stellar_cataclysm", true, 3, 60));
            list.add(new SynergyEntry("stormbreaker", true, 3, 45));
            list.add(new SynergyEntry("thunderstorm", true, 3, 30));
            list.add(new SynergyEntry("vacuum_reversal", true, 3, 45));
            list.add(new SynergyEntry("warcry", true, 3, 30));
            list.add(new SynergyEntry("warp_drive", true, 3, 30));
            return list;
        }
    }

    /**
     * Augments/inscriptions configuration (slot caps, rarity weights, magnitude ranges).
     */
    public static final class Augments {
        /** Max augment slots for a gem. */
        public int gemMaxSlots = 4;
        /** Max inscription slots for a legendary item. */
        public int legendaryMaxSlots = 2;

        /** Rarity roll weights (higher = more common). */
        public int rarityCommonWeight = 70;
        public int rarityRareWeight = 25;
        public int rarityEpicWeight = 5;

        /** Magnitude roll ranges (multiplies base augment values). */
        public float commonMagnitudeMin = 0.9F;
        public float commonMagnitudeMax = 1.1F;
        public float rareMagnitudeMin = 1.1F;
        public float rareMagnitudeMax = 1.3F;
        public float epicMagnitudeMin = 1.3F;
        public float epicMagnitudeMax = 1.6F;
    }
}
