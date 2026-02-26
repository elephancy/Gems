package com.feel.gems.net;

import com.feel.gems.core.GemId;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.mastery.MasteryReward;
import com.feel.gems.mastery.MasteryRewards;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side networking for mastery title selection.
 */
public final class ServerTitleSelectionNetworking {
    private ServerTitleSelectionNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TitleSelectionOpenRequestPayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> open(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(TitleSelectionSelectPayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> handleSelect(context.player(), payload)));
    }

    private static void open(ServerPlayer player) {
        ServerPlayNetworking.send(player, buildPayload(player));
    }

    private static void handleSelect(ServerPlayer player, TitleSelectionSelectPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        String titleId = payload.titleId();
        if (titleId == null || titleId.isBlank()) {
            GemMastery.setSelectedTitle(player, "");
            open(player);
            return;
        }
        if (titleId.startsWith("leaderboard:")) {
            LeaderboardTracker.LeaderboardCategory category = parseGeneralCategory(titleId);
            if (category == null || !LeaderboardTracker.holdsTitle(player, category)) {
                return;
            }
            GemMastery.setSelectedTitle(player, titleId);
            open(player);
            return;
        }
        MasteryReward reward = MasteryRewards.findById(titleId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.TITLE) {
            return;
        }
        GemId gem = GemMastery.gemFromRewardId(titleId);
        if (gem == null) {
            return;
        }
        int usage = GemMastery.getUsage(player, gem);
        if (usage < reward.threshold()) {
            return;
        }
        GemMastery.setSelectedTitle(player, titleId);
        open(player);
    }

    private static TitleSelectionScreenPayload buildPayload(ServerPlayer player) {
        String selected = GemMastery.getSelectedTitle(player);
        boolean forced = GemMastery.isSelectedTitleForced(player);

        List<TitleSelectionScreenPayload.Entry> entries = new ArrayList<>();
        for (GemId gem : GemId.values()) {
            int usage = GemMastery.getUsage(player, gem);
            for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
                if (reward.type() != MasteryReward.MasteryRewardType.TITLE) {
                    continue;
                }
                boolean unlocked = usage >= reward.threshold();
                boolean selectedEntry = reward.id().equals(selected);
                entries.add(new TitleSelectionScreenPayload.Entry(
                        reward.id(),
                        gem.ordinal(),
                        reward.displayKey(),
                        usage,
                        reward.threshold(),
                        unlocked,
                        selectedEntry,
                        selectedEntry && forced
                ));
            }
        }
        for (LeaderboardTracker.LeaderboardCategory category : LeaderboardTracker.LeaderboardCategory.values()) {
            String id = "leaderboard:" + category.name().toLowerCase();
            boolean holds = LeaderboardTracker.holdsTitle(player, category);
            boolean selectedEntry = id.equals(selected);
            entries.add(new TitleSelectionScreenPayload.Entry(
                    id,
                    -1,
                    category.translationKey(),
                    0,
                    0,
                    holds,
                    selectedEntry,
                    selectedEntry && forced
            ));
        }
        return new TitleSelectionScreenPayload(entries);
    }

    private static LeaderboardTracker.LeaderboardCategory parseGeneralCategory(String id) {
        if (id == null || !id.startsWith("leaderboard:")) {
            return null;
        }
        String raw = id.substring("leaderboard:".length());
        for (LeaderboardTracker.LeaderboardCategory category : LeaderboardTracker.LeaderboardCategory.values()) {
            if (category.name().equalsIgnoreCase(raw)) {
                return category;
            }
        }
        return null;
    }
}
