package com.feel.gems.power.registry;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.gem.terror.TerrorDreadAuraPassive;
import com.feel.gems.power.gem.terror.TerrorFearlessPassive;
import com.feel.gems.power.passive.AutoEnchantPassive;
import com.feel.gems.power.passive.MarkerPassive;
import com.feel.gems.power.passive.StatusEffectPassive;
import com.feel.gems.power.passive.bonus.*;
import com.feel.gems.power.passive.duelist.DuelistCombatStancePassive;
import com.feel.gems.power.passive.flux.FluxCapacitorPassive;
import com.feel.gems.power.passive.puff.PuffWindbornePassive;
import com.feel.gems.power.passive.sentinel.SentinelFortressPassive;
import com.feel.gems.power.passive.strength.StrengthAdrenalinePassive;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;




public final class ModPassives {
    private static final Map<Identifier, GemPassive> PASSIVES = new HashMap<>();

    static {
        register(new MarkerPassive(
                PowerIds.SOUL_CAPTURE,
                "Soul Capture",
                "Stores the most recently killed mob for later release."
        ));
        register(new MarkerPassive(
                PowerIds.SOUL_HEALING,
                "Soul Healing",
                "Heals the holder on successful soul capture or release."
        ));

        register(new StatusEffectPassive(
                PowerIds.FIRE_RESISTANCE,
                "Fire Resistance",
                "Permanent immunity to fire/lava damage.",
                MobEffects.FIRE_RESISTANCE,
                0
        ));
        register(new MarkerPassive(
                PowerIds.AUTO_SMELT,
                "Auto-smelt",
                "Automatically smelts smeltable block drops."
        ));
        register(AutoEnchantPassive.fireAspect(
                PowerIds.AUTO_ENCHANT_FIRE_ASPECT,
                "Auto-enchant Fire Aspect",
                "Automatically applies Fire Aspect to melee weapons.",
                1
        ));

        register(new MarkerPassive(
                PowerIds.FLUX_CHARGE_STORAGE,
                "Charge Storage",
                "Consume valuables (offhand/inventory) to charge Flux (Diamond/Gold/Copper/Emerald/Amethyst Blocks, Netherite Scrap, enchanted diamond gear)."
        ));
        register(new MarkerPassive(
                PowerIds.FLUX_ALLY_INVERSION,
                "Ally Inversion",
                "Offensive effects on trusted players repair armor instead of damaging."
        ));
        register(new MarkerPassive(
                PowerIds.FLUX_OVERCHARGE_RAMP,
                "Overcharge Ramp",
                "At 100% charge, begins charging toward 200% while damaging the holder."
        ));
        register(new FluxCapacitorPassive());
        register(new MarkerPassive(
                PowerIds.FLUX_CONDUCTIVITY,
                "Flux Conductivity",
                "Taking damage also converts some of it into Flux charge."
        ));
        register(new MarkerPassive(
                PowerIds.FLUX_INSULATION,
                "Flux Insulation",
                "While highly charged, incoming damage is reduced."
        ));

        register(AutoEnchantPassive.unbreaking(
                PowerIds.AUTO_ENCHANT_UNBREAKING,
                "Auto-enchant Unbreaking",
                "Automatically applies Unbreaking to tools/armor.",
                3
        ));
        register(new MarkerPassive(
                PowerIds.DOUBLE_SATURATION,
                "Double Saturation",
                "Food restores double saturation."
        ));

        register(new MarkerPassive(
                PowerIds.FALL_DAMAGE_IMMUNITY,
                "Fall Damage Immunity",
                "Negates fall damage entirely."
        ));
        register(AutoEnchantPassive.power(
                PowerIds.AUTO_ENCHANT_POWER,
                "Auto-enchant Power",
                "Automatically applies Power to bows.",
                3
        ));
        register(AutoEnchantPassive.punch(
                PowerIds.AUTO_ENCHANT_PUNCH,
                "Auto-enchant Punch",
                "Automatically applies Punch to bows.",
                1
        ));
        register(new MarkerPassive(
                PowerIds.SCULK_SILENCE,
                "Sculk Silence",
                "Immune to triggering sculk shriekers."
        ));
        register(new MarkerPassive(
                PowerIds.CROP_TRAMPLE_IMMUNITY,
                "Crop-Trample Immunity",
                "Prevents trampling farmland."
        ));
        register(new PuffWindbornePassive());

        register(new StatusEffectPassive(
                PowerIds.SPEED_I,
                "Speed I",
                "Permanent Speed I effect.",
                MobEffects.SPEED,
                0
        ));
        register(new StatusEffectPassive(
                PowerIds.SPEED_HASTE,
                "Haste I",
                "Permanent Haste I effect.",
                MobEffects.HASTE,
                0
        ));
        register(new MarkerPassive(
                PowerIds.SPEED_MOMENTUM,
                "Momentum",
                "Abilities scale with your movement speed at cast time."
        ));
        register(new MarkerPassive(
                PowerIds.SPEED_FRICTIONLESS,
                "Frictionless Steps",
                "Reduced slowdown from cobweb, honey, and powder snow."
        ));

        register(new StatusEffectPassive(
                PowerIds.STRENGTH_I,
                "Strength I",
                "Permanent Strength I effect.",
                MobEffects.STRENGTH,
                0
        ));
        register(AutoEnchantPassive.sharpness(
                PowerIds.AUTO_ENCHANT_SHARPNESS,
                "Auto-enchant Sharpness",
                "Automatically applies Sharpness to melee weapons.",
                3
        ));
        register(new StrengthAdrenalinePassive());

        register(AutoEnchantPassive.mending(
                PowerIds.AUTO_ENCHANT_MENDING,
                "Auto-enchant Mending",
                "Automatically applies Mending to tools/armor."
        ));
        register(AutoEnchantPassive.fortune(
                PowerIds.AUTO_ENCHANT_FORTUNE,
                "Auto-enchant Fortune",
                "Automatically applies Fortune to tools.",
                3
        ));
        register(AutoEnchantPassive.looting(
                PowerIds.AUTO_ENCHANT_LOOTING,
                "Auto-enchant Looting",
                "Automatically applies Looting to melee weapons.",
                3
        ));
        register(new StatusEffectPassive(
                PowerIds.LUCK,
                "Luck",
                "Permanent Luck effect.",
                MobEffects.LUCK,
                0
        ));
        register(new StatusEffectPassive(
                PowerIds.HERO_OF_THE_VILLAGE,
                "Hero of the Village",
                "Permanent Hero of the Village effect.",
                MobEffects.HERO_OF_THE_VILLAGE,
                4
        ));
        register(new MarkerPassive(
                PowerIds.WEALTH_CURED_PRICES,
                "Cured Prices",
                "Villager trades cost 1 item or 1 emerald while Wealth is active."
        ));
        register(new MarkerPassive(
                PowerIds.DURABILITY_CHIP,
                "Durability chip",
                "Does extra armor durability damage per strike."
        ));
        register(new MarkerPassive(
                PowerIds.ARMOR_MEND_ON_HIT,
                "Armor mend on hit",
                "Slowly repairs the holder's armor when hitting enemies."
        ));
        register(new MarkerPassive(
                PowerIds.DOUBLE_DEBRIS,
                "Double Debris",
                "Furnace outputs double netherite scrap."
        ));

        register(new TerrorDreadAuraPassive());
        register(new TerrorFearlessPassive());
        register(new MarkerPassive(
                PowerIds.TERROR_BLOOD_PRICE,
                "Blood Price",
                "On killing a player, gain a short burst of power."
        ));

        register(new MarkerPassive(
                PowerIds.SUMMONER_BOND,
                "Summoner's Bond",
                "Your summons will never harm you or your trusted players."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_COMMANDERS_MARK,
                "Commander's Mark",
                "Sword hits mark targets; summons prioritize and gain a short damage buff."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_SOULBOUND,
                "Soulbound Minions",
                "Summons despawn when you die or log out."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_FAMILIARS_BLESSING,
                "Familiar's Blessing",
                "Summons spawn with bonus health."
        ));

        // Space
        register(new MarkerPassive(
                PowerIds.SPACE_LUNAR_SCALING,
                "Lunar Scaling",
                "Outgoing damage and self-healing scale with the current moon phase (full moon strongest)."
        ));
        register(new StatusEffectPassive(
                PowerIds.SPACE_LOW_GRAVITY,
                "Low Gravity",
                "Minor Slow Falling and no fall damage while the gem is active.",
                MobEffects.SLOW_FALLING,
                0
        ));
        register(new MarkerPassive(
                PowerIds.SPACE_STARSHIELD,
                "Starshield",
                "Reduces projectile damage while outdoors at night."
        ));

        // Reaper
        register(new MarkerPassive(
                PowerIds.REAPER_ROT_EATER,
                "Rot Eater",
                "Eating rotten flesh or spider eyes does not apply negative effects."
        ));
        register(new MarkerPassive(
                PowerIds.REAPER_UNDEAD_WARD,
                "Undead Ward",
                "Reduces damage taken from undead mobs."
        ));
        register(new MarkerPassive(
                PowerIds.REAPER_HARVEST,
                "Harvest",
                "Killing mobs grants a brief burst of regeneration."
        ));

        // Pillager
        register(new MarkerPassive(
                PowerIds.PILLAGER_RAIDERS_TRAINING,
                "Raider's Training",
                "Your fired projectiles travel faster."
        ));
        register(new MarkerPassive(
                PowerIds.PILLAGER_SHIELDBREAKER,
                "Shieldbreaker",
                "Your melee hits can disable shields without using an axe."
        ));
        register(new MarkerPassive(
                PowerIds.PILLAGER_ILLAGER_DISCIPLINE,
                "Illager Discipline",
                "When you drop low, gain a brief burst of Resistance (cooldown)."
        ));
        register(AutoEnchantPassive.quickCharge(
                PowerIds.PILLAGER_CROSSBOW_MASTERY,
                "Crossbow Mastery",
                "Automatically applies Quick Charge to crossbows.",
                2
        ));
        register(new StatusEffectPassive(
                PowerIds.PILLAGER_RAIDER_STRIDE,
                "Raider's Stride",
                "Permanent minor Speed while the gem is active.",
                MobEffects.SPEED,
                0
        ));

        // Spy
        register(new MarkerPassive(
                PowerIds.SPY_STILLNESS_CLOAK,
                "Stillness Cloak",
                "Stand still to become invisible with no particles; moving cancels."
        ));
        register(new MarkerPassive(
                PowerIds.SPY_SILENT_STEP,
                "Silent Step",
                "Suppresses sculk sensor/shrieker activation from your actions."
        ));
        register(new MarkerPassive(
                PowerIds.SPY_FALSE_SIGNATURE,
                "False Signature",
                "Blocks tracking effects/items from revealing your location or gem info."
        ));
        register(new MarkerPassive(
                PowerIds.SPY_BACKSTEP,
                "Backstab",
                "Attacking from behind deals bonus damage."
        ));
        register(new StatusEffectPassive(
                PowerIds.SPY_QUICK_HANDS,
                "Quick Hands",
                "Permanent minor Haste while the gem is active.",
                MobEffects.HASTE,
                0
        ));

        // Beacon
        register(new MarkerPassive(
                PowerIds.BEACON_CORE,
                "Beacon Core",
                "Trusted allies near you receive periodic Regeneration pulses."
        ));
        register(new MarkerPassive(
                PowerIds.BEACON_STABILIZE,
                "Stabilize",
                "Reduces harmful effect durations on trusted allies nearby."
        ));
        register(new MarkerPassive(
                PowerIds.BEACON_RALLY,
                "Rally",
                "Casting a beacon aura grants allies brief Absorption."
        ));

        // Air
        register(new AirMacePassive(
                PowerIds.AIR_WINDBURST_MACE,
                "Windburst Mace",
                "Grants a maxed-out mace while the gem is active."
        ));
        register(new MarkerPassive(
                PowerIds.AIR_AERIAL_GUARD,
                "Aerial Guard",
                "Reduced damage and knockback while holding the mace."
        ));
        register(new MarkerPassive(
                PowerIds.AIR_WIND_SHEAR,
                "Wind Shear",
                "Mace strikes add extra knockback and a short slow."
        ));

        // Bonus Pool Passives (claimable at energy 10/10) - Original 20
        register(new BonusThornsAuraPassive());
        register(new BonusLifestealPassive());
        register(new BonusDodgeChancePassive());
        register(new BonusCriticalStrikePassive());
        register(new BonusManaShieldPassive());
        register(new BonusRegenerationBoostPassive());
        register(new BonusDamageReductionPassive());
        register(new BonusAttackSpeedPassive());
        register(new BonusReachExtendPassive());
        register(new BonusImpactAbsorbPassive());
        register(new BonusAdrenalineSurgePassive());
        register(new BonusIntimidatePassive());
        register(new BonusEvasiveRollPassive());
        register(new BonusCombatMeditatePassive());
        register(new BonusWeaponMasteryPassive());
        register(new BonusCullingBladePassive());
        register(new BonusThickSkinPassive());
        register(new BonusXpBoostPassive());
        register(new BonusHungerResistPassive());
        register(new BonusPoisonImmunityPassive());
        
        // Bonus Pool Passives - New 30
        register(new BonusSecondWindPassive());
        register(new BonusEchoStrikePassive());
        register(new BonusChainBreakerPassive());
        register(new BonusStoneSkinPassive());
        register(new BonusArcaneBarrierPassive());
        register(new BonusPredatorSensePassive());
        register(new BonusBattleMedicPassive());
        register(new BonusLastStandPassive());
        register(new BonusExecutionerPassive());
        register(new BonusBloodthirstPassive());
        register(new BonusSteelResolvePassive());
        register(new BonusElementalHarmonyPassive());
        register(new BonusTreasureHunterPassive());
        register(new BonusCounterStrikePassive());
        register(new BonusBulwarkPassive());
        register(new BonusQuickRecoveryPassive());
        register(new BonusOverflowingVitalityPassive());
        register(new BonusMagneticPullPassive());
        register(new BonusVengeancePassive());
        register(new BonusNemesisPassive());
        register(new BonusHuntersInstinctPassive());
        register(new BonusBerserkerBloodPassive());
        register(new BonusOpportunistPassive());
        register(new BonusIroncladPassive());
        register(new BonusSpectralFormPassive());
        register(new BonusWarCryPassive());
        register(new BonusAdrenalineRushPassive());
        register(new BonusUnbreakablePassive());
        register(new BonusFocusedMindPassive());
        register(new BonusSixthSensePassive());

        // Void gem passive
        register(new MarkerPassive(
                PowerIds.VOID_IMMUNITY,
                "Void Immunity",
                "Immune to all gem abilities and passives from other players."
        ));

        // Chaos gem passive
        register(new MarkerPassive(
                PowerIds.CHAOS_RANDOM_ROTATION,
                "Chaos Rotation",
                "Gain a random ability and passive every 5 minutes."
        ));

        // Duelist gem passives
        register(new MarkerPassive(
                PowerIds.DUELIST_RIPOSTE,
                "Riposte",
                "After a successful parry, your next melee attack deals bonus damage."
        ));
        register(new MarkerPassive(
                PowerIds.DUELIST_FOCUS,
                "Duelist's Focus",
                "Deal 25% more damage in 1v1 combat when no other players are within 15 blocks."
        ));
        register(new DuelistCombatStancePassive());

        // Hunter gem passives
        register(new MarkerPassive(
                PowerIds.HUNTER_PREY_MARK,
                "Prey Mark",
                "Hitting an enemy marks them as prey; deal bonus damage to marked targets."
        ));
        register(new MarkerPassive(
                PowerIds.HUNTER_TRACKERS_EYE,
                "Tracker's Eye",
                "Marked enemies are visible through walls within 30 blocks."
        ));
        register(new MarkerPassive(
                PowerIds.HUNTER_TROPHY_HUNTER,
                "Trophy Hunter",
                "On player kill, temporarily gain one of their random passives."
        ));

        // Sentinel gem passives
        register(new MarkerPassive(
                PowerIds.SENTINEL_GUARDIAN_AURA,
                "Guardian Aura",
                "Nearby trusted allies take 15% less damage."
        ));
        register(new SentinelFortressPassive());
        register(new MarkerPassive(
                PowerIds.SENTINEL_RETRIBUTION_THORNS,
                "Retribution Thorns",
                "Attackers take reflected damage when they hit you."
        ));

        // Trickster gem passives
        register(new MarkerPassive(
                PowerIds.TRICKSTER_SLEIGHT_OF_HAND,
                "Sleight of Hand",
                "20% chance to not consume items when using throwables."
        ));
        register(new MarkerPassive(
                PowerIds.TRICKSTER_CHAOS_AGENT,
                "Chaos Agent",
                "Your abilities have randomized bonus effects (can be beneficial or detrimental)."
        ));
        register(new MarkerPassive(
                PowerIds.TRICKSTER_SLIPPERY,
                "Slippery",
                "25% chance to ignore slowing effects."
        ));
    }

    private ModPassives() {
    }

    public static GemPassive get(Identifier id) {
        return PASSIVES.get(id);
    }

    public static Map<Identifier, GemPassive> all() {
        return Collections.unmodifiableMap(PASSIVES);
    }

    private static void register(GemPassive passive) {
        PASSIVES.put(passive.id(), passive);
    }
}
