package com.feel.gems.net;

import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.bounty.BountyBoard.BountyListEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerBountyNetworking {
    private ServerBountyNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BountyBoardOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> open(context.player()))
        );

        ServerPlayNetworking.registerGlobalReceiver(BountyPlacePayload.ID, (payload, context) ->
                context.server().execute(() -> handlePlace(context.player(), payload))
        );
    }

    public static void open(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        List<BountyBoardPayload.Entry> bounties = BountyBoard.listEntries(server).stream()
                .sorted(Comparator.comparing((BountyListEntry e) -> e.targetName().toLowerCase(Locale.ROOT))
                        .thenComparing(e -> e.placerName().toLowerCase(Locale.ROOT)))
                .map(entry -> new BountyBoardPayload.Entry(
                        entry.targetId(),
                        entry.targetName(),
                        entry.placerId(),
                        entry.placerName(),
                        entry.hearts(),
                        entry.energy()
                ))
                .toList();

        List<BountyBoardPayload.PlayerEntry> players = server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getUUID().equals(player.getUUID()))
                .sorted(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)))
                .map(p -> new BountyBoardPayload.PlayerEntry(p.getUUID(), p.getName().getString()))
                .toList();

        int maxHearts = BountyBoard.maxAdditionalHearts(player);
        int maxEnergy = BountyBoard.maxAdditionalEnergy(player);

        ServerPlayNetworking.send(player, new BountyBoardPayload(player.getUUID(), maxHearts, maxEnergy, bounties, players));
    }

    private static void handlePlace(ServerPlayer placer, BountyPlacePayload payload) {
        if (placer == null) {
            return;
        }
        MinecraftServer server = placer.level().getServer();
        if (server == null) {
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetId());
        if (target == null) {
            placer.sendSystemMessage(Component.translatable("gems.bounty.error.target_offline"), true);
            return;
        }
        BountyBoard.PlaceResult result = BountyBoard.placeBounty(placer, target, payload.hearts(), payload.energy());
        if (result == BountyBoard.PlaceResult.SUCCESS) {
            placer.sendSystemMessage(Component.translatable("gems.bounty.place.success", target.getName().getString(), payload.hearts(), payload.energy()), true);
            target.sendSystemMessage(Component.translatable("gems.bounty.placed_on_you", placer.getName().getString(), payload.hearts(), payload.energy()), false);
            return;
        }
        placer.sendSystemMessage(messageFor(result), true);
    }

    private static Component messageFor(BountyBoard.PlaceResult result) {
        return switch (result) {
            case ASSASSIN_BLOCKED -> Component.translatable("gems.bounty.error.assassin_blocked");
            case INVALID_TARGET -> Component.translatable("gems.bounty.error.invalid_target");
            case INVALID_AMOUNT -> Component.translatable("gems.bounty.error.invalid_amount");
            case NOT_HIGHER -> Component.translatable("gems.bounty.error.not_higher");
            case NO_CHANGE -> Component.translatable("gems.bounty.error.no_change");
            case INSUFFICIENT_HEARTS -> Component.translatable("gems.bounty.error.insufficient_hearts");
            case INSUFFICIENT_ENERGY -> Component.translatable("gems.bounty.error.insufficient_energy");
            default -> Component.translatable("gems.bounty.error.unknown");
        };
    }
}
