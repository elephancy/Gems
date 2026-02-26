package com.feel.gems.mastery;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;

public final class TitleDisplay {
    private TitleDisplay() {
    }

    public static Component withTitlePrefix(ServerPlayer player, Component baseName) {
        TitleInfo info = titleInfo(player);
        if (info == null) {
            return baseName;
        }
        Component coloredName = baseName.copy().setStyle(baseName.getStyle().withColor(info.color()));
        return Component.empty().append(info.prefix()).append(coloredName);
    }

    public static ChatFormatting titleColor(ServerPlayer player) {
        TitleInfo info = titleInfo(player);
        return info == null ? null : info.color();
    }

    public static Component titlePrefix(ServerPlayer player) {
        TitleInfo info = titleInfo(player);
        return info == null ? null : info.prefix();
    }

    public static ChatFormatting titleColorForReward(MasteryReward reward) {
        if (reward == null) {
            return null;
        }
        GemId gem = GemMastery.gemFromRewardId(reward.id());
        if (gem != null) {
            return gemColor(gem);
        }
        if (reward.threshold() >= 500) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.LIGHT_PURPLE;
    }

    private static MutableComponent bracket(Component title, ChatFormatting color) {
        return Component.literal("[").withStyle(color)
            .append(title)
            .append(Component.literal("] ").withStyle(color));
    }

    private static TitleInfo titleInfo(ServerPlayer player) {
        String selectedId = GemMastery.getSelectedTitle(player);
        if (!selectedId.isEmpty() && selectedId.startsWith("leaderboard:")) {
            LeaderboardTracker.LeaderboardCategory category = generalFromId(selectedId);
            boolean forced = GemMastery.isSelectedTitleForced(player);
            if (category != null && (forced || LeaderboardTracker.holdsTitle(player, category))) {
                ChatFormatting color = generalTitleColor(category);
                MutableComponent title = LeaderboardTracker.getTitleText(category).copy();
                title.setStyle(title.getStyle().withColor(color));
                return new TitleInfo(bracket(title, color), color);
            }
        }

        MasteryReward selected = GemMastery.getSelectedTitleReward(player);
        if (selected != null) {
            ChatFormatting color = titleColorForReward(selected);
            Component title = Component.translatable(selected.displayKey()).withStyle(color);
            return new TitleInfo(bracket(title, color), color);
        }

        List<LeaderboardTracker.LeaderboardCategory> general = LeaderboardTracker.getTitles(player);
        if (!general.isEmpty()) {
            ChatFormatting color = generalTitleColor(general.get(0));
            MutableComponent title = LeaderboardTracker.getTitleText(general.get(0)).copy();
            title.setStyle(title.getStyle().withColor(color));
            return new TitleInfo(bracket(title, color), color);
        }

        GemId active = GemPlayerState.getActiveGem(player);
        int usage = GemMastery.getUsage(player, active);
        List<MasteryReward> unlocked = MasteryRewards.getUnlockedTitles(active, usage);
        if (!unlocked.isEmpty()) {
            MasteryReward reward = unlocked.get(unlocked.size() - 1);
            ChatFormatting color = titleColorForReward(reward);
            Component title = Component.translatable(reward.displayKey()).withStyle(color);
            return new TitleInfo(bracket(title, color), color);
        }

        return null;
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                List.of(player)
        );
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(packet);
        }
    }

    public static void refreshAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refresh(player);
        }
    }

    private record TitleInfo(Component prefix, ChatFormatting color) {
    }

    private static ChatFormatting generalTitleColor(LeaderboardTracker.LeaderboardCategory category) {
        if (category.gem() != null) {
            return gemColor(category.gem());
        }
        return switch (category) {
            case LEAST_DEATHS -> ChatFormatting.AQUA;
            case MOST_KILLS -> ChatFormatting.RED;
            case MOST_HEARTS -> ChatFormatting.DARK_PURPLE;
            case MAX_ENERGY -> ChatFormatting.GOLD;
            case MOST_SYNERGY_TRIGGERS -> ChatFormatting.DARK_AQUA;
            case MOST_ABILITY_CASTS -> ChatFormatting.LIGHT_PURPLE;
            case MOST_DAMAGE_DEALT -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.WHITE;
        };
    }

    private static LeaderboardTracker.LeaderboardCategory generalFromId(String id) {
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

    private static ChatFormatting gemColor(GemId gem) {
        return switch (gem) {
            case FIRE -> ChatFormatting.RED;
            case FLUX -> ChatFormatting.YELLOW;
            case LIFE -> ChatFormatting.GREEN;
            case PUFF -> ChatFormatting.WHITE;
            case SPEED -> ChatFormatting.AQUA;
            case STRENGTH -> ChatFormatting.DARK_RED;
            case WEALTH -> ChatFormatting.GOLD;
            case TERROR -> ChatFormatting.DARK_PURPLE;
            case SUMMONER -> ChatFormatting.DARK_GREEN;
            case SPACE -> ChatFormatting.DARK_BLUE;
            case REAPER -> ChatFormatting.DARK_GRAY;
            case PILLAGER -> ChatFormatting.GRAY;
            case SPY -> ChatFormatting.LIGHT_PURPLE;
            case BEACON -> ChatFormatting.BLUE;
            case AIR -> ChatFormatting.WHITE;
            case ASTRA -> ChatFormatting.DARK_PURPLE;
            case VOID -> ChatFormatting.BLACK;
            case CHAOS -> ChatFormatting.RED;
            case PRISM -> ChatFormatting.LIGHT_PURPLE;
            case DUELIST -> ChatFormatting.GOLD;
            case HUNTER -> ChatFormatting.DARK_GREEN;
            case SENTINEL -> ChatFormatting.BLUE;
            case TRICKSTER -> ChatFormatting.DARK_PURPLE;
        };
    }
}
