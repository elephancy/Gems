package com.feel.gems.mastery;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.stats.GemsStats;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks server-wide leaderboard stats for general titles.
 * General titles are awarded to the current leader in each category.
 */
public final class LeaderboardTracker {
    private static final String KEY_KILLS = "gemsKillCount";

    /**
     * Categories for general titles based on leaderboard stats.
     */
    public enum LeaderboardCategory {
        LEAST_DEATHS("gems.title.general.immortal", "Immortal"),
        MOST_KILLS("gems.title.general.king_slayer", "King Slayer"),
        MOST_HEARTS("gems.title.general.titan", "Titan"),
        MAX_ENERGY("gems.title.general.whale", "Whale"),
        MOST_SYNERGY_TRIGGERS("gems.title.general.synergy_master", "Synergy Master"),
        MOST_ABILITY_CASTS("gems.title.general.spell_slinger", "Spell Slinger"),
        MOST_DAMAGE_DEALT("gems.title.general.worldbreaker", "Worldbreaker"),
        MOST_KILLS_ASTRA("gems.title.general.kills_astra", "Astra Slayer", GemId.ASTRA),
        MOST_KILLS_FIRE("gems.title.general.kills_fire", "Fire Slayer", GemId.FIRE),
        MOST_KILLS_FLUX("gems.title.general.kills_flux", "Flux Slayer", GemId.FLUX),
        MOST_KILLS_LIFE("gems.title.general.kills_life", "Life Slayer", GemId.LIFE),
        MOST_KILLS_PUFF("gems.title.general.kills_puff", "Puff Slayer", GemId.PUFF),
        MOST_KILLS_SPEED("gems.title.general.kills_speed", "Speed Slayer", GemId.SPEED),
        MOST_KILLS_STRENGTH("gems.title.general.kills_strength", "Strength Slayer", GemId.STRENGTH),
        MOST_KILLS_WEALTH("gems.title.general.kills_wealth", "Wealth Slayer", GemId.WEALTH),
        MOST_KILLS_TERROR("gems.title.general.kills_terror", "Terror Slayer", GemId.TERROR),
        MOST_KILLS_SUMMONER("gems.title.general.kills_summoner", "Summoner Slayer", GemId.SUMMONER),
        MOST_KILLS_SPACE("gems.title.general.kills_space", "Space Slayer", GemId.SPACE),
        MOST_KILLS_REAPER("gems.title.general.kills_reaper", "Reaper Slayer", GemId.REAPER),
        MOST_KILLS_PILLAGER("gems.title.general.kills_pillager", "Pillager Slayer", GemId.PILLAGER),
        MOST_KILLS_SPY("gems.title.general.kills_spy", "Spy Slayer", GemId.SPY),
        MOST_KILLS_BEACON("gems.title.general.kills_beacon", "Beacon Slayer", GemId.BEACON),
        MOST_KILLS_AIR("gems.title.general.kills_air", "Air Slayer", GemId.AIR),
        MOST_KILLS_VOID("gems.title.general.kills_void", "Void Slayer", GemId.VOID),
        MOST_KILLS_CHAOS("gems.title.general.kills_chaos", "Chaos Slayer", GemId.CHAOS),
        MOST_KILLS_PRISM("gems.title.general.kills_prism", "Prism Slayer", GemId.PRISM),
        MOST_KILLS_DUELIST("gems.title.general.kills_duelist", "Duelist Slayer", GemId.DUELIST),
        MOST_KILLS_HUNTER("gems.title.general.kills_hunter", "Hunter Slayer", GemId.HUNTER),
        MOST_KILLS_SENTINEL("gems.title.general.kills_sentinel", "Sentinel Slayer", GemId.SENTINEL),
        MOST_KILLS_TRICKSTER("gems.title.general.kills_trickster", "Trickster Slayer", GemId.TRICKSTER);

        private final String translationKey;
        private final String fallbackName;
        private final GemId gem;

        LeaderboardCategory(String translationKey, String fallbackName) {
            this(translationKey, fallbackName, null);
        }

        LeaderboardCategory(String translationKey, String fallbackName, GemId gem) {
            this.translationKey = translationKey;
            this.fallbackName = fallbackName;
            this.gem = gem;
        }

        public String translationKey() {
            return translationKey;
        }

