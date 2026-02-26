package com.feel.gems.stats;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class GemsStats {
    private static final String KEY_STATS = "gemsStats";
    private static final String KEY_LAST_ABILITY = "lastAbilityId";

    private static final String KEY_KILLS = "kills";
    private static final String KEY_DEATHS = "deaths";
    private static final String KEY_PLAYER_KILLS = "playerKills";
    private static final String KEY_PLAYER_DEATHS = "playerDeaths";
    private static final String KEY_MOB_KILLS = "mobKills";
    private static final String KEY_ASSASSIN_KILLS = "assassinKills";
    private static final String KEY_ASSASSIN_DEATHS = "assassinDeaths";
    private static final String KEY_FINAL_KILLS = "finalKills";
    private static final String KEY_FINAL_DEATHS = "finalDeaths";

    private static final String KEY_KILLS_BY_GEM = "killsByGem";
    private static final String KEY_PLAYER_KILLS_BY_GEM = "playerKillsByGem";
    private static final String KEY_DEATHS_BY_GEM = "deathsByGem";
    private static final String KEY_KILLS_BY_ABILITY = "killsByAbility";
    private static final String KEY_DEATHS_BY_ABILITY = "deathsByAbility";
    private static final String KEY_KILLS_BY_VICTIM_GEM = "killsByVictimGem";
    private static final String KEY_DEATHS_BY_KILLER_GEM = "deathsByKillerGem";
    private static final String KEY_ABILITY_USES = "abilityUses";
    private static final String KEY_SYNERGY_TRIGGERS = "synergyTriggers";
    private static final String KEY_SYNERGY_TRIGGERS_BY_ID = "synergyTriggersById";
    private static final String KEY_DAMAGE_DEALT = "damageDealt";

    private GemsStats() {
    }

    public static void recordAbilityUse(ServerPlayer player, Identifier abilityId) {
        CompoundTag stats = statsRoot(player);
        if (abilityId != null) {
            stats.putString(KEY_LAST_ABILITY, abilityId.toString());
            incrementMap(stats, KEY_ABILITY_USES, abilityId.toString());
        }
    }

    public static void recordSynergyTrigger(ServerPlayer player, Identifier synergyId) {
        CompoundTag stats = statsRoot(player);
        increment(stats, KEY_SYNERGY_TRIGGERS);
        if (synergyId != null) {
            incrementMap(stats, KEY_SYNERGY_TRIGGERS_BY_ID, synergyId.toString());
        }
    }

    public static void recordDamageDealt(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        CompoundTag stats = statsRoot(player);
        double current = stats.getDoubleOr(KEY_DAMAGE_DEALT, 0.0D);
        stats.putDouble(KEY_DAMAGE_DEALT, current + amount);
    }

    public static Identifier lastAbility(ServerPlayer player) {
        CompoundTag stats = statsRoot(player);
        String raw = stats.getStringOr(KEY_LAST_ABILITY, "");
        if (raw.isEmpty()) {
            return null;
        }
        return Identifier.tryParse(raw);
    }

    public static void recordMobKill(ServerPlayer killer) {
        recordMobKill(killer, 1);
    }

    public static void recordMobKill(ServerPlayer killer, int count) {
        if (count <= 0) {
            return;
        }
        CompoundTag stats = statsRoot(killer);
        increment(stats, KEY_KILLS, count);
        increment(stats, KEY_MOB_KILLS, count);
        incrementMap(stats, KEY_KILLS_BY_GEM, GemPlayerState.getActiveGem(killer).name(), count);
        Identifier last = lastAbility(killer);
        if (last != null) {
            incrementMap(stats, KEY_KILLS_BY_ABILITY, last.toString(), count);
        }
    }

    public static void recordPlayerKill(ServerPlayer killer, ServerPlayer victim, boolean killerAssassin, boolean victimAssassin, boolean finalKill) {
        CompoundTag stats = statsRoot(killer);
        increment(stats, KEY_KILLS);
        increment(stats, KEY_PLAYER_KILLS);
        if (killerAssassin) {
            increment(stats, KEY_ASSASSIN_KILLS);
        }
        if (finalKill) {
            increment(stats, KEY_FINAL_KILLS);
        }

        incrementMap(stats, KEY_KILLS_BY_GEM, GemPlayerState.getActiveGem(killer).name());
        incrementMap(stats, KEY_PLAYER_KILLS_BY_GEM, GemPlayerState.getActiveGem(killer).name());
        incrementMap(stats, KEY_KILLS_BY_VICTIM_GEM, GemPlayerState.getActiveGem(victim).name());

        Identifier last = lastAbility(killer);
        if (last != null) {
            incrementMap(stats, KEY_KILLS_BY_ABILITY, last.toString());
        }
    }

    public static void recordPlayerDeath(ServerPlayer victim, ServerPlayer killer, boolean victimAssassin, boolean finalDeath) {
        CompoundTag stats = statsRoot(victim);
        increment(stats, KEY_DEATHS);
        increment(stats, KEY_PLAYER_DEATHS);
        if (victimAssassin) {
            increment(stats, KEY_ASSASSIN_DEATHS);
        }
        if (finalDeath) {
            increment(stats, KEY_FINAL_DEATHS);
        }

        incrementMap(stats, KEY_DEATHS_BY_GEM, GemPlayerState.getActiveGem(victim).name());
        if (killer != null) {
            incrementMap(stats, KEY_DEATHS_BY_KILLER_GEM, GemPlayerState.getActiveGem(killer).name());
            Identifier last = lastAbility(killer);
            if (last != null) {
                incrementMap(stats, KEY_DEATHS_BY_ABILITY, last.toString());
            }
        }
    }

    public static int deaths(ServerPlayer player) {
        CompoundTag stats = statsRoot(player);
        return stats.getIntOr(KEY_DEATHS, 0);
    }

    public static int abilityUses(ServerPlayer player) {
        return sumMap(statsRoot(player), KEY_ABILITY_USES);
    }

    public static int synergyTriggers(ServerPlayer player) {
        return statsRoot(player).getIntOr(KEY_SYNERGY_TRIGGERS, 0);
    }

    public static double damageDealt(ServerPlayer player) {
        return statsRoot(player).getDoubleOr(KEY_DAMAGE_DEALT, 0.0D);
    }

    public static int playerKillsWithGem(ServerPlayer player, com.feel.gems.core.GemId gem) {
        if (player == null || gem == null) {
            return 0;
        }
        return mapValue(statsRoot(player), KEY_PLAYER_KILLS_BY_GEM, gem.name());
    }

    public static void reset(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_STATS);
    }

    public static boolean setStat(ServerPlayer player, String statId, int value) {
        if (player == null || statId == null) {
            return false;
        }
        String key = statId.trim().toLowerCase(java.util.Locale.ROOT);
        String target = switch (key) {
            case "kills" -> KEY_KILLS;
            case "deaths" -> KEY_DEATHS;
            case "playerkills", "player_kills", "player-kills" -> KEY_PLAYER_KILLS;
            case "playerdeaths", "player_deaths", "player-deaths" -> KEY_PLAYER_DEATHS;
            case "mobkills", "mob_kills", "mob-kills" -> KEY_MOB_KILLS;
            case "assassinkills", "assassin_kills", "assassin-kills" -> KEY_ASSASSIN_KILLS;
            case "assassindeaths", "assassin_deaths", "assassin-deaths" -> KEY_ASSASSIN_DEATHS;
            case "finalkills", "final_kills", "final-kills" -> KEY_FINAL_KILLS;
            case "finaldeaths", "final_deaths", "final-deaths" -> KEY_FINAL_DEATHS;
            default -> null;
        };
        if (target == null) {
            return false;
        }
        CompoundTag stats = statsRoot(player);
        stats.putInt(target, Math.max(0, value));
        return true;
    }

    public static List<Component> buildSummary(ServerPlayer player) {
        CompoundTag stats = statsRoot(player);
        int kills = stats.getIntOr(KEY_KILLS, 0);
        int deaths = stats.getIntOr(KEY_DEATHS, 0);
        int playerKills = stats.getIntOr(KEY_PLAYER_KILLS, 0);
        int playerDeaths = stats.getIntOr(KEY_PLAYER_DEATHS, 0);
        int mobKills = stats.getIntOr(KEY_MOB_KILLS, 0);
        int assassinKills = stats.getIntOr(KEY_ASSASSIN_KILLS, 0);
        int assassinDeaths = stats.getIntOr(KEY_ASSASSIN_DEATHS, 0);
        int finalKills = stats.getIntOr(KEY_FINAL_KILLS, 0);
        int finalDeaths = stats.getIntOr(KEY_FINAL_DEATHS, 0);
        int totalAbilityUses = sumMap(stats, KEY_ABILITY_USES);
        int synergyTriggers = stats.getIntOr(KEY_SYNERGY_TRIGGERS, 0);
        double damageDealt = stats.getDoubleOr(KEY_DAMAGE_DEALT, 0.0D);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Stats for " + player.getName().getString() + ":"));
        lines.add(Component.literal("Kills: " + kills + " (Players: " + playerKills + ", Mobs: " + mobKills + ")"));
        lines.add(Component.literal("Deaths: " + deaths + " (Player deaths: " + playerDeaths + ")"));
        lines.add(Component.literal("Assassin K/D: " + assassinKills + "/" + assassinDeaths + " | Final K/D: " + finalKills + "/" + finalDeaths));
        lines.add(Component.literal("Ability uses: " + totalAbilityUses));
        lines.add(Component.literal("Synergy triggers: " + synergyTriggers));
        lines.add(Component.literal(String.format(java.util.Locale.ROOT, "Damage dealt: %.1f", damageDealt)));

        String topAbility = topKey(stats, KEY_ABILITY_USES);
        if (topAbility != null) {
            lines.add(Component.literal("Top ability: " + topAbility));
        }
        String topGemKills = topKey(stats, KEY_KILLS_BY_GEM);
        if (topGemKills != null) {
            lines.add(Component.literal("Top kill gem: " + topGemKills));
        }
        String topVictimGem = topKey(stats, KEY_KILLS_BY_VICTIM_GEM);
        if (topVictimGem != null) {
            lines.add(Component.literal("Top victim gem: " + topVictimGem));
        }
        String topDeathAbility = topKey(stats, KEY_DEATHS_BY_ABILITY);
        if (topDeathAbility != null) {
            lines.add(Component.literal("Top death ability: " + topDeathAbility));
        }
        String topDeathGem = topKey(stats, KEY_DEATHS_BY_GEM);
        if (topDeathGem != null) {
            lines.add(Component.literal("Top death gem: " + topDeathGem));
        }
        String topKillerGem = topKey(stats, KEY_DEATHS_BY_KILLER_GEM);
        if (topKillerGem != null) {
            lines.add(Component.literal("Top killer gem: " + topKillerGem));
        }
        return lines;
    }

    private static CompoundTag statsRoot(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag stats = root.getCompound(KEY_STATS).orElse(null);
        if (stats == null) {
            stats = new CompoundTag();
            root.put(KEY_STATS, stats);
        }
        return stats;
    }

    private static void increment(CompoundTag stats, String key) {
        int current = stats.getIntOr(key, 0);
        stats.putInt(key, current + 1);
    }

    private static void increment(CompoundTag stats, String key, int delta) {
        if (delta <= 0) {
            return;
        }
        int current = stats.getIntOr(key, 0);
        stats.putInt(key, current + delta);
    }

    private static void incrementMap(CompoundTag stats, String mapKey, String key) {
        CompoundTag map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            map = new CompoundTag();
            stats.put(mapKey, map);
        }
        int current = map.getIntOr(key, 0);
        map.putInt(key, current + 1);
    }

    private static void incrementMap(CompoundTag stats, String mapKey, String key, int delta) {
        if (delta <= 0) {
            return;
        }
        CompoundTag map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            map = new CompoundTag();
            stats.put(mapKey, map);
        }
        int current = map.getIntOr(key, 0);
        map.putInt(key, current + delta);
    }

    private static int mapValue(CompoundTag stats, String mapKey, String key) {
        CompoundTag map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            return 0;
        }
        return map.getIntOr(key, 0);
    }

    private static String topKey(CompoundTag stats, String mapKey) {
        CompoundTag map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            return null;
        }
        Map<String, Integer> values = new TreeMap<>();
        for (String key : map.keySet()) {
            values.put(key, map.getIntOr(key, 0));
        }
        return values.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static int sumMap(CompoundTag stats, String mapKey) {
        CompoundTag map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            return 0;
        }
        int total = 0;
        for (String key : map.keySet()) {
            total += map.getIntOr(key, 0);
        }
        return total;
    }
}
