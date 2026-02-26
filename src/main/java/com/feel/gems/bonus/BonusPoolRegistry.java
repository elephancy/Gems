package com.feel.gems.bonus;

import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Defines the pool of claimable bonus abilities and passives.
 * These are available to players at energy level 10/10.
 */
public final class BonusPoolRegistry {
    
    /**
     * All bonus abilities that can be claimed (50 total).
     */
    public static final List<Identifier> BONUS_ABILITIES = List.of(
            // Original 20
            PowerIds.BONUS_THUNDERSTRIKE,
            PowerIds.BONUS_FROSTBITE,
            PowerIds.BONUS_EARTHSHATTER,
            PowerIds.BONUS_SHADOWSTEP,
            PowerIds.BONUS_RADIANT_BURST,
            PowerIds.BONUS_VENOMSPRAY,
            PowerIds.BONUS_TIMEWARP,
            PowerIds.BONUS_DECOY_TRAP,
            PowerIds.BONUS_GRAVITY_WELL,
            PowerIds.BONUS_CHAIN_LIGHTNING,
            PowerIds.BONUS_MAGMA_POOL,
            PowerIds.BONUS_ICE_WALL,
            PowerIds.BONUS_WIND_SLASH,
            PowerIds.BONUS_CURSE_BOLT,
            PowerIds.BONUS_BERSERKER_RAGE,
            PowerIds.BONUS_ETHEREAL_STEP,
            PowerIds.BONUS_ARCANE_MISSILES,
            PowerIds.BONUS_LIFE_TAP,
            PowerIds.BONUS_DOOM_BOLT,
            PowerIds.BONUS_SANCTUARY,
            // New 30
            PowerIds.BONUS_SPECTRAL_CHAINS,
            PowerIds.BONUS_VOID_RIFT,
            PowerIds.BONUS_INFERNO_DASH,
            PowerIds.BONUS_TIDAL_WAVE,
            PowerIds.BONUS_STARFALL,
            PowerIds.BONUS_BLOODLUST,
            PowerIds.BONUS_CRYSTAL_CAGE,
            PowerIds.BONUS_PHANTASM,
            PowerIds.BONUS_SONIC_BOOM,
            PowerIds.BONUS_VAMPIRIC_TOUCH,
            PowerIds.BONUS_BLINDING_FLASH,
            PowerIds.BONUS_STORM_CALL,
            PowerIds.BONUS_QUICKSAND,
            PowerIds.BONUS_SEARING_LIGHT,
            PowerIds.BONUS_SPECTRAL_BLADE,      // Was: Shadow Clone (duplicate of Reaper's)
            PowerIds.BONUS_NETHER_PORTAL,
            PowerIds.BONUS_ENTANGLE,
            PowerIds.BONUS_MIND_SPIKE,
            PowerIds.BONUS_TREMOR,
            PowerIds.BONUS_ICICLE_BARRAGE,
            PowerIds.BONUS_BANISHMENT,
            PowerIds.BONUS_CORPSE_EXPLOSION,
            PowerIds.BONUS_SOUL_SWAP,
            PowerIds.BONUS_MARK_OF_DEATH,
            PowerIds.BONUS_IRON_MAIDEN,
            PowerIds.BONUS_WARP_STRIKE,
            PowerIds.BONUS_VORTEX_STRIKE,
            PowerIds.BONUS_PLAGUE_CLOUD,
            PowerIds.BONUS_OVERCHARGE,
            PowerIds.BONUS_GRAVITY_CRUSH
    );

    /**
     * All bonus passives that can be claimed (50 total).
     */
    public static final List<Identifier> BONUS_PASSIVES = List.of(
            // Original 20 (with replacements for duplicates)
            PowerIds.BONUS_THORNS_AURA,
            PowerIds.BONUS_LIFESTEAL,
            PowerIds.BONUS_DODGE_CHANCE,
            PowerIds.BONUS_CRITICAL_STRIKE,
            PowerIds.BONUS_MANA_SHIELD,
            PowerIds.BONUS_REGENERATION_BOOST,
            PowerIds.BONUS_DAMAGE_REDUCTION,
            PowerIds.BONUS_ATTACK_SPEED,
            PowerIds.BONUS_REACH_EXTEND,
            PowerIds.BONUS_IMPACT_ABSORB,       // Was: Night Vision (Supreme Helmet dupe)
            PowerIds.BONUS_ADRENALINE_SURGE,    // Was: Water Breathing (Supreme Helmet dupe)
            PowerIds.BONUS_INTIMIDATE,          // Was: Fire Walker (Fire Gem dupe)
            PowerIds.BONUS_EVASIVE_ROLL,        // Was: Feather Fall (Puff Gem dupe)
            PowerIds.BONUS_COMBAT_MEDITATE,     // Was: Swift Sneak (vanilla enchant)
            PowerIds.BONUS_WEAPON_MASTERY,      // Was: Soul Speed (vanilla enchant)
            PowerIds.BONUS_CULLING_BLADE,       // Was: Depth Strider (vanilla enchant)
            PowerIds.BONUS_THICK_SKIN,          // Was: Frost Walker (vanilla enchant)
            PowerIds.BONUS_XP_BOOST,
            PowerIds.BONUS_HUNGER_RESIST,
            PowerIds.BONUS_POISON_IMMUNITY,
            // New 30 (with replacements for duplicates)
            PowerIds.BONUS_SECOND_WIND,
            PowerIds.BONUS_ECHO_STRIKE,
            PowerIds.BONUS_CHAIN_BREAKER,       // Was: Momentum (Speed Gem dupe)
            PowerIds.BONUS_STONE_SKIN,
            PowerIds.BONUS_ARCANE_BARRIER,
            PowerIds.BONUS_PREDATOR_SENSE,
            PowerIds.BONUS_BATTLE_MEDIC,
            PowerIds.BONUS_LAST_STAND,
            PowerIds.BONUS_EXECUTIONER,
            PowerIds.BONUS_BLOODTHIRST,
            PowerIds.BONUS_STEEL_RESOLVE,
            PowerIds.BONUS_ELEMENTAL_HARMONY,
            PowerIds.BONUS_TREASURE_HUNTER,
            PowerIds.BONUS_COUNTER_STRIKE,      // Was: Shadowmeld (Spy Gem dupe)
            PowerIds.BONUS_BULWARK,
            PowerIds.BONUS_QUICK_RECOVERY,
            PowerIds.BONUS_OVERFLOWING_VITALITY,
            PowerIds.BONUS_MAGNETIC_PULL,
            PowerIds.BONUS_VENGEANCE,
            PowerIds.BONUS_NEMESIS,             // Was: Phoenix Blessing (similar to Second Wind)
            PowerIds.BONUS_HUNTERS_INSTINCT,    // Was: Spectral Sight (too similar to Predator Sense)
            PowerIds.BONUS_BERSERKER_BLOOD,
            PowerIds.BONUS_OPPORTUNIST,
            PowerIds.BONUS_IRONCLAD,
            PowerIds.BONUS_SPECTRAL_FORM,       // Was: Mist Form (identical to Dodge Chance)
            PowerIds.BONUS_WAR_CRY,
            PowerIds.BONUS_ADRENALINE_RUSH,     // Was: Siphon Soul (too similar to Bloodthirst)
            PowerIds.BONUS_UNBREAKABLE,
            PowerIds.BONUS_FOCUSED_MIND,
            PowerIds.BONUS_SIXTH_SENSE
    );

