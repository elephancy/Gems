package com.feel.gems.debug;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;




/**
 * Simple in-game stress runner to drive ability code paths for profiling (spark/JFR).
 * Designed to be op-only via commands.
 */
public final class GemsStressTest {
    public enum Mode {
        REALISTIC,
        FORCE
    }

    private static final Map<UUID, Task> TASKS = new Object2ObjectOpenHashMap<>();

    private GemsStressTest() {
    }

    public static Mode parseMode(String raw) {
        if (raw == null) {
            return Mode.REALISTIC;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "force", "forced" -> Mode.FORCE;
            default -> Mode.REALISTIC;
        };
    }

    public static void start(ServerPlayer player, int seconds, int periodTicks, Mode mode, boolean cycleGems, boolean forceEnergy10) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        long now = server.overworld().getGameTime();
        long durationTicks = Math.max(1L, (long) seconds * 20L);
        Task task = new Task(
                now + durationTicks,
                now,
                Math.max(1, periodTicks),
                mode,
                cycleGems,
                forceEnergy10,
                GemPlayerState.getActiveGem(player).ordinal(),
                0
        );
        TASKS.put(player.getUUID(), task);
    }

    public static boolean stop(UUID playerId) {
        return TASKS.remove(playerId) != null;
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) {
            return;
        }

        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Task>> it = TASKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Task> entry = it.next();
            UUID uuid = entry.getKey();
            Task task = entry.getValue();

            if (now >= task.endTick) {
                it.remove();
                continue;
            }
            if (now < task.nextTick) {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                it.remove();
                continue;
            }

            GemPlayerState.initIfNeeded(player);
            if (task.forceEnergy10) {
                GemPlayerState.setEnergy(player, 10);
            }

            GemId active = GemPlayerState.getActiveGem(player);
            if (task.cycleGems) {
                GemId[] values = GemId.values();
                int ord = Math.floorMod(task.currentGemOrdinal, values.length);
                GemId next = values[ord];
                if (next != active) {
                    GemPlayerState.setActiveGem(player, next);
                    GemPowers.sync(player);
                    GemItemGlint.sync(player);
                    GemStateSync.send(player);
                    active = next;
                }
            }

            int abilityCount = GemRegistry.definition(active).abilities().size();
            int slots = abilityCount + (active == GemId.ASTRA ? 1 : 0);

            int slot = task.nextSlot;
            if (slot < 0 || slot >= Math.max(1, slots)) {
                slot = 0;
            }

            try {
                // Slot == abilityCount is Soul Release for Astra.
                if (active == GemId.ASTRA && slot == abilityCount) {
                    SoulSystem.release(player);
                } else if (task.mode == Mode.FORCE) {
                    var id = GemRegistry.definition(active).abilities().get(slot);
                    var ability = com.feel.gems.power.registry.ModAbilities.get(id);
                    if (ability != null) {
                        ability.activate(player);
                    }
                } else {
                    GemAbilities.activateByIndex(player, slot);
                }
            } catch (Throwable t) {
                GemsMod.LOGGER.error("Stress test crashed for player {} on {} slot {}", player.getName().getString(), active.name(), slot + 1, t);
                player.sendSystemMessage(Component.literal("Gems stress test stopped due to an error (see logs)."), false);
                it.remove();
                continue;
            }

            slot++;
            int nextGemOrdinal = task.currentGemOrdinal;
            if (slot >= slots) {
                slot = 0;
                if (task.cycleGems) {
                    nextGemOrdinal++;
                }
            }

            entry.setValue(task.withNext(now + task.periodTicks, nextGemOrdinal, slot));
        }
    }

    private record Task(
            long endTick,
            long nextTick,
            int periodTicks,
            Mode mode,
            boolean cycleGems,
            boolean forceEnergy10,
            int currentGemOrdinal,
            int nextSlot
    ) {
        Task withNext(long nextTick, int nextGemOrdinal, int nextSlot) {
            return new Task(endTick, nextTick, periodTicks, mode, cycleGems, forceEnergy10, nextGemOrdinal, nextSlot);
        }
    }
}
