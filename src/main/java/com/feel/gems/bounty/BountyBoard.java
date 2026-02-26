package com.feel.gems.bounty;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BountyBoard {
    public enum PlaceResult {
        SUCCESS,
        ASSASSIN_BLOCKED,
        INVALID_TARGET,
        INVALID_AMOUNT,
        NOT_HIGHER,
        NO_CHANGE,
        INSUFFICIENT_HEARTS,
        INSUFFICIENT_ENERGY
    }

    private BountyBoard() {
    }

    public static int maxAdditionalHearts(ServerPlayer player) {
        int current = GemPlayerState.getMaxHearts(player);
        int min = GemPlayerState.MIN_MAX_HEARTS;
        return Math.max(0, current - min);
    }

    public static int maxAdditionalEnergy(ServerPlayer player) {
        int current = GemPlayerState.getEnergy(player);
        return Math.max(0, Math.min(GemPlayerState.MAX_ENERGY, current));
    }

    public static PlaceResult placeBounty(ServerPlayer placer, ServerPlayer target, int hearts, int energy) {
        if (placer == null || target == null || placer.getUUID().equals(target.getUUID())) {
            return PlaceResult.INVALID_TARGET;
        }
        if (AssassinState.isAssassin(placer)) {
            return PlaceResult.ASSASSIN_BLOCKED;
        }
        if (hearts < 0 || energy < 0 || (hearts == 0 && energy == 0)) {
            return PlaceResult.INVALID_AMOUNT;
        }
        MinecraftServer server = placer.level().getServer();
        if (server == null) {
            return PlaceResult.INVALID_TARGET;
        }
        BountyState state = BountyState.get(server);
        BountyState.BountyEntry existing = state.getEntry(target.getUUID(), placer.getUUID());
        int existingHearts = existing == null ? 0 : existing.hearts();
        int existingEnergy = existing == null ? 0 : existing.energy();

        if (hearts < existingHearts || energy < existingEnergy) {
            return PlaceResult.NOT_HIGHER;
        }
        if (hearts == existingHearts && energy == existingEnergy) {
            return PlaceResult.NO_CHANGE;
        }

        int addHearts = hearts - existingHearts;
        int addEnergy = energy - existingEnergy;

        int currentHearts = GemPlayerState.getMaxHearts(placer);
        int minHearts = GemPlayerState.MIN_MAX_HEARTS;
        if (currentHearts - addHearts < minHearts) {
            return PlaceResult.INSUFFICIENT_HEARTS;
        }
        int currentEnergy = GemPlayerState.getEnergy(placer);
        if (currentEnergy - addEnergy < 0) {
            return PlaceResult.INSUFFICIENT_ENERGY;
        }

        if (addHearts > 0) {
            GemPlayerState.setMaxHearts(placer, currentHearts - addHearts);
            GemPlayerState.applyMaxHearts(placer);
            GemStateSync.send(placer);
        }
        if (addEnergy > 0) {
            GemPlayerState.setEnergy(placer, currentEnergy - addEnergy);
            GemPowers.sync(placer);
            GemStateSync.send(placer);
        }

        state.putEntry(
                target.getUUID(),
                target.getName().getString(),
                placer.getUUID(),
                placer.getName().getString(),
                hearts,
                energy
        );

        return PlaceResult.SUCCESS;
    }

    public static boolean handleKill(ServerPlayer victim, ServerPlayer killer, int bonusHearts, int bonusEnergy) {
        if (victim == null || killer == null || killer == victim) {
            return false;
        }
        MinecraftServer server = victim.level().getServer();
        if (server == null) {
            return false;
        }
        BountyState state = BountyState.get(server);
        UUID victimId = victim.getUUID();
        UUID killerId = killer.getUUID();
        int bonusHeartsRemaining = Math.max(0, bonusHearts);
        int bonusEnergyRemaining = Math.max(0, bonusEnergy);
        boolean claimed = false;

        BountyState.TargetBounty victimBounties = state.getTarget(victimId);
        if (victimBounties != null) {
            int heartsTotal = 0;
            int energyTotal = 0;
            for (BountyState.BountyEntry entry : victimBounties.placers().values()) {
                heartsTotal += Math.max(0, entry.hearts());
                energyTotal += Math.max(0, entry.energy());
            }
            state.removeTarget(victimId);
            reward(killer, heartsTotal + bonusHeartsRemaining, energyTotal + bonusEnergyRemaining);
            bonusHeartsRemaining = 0;
            bonusEnergyRemaining = 0;
            claimed = true;
            killer.sendSystemMessage(Component.translatable("gems.bounty.claimed", victim.getName().getString(), heartsTotal, energyTotal), true);
        }

        BountyState.BountyEntry revenge = state.removeEntry(killerId, victimId);
        if (revenge != null) {
            reward(killer, revenge.hearts() + bonusHeartsRemaining, revenge.energy() + bonusEnergyRemaining);
            claimed = true;
            killer.sendSystemMessage(Component.translatable("gems.bounty.claimed_revenge", victim.getName().getString(), revenge.hearts(), revenge.energy()), true);
        }
        return claimed;
    }

    public static void notifyOnLogin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        BountyState.TargetBounty bounty = BountyState.get(server).getTarget(player.getUUID());
        if (bounty == null || bounty.placers().isEmpty()) {
            return;
        }
        int heartsTotal = 0;
        int energyTotal = 0;
        for (BountyState.BountyEntry entry : bounty.placers().values()) {
            heartsTotal += Math.max(0, entry.hearts());
            energyTotal += Math.max(0, entry.energy());
        }
        int count = bounty.placers().size();
        player.sendSystemMessage(Component.translatable("gems.bounty.login_notice", count, heartsTotal, energyTotal), false);
    }

    public static void voidBountiesForAssassin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        BountyState state = BountyState.get(server);
        BountyState.TargetBounty removed = state.removeTarget(player.getUUID());
        if (removed == null) {
            return;
        }
        Component message = Component.translatable("gems.bounty.voided", player.getName().getString());
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(message, false);
        }
    }

    public static List<BountyListEntry> listEntries(MinecraftServer server) {
        List<BountyListEntry> entries = new ArrayList<>();
        if (server == null) {
            return entries;
        }
        BountyState state = BountyState.get(server);
        for (Map.Entry<UUID, BountyState.TargetBounty> targetEntry : state.getAll().entrySet()) {
            UUID targetId = targetEntry.getKey();
            BountyState.TargetBounty target = targetEntry.getValue();
            for (Map.Entry<UUID, BountyState.BountyEntry> placerEntry : target.placers().entrySet()) {
                BountyState.BountyEntry bounty = placerEntry.getValue();
                entries.add(new BountyListEntry(
                        targetId,
                        target.targetName(),
                        placerEntry.getKey(),
                        bounty.placerName(),
                        bounty.hearts(),
                        bounty.energy()
                ));
            }
        }
        return entries;
    }

    public static void clearForPlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) {
            return;
        }
        BountyState state = BountyState.get(server);
        state.removeTarget(playerId);
        for (UUID targetId : List.copyOf(state.getAll().keySet())) {
            state.removeEntry(targetId, playerId);
        }
    }

    private static void reward(ServerPlayer player, int hearts, int energy) {
        if (hearts > 0) {
            giveHearts(player, hearts);
        }
        if (energy > 0) {
            giveEnergyUpgrades(player, energy);
        }
    }

    private static void giveHearts(ServerPlayer player, int hearts) {
        int remaining = hearts;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(ModItems.HEART);
            int count = Math.min(stack.getMaxStackSize(), remaining);
            stack.setCount(count);
            if (!player.getInventory().add(stack)) {
                player.spawnAtLocation(player.level(), stack);
            }
            remaining -= count;
        }
    }

    private static void giveEnergyUpgrades(ServerPlayer player, int upgrades) {
        int remaining = upgrades;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(ModItems.ENERGY_UPGRADE);
            int count = Math.min(stack.getMaxStackSize(), remaining);
            stack.setCount(count);
            if (!player.getInventory().add(stack)) {
                player.spawnAtLocation(player.level(), stack);
            }
            remaining -= count;
        }
    }

    public record BountyListEntry(
            UUID targetId,
            String targetName,
            UUID placerId,
            String placerName,
            int hearts,
            int energy
    ) {}
}
