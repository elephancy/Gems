package com.feel.gems.power.gem.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Trophy Hunter passive - on player kill, gain one of their random passives temporarily.
 * The gained passive persists through death.
 */
public final class HunterTrophyHunterRuntime {
    private static final String TROPHY_PASSIVE_KEY = "hunter_trophy_passive";
    private static final String TROPHY_END_KEY = "hunter_trophy_end";
    private static final Random RANDOM = new Random();

    private HunterTrophyHunterRuntime() {}

    public static void onPlayerKill(ServerPlayer hunter, ServerPlayer victim) {
        // Get victim's active passives
        List<Identifier> victimPassives = GemPowers.getActivePassives(victim);
        if (victimPassives.isEmpty()) return;

        // Pick a random passive
        Identifier stolenPassive = victimPassives.get(RANDOM.nextInt(victimPassives.size()));

        int durationTicks = GemsBalance.v().hunter().trophyHunterDurationTicks();
        long endTime = hunter.level().getGameTime() + durationTicks;

        PlayerStateManager.setPersistent(hunter, TROPHY_PASSIVE_KEY, stolenPassive.toString());
        PlayerStateManager.setPersistent(hunter, TROPHY_END_KEY, String.valueOf(endTime));
    }

    public static Identifier getTrophyPassive(ServerPlayer hunter) {
        String passiveStr = PlayerStateManager.getPersistent(hunter, TROPHY_PASSIVE_KEY);
        if (passiveStr == null || passiveStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(hunter, TROPHY_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (hunter.level().getGameTime() > endTime) {
            clearTrophy(hunter);
            return null;
        }

        return Identifier.tryParse(passiveStr);
    }

    public static boolean hasTrophyPassive(ServerPlayer hunter, Identifier passiveId) {
        Identifier trophy = getTrophyPassive(hunter);
        return trophy != null && trophy.equals(passiveId);
    }

    public static void clearTrophy(ServerPlayer hunter) {
        PlayerStateManager.clearPersistent(hunter, TROPHY_PASSIVE_KEY);
        PlayerStateManager.clearPersistent(hunter, TROPHY_END_KEY);
    }
}