        public String fallbackName() {
            return fallbackName;
        }

        public GemId gem() {
            return gem;
        }
    }

    /**
     * Record of a player's leaderboard entry.
     */
    public record LeaderboardEntry(UUID playerId, String playerName, int value) {}

    /**
     * Current leaderboard leaders by category.
     */
    private static final Map<LeaderboardCategory, LeaderboardEntry> LEADERS = new EnumMap<>(LeaderboardCategory.class);

    private LeaderboardTracker() {
    }

    // ========== Kill Tracking ==========

    /**
     * Increment the player's kill count.
     * Called from GemsPlayerDeath when a player kills another player.
     */
    public static void incrementKills(ServerPlayer killer) {
        CompoundTag nbt = ((GemsPersistentDataHolder) killer).gems$getPersistentData();
        int kills = nbt.getIntOr(KEY_KILLS, 0);
        nbt.putInt(KEY_KILLS, kills + 1);
    }

    /**
     * Get the player's kill count.
     */
    public static int getKills(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData().getIntOr(KEY_KILLS, 0);
    }

    // ========== Leaderboard Updates ==========

    /**
     * Update the leaderboard for all categories.
     * Should be called periodically (e.g., every minute) or on significant events.
     */
    public static void updateLeaderboards(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        // Least Deaths - player with fewest deaths (requires at least 1 kill to qualify)
        updateLeastDeaths(players);

        // Most Kills
        updateMostKills(players);

        // Most Hearts
        updateMostHearts(players);

        // Max Energy
        updateMaxEnergy(players);

        // Most Synergy Triggers
        updateMostSynergyTriggers(players);

        // Most Ability Casts
        updateMostAbilityCasts(players);

        // Most Damage Dealt
        updateMostDamageDealt(players);

        // Per-gem kill leaders (player kills only)
        updateGemKillLeaders(players);

        TitleDisplay.refreshAll(server);
    }

