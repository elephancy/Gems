package com.feel.gems.mastery;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages gem mastery data for players.
 * Tracks ability usage per gem and manages cosmetic selections.
 */
public final class GemMastery {
    private static final String KEY_MASTERY = "gemMastery";
    private static final String KEY_USAGE = "usage";
    private static final String KEY_SELECTED_TITLE = "selectedTitle";
    private static final String KEY_SELECTED_AURA = "selectedAura";
    private static final String KEY_SELECTED_TITLE_OVERRIDE = "selectedTitleOverride";

    private GemMastery() {
    }

    // ========== Ability Usage Tracking ==========

    /**
     * Increment ability usage count for a gem.
     * Called whenever a player successfully uses a gem ability.
     */
    public static void incrementUsage(ServerPlayer player, GemId gem) {
        CompoundTag gemData = getGemMasteryData(player, gem);
        int current = gemData.getInt(KEY_USAGE).orElse(0);
        int updated = current + 1;
        gemData.putInt(KEY_USAGE, updated);
        if (updated == 100 || updated == 500) {
            TitleDisplay.refresh(player);
            return;
        }
        for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
            if (reward.type() == MasteryReward.MasteryRewardType.TITLE && reward.threshold() == updated) {
                TitleDisplay.refresh(player);
                break;
            }
        }
    }

    /**
     * Get the ability usage count for a gem.
     */
    public static int getUsage(ServerPlayer player, GemId gem) {
        CompoundTag gemData = getGemMasteryData(player, gem);
        return gemData.getInt(KEY_USAGE).orElse(0);
    }

    /**
     * Get all gem usage counts for a player.
     */
    public static Map<GemId, Integer> getAllUsage(ServerPlayer player) {
        Map<GemId, Integer> result = new EnumMap<>(GemId.class);
        for (GemId gem : GemId.values()) {
            result.put(gem, getUsage(player, gem));
        }
        return result;
    }

    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_MASTERY);
    }

    // ========== Title Selection ==========

    /**
     * Set the player's selected title (from any gem they've unlocked).
     * Pass null or empty string to clear.
     */
    public static void setSelectedTitle(ServerPlayer player, String titleId) {
        setSelectedTitle(player, titleId, false);
    }

    /**
     * Set the player's selected title, optionally bypassing unlock checks.
     */
    public static void setSelectedTitle(ServerPlayer player, String titleId, boolean force) {
        CompoundTag mastery = getMasteryRoot(player);
        if (titleId == null || titleId.isEmpty()) {
            mastery.remove(KEY_SELECTED_TITLE);
            mastery.remove(KEY_SELECTED_TITLE_OVERRIDE);
        } else {
            mastery.putString(KEY_SELECTED_TITLE, titleId);
            mastery.putBoolean(KEY_SELECTED_TITLE_OVERRIDE, force);
        }
        TitleDisplay.refresh(player);
    }

    /**
     * Get the player's currently selected title id.
     */
    public static String getSelectedTitle(ServerPlayer player) {
        CompoundTag mastery = getMasteryRoot(player);
        return mastery.getString(KEY_SELECTED_TITLE).orElse("");
    }

    /**
     * Check if the selected title is forced (admin override).
     */
    public static boolean isSelectedTitleForced(ServerPlayer player) {
        CompoundTag mastery = getMasteryRoot(player);
        return mastery.getBooleanOr(KEY_SELECTED_TITLE_OVERRIDE, false);
    }

    /**
     * Get the player's selected title reward, or null if none/invalid.
     */
    public static MasteryReward getSelectedTitleReward(ServerPlayer player) {
        String titleId = getSelectedTitle(player);
        if (titleId.isEmpty()) {
            return null;
        }
        MasteryReward reward = MasteryRewards.findById(titleId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.TITLE) {
            return null;
        }
        CompoundTag mastery = getMasteryRoot(player);
        boolean override = mastery.getBooleanOr(KEY_SELECTED_TITLE_OVERRIDE, false);
        if (override) {
            return reward;
        }
        // Verify it's actually unlocked
        GemId gem = gemFromRewardId(titleId);
        if (gem == null) {
            return null;
        }
        int usage = getUsage(player, gem);
        if (usage < reward.threshold()) {
            return null;
        }
        return reward;
    }

    // ========== Aura Selection ==========

    /**
     * Set the player's selected aura (from any gem they've unlocked).
     * Pass null or empty string to clear.
     */
    public static void setSelectedAura(ServerPlayer player, String auraId) {
        CompoundTag mastery = getMasteryRoot(player);
        if (auraId == null || auraId.isEmpty()) {
            mastery.remove(KEY_SELECTED_AURA);
        } else {
            mastery.putString(KEY_SELECTED_AURA, auraId);
        }
    }

    /**
     * Get the player's currently selected aura id.
     */
    public static String getSelectedAura(ServerPlayer player) {
        CompoundTag mastery = getMasteryRoot(player);
        return mastery.getString(KEY_SELECTED_AURA).orElse("");
    }

    /**
     * Get the player's selected aura reward, or null if none/invalid.
     */
    public static MasteryReward getSelectedAuraReward(ServerPlayer player) {
        String auraId = getSelectedAura(player);
        if (auraId.isEmpty()) {
            return null;
        }
        MasteryReward reward = MasteryRewards.findById(auraId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.AURA) {
            return null;
        }
        // Verify it's actually unlocked
        GemId gem = gemFromRewardId(auraId);
        if (gem == null) {
            return null;
        }
        int usage = getUsage(player, gem);
        if (usage < reward.threshold()) {
            return null;
        }
        return reward;
    }

    // ========== Helpers ==========

    private static CompoundTag getMasteryRoot(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag mastery = root.getCompound(KEY_MASTERY).orElse(null);
        if (mastery == null) {
            mastery = new CompoundTag();
            root.put(KEY_MASTERY, mastery);
        }
        return mastery;
    }

    private static CompoundTag getGemMasteryData(ServerPlayer player, GemId gem) {
        CompoundTag mastery = getMasteryRoot(player);
        String gemKey = gem.name().toLowerCase();
        CompoundTag gemData = mastery.getCompound(gemKey).orElse(null);
        if (gemData == null) {
            gemData = new CompoundTag();
            mastery.put(gemKey, gemData);
        }
        return gemData;
    }

    /**
     * Extract the gem from a reward id (e.g., "astra_master" -> ASTRA).
     */
    public static GemId gemFromRewardId(String rewardId) {
        if (rewardId == null || rewardId.isEmpty()) {
            return null;
        }
        for (GemId gem : GemId.values()) {
            String prefix = gem.name().toLowerCase() + "_";
            if (rewardId.startsWith(prefix)) {
                return gem;
            }
        }
        return null;
    }
}