    /**
     * Blacklisted abilities/passives that Prism gem cannot pick.
     * These are considered too powerful to be mixed with other gems.
     */
    public static final List<Identifier> PRISM_BLACKLIST = List.of(
            PowerIds.VOID_IMMUNITY,           // Full immunity is too strong
            PowerIds.CHAOS_RANDOM_ROTATION,   // Special chaos mechanic
            PowerIds.NULLIFY                  // Strength's immunity ability
    );

    /**
     * Abilities that require gem-specific state and won't work standalone for Chaos gem.
     * These abilities depend on state accumulated by other passives.
     */
    public static final List<Identifier> CHAOS_ABILITY_BLACKLIST = List.of(
            // Flux abilities require FluxCharge state
            PowerIds.FLUX_BEAM,
            PowerIds.FLUX_SURGE,
            PowerIds.FLUX_DISCHARGE,
            // Static Burst requires accumulated damage from taking hits
            PowerIds.STATIC_BURST,
            // Summoner abilities require loadout configuration
            PowerIds.SUMMON_SLOT_1,
            PowerIds.SUMMON_SLOT_2,
            PowerIds.SUMMON_SLOT_3,
            PowerIds.SUMMON_SLOT_4,
            PowerIds.SUMMON_SLOT_5,
            // Spy abilities require observed/killed state
            PowerIds.SPY_MIMIC_FORM,  // Requires lastKilledType from SpySystem
            PowerIds.SPY_ECHO,
            PowerIds.SPY_STEAL,
            PowerIds.SPY_STOLEN_CAST,
            // Skinshift requires target player state
            PowerIds.SPY_SKINSHIFT,
            // Chaos/Prism/Void special mechanics
            PowerIds.NULLIFY,
            PowerIds.CHAOS_RANDOM_ROTATION
    );

    /**
     * Passives that require gem-specific state and won't work standalone for Chaos gem.
     */
    public static final List<Identifier> CHAOS_PASSIVE_BLACKLIST = List.of(
            // Flux passives require FluxCharge system
            PowerIds.FLUX_CHARGE_STORAGE,  // Core flux system
            PowerIds.FLUX_CAPACITOR,
            PowerIds.FLUX_OVERCHARGE_RAMP,
            PowerIds.FLUX_CONDUCTIVITY,
            PowerIds.FLUX_INSULATION,
            PowerIds.FLUX_ALLY_INVERSION,
            // Summoner passives require summon system
            PowerIds.SUMMONER_BOND,
            PowerIds.SUMMONER_COMMANDERS_MARK,
            PowerIds.SUMMONER_SOULBOUND,
            PowerIds.SUMMONER_FAMILIARS_BLESSING,
            // Spy passives that depend on spy-specific state tracking
            PowerIds.SPY_STILLNESS_CLOAK,  // Requires position tracking state
            PowerIds.SPY_FALSE_SIGNATURE,  // Requires spy system state
            // Astra passives - soul system state
            PowerIds.SOUL_CAPTURE,
            PowerIds.SOUL_HEALING,
            // Void immunity - too powerful
            PowerIds.VOID_IMMUNITY,
            // Chaos rotation - causes recursive issues
            PowerIds.CHAOS_RANDOM_ROTATION
    );

    private BonusPoolRegistry() {
    }

    public static boolean isBonusAbility(Identifier id) {
        return BONUS_ABILITIES.contains(id);
    }

    public static boolean isBonusPassive(Identifier id) {
        return BONUS_PASSIVES.contains(id);
    }

    public static boolean isBlacklisted(Identifier id) {
        return PRISM_BLACKLIST.contains(id);
    }

    public static boolean isChaosAbilityBlacklisted(Identifier id) {
        return CHAOS_ABILITY_BLACKLIST.contains(id);
    }

    public static boolean isChaosPassiveBlacklisted(Identifier id) {
        return CHAOS_PASSIVE_BLACKLIST.contains(id);
    }
}