    private static void updateLeastDeaths(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .filter(p -> getKills(p) >= 1) // Must have at least 1 kill to qualify
                .min(Comparator.comparingInt(p -> SpySystem.deaths(p)))
                .orElse(null);

        if (leader != null) {
            int deaths = SpySystem.deaths(leader);
            LEADERS.put(LeaderboardCategory.LEAST_DEATHS, 
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), deaths));
        }
    }

    private static void updateMostKills(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingInt(LeaderboardTracker::getKills))
                .filter(p -> getKills(p) >= 1) // Must have at least 1 kill
                .orElse(null);

        if (leader != null) {
            int kills = getKills(leader);
            LEADERS.put(LeaderboardCategory.MOST_KILLS,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), kills));
        }
    }

    private static void updateMostHearts(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingInt(GemPlayerState::getMaxHearts))
                .orElse(null);

        if (leader != null) {
            int hearts = GemPlayerState.getMaxHearts(leader);
            LEADERS.put(LeaderboardCategory.MOST_HEARTS,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), hearts));
        }
    }

    private static void updateMaxEnergy(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingInt(GemPlayerState::getEnergy))
                .filter(p -> GemPlayerState.getEnergy(p) >= GemPlayerState.MAX_ENERGY) // Must be at max energy
                .orElse(null);

        if (leader != null) {
            int energy = GemPlayerState.getEnergy(leader);
            LEADERS.put(LeaderboardCategory.MAX_ENERGY,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), energy));
        }
    }

    private static void updateMostSynergyTriggers(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingInt(GemsStats::synergyTriggers))
                .filter(p -> GemsStats.synergyTriggers(p) > 0)
                .orElse(null);
        if (leader != null) {
            LEADERS.put(LeaderboardCategory.MOST_SYNERGY_TRIGGERS,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), GemsStats.synergyTriggers(leader)));
        }
    }

    private static void updateMostAbilityCasts(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingInt(GemsStats::abilityUses))
                .filter(p -> GemsStats.abilityUses(p) > 0)
                .orElse(null);
        if (leader != null) {
            LEADERS.put(LeaderboardCategory.MOST_ABILITY_CASTS,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), GemsStats.abilityUses(leader)));
        }
    }

    private static void updateMostDamageDealt(List<ServerPlayer> players) {
        ServerPlayer leader = players.stream()
                .max(Comparator.comparingDouble(GemsStats::damageDealt))
                .filter(p -> GemsStats.damageDealt(p) > 0.0D)
                .orElse(null);
        if (leader != null) {
            LEADERS.put(LeaderboardCategory.MOST_DAMAGE_DEALT,
                    new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), (int) Math.round(GemsStats.damageDealt(leader))));
        }
    }

    private static void updateGemKillLeaders(List<ServerPlayer> players) {
        for (GemId gem : GemId.values()) {
            LeaderboardCategory category = killCategoryForGem(gem);
            if (category == null) {
                continue;
            }
            ServerPlayer leader = players.stream()
                    .max(Comparator.comparingInt(p -> GemsStats.playerKillsWithGem(p, gem)))
                    .filter(p -> GemsStats.playerKillsWithGem(p, gem) > 0)
                    .orElse(null);
            if (leader != null) {
                int kills = GemsStats.playerKillsWithGem(leader, gem);
                LEADERS.put(category, new LeaderboardEntry(leader.getUUID(), leader.getName().getString(), kills));
            }
        }
    }

    private static LeaderboardCategory killCategoryForGem(GemId gem) {
        return switch (gem) {
            case ASTRA -> LeaderboardCategory.MOST_KILLS_ASTRA;
            case FIRE -> LeaderboardCategory.MOST_KILLS_FIRE;
            case FLUX -> LeaderboardCategory.MOST_KILLS_FLUX;
            case LIFE -> LeaderboardCategory.MOST_KILLS_LIFE;
            case PUFF -> LeaderboardCategory.MOST_KILLS_PUFF;
            case SPEED -> LeaderboardCategory.MOST_KILLS_SPEED;
            case STRENGTH -> LeaderboardCategory.MOST_KILLS_STRENGTH;
            case WEALTH -> LeaderboardCategory.MOST_KILLS_WEALTH;
            case TERROR -> LeaderboardCategory.MOST_KILLS_TERROR;
            case SUMMONER -> LeaderboardCategory.MOST_KILLS_SUMMONER;
            case SPACE -> LeaderboardCategory.MOST_KILLS_SPACE;
            case REAPER -> LeaderboardCategory.MOST_KILLS_REAPER;
            case PILLAGER -> LeaderboardCategory.MOST_KILLS_PILLAGER;
            case SPY -> LeaderboardCategory.MOST_KILLS_SPY;
            case BEACON -> LeaderboardCategory.MOST_KILLS_BEACON;
            case AIR -> LeaderboardCategory.MOST_KILLS_AIR;
            case VOID -> LeaderboardCategory.MOST_KILLS_VOID;
            case CHAOS -> LeaderboardCategory.MOST_KILLS_CHAOS;
            case PRISM -> LeaderboardCategory.MOST_KILLS_PRISM;
            case DUELIST -> LeaderboardCategory.MOST_KILLS_DUELIST;
            case HUNTER -> LeaderboardCategory.MOST_KILLS_HUNTER;
            case SENTINEL -> LeaderboardCategory.MOST_KILLS_SENTINEL;
            case TRICKSTER -> LeaderboardCategory.MOST_KILLS_TRICKSTER;
        };
    }

    // ========== Title Queries ==========

    /**
     * Check if a player holds a general title.
     */
    public static boolean holdsTitle(ServerPlayer player, LeaderboardCategory category) {
        LeaderboardEntry leader = LEADERS.get(category);
        return leader != null && leader.playerId().equals(player.getUUID());
    }

    /**
     * Get all general titles held by a player.
     */
    public static List<LeaderboardCategory> getTitles(ServerPlayer player) {
        UUID playerId = player.getUUID();
        return LEADERS.entrySet().stream()
                .filter(e -> e.getValue().playerId().equals(playerId))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Get the current leader for a category.
     */
    public static LeaderboardEntry getLeader(LeaderboardCategory category) {
        return LEADERS.get(category);
    }

    /**
     * Get all current leaders.
     */
    public static Map<LeaderboardCategory, LeaderboardEntry> getAllLeaders() {
        return Map.copyOf(LEADERS);
    }

    /**
     * Get the translatable title text for a category.
     */
    public static Component getTitleText(LeaderboardCategory category) {
        return Component.translatable(category.translationKey());
    }
}
