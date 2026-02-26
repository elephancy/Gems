package com.feel.gems.synergy;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTickScheduler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Runtime for tracking and triggering team synergies.
 * Synergies trigger when two or more different gem abilities are cast within a short window.
 * Works across trusted allies or for multi-gem players (Prism).
 */
@SuppressWarnings("unused")
public final class SynergyRuntime {
    // Recent ability casts: player UUID -> list of recent casts
    private static final Map<UUID, List<RecentCast>> RECENT_CASTS = new ConcurrentHashMap<>();

    // Synergy cooldowns: synergy ID -> (player group hash -> next allowed tick)
    private static final Map<String, Map<Integer, Long>> SYNERGY_COOLDOWNS = new ConcurrentHashMap<>();

    private SynergyRuntime() {
    }

    /**
     * Record of a recent ability cast for synergy tracking.
     */
    public record RecentCast(
            UUID playerId,
            GemId gem,
            Identifier abilityId,
            long castTick
    ) {}

    // ========== Ability Cast Registration ==========

    /**
     * Called when a player casts an ability.
     * Tracks the cast for potential synergy combos.
     */
    public static void onAbilityCast(ServerPlayer player, GemId gem, Identifier abilityId) {
        if (!GemsBalance.v().synergies().enabled()) {
            return;
        }

        long now = player.level().getGameTime();
        UUID playerId = player.getUUID();

        // Add this cast to recent casts
        RECENT_CASTS.computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(new RecentCast(playerId, gem, abilityId, now));

        // Check for synergies
        checkSynergies(player, now);
    }

    /**
     * Check for synergies involving the player and their trusted allies.
     */
    private static void checkSynergies(ServerPlayer player, long now) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        // Collect all recent casts from player and trusted allies
        List<RecentCast> allRecentCasts = new ArrayList<>();
        Set<UUID> checkedPlayers = new HashSet<>();

        // Add player's casts
        collectRecentCasts(player.getUUID(), now, allRecentCasts);
        checkedPlayers.add(player.getUUID());

        // Add trusted allies' casts
        for (UUID trustedId : GemTrust.getTrusted(player)) {
            if (!checkedPlayers.contains(trustedId)) {
                collectRecentCasts(trustedId, now, allRecentCasts);
                checkedPlayers.add(trustedId);
            }
        }

