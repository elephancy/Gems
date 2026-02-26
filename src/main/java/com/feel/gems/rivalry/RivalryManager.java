package com.feel.gems.rivalry;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.RivalrySyncPayload;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages the Rivalry system where players are assigned targets for bonus damage.
 */
public final class RivalryManager {
    private static final String KEY_RIVALRY_TARGET = "rivalryTarget";

    private RivalryManager() {
    }

    // ========== Target Assignment ==========

    /**
     * Assign a new rivalry target to a player.
     * Called on spawn/respawn.
     */
    public static void assignTarget(ServerPlayer player) {
        UUID newTarget = selectRandomTarget(player);
        setTarget(player, newTarget);

        String targetName = "";
        if (newTarget != null) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(newTarget);
                targetName = targetPlayer != null ? targetPlayer.getGameProfile().name() : "Unknown";
            }
            player.sendSystemMessage(
                    Component.translatable("gems.rivalry.target_assigned", targetName).withStyle(ChatFormatting.GOLD),
                    false
            );
        }

        // Sync to client for HUD
        syncToClient(player, targetName);
    }

    /**
     * Reroll a new rivalry target (called after killing current target).
     */
    public static void rerollTarget(ServerPlayer player) {
        assignTarget(player);
    }

    /**
     * Clear the player's rivalry target.
     */
    public static void clearTarget(ServerPlayer player) {
        setTarget(player, null);
    }

    // ========== Target Queries ==========

    /**
     * Get the player's current rivalry target UUID, or null if none.
     */
    public static UUID getTarget(ServerPlayer player) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return GemsNbt.getUuid(nbt, KEY_RIVALRY_TARGET);
    }

    /**
     * Check if a player has a rivalry target.
     */
    public static boolean hasTarget(ServerPlayer player) {
        return getTarget(player) != null;
    }

    /**
     * Check if the victim is the attacker's rivalry target.
     */
    public static boolean isRivalryTarget(ServerPlayer attacker, ServerPlayer victim) {
        UUID target = getTarget(attacker);
        return target != null && target.equals(victim.getUUID());
    }

    /**
     * Get the name of the player's current target, or null if none/offline.
     */
    public static String getTargetName(ServerPlayer player) {
        UUID target = getTarget(player);
        if (target == null) {
            return null;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
        return targetPlayer != null ? targetPlayer.getGameProfile().name() : null;
    }

    // ========== Damage Bonus ==========

    /**
     * Get the damage multiplier when attacking rivalry target.
     */
    public static float getDamageMultiplier() {
        return (float) GemsBalance.v().rivalry().damageMultiplier();
    }

    /**
     * Apply rivalry damage bonus if applicable.
     * Returns the modified damage.
     */
    public static float applyRivalryBonus(ServerPlayer attacker, ServerPlayer victim, float baseDamage) {
        if (!isRivalryTarget(attacker, victim)) {
            return baseDamage;
        }
        return baseDamage * getDamageMultiplier();
    }

    // ========== Kill Handler ==========

    /**
     * Called when a player kills another player.
     * Rerolls target if the killed player was the rivalry target.
     */
    public static void onPlayerKill(ServerPlayer killer, ServerPlayer victim) {
        if (isRivalryTarget(killer, victim)) {
            killer.sendSystemMessage(
                    Component.translatable("gems.rivalry.target_killed", victim.getGameProfile().name())
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
            rerollTarget(killer);
        }
    }

    // ========== Internal Helpers ==========

    private static void setTarget(ServerPlayer player, UUID target) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        GemsNbt.putUuid(nbt, KEY_RIVALRY_TARGET, target);
    }

    private static void syncToClient(ServerPlayer player, String targetName) {
        if (GemsBalance.v().rivalry().showInHud()) {
            ServerPlayNetworking.send(player, new RivalrySyncPayload(targetName != null ? targetName : ""));
        }
    }

    private static UUID selectRandomTarget(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            // Cannot target self
            if (other.getUUID().equals(player.getUUID())) {
                continue;
            }
            candidates.add(other);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Random selection
        int index = player.getRandom().nextInt(candidates.size());
        return candidates.get(index).getUUID();
    }
}
