package com.feel.gems.assassin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;




/**
 * Persistent per-player state for the "Assassin endgame" loop.
 *
 * <p>Assassins keep gems/energy as normal; this only governs max-hearts and scoring.</p>
 */
public final class AssassinState {
    private static final int CHOICE_POINTS = 10;
    private static final String KEY_IS_ASSASSIN = "assassinIsAssassin";
    private static final String KEY_ELIMINATED = "assassinEliminated";
    private static final String KEY_ASSASSIN_HEARTS = "assassinHearts";
    private static final String KEY_CHOICE_UNLOCKED = "assassinChoiceUnlocked";

    private static final String KEY_TOTAL_NORMAL_KILLS = "assassinTotalNormalKills";
    private static final String KEY_TOTAL_FINAL_KILLS = "assassinTotalFinalKills";

    private static final String KEY_A_NORMAL_KILLS = "assassinNormalKills";
    private static final String KEY_A_FINAL_KILLS = "assassinFinalKills";

    private static final String KEY_A_NORMAL_KILLS_VS_NON = "assassinNormalKillsVsNon";
    private static final String KEY_A_FINAL_KILLS_VS_NON = "assassinFinalKillsVsNon";

    private AssassinState() {
    }

    public static void initIfNeeded(ServerPlayer player) {
        CompoundTag nbt = root(player);
        if (nbt.getBoolean(KEY_IS_ASSASSIN).isEmpty()) {
            nbt.putBoolean(KEY_IS_ASSASSIN, false);
        }
        if (nbt.getBoolean(KEY_ELIMINATED).isEmpty()) {
            nbt.putBoolean(KEY_ELIMINATED, false);
        }
        if (nbt.getInt(KEY_ASSASSIN_HEARTS).isEmpty()) {
            nbt.putInt(KEY_ASSASSIN_HEARTS, maxHearts());
        }
        if (nbt.getInt(KEY_TOTAL_NORMAL_KILLS).isEmpty()) {
            nbt.putInt(KEY_TOTAL_NORMAL_KILLS, 0);
        }
        if (nbt.getInt(KEY_TOTAL_FINAL_KILLS).isEmpty()) {
            nbt.putInt(KEY_TOTAL_FINAL_KILLS, 0);
        }
        if (nbt.getInt(KEY_A_NORMAL_KILLS).isEmpty()) {
            nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        }
        if (nbt.getInt(KEY_A_FINAL_KILLS).isEmpty()) {
            nbt.putInt(KEY_A_FINAL_KILLS, 0);
        }
        if (nbt.getInt(KEY_A_NORMAL_KILLS_VS_NON).isEmpty()) {
            nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        }
        if (nbt.getInt(KEY_A_FINAL_KILLS_VS_NON).isEmpty()) {
            nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);
        }
        if (nbt.getBoolean(KEY_CHOICE_UNLOCKED).isEmpty()) {
            nbt.putBoolean(KEY_CHOICE_UNLOCKED, false);
        }
    }

    public static boolean isAssassin(ServerPlayer player) {
        return root(player).getBooleanOr(KEY_IS_ASSASSIN, false);
    }

    public static boolean isEliminated(ServerPlayer player) {
        return root(player).getBooleanOr(KEY_ELIMINATED, false);
    }

    public static int getAssassinHearts(ServerPlayer player) {
        if (!isAssassin(player)) {
            return maxHearts();
        }
        int raw = root(player).getIntOr(KEY_ASSASSIN_HEARTS, maxHearts());
        return clamp(raw, 0, maxHearts());
    }

    public static int getAssassinHeartsForAttribute(ServerPlayer player) {
        // Avoid 0 max-health edge-cases; eliminated players are handled by game mode, but we keep attribute valid.
        return Math.max(1, getAssassinHearts(player));
    }

    public static void becomeAssassin(ServerPlayer player) {
        initIfNeeded(player);
        CompoundTag nbt = root(player);
        if (nbt.getBooleanOr(KEY_IS_ASSASSIN, false)) {
            return;
        }
        nbt.putBoolean(KEY_IS_ASSASSIN, true);
        nbt.putBoolean(KEY_ELIMINATED, false);
        nbt.putInt(KEY_ASSASSIN_HEARTS, maxHearts());

        // Reset the "after turning" counters.
        nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        nbt.putInt(KEY_A_FINAL_KILLS, 0);
        nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);

        BountyBoard.voidBountiesForAssassin(player);
    }

    public static void reset(ServerPlayer player) {
        initIfNeeded(player);
        CompoundTag nbt = root(player);
        nbt.putBoolean(KEY_IS_ASSASSIN, false);
        nbt.putBoolean(KEY_ELIMINATED, false);
        nbt.putInt(KEY_ASSASSIN_HEARTS, maxHearts());
        nbt.putBoolean(KEY_CHOICE_UNLOCKED, false);
        nbt.putInt(KEY_TOTAL_NORMAL_KILLS, 0);
        nbt.putInt(KEY_TOTAL_FINAL_KILLS, 0);
        nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        nbt.putInt(KEY_A_FINAL_KILLS, 0);
        nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);
    }

    public static void setEliminated(ServerPlayer player, boolean eliminated) {
        initIfNeeded(player);
        root(player).putBoolean(KEY_ELIMINATED, eliminated);
    }

    public static void setAssassin(ServerPlayer player, boolean assassin) {
        initIfNeeded(player);
        CompoundTag nbt = root(player);
        boolean wasAssassin = nbt.getBooleanOr(KEY_IS_ASSASSIN, false);
        nbt.putBoolean(KEY_IS_ASSASSIN, assassin);
        if (!assassin) {
            nbt.putBoolean(KEY_ELIMINATED, false);
            nbt.putInt(KEY_ASSASSIN_HEARTS, maxHearts());
        } else if (nbt.getIntOr(KEY_ASSASSIN_HEARTS, 0) <= 0) {
            nbt.putInt(KEY_ASSASSIN_HEARTS, maxHearts());
        }
        if (assassin && !wasAssassin) {
            BountyBoard.voidBountiesForAssassin(player);
        }
    }

    public static void resetAssassinPoints(ServerPlayer player) {
        initIfNeeded(player);
        CompoundTag nbt = root(player);
        nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        nbt.putInt(KEY_A_FINAL_KILLS, 0);
        nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);
    }

    public static int setAssassinHearts(ServerPlayer player, int hearts) {
        initIfNeeded(player);
        int clamped = clamp(hearts, 0, maxHearts());
        root(player).putInt(KEY_ASSASSIN_HEARTS, clamped);
        com.feel.gems.state.GemPlayerState.applyMaxHearts(player);
        return clamped;
    }

    public static boolean setCounter(ServerPlayer player, String counterId, int value) {
        if (player == null || counterId == null) {
            return false;
        }
        initIfNeeded(player);
        String key = counterId.trim().toLowerCase(java.util.Locale.ROOT);
        String target = switch (key) {
            case "totalnormalkills", "total_normal_kills", "total-normal-kills" -> KEY_TOTAL_NORMAL_KILLS;
            case "totalfinalkills", "total_final_kills", "total-final-kills" -> KEY_TOTAL_FINAL_KILLS;
            case "assassinnormalkills", "assassin_normal_kills", "assassin-normal-kills" -> KEY_A_NORMAL_KILLS;
            case "assassinfinalkills", "assassin_final_kills", "assassin-final-kills" -> KEY_A_FINAL_KILLS;
            case "assassinnormalkillsvsnon", "assassin_normal_kills_vs_non", "assassin-normal-kills-vs-non" -> KEY_A_NORMAL_KILLS_VS_NON;
            case "assassinfinalkillsvsnon", "assassin_final_kills_vs_non", "assassin-final-kills-vs-non" -> KEY_A_FINAL_KILLS_VS_NON;
            default -> null;
        };
        if (target == null) {
            return false;
        }
        root(player).putInt(target, Math.max(0, value));
        maybeUnlockChoice(player);
        return true;
    }

    public static int addAssassinHearts(ServerPlayer player, int delta) {
        initIfNeeded(player);
        int next = clamp(getAssassinHearts(player) + delta, 0, maxHearts());
        root(player).putInt(KEY_ASSASSIN_HEARTS, next);
        com.feel.gems.state.GemPlayerState.applyMaxHearts(player);
        if (delta > 0) {
            float maxHealth = player.getMaxHealth();
            float healed = Math.min(maxHealth, player.getHealth() + (delta * 2.0F));
            player.setHealth(healed);
        }
        return next;
    }

    public static void recordKill(ServerPlayer killer, boolean finalKill, boolean victimWasAssassin) {
        initIfNeeded(killer);
        CompoundTag nbt = root(killer);

        if (finalKill) {
            nbt.putInt(KEY_TOTAL_FINAL_KILLS, nbt.getIntOr(KEY_TOTAL_FINAL_KILLS, 0) + 1);
        } else {
            nbt.putInt(KEY_TOTAL_NORMAL_KILLS, nbt.getIntOr(KEY_TOTAL_NORMAL_KILLS, 0) + 1);
        }

        if (isAssassin(killer)) {
            if (finalKill) {
                nbt.putInt(KEY_A_FINAL_KILLS, nbt.getIntOr(KEY_A_FINAL_KILLS, 0) + 1);
                if (!victimWasAssassin) {
                    nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, nbt.getIntOr(KEY_A_FINAL_KILLS_VS_NON, 0) + 1);
                }
            } else {
                nbt.putInt(KEY_A_NORMAL_KILLS, nbt.getIntOr(KEY_A_NORMAL_KILLS, 0) + 1);
                if (!victimWasAssassin) {
                    nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, nbt.getIntOr(KEY_A_NORMAL_KILLS_VS_NON, 0) + 1);
                }
            }
            if (maybeUnlockChoice(killer)) {
                sendChoicePrompt(killer);
            }
        }
    }

    public static int choicePointsRequired() {
        return CHOICE_POINTS;
    }

    public static boolean isChoiceUnlocked(ServerPlayer player) {
        return root(player).getBooleanOr(KEY_CHOICE_UNLOCKED, false);
    }

    public static boolean maybeUnlockChoice(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        initIfNeeded(player);
        CompoundTag nbt = root(player);
        if (nbt.getBooleanOr(KEY_CHOICE_UNLOCKED, false)) {
            return false;
        }
        if (assassinPoints(player) < CHOICE_POINTS) {
            return false;
        }
        nbt.putBoolean(KEY_CHOICE_UNLOCKED, true);
        return true;
    }

    public static void sendChoicePrompt(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.assassin.choice_unlocked").withStyle(net.minecraft.ChatFormatting.GOLD), false);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.assassin.choice_leave").withStyle(net.minecraft.ChatFormatting.GRAY), false);
    }

    /**
     * Transfers all "as assassin" points (and the "vs non-assassins" subset) from {@code from} to {@code to}.
     *
     * <p>This does not modify the global kill totals; it only moves the Assassin endgame scoring counters.</p>
     *
     * @return the number of assassin points transferred (as computed from the moved counters)
     */
    public static int transferAssassinPoints(ServerPlayer from, ServerPlayer to) {
        initIfNeeded(from);
        initIfNeeded(to);
        if (!isAssassin(from) || !isAssassin(to)) {
            return 0;
        }

        CompoundTag fromNbt = root(from);
        CompoundTag toNbt = root(to);

        int fromNormal = Math.max(0, fromNbt.getIntOr(KEY_A_NORMAL_KILLS, 0));
        int fromFinal = Math.max(0, fromNbt.getIntOr(KEY_A_FINAL_KILLS, 0));
        int fromNormalVsNon = Math.max(0, fromNbt.getIntOr(KEY_A_NORMAL_KILLS_VS_NON, 0));
        int fromFinalVsNon = Math.max(0, fromNbt.getIntOr(KEY_A_FINAL_KILLS_VS_NON, 0));

        if (fromNormal == 0 && fromFinal == 0 && fromNormalVsNon == 0 && fromFinalVsNon == 0) {
            return 0;
        }

        addNonNegative(toNbt, KEY_A_NORMAL_KILLS, fromNormal);
        addNonNegative(toNbt, KEY_A_FINAL_KILLS, fromFinal);
        addNonNegative(toNbt, KEY_A_NORMAL_KILLS_VS_NON, fromNormalVsNon);
        addNonNegative(toNbt, KEY_A_FINAL_KILLS_VS_NON, fromFinalVsNon);

        fromNbt.putInt(KEY_A_NORMAL_KILLS, 0);
        fromNbt.putInt(KEY_A_FINAL_KILLS, 0);
        fromNbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        fromNbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);

        return points(fromNormal, fromFinal);
    }

    public static int totalNormalKills(ServerPlayer player) {
        return root(player).getIntOr(KEY_TOTAL_NORMAL_KILLS, 0);
    }

    public static int totalFinalKills(ServerPlayer player) {
        return root(player).getIntOr(KEY_TOTAL_FINAL_KILLS, 0);
    }

    public static int totalPoints(ServerPlayer player) {
        return points(totalNormalKills(player), totalFinalKills(player));
    }

    public static int assassinNormalKills(ServerPlayer player) {
        return root(player).getIntOr(KEY_A_NORMAL_KILLS, 0);
    }

    public static int assassinFinalKills(ServerPlayer player) {
        return root(player).getIntOr(KEY_A_FINAL_KILLS, 0);
    }

    public static int assassinPoints(ServerPlayer player) {
        return points(assassinNormalKills(player), assassinFinalKills(player));
    }

    public static int assassinNormalKillsVsNonAssassins(ServerPlayer player) {
        return root(player).getIntOr(KEY_A_NORMAL_KILLS_VS_NON, 0);
    }

    public static int assassinFinalKillsVsNonAssassins(ServerPlayer player) {
        return root(player).getIntOr(KEY_A_FINAL_KILLS_VS_NON, 0);
    }

    public static int assassinPointsVsNonAssassins(ServerPlayer player) {
        return points(assassinNormalKillsVsNonAssassins(player), assassinFinalKillsVsNonAssassins(player));
    }

    public static int points(int normalKills, int finalKills) {
        return Math.max(0, normalKills) + Math.max(0, finalKills) * 3;
    }

    public static int maxHearts() {
        return GemsBalance.v().systems().assassinMaxHearts();
    }

    public static int eliminationThreshold() {
        return GemsBalance.v().systems().assassinEliminationHeartsThreshold();
    }

    public static boolean isEliminatedByHearts(int hearts) {
        return hearts <= eliminationThreshold();
    }

    private static CompoundTag root(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static void addNonNegative(CompoundTag nbt, String key, int delta) {
        nbt.putInt(key, Math.max(0, nbt.getIntOr(key, 0) + Math.max(0, delta)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
