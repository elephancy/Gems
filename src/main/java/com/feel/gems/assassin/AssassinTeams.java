package com.feel.gems.assassin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;




public final class AssassinTeams {
    private static final String TEAM_NAME = "gems_assassins";
    private static final String POINTS_OBJECTIVE_NAME = "gems_assassin_points";

    private AssassinTeams() {
    }

    public static void sync(MinecraftServer server, ServerPlayer player) {
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setColor(java.util.Optional.of(net.minecraft.world.scores.TeamColor.RED));
            team.setDisplayName(Component.literal("Assassins").withStyle(ChatFormatting.RED));
            // No prefix/suffix: keep it as “names are red in tab”, matching the requested UX.
        }

        String entry = player.getScoreboardName();
        boolean shouldBeMember = AssassinState.isAssassin(player);
        boolean isMember = team.getPlayers().contains(entry);
        if (shouldBeMember && !isMember) {
            scoreboard.addPlayerToTeam(entry, team);
        } else if (!shouldBeMember && isMember) {
            scoreboard.removePlayerFromTeam(entry, team);
        }

        // Show assassin points in the player list (tab) via a LIST scoreboard objective.
        var obj = scoreboard.getObjective(POINTS_OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.addObjective(
                    POINTS_OBJECTIVE_NAME,
                    ObjectiveCriteria.DUMMY,
                    Component.translatable("gems.assassin.points_objective").withStyle(ChatFormatting.RED),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    StyledFormat.SIDEBAR_DEFAULT
            );
        }
        scoreboard.setDisplayObjective(DisplaySlot.LIST, obj);

        ScoreHolder holder = ScoreHolder.forNameOnly(entry);
        if (shouldBeMember) {
            scoreboard.getOrCreatePlayerScore(holder, obj).set(AssassinState.assassinPoints(player));
        } else {
            scoreboard.resetSinglePlayerScore(holder, obj);
        }
    }
}