        // Also check other online players who trust this player
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (!checkedPlayers.contains(other.getUUID()) && GemTrust.isTrusted(other, player)) {
                collectRecentCasts(other.getUUID(), now, allRecentCasts);
                checkedPlayers.add(other.getUUID());
            }
        }

        if (allRecentCasts.size() < 2) {
            return; // Need at least 2 casts for a synergy
        }

            // Check each registered synergy
            for (SynergyDefinition synergy : SynergyRegistry.getAll()) {
                checkSynergyMatch(synergy, allRecentCasts, server, now);
            }
    }

    private static void collectRecentCasts(UUID playerId, long now, List<RecentCast> out) {
        List<RecentCast> casts = RECENT_CASTS.get(playerId);
        if (casts == null) {
            return;
        }

        int windowTicks = GemsBalance.v().synergies().maxWindowTicks();
        long cutoff = now - windowTicks;

        // Remove old casts and collect valid ones
        casts.removeIf(cast -> cast.castTick() < cutoff);

        out.addAll(casts);
    }

    private static void checkSynergyMatch(
            SynergyDefinition synergy,
            List<RecentCast> allRecentCasts,
            MinecraftServer server,
            long now
    ) {
        if (!GemsBalance.v().synergies().isSynergyEnabled(synergy.id())) {
            return;
        }
        int windowTicks = GemsBalance.v().synergies().windowTicksFor(synergy.id(), synergy.windowTicks());
        long cutoff = now - windowTicks;
        List<RecentCast> recentCasts = new ArrayList<>();
        for (RecentCast cast : allRecentCasts) {
            if (cast.castTick() >= cutoff) {
                recentCasts.add(cast);
            }
        }
        if (recentCasts.size() < 2) {
            return;
        }
        Set<GemId> requiredGems = synergy.requiredGems();

        // Group casts by gem
        Map<GemId, List<RecentCast>> castsByGem = new EnumMap<>(GemId.class);
        for (RecentCast cast : recentCasts) {
            if (requiredGems.contains(cast.gem())) {
                castsByGem.computeIfAbsent(cast.gem(), k -> new ArrayList<>()).add(cast);
            }
        }

        // Check if all required gems have at least one cast
        if (!castsByGem.keySet().containsAll(requiredGems)) {
            return;
        }

        // For ability-specific synergies, also check that the required abilities were cast
        if (synergy.isAbilitySpecific()) {
            Set<Identifier> requiredAbilities = synergy.requiredAbilities().orElse(Set.of());
            Set<Identifier> castAbilities = new HashSet<>();
            for (RecentCast cast : recentCasts) {
                castAbilities.add(cast.abilityId());
            }
            if (!castAbilities.containsAll(requiredAbilities)) {
                return; // Not all required abilities were cast
            }
        }

        // Collect participants (one per gem, most recent cast - preferring required abilities)
        List<SynergyDefinition.SynergyParticipant> participants = new ArrayList<>();
        Set<UUID> participantIds = new HashSet<>();

        for (GemId gem : requiredGems) {
            List<RecentCast> castsForGem = castsByGem.get(gem);
            if (castsForGem == null || castsForGem.isEmpty()) {
                return;
            }

            // For ability-specific synergies, prefer casts with required abilities
            RecentCast recentCast;
            if (synergy.isAbilitySpecific()) {
                Set<Identifier> requiredAbilities = synergy.requiredAbilities().orElse(Set.of());
                recentCast = castsForGem.stream()
                        .filter(c -> requiredAbilities.contains(c.abilityId()))
                        .max(Comparator.comparingLong(RecentCast::castTick))
                        .orElse(null);
            } else {
                recentCast = castsForGem.stream()
                        .max(Comparator.comparingLong(RecentCast::castTick))
                        .orElse(null);
            }

            if (recentCast == null) {
                return;
            }

            ServerPlayer participant = server.getPlayerList().getPlayer(recentCast.playerId());
            if (participant == null) {
                return;
            }

            participants.add(new SynergyDefinition.SynergyParticipant(
                    participant,
                    recentCast.gem(),
                    recentCast.abilityId(),
                    recentCast.castTick()
            ));
            participantIds.add(recentCast.playerId());
        }

        // Check cooldown
        int groupHash = participantIds.hashCode();
        Map<Integer, Long> cooldowns = SYNERGY_COOLDOWNS.computeIfAbsent(synergy.id(), k -> new ConcurrentHashMap<>());
        Long nextAllowed = cooldowns.get(groupHash);
        if (nextAllowed != null && now < nextAllowed) {
            return; // Still on cooldown for this player group
        }

        // Enforce mutual trust for multi-player synergies
        if (!areMutuallyTrusted(participants)) {
            return;
        }

        // Trigger the synergy!
        triggerSynergy(synergy, participants);

        // Set cooldown
        int cooldownTicks = GemsBalance.v().synergies().cooldownTicksFor(synergy.id(), synergy.cooldownTicks());
        cooldowns.put(groupHash, now + cooldownTicks);

        // Clear consumed casts
        for (SynergyDefinition.SynergyParticipant p : participants) {
            List<RecentCast> casts = RECENT_CASTS.get(p.player().getUUID());
            if (casts != null) {
                casts.removeIf(c -> c.gem() == p.gem() && c.castTick() == p.castTick());
            }
        }
    }

    private static void triggerSynergy(
            SynergyDefinition synergy,
            List<SynergyDefinition.SynergyParticipant> participants
    ) {
        // Apply the synergy effect
        synergy.effect().apply(participants);

        for (SynergyDefinition.SynergyParticipant p : participants) {
            GemsStats.recordSynergyTrigger(p.player(), Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, synergy.id()));
        }

        // Notify all participants if notifications are enabled
        if (GemsBalance.v().synergies().showNotifications()) {
            Component message = Component.translatable("gems.synergy.triggered", Component.translatable(synergy.translationKey()))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            for (SynergyDefinition.SynergyParticipant p : participants) {
                // Action bar HUD feedback for ~3 seconds
                sendActionBarForDuration(p.player(), message, 60);
                // Light VFX/SFX feedback
                var world = p.player().level();
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        p.player().getX(), p.player().getY() + 1.0, p.player().getZ(),
                        18, 0.6, 0.4, 0.6, 0.02);
                com.feel.gems.power.runtime.AbilityFeedback.sound(
                    p.player(),
                    net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                    0.7f,
                    1.1f
                );
            }
        }
    }

    private static void sendActionBarForDuration(ServerPlayer player, Component message, int durationTicks) {
        player.sendSystemMessage(message, true);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        int interval = 20;
        int repeats = Math.max(0, (durationTicks / interval) - 1);
        for (int i = 1; i <= repeats; i++) {
            int delay = i * interval;
            GemsTickScheduler.schedule(server, delay, srv -> {
                if (!player.isAlive()) {
                    return;
                }
                player.sendSystemMessage(message, true);
            });
        }
    }

    private static boolean areMutuallyTrusted(List<SynergyDefinition.SynergyParticipant> participants) {
        if (participants.size() < 2) {
            return true;
        }
        for (int i = 0; i < participants.size(); i++) {
            ServerPlayer a = participants.get(i).player();
            for (int j = i + 1; j < participants.size(); j++) {
                ServerPlayer b = participants.get(j).player();
                if (a.getUUID().equals(b.getUUID())) {
                    continue;
                }
                if (!GemTrust.isTrusted(a, b) || !GemTrust.isTrusted(b, a)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ========== Cleanup ==========

    /**
     * Clean up old casts periodically.
     */
    public static void cleanup(long currentTick) {
        int windowTicks = GemsBalance.v().synergies().windowTicks();
        long cutoff = currentTick - windowTicks - 100; // Extra buffer

        RECENT_CASTS.values().forEach(casts -> casts.removeIf(cast -> cast.castTick() < cutoff));
        RECENT_CASTS.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Clear all synergy state (for server stop/reload).
     */
    public static void clearAll() {
        RECENT_CASTS.clear();
        SYNERGY_COOLDOWNS.clear();
    }
}
