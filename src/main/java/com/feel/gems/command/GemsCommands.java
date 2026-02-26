package com.feel.gems.command;

import com.feel.gems.GemsMod;
import com.feel.gems.GemsModEvents;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.augment.AugmentRarity;
import com.feel.gems.assassin.AssassinState;
import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.config.GemsDisablesConfigManager;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.item.GemItem;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.item.legendary.TrackerCompassItem;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.mastery.MasteryReward;
import com.feel.gems.mastery.MasteryRewards;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.net.ServerBountyNetworking;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;




public final class GemsCommands {
    private GemsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("gems")
                        .executes(ctx -> help(ctx.getSource()))
                        .then(Commands.literal("help")
                                .executes(ctx -> help(ctx.getSource())))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("assassin")
                            .then(Commands.literal("leave")
                                .executes(ctx -> assassinLeave(ctx.getSource()))))
                        .then(Commands.literal("reloadBalance")
                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                                .executes(ctx -> reloadBalance(ctx.getSource())))
                        .then(Commands.literal("reloadDisables")
                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                                .executes(ctx -> reloadDisables(ctx.getSource())))
                        .then(Commands.literal("dumpBalance")
                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                                .executes(ctx -> dumpBalance(ctx.getSource())))
                        .then(Commands.literal("trust")
                                .executes(ctx -> usage(ctx.getSource(), "/gems trust <players>"))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> {
                                            ServerPlayer owner = ctx.getSource().getPlayerOrException();
                                            for (ServerPlayer other : EntityArgument.getPlayers(ctx, "players")) {
                                                trust(owner, other);
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("untrust")
                                .executes(ctx -> usage(ctx.getSource(), "/gems untrust <players>"))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> {
                                            ServerPlayer owner = ctx.getSource().getPlayerOrException();
                                            for (ServerPlayer other : EntityArgument.getPlayers(ctx, "players")) {
                                                untrust(owner, other);
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("trustlist")
                                .executes(ctx -> trustList(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("track")
                                .executes(ctx -> usage(ctx.getSource(), "/gems track <player>"))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestOnlinePlayers(ctx.getSource(), builder))
                                        .executes(ctx -> track(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "player")))))
                        .then(Commands.literal("bounty")
                                .executes(ctx -> bountyOpen(ctx.getSource()))
                                .then(Commands.literal("place")
                                        .executes(ctx -> usage(ctx.getSource(), "/gems bounty place <player> <hearts> <energy>"))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("hearts", IntegerArgumentType.integer(0, GemPlayerState.MAX_MAX_HEARTS))
                                                        .then(Commands.argument("energy", IntegerArgumentType.integer(0, GemPlayerState.MAX_ENERGY))
                                                                .executes(ctx -> bountyPlace(
                                                                        ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        IntegerArgumentType.getInteger(ctx, "hearts"),
                                                                        IntegerArgumentType.getInteger(ctx, "energy")
                                                                ))))))
                                .then(Commands.literal("list")
                                        .executes(ctx -> bountyList(ctx.getSource()))))
                        .then(Commands.literal("trade")
                                .executes(ctx -> usage(ctx.getSource(), "/gems trade <gem>"))
                                .then(Commands.argument("gem", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestGems(builder))
                                        .executes(ctx -> trade(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "gem")))))
                        .then(Commands.literal("admin")
                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                                .executes(ctx -> adminHelp(ctx.getSource(), ""))
                                .then(Commands.literal("help")
                                        .executes(ctx -> adminHelp(ctx.getSource(), ""))
                                        .then(Commands.argument("topic", StringArgumentType.word())
                                                .suggests((ctx, builder) -> suggestAdminHelpTopics(builder))
                                                .executes(ctx -> adminHelp(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "topic")))))
                                .then(Commands.literal("status")
                                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                        status(ctx.getSource(), player);
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("cast")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                                        .executes(ctx -> {
                                                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                                            for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                                castSlot(ctx.getSource(), player, slot);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(Commands.literal("resync")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                        GemPlayerState.initIfNeeded(player);
                                                        resync(player);
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Resynced " + player.getName().getString()), true);
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("giveItem")
                                        .executes(ctx -> usage(ctx.getSource(), "/gems admin giveItem <players> <item>"))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("item", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestAdminItems(builder))
                                                        .executes(ctx -> giveItemToPlayers(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "item"))))))
                                .then(Commands.literal("giveGem")
                                        .executes(ctx -> usage(ctx.getSource(), "/gems admin giveGem <players> <gem>"))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> giveGemToPlayers(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem"))))))
                                .then(Commands.literal("giveAllGems")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> giveAllGems(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
                                .then(Commands.literal("giveAllLegendaries")
                                    .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> giveAllLegendaries(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
                                .then(Commands.literal("clearGems")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> clearGems(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
                                .then(Commands.literal("setEnergy")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("energy", IntegerArgumentType.integer(GemPlayerState.MIN_ENERGY, GemPlayerState.MAX_ENERGY))
                                                        .executes(ctx -> {
                                                            int energy = IntegerArgumentType.getInteger(ctx, "energy");
                                                            for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                                setEnergy(ctx.getSource(), player, energy);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(Commands.literal("setHearts")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("hearts", IntegerArgumentType.integer(GemPlayerState.minMaxHearts(), GemPlayerState.MAX_MAX_HEARTS))
                                                        .executes(ctx -> {
                                                            int hearts = IntegerArgumentType.getInteger(ctx, "hearts");
                                                            for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                                setHearts(ctx.getSource(), player, hearts);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(Commands.literal("setGem")
                                        .executes(ctx -> usage(ctx.getSource(), "/gems admin setGem <players> <gem>"))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> setGemForPlayers(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem"))))))
                                .then(Commands.literal("reclaimGem")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                        reclaimGem(ctx.getSource(), player);
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                                                        reset(ctx.getSource(), player);
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("resetAll")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> resetAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
                                .then(Commands.literal("cooldowns")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("disabled", BoolArgumentType.bool())
                                                        .executes(ctx -> setCooldownsDisabled(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                BoolArgumentType.getBool(ctx, "disabled")
                                                        )))))
                                .then(Commands.literal("legendaryCooldowns")
                                    .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("disabled", BoolArgumentType.bool())
                                            .executes(ctx -> setLegendaryCooldownsDisabled(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "players"),
                                                BoolArgumentType.getBool(ctx, "disabled")
                                            )))))
                                .then(Commands.literal("stats")
                                    .then(Commands.literal("show")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .executes(ctx -> statsShow(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "players")
                                            ))))
                                    .then(Commands.literal("set")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("stat", StringArgumentType.word())
                                                .suggests((ctx, builder) -> suggestOptions(builder, List.of(
                                                        "kills", "deaths", "player_kills", "player_deaths", "mob_kills",
                                                        "assassin_kills", "assassin_deaths", "final_kills", "final_deaths")))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000000))
                                                    .executes(ctx -> statsSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                    ))))))
                                    .then(Commands.literal("reset")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .executes(ctx -> statsReset(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "players")
                                            )))))
                                .then(Commands.literal("assassin")
                                    .then(Commands.literal("set")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAssassin(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "players"),
                                                    BoolArgumentType.getBool(ctx, "value")
                                                )))))
                                    .then(Commands.literal("setHearts")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("hearts", IntegerArgumentType.integer(0, AssassinState.maxHearts()))
                                                .executes(ctx -> setAssassinHearts(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "hearts")
                                                )))))
                                    .then(Commands.literal("setCounter")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("counter", StringArgumentType.word())
                                                .suggests((ctx, builder) -> suggestOptions(builder, List.of(
                                                        "total_normal_kills", "total_final_kills", "assassin_normal_kills",
                                                        "assassin_final_kills", "assassin_normal_kills_vs_non",
                                                        "assassin_final_kills_vs_non")))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000000))
                                                    .executes(ctx -> setAssassinCounter(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        StringArgumentType.getString(ctx, "counter"),
                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                    ))))))
                                    .then(Commands.literal("reset")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .executes(ctx -> resetAssassin(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))))
                                .then(Commands.literal("enhancements")
                                    .then(Commands.literal("setCapPenalty")
                                        .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("value", IntegerArgumentType.integer(0, GemPlayerState.MAX_ENERGY))
                                                .executes(ctx -> setEnhancementPenalty(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "value")
                                                ))))))
                                .then(Commands.literal("perf")
                                        .then(Commands.literal("reset")
                                                .executes(ctx -> perfReset(ctx.getSource())))
                                        .then(Commands.literal("snapshot")
                                                .executes(ctx -> perfSnapshot(ctx.getSource(), 1200))
                                                .then(Commands.argument("windowTicks", IntegerArgumentType.integer(1, 72000))
                                                        .executes(ctx -> perfSnapshot(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "windowTicks"))))))
                                .then(Commands.literal("stress")
                                        .then(Commands.literal("start")
                                                .then(Commands.argument("players", EntityArgument.players())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                .then(Commands.argument("periodTicks", IntegerArgumentType.integer(1, 200))
                                                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> suggestStressMode(builder))
                                                                                .then(Commands.argument("cycleGems", BoolArgumentType.bool())
                                                                                        .then(Commands.argument("forceEnergy10", BoolArgumentType.bool())
                                                                                                .executes(ctx -> stressStart(
                                                                                                        ctx.getSource(),
                                                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                                        IntegerArgumentType.getInteger(ctx, "periodTicks"),
                                                                                                        GemsStressTest.parseMode(StringArgumentType.getString(ctx, "mode")),
                                                                                                        BoolArgumentType.getBool(ctx, "cycleGems"),
                                                                                                        BoolArgumentType.getBool(ctx, "forceEnergy10")
                                                                                                ))))))))))
                                        .then(Commands.literal("stop")
                                                .then(Commands.argument("players", EntityArgument.players())
                                                        .executes(ctx -> stressStop(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))))
                        );

                registerTitleCommands(dispatcher);
                registerAugmentCommands(dispatcher);
                registerBonusCommands(dispatcher);
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Gems commands:"), false);
        source.sendSuccess(() -> Component.literal("Your gem: /gems status, /gems trade <gem>"), false);
        source.sendSuccess(() -> Component.literal("Other players: /gems trust <players>, untrust <players>, trustlist"), false);
        source.sendSuccess(() -> Component.literal("Hunt: /gems bounty, /gems track <player>"), false);
        source.sendSuccess(() -> Component.literal("Assassin choice: /gems assassin leave (after it unlocks)"), false);
        source.sendSuccess(() -> Component.literal("Tab completes player and gem names. Use /gems bounty place for bounties."), false);
        if (source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) {
            source.sendSuccess(() -> Component.literal("Admin tools: /gems admin help"), false);
        }
        return 1;
    }

    private static int adminHelp(CommandSourceStack source, String topic) {
        switch (topic.toLowerCase(Locale.ROOT)) {
            case "" -> {
                source.sendSuccess(() -> Component.literal("Gems admin help: /gems admin help <topic>"), false);
                source.sendSuccess(() -> Component.literal("setup — give gems/items, equip gems and set energy or hearts"), false);
                source.sendSuccess(() -> Component.literal("players — inspect, resync or reset player data"), false);
                source.sendSuccess(() -> Component.literal("powers — cast powers and adjust cooldowns or unlocks"), false);
                source.sendSuccess(() -> Component.literal("server — reload world settings; testing — measure performance"), false);
                source.sendSuccess(() -> Component.literal("These commands need operator level 2. Tab completes player, item and gem names."), false);
            }
            case "setup" -> {
                source.sendSuccess(() -> Component.literal("Setup — /gems admin giveGem <players> <gem>, giveItem <players> <item>"), false);
                source.sendSuccess(() -> Component.literal("All items: giveAllGems <players>, giveAllLegendaries <players>"), false);
                source.sendSuccess(() -> Component.literal("Equip or change: setGem <players> <gem>, reclaimGem <players>, clearGems <players>"), false);
                source.sendSuccess(() -> Component.literal("Set values: setEnergy <players> <0-10>, setHearts <players> <hearts>"), false);
            }
            case "players" -> {
                source.sendSuccess(() -> Component.literal("Players — /gems admin status [players], resync <players>"), false);
                source.sendSuccess(() -> Component.literal("Stats: stats show|set|reset; assassin: assassin set|setHearts|setCounter|reset"), false);
                source.sendSuccess(() -> Component.literal("Reset gem state: reset <players>. Wipe all mod player data: resetAll <players>."), false);
                source.sendSuccess(() -> Component.literal("Reset commands change saved state. Use a specific player before using @a."), false);
            }
            case "powers" -> {
                source.sendSuccess(() -> Component.literal("Powers — /gems admin cast <players> <slot>"), false);
                source.sendSuccess(() -> Component.literal("Cooldowns: cooldowns <players> <disabled>, legendaryCooldowns <players> <disabled>"), false);
                source.sendSuccess(() -> Component.literal("Extras: title set|clear|list; augment list|remove|clear; bonus list|reset|setAbilities|setPassives"), false);
                source.sendSuccess(() -> Component.literal("Advanced: enhancements setCapPenalty <players> <value>"), false);
            }
            case "server" -> {
                source.sendSuccess(() -> Component.literal("Server settings — /gems reloadBalance, /gems reloadDisables"), false);
                source.sendSuccess(() -> Component.literal("/gems dumpBalance writes the effective settings to config/gems/balance.effective.json"), false);
            }
            case "testing" -> {
                source.sendSuccess(() -> Component.literal("Testing — /gems admin perf reset|snapshot [windowTicks]"), false);
                source.sendSuccess(() -> Component.literal("/gems admin stress start <players> <seconds> <periodTicks> <realistic|force> <cycleGems> <forceEnergy10>"), false);
                source.sendSuccess(() -> Component.literal("/gems admin stress stop <players> — stop a running stress test"), false);
            }
            default -> {
                source.sendFailure(Component.literal("Unknown topic. Choose setup, players, powers, server or testing."));
                return 0;
            }
        }
        return 1;
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        int hearts = GemPlayerState.getMaxHearts(player);
        boolean assassin = AssassinState.isAssassin(player);

        GemDefinition def = GemRegistry.definition(active);
        GemEnergyState energyState = new GemEnergyState(energy);

        List<Identifier> passives = def.availablePassives(energyState);
        List<Identifier> abilities = def.availableAbilities(energyState);

        source.sendSuccess(() -> Component.literal("Player: " + player.getName().getString()), false);
        source.sendSuccess(() -> Component.literal("Gem: " + active.name()), false);
        source.sendSuccess(() -> Component.literal("Energy: " + energyState.tier().name() + " (" + energy + "/10)"), false);
        if (assassin) {
            int maxAssassinHearts = AssassinState.maxHearts();
            source.sendSuccess(() -> Component.literal("Assassin: YES (hearts=" + AssassinState.getAssassinHearts(player) + "/" + maxAssassinHearts + ", eliminated=" + AssassinState.isEliminated(player) + ")"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Assassin: no"), false);
        }
        source.sendSuccess(() -> Component.literal("Max hearts: " + hearts + "/20 (base system)"), false);
        source.sendSuccess(() -> Component.literal("Passives: " + (passives.isEmpty() ? "-" : passives.stream().map(GemsCommands::passiveName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        source.sendSuccess(() -> Component.literal("Abilities: " + (abilities.isEmpty() ? "-" : abilities.stream().map(GemsCommands::abilityName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        source.sendSuccess(() -> Component.literal("Owned: " + GemPlayerState.getOwnedGems(player)), false);
        source.sendSuccess(() -> Component.literal("Trusted: " + GemTrust.getTrusted(player).size()), false);
        source.sendSuccess(() -> Component.literal("Kills (total): normal=" + AssassinState.totalNormalKills(player) + " final=" + AssassinState.totalFinalKills(player) + " points=" + AssassinState.totalPoints(player)), false);
        if (assassin) {
            source.sendSuccess(() -> Component.literal("Kills (as assassin): normal=" + AssassinState.assassinNormalKills(player) + " final=" + AssassinState.assassinFinalKills(player) + " points=" + AssassinState.assassinPoints(player)), false);
            source.sendSuccess(() -> Component.literal("Winner points (vs non-assassins): normal=" + AssassinState.assassinNormalKillsVsNonAssassins(player) + " final=" + AssassinState.assassinFinalKillsVsNonAssassins(player) + " points=" + AssassinState.assassinPointsVsNonAssassins(player)), false);
        }
        if (active == GemId.FLUX) {
            source.sendSuccess(() -> Component.literal("Flux charge: " + FluxCharge.get(player) + "%"), false);
        }
        return 1;
    }

    private static int castSlot(CommandSourceStack source, ServerPlayer player, int slotNumber) {
        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);
        int abilityCount = GemRegistry.definition(active).abilities().size();

        int zeroBased = slotNumber - 1;
        if (active == GemId.ASTRA && zeroBased == abilityCount) {
            SoulSystem.release(player);
            source.sendSuccess(() -> Component.literal("Cast Soul Release for " + player.getName().getString()), true);
            return 1;
        }
        if (zeroBased < 0 || zeroBased >= abilityCount) {
            source.sendFailure(Component.literal("Slot " + slotNumber + " is not valid for " + active.name() + " (abilities: " + abilityCount + ")."));
            return 0;
        }

        GemAbilities.activateByIndex(player, zeroBased);
        source.sendSuccess(() -> Component.literal("Cast slot " + slotNumber + " for " + player.getName().getString() + " (" + active.name() + ")"), true);
        return 1;
    }

    private static int perfReset(CommandSourceStack source) {
        GemsPerfMonitor.reset();
        source.sendSuccess(() -> Component.literal("GEMS_PERF reset"), true);
        return 1;
    }

    private static int perfSnapshot(CommandSourceStack source, int windowTicks) {
        GemsPerfMonitor.Snapshot snap = GemsPerfMonitor.snapshot(windowTicks);
        source.sendSuccess(() -> Component.literal(String.format(
                java.util.Locale.ROOT,
                "GEMS_PERF samples=%d avg_mspt=%.2f med_mspt=%.2f p95_mspt=%.2f max_mspt=%.2f",
                snap.samples(),
                snap.avgMspt(),
                snap.medianMspt(),
                snap.p95Mspt(),
                snap.maxMspt()
        )), false);
        return 1;
    }

    private static int reloadBalance(CommandSourceStack source) {
        GemsBalance.ReloadResult result = GemsBalance.reloadFromDisk();
        if (!result.applied()) {
            String error = result.loadResult().error();
            source.sendFailure(Component.literal(error == null ? "Failed to reload balance config." : error));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Reloaded balance config (" + result.loadResult().status() + "): " + result.loadResult().path()), true);
        return 1;
    }

    private static int reloadDisables(CommandSourceStack source) {
        GemsDisablesConfigManager.LoadResult load = GemsDisablesConfigManager.loadOrCreateStrict();
        if (load.status() == GemsDisablesConfigManager.LoadStatus.ERROR || load.config() == null) {
            String error = load.error();
            source.sendFailure(Component.literal(error == null ? "Failed to reload disables config." : error));
            return 0;
        }
        GemsDisables.apply(load.config());
        source.sendSuccess(() -> Component.literal("Reloaded disables config (" + load.status() + "): " + load.path()), true);
        return 1;
    }

    private static int dumpBalance(CommandSourceStack source) {
        var out = GemsBalance.dumpEffectiveBalance();
        if (out == null) {
            source.sendFailure(Component.literal("Could not write the effective balance file. Check the server log."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Wrote effective balance to: " + out), true);
        return 1;
    }

    private static int setCooldownsDisabled(CommandSourceStack source, java.util.Collection<ServerPlayer> players, boolean disabled) {
        for (ServerPlayer player : players) {
            GemsAdmin.setNoCooldowns(player, disabled);
        }
        source.sendSuccess(() -> Component.literal((disabled ? "Disabled" : "Enabled") + " ability cooldowns for " + players.size() + " player(s)."), true);
        return 1;
    }

    private static int setLegendaryCooldownsDisabled(CommandSourceStack source, java.util.Collection<ServerPlayer> players, boolean disabled) {
        for (ServerPlayer player : players) {
            GemsAdmin.setNoLegendaryCooldowns(player, disabled);
        }
        source.sendSuccess(() -> Component.literal((disabled ? "Disabled" : "Enabled") + " legendary item cooldowns for " + players.size() + " player(s)."), true);
        return 1;
    }

    private static int statsShow(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            List<Component> lines = GemsStats.buildSummary(player);
            for (Component line : lines) {
                source.sendSuccess(() -> line, false);
            }
        }
        return 1;
    }

    private static int statsReset(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            GemsStats.reset(player);
            source.sendSuccess(() -> Component.literal("Reset stats for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int statsSet(CommandSourceStack source, java.util.Collection<ServerPlayer> players, String stat, int value) {
        int updated = 0;
        for (ServerPlayer player : players) {
            if (GemsStats.setStat(player, stat, value)) {
                updated++;
                source.sendSuccess(() -> Component.literal("Set stat " + stat + "=" + value + " for " + player.getName().getString()), true);
            } else {
                source.sendFailure(Component.literal("Unknown stat '" + stat + "'."));
            }
        }
        return updated > 0 ? 1 : 0;
    }

    private static int resetAssassin(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            boolean wasEliminated = AssassinState.isEliminated(player);
            AssassinState.reset(player);
            if (wasEliminated && player.isSpectator()) {
                player.setGameMode(GameType.SURVIVAL);
            }
            GemPlayerState.initIfNeeded(player);
            resync(player);
            AssassinTeams.sync(source.getServer(), player);
            source.sendSuccess(() -> Component.literal("Reset assassin state for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassin(CommandSourceStack source, java.util.Collection<ServerPlayer> players, boolean value) {
        for (ServerPlayer player : players) {
            AssassinState.setAssassin(player, value);
            if (!value && player.isSpectator()) {
                player.setGameMode(GameType.SURVIVAL);
            }
            GemPlayerState.initIfNeeded(player);
            resync(player);
            AssassinTeams.sync(source.getServer(), player);
            source.sendSuccess(() -> Component.literal("Set assassin=" + value + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassinHearts(CommandSourceStack source, java.util.Collection<ServerPlayer> players, int hearts) {
        for (ServerPlayer player : players) {
            int set = AssassinState.setAssassinHearts(player, hearts);
            GemPlayerState.applyMaxHearts(player);
            source.sendSuccess(() -> Component.literal("Set assassin hearts=" + set + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassinCounter(CommandSourceStack source, java.util.Collection<ServerPlayer> players, String counter, int value) {
        int updated = 0;
        for (ServerPlayer player : players) {
            if (AssassinState.setCounter(player, counter, value)) {
                updated++;
                source.sendSuccess(() -> Component.literal("Set assassin counter " + counter + "=" + value + " for " + player.getName().getString()), true);
            } else {
                source.sendFailure(Component.literal("Unknown assassin counter '" + counter + "'."));
            }
        }
        return updated > 0 ? 1 : 0;
    }

    private static int assassinLeave(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
        AssassinState.initIfNeeded(player);
        if (!AssassinState.isChoiceUnlocked(player)) {
            source.sendFailure(Component.translatable("gems.assassin.choice_locked", AssassinState.choicePointsRequired()));
            return 0;
        }
        if (!AssassinState.isAssassin(player)) {
            source.sendSuccess(() -> Component.translatable("gems.assassin.choice_already_normal"), false);
            return 1;
        }
        AssassinState.setAssassin(player, false);
        AssassinState.resetAssassinPoints(player);
        GemPlayerState.setMaxHearts(player, GemPlayerState.DEFAULT_MAX_HEARTS);
        player.setHealth(20.0f); // 10 hearts
        if (player.isSpectator()) {
            player.setGameMode(GameType.SURVIVAL);
        }
        GemPlayerState.initIfNeeded(player);
        resync(player);
        AssassinTeams.sync(source.getServer(), player);
        source.sendSuccess(() -> Component.translatable("gems.assassin.choice_set_normal"), false);
        return 1;
    }

    private static int giveAllGems(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        EnumSet<GemId> all = EnumSet.allOf(GemId.class);
        for (ServerPlayer player : players) {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setOwnedGemsExact(player, all);
            for (GemId gemId : GemId.values()) {
                ensurePlayerHasItem(player, ModItems.gemItem(gemId));
            }
            resync(player);
            source.sendSuccess(() -> Component.literal("Gave all gems to " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int giveAllLegendaries(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        List<Item> items = List.of(
                ModItems.TRACKER_COMPASS,
                ModItems.RECALL_RELIC,
                ModItems.HYPNO_STAFF,
                ModItems.EARTHSPLITTER_PICK,
                ModItems.SUPREME_HELMET,
                ModItems.SUPREME_CHESTPLATE,
                ModItems.SUPREME_LEGGINGS,
                ModItems.SUPREME_BOOTS,
                ModItems.BLOOD_OATH_BLADE,
                ModItems.DEMOLITION_BLADE,
                ModItems.HUNTERS_SIGHT_BOW,
                ModItems.THIRD_STRIKE_BLADE,
                ModItems.VAMPIRIC_EDGE,
                ModItems.EXPERIENCE_BLADE,
                ModItems.REVERSAL_MIRROR,
                ModItems.HUNTERS_TROPHY_NECKLACE,
                ModItems.GLADIATORS_MARK,
                ModItems.SOUL_SHACKLE,
                ModItems.DUELISTS_RAPIER,
                ModItems.CHALLENGERS_GAUNTLET,
                ModItems.CHRONO_CHARM,
                ModItems.GEM_SEER
        );
        for (ServerPlayer player : players) {
            for (Item item : items) {
                ensurePlayerHasItem(player, item);
            }
            source.sendSuccess(() -> Component.literal("Gave all legendaries to " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setEnhancementPenalty(CommandSourceStack source, java.util.Collection<ServerPlayer> players, int value) {
        for (ServerPlayer player : players) {
            int set = GemPlayerState.setEnergyCapPenalty(player, value);
            resync(player);
            source.sendSuccess(() -> Component.literal("Set enhancement cap penalty=" + set + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int clearGems(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            GemPlayerState.initIfNeeded(player);
            GemId active = GemPlayerState.getActiveGem(player);
            GemPlayerState.setOwnedGemsExact(player, EnumSet.of(active));
            removeGemItems(player, active);
            ensurePlayerHasItem(player, ModItems.gemItem(active));
            resync(player);
            source.sendSuccess(() -> Component.literal("Cleared owned gems for " + player.getName().getString() + " (kept " + active.name() + ")"), true);
        }
        return 1;
    }

    private static void removeGemItems(ServerPlayer player, GemId keep) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        for (GemId gemId : GemId.values()) {
            if (gemId == keep) {
                continue;
            }
            Item item = ModItems.gemItem(gemId);
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && stack.is(item)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static int stressStart(
            CommandSourceStack source,
            java.util.Collection<ServerPlayer> players,
            int seconds,
            int periodTicks,
            GemsStressTest.Mode mode,
            boolean cycleGems,
            boolean forceEnergy10
    ) {
        for (ServerPlayer p : players) {
            GemsStressTest.start(p, seconds, periodTicks, mode, cycleGems, forceEnergy10);
        }
        source.sendSuccess(() -> Component.literal("Started stress test for " + players.size() + " player(s): " +
                seconds + "s, period=" + periodTicks + "t, mode=" + mode + ", cycleGems=" + cycleGems + ", forceEnergy10=" + forceEnergy10), true);
        return 1;
    }

    private static int stressStop(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        int stopped = 0;
        for (ServerPlayer p : players) {
            if (GemsStressTest.stop(p.getUUID())) {
                stopped++;
            }
        }
        int finalStopped = stopped;
        source.sendSuccess(() -> Component.literal("Stopped stress test for " + finalStopped + " player(s)."), true);
        return 1;
    }

    private static int trust(ServerPlayer owner, ServerPlayer other) {
        if (owner == other) {
            owner.sendSystemMessage(Component.translatable("gems.trust.always_trusted"), false);
            return 1;
        }
        boolean changed = GemTrust.trust(owner, other.getUUID());
        owner.sendSystemMessage(changed
                ? Component.translatable("gems.trust.trusted", other.getName().getString())
                : Component.translatable("gems.trust.already_trusted", other.getName().getString()), false);
        return 1;
    }

    private static int untrust(ServerPlayer owner, ServerPlayer other) {
        if (owner == other) {
            owner.sendSystemMessage(Component.translatable("gems.trust.cannot_untrust_self"), false);
            return 0;
        }
        boolean changed = GemTrust.untrust(owner, other.getUUID());
        owner.sendSystemMessage(changed
                ? Component.translatable("gems.trust.untrusted", other.getName().getString())
                : Component.translatable("gems.trust.was_not_trusted", other.getName().getString()), false);
        return 1;
    }

    private static int trustList(ServerPlayer owner) {
        var trusted = GemTrust.getTrusted(owner);
        if (trusted.isEmpty()) {
            owner.sendSystemMessage(Component.translatable("gems.trust.list_empty"), false);
            return 1;
        }
        owner.sendSystemMessage(Component.translatable("gems.trust.list_header", trusted.size()), false);
        for (var uuid : trusted) {
            owner.sendSystemMessage(Component.literal("- " + uuid), false);
        }
        return 1;
    }

    private static int track(ServerPlayer player, String name) {
        TrackerCompassItem.setTarget(player, name);
        return 1;
    }

    private static int bountyOpen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerBountyNetworking.open(player);
        return 1;
    }

    private static int bountyPlace(CommandSourceStack source, ServerPlayer target, int hearts, int energy) throws CommandSyntaxException {
        ServerPlayer placer = source.getPlayerOrException();
        BountyBoard.PlaceResult result = BountyBoard.placeBounty(placer, target, hearts, energy);
        if (result == BountyBoard.PlaceResult.SUCCESS) {
            placer.sendSystemMessage(Component.translatable("gems.bounty.place.success", target.getName().getString(), hearts, energy), true);
            target.sendSystemMessage(Component.translatable("gems.bounty.placed_on_you", placer.getName().getString(), hearts, energy), false);
            return 1;
        }
        source.sendFailure(bountyErrorMessage(result));
        return 0;
    }

    private static Component bountyErrorMessage(BountyBoard.PlaceResult result) {
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

    private static int bountyList(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<BountyBoard.BountyListEntry> entries = BountyBoard.listEntries(server);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("gems.bounty.list.empty"), false);
            return 1;
        }
        entries.sort(Comparator.comparing((BountyBoard.BountyListEntry e) -> e.targetName().toLowerCase(Locale.ROOT))
                .thenComparing(e -> e.placerName().toLowerCase(Locale.ROOT)));
        source.sendSuccess(() -> Component.translatable("gems.bounty.list.header", entries.size()), false);
        for (BountyBoard.BountyListEntry entry : entries) {
            String line = entry.targetName() + " | " + entry.placerName()
                    + " | " + entry.hearts() + "H " + entry.energy() + "E";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static String passiveName(Identifier id) {
        GemPassive passive = ModPassives.get(id);
        return passive == null ? id.toString() : passive.name();
    }

    private static String abilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability == null ? id.toString() : ability.name();
    }

    private static int giveItemToPlayers(CommandSourceStack source, Iterable<ServerPlayer> players, String rawItem) {
        String normalized = rawItem.toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(normalized.contains(":") ? normalized : GemsMod.MOD_ID + ":" + normalized);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown item '" + rawItem
                    + "'. Tab complete an item ID after /gems admin giveItem <players>."));
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        int count = 0;
        for (ServerPlayer player : players) {
            player.addItem(new ItemStack(item));
            source.sendSuccess(() -> Component.literal("Gave " + id.getPath() + " to " + player.getName().getString()), true);
            count++;
        }
        return count;
    }

    private static GemId parseAdminGem(CommandSourceStack source, String rawGem) {
        try {
            return GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Unknown gem '" + rawGem
                    + "'. Use Tab after the player argument to choose a gem."));
            return null;
        }
    }

    private static int giveGemToPlayers(CommandSourceStack source, Iterable<ServerPlayer> players, String rawGem) {
        GemId gemId = parseAdminGem(source, rawGem);
        if (gemId == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer player : players) {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.addOwnedGem(player, gemId);
            ensurePlayerHasItem(player, ModItems.gemItem(gemId));
            resync(player);
            source.sendSuccess(() -> Component.literal("Gave gem " + gemId.name() + " to " + player.getName().getString()), true);
            count++;
        }
        return count;
    }

    private static int setEnergy(CommandSourceStack source, ServerPlayer player, int energy) {
        GemPlayerState.initIfNeeded(player);
        int set = GemPlayerState.setEnergy(player, energy);
        resync(player);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + " energy to " + set + "/10"), true);
        return 1;
    }

    private static int setHearts(CommandSourceStack source, ServerPlayer player, int hearts) {
        GemPlayerState.initIfNeeded(player);
        int set = GemPlayerState.setMaxHearts(player, hearts);
        resync(player);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + " max hearts to " + set + "/20"), true);
        return 1;
    }

    private static int setGemForPlayers(CommandSourceStack source, Iterable<ServerPlayer> players, String rawGem) {
        GemId gemId = parseAdminGem(source, rawGem);
        if (gemId == null) {
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : players) {
            setGem(source, player, gemId);
            count++;
        }
        return count;
    }

    private static void setGem(CommandSourceStack source, ServerPlayer player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);
        GemId activeBefore = GemPlayerState.getActiveGem(player);
        GemPlayerState.setActiveGem(player, gemId);
        if (activeBefore != gemId) {
            MinecraftServer server = source.getServer();
            if (server != null) {
                int baseEpoch = GemPlayerState.getGemEpoch(player);
                int nextEpoch = com.feel.gems.item.GemOwnership.nextGemEpoch(server, player.getUUID(), activeBefore, baseEpoch);
                com.feel.gems.item.GemOwnership.setGemEpochOverride(server, player.getUUID(), activeBefore, nextEpoch);
                com.feel.gems.item.GemOwnership.requestDeferredPurge(server, player.getUUID());
            }
            com.feel.gems.item.GemOwnership.removeOwnedGemFromInventory(player, player.getUUID(), activeBefore);
            com.feel.gems.item.GemOwnership.removeOwnedGemFromEnderChest(player, player.getUUID(), activeBefore);
        }
        ensurePlayerHasItem(player, ModItems.gemItem(gemId));
        resync(player);
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + " active gem to " + gemId.name()), true);
    }

    private static int usage(CommandSourceStack source, String command) {
        source.sendFailure(Component.literal("Use " + command));
        return 0;
    }

    private static int reclaimGem(CommandSourceStack source, ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);
        MinecraftServer server = source.getServer();
        int baseEpoch = GemPlayerState.getGemEpoch(player);
        if (server != null) {
            int nextEpoch = com.feel.gems.item.GemOwnership.nextGemEpoch(server, player.getUUID(), active, baseEpoch);
            com.feel.gems.item.GemOwnership.setGemEpochOverride(server, player.getUUID(), active, nextEpoch);
            com.feel.gems.item.GemOwnership.recordOwnerEpoch(server, player.getUUID(), baseEpoch);
        }
        com.feel.gems.item.GemOwnership.removeOwnedGemFromInventory(player, player.getUUID(), active);
        com.feel.gems.item.GemOwnership.removeOwnedGemFromEnderChest(player, player.getUUID(), active);
        ensurePlayerHasItem(player, ModItems.gemItem(active));
        if (server != null) {
            com.feel.gems.item.GemOwnership.requestDeferredPurge(server, player.getUUID());
        }
        resync(player);
        source.sendSuccess(() -> Component.literal("Reclaimed active gem for " + player.getName().getString()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        GemId[] values = GemId.values();
        GemId assigned = values[player.getRandom().nextInt(values.length)];
        GemPlayerState.resetToNew(player, assigned);
        ensurePlayerHasItem(player, ModItems.gemItem(assigned));
        resync(player);
        source.sendSuccess(() -> Component.literal("Reset " + player.getName().getString() + " gem state (assigned " + assigned.name() + ")"), true);
        return 1;
    }

    private static int resetAll(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        MinecraftServer server = source.getServer();
        for (ServerPlayer player : players) {
            resetAllForPlayer(server, player);
        }
        source.sendSuccess(() -> Component.literal("Reset all player data for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static void resetAllForPlayer(MinecraftServer server, ServerPlayer player) {
        if (player == null) {
            return;
        }

        // Restore anything stolen from others before wiping player data.
        SpySystem.restoreStolenFromThief(player);
        HuntersTrophyNecklaceItem.restoreStolenFromThief(player);

        UUID playerId = player.getUUID();
        if (server != null) {
            BonusClaimsState.get(server).releaseAllClaims(playerId);
            PrismSelectionsState.get(server).clearSelections(playerId);
            BountyBoard.clearForPlayer(server, playerId);
        }

        GemAbilityCooldowns.clearAll(player);
        clearItemCooldowns(player);
        GemTrust.clearRuntimeCache(playerId);
        LegendaryCooldowns.clearCache(playerId);

        ((GemsPersistentDataHolder) player).gems$setPersistentData(new CompoundTag());

        GemPlayerState.initIfNeeded(player);
        AssassinState.reset(player);
        GemPlayerState.applyMaxHearts(player);
        GemMastery.reset(player);
        GemsStats.reset(player);

        GemId active = GemPlayerState.getActiveGem(player);
        ensurePlayerHasItem(player, ModItems.gemItem(active));
        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
        if (server != null) {
            GemsModEvents.unlockStartingRecipes(server, player);
            AssassinTeams.sync(server, player);
        }

        resetVanillaStats(player);
    }

    private static void resetVanillaStats(ServerPlayer player) {
        StatsCounter handler = player.getStats();
        for (StatType<?> type : BuiltInRegistries.STAT_TYPE) {
            resetStatsForType(player, handler, type);
        }
    }

    private static void clearItemCooldowns(ServerPlayer player) {
        var manager = player.getCooldowns();
        // Avoid hard dependency on a clear() method, which differs by version.
        try {
            var method = manager.getClass().getMethod("clear");
            method.invoke(manager);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Item item : BuiltInRegistries.ITEM) {
            manager.addCooldown(item.getDefaultInstance(), 0);
        }
    }

    private static <T> void resetStatsForType(ServerPlayer player, StatsCounter handler, StatType<T> type) {
        Registry<T> registry = type.getRegistry();
        if (registry == null) {
            return;
        }
        for (T value : registry) {
            Stat<T> stat = type.get(value);
            handler.setValue(player, stat, 0);
        }
    }

    private static int trade(ServerPlayer player, String rawGem) {
        GemPlayerState.initIfNeeded(player);

        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.translatable("gems.trade.unknown_gem", rawGem), false);
            return 0;
        }

        GemTrading.Result result = GemTrading.trade(player, gemId);
        if (!result.success()) {
            return 0;
        }
        player.sendSystemMessage(Component.translatable("gems.trade.traded_for", gemId.name()), false);
        return 1;
    }

    private static void registerTitleCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gems")
                .then(Commands.literal("admin")
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                        .then(Commands.literal("title")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("titleId", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestTitles(builder))
                                                        .executes(ctx -> titleSet(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "titleId")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> titleClear(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players")
                                                ))))
                                .then(Commands.literal("list")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> titleList(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players")
                                                ))))
                        )));
    }

    private static void registerAugmentCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gems")
                .then(Commands.literal("admin")
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                        .then(Commands.literal("augment")
                                .then(Commands.literal("list")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> augmentList(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 16))
                                                                .executes(ctx -> augmentRemove(
                                                                        ctx.getSource(),
                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                        StringArgumentType.getString(ctx, "gem"),
                                                                        IntegerArgumentType.getInteger(ctx, "slot")
                                                                ))))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> augmentClear(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                        )));
    }

    private static void registerBonusCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gems")
                .then(Commands.literal("admin")
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                        .then(Commands.literal("bonus")
                                .then(Commands.literal("list")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> bonusList(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players")
                                                )))
                                        .then(Commands.argument("targets", StringArgumentType.greedyString())
                                                .executes(ctx -> bonusListOffline(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "targets")
                                                ))))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> bonusReset(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players")
                                                )))
                                        .then(Commands.argument("targets", StringArgumentType.greedyString())
                                                .executes(ctx -> bonusResetOffline(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "targets")
                                                ))))
                                .then(Commands.literal("setAbilities")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("abilities", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> suggestBonusAbilities(builder))
                                                        .executes(ctx -> bonusSetAbilities(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "abilities")
                                                        ))))
                                        .then(Commands.argument("targets", StringArgumentType.string())
                                                .then(Commands.argument("abilities", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> suggestBonusAbilities(builder))
                                                        .executes(ctx -> bonusSetAbilitiesOffline(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "abilities")
                                                        )))))
                                .then(Commands.literal("setPassives")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .then(Commands.argument("passives", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> suggestBonusPassives(builder))
                                                        .executes(ctx -> bonusSetPassives(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "passives")
                                                        ))))
                                        .then(Commands.argument("targets", StringArgumentType.string())
                                                .then(Commands.argument("passives", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> suggestBonusPassives(builder))
                                                        .executes(ctx -> bonusSetPassivesOffline(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "targets"),
                                                                StringArgumentType.getString(ctx, "passives")
                                                        )))))
                        )));
    }

    private static int titleSet(CommandSourceStack source, Iterable<ServerPlayer> players, String titleId) {
        MasteryReward reward = MasteryRewards.findById(titleId);
        boolean isGeneral = titleId != null && titleId.startsWith("leaderboard:");
        if ((reward == null || reward.type() != MasteryReward.MasteryRewardType.TITLE) && !isGeneral) {
            source.sendFailure(Component.translatable("gems.title.admin.invalid", titleId));
            return 0;
        }
        for (ServerPlayer player : players) {
            GemMastery.setSelectedTitle(player, isGeneral ? titleId : reward.id(), true);
            source.sendSuccess(() -> Component.translatable("gems.title.admin.set", player.getName().getString(), titleId), true);
        }
        return 1;
    }

    private static int titleClear(CommandSourceStack source, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            GemMastery.setSelectedTitle(player, "", true);
            source.sendSuccess(() -> Component.translatable("gems.title.admin.cleared", player.getName().getString()), true);
        }
        return 1;
    }

    private static int titleList(CommandSourceStack source, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            source.sendSuccess(() -> Component.translatable("gems.title.admin.list_header", player.getName().getString()), false);
            List<Component> lines = new ArrayList<>();
            for (GemId gem : GemId.values()) {
                int usage = GemMastery.getUsage(player, gem);
                for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
                    if (reward.type() != MasteryReward.MasteryRewardType.TITLE) {
                        continue;
                    }
                    boolean unlocked = usage >= reward.threshold();
                    Component name = Component.translatable(reward.displayKey());
                    String status = unlocked ? "unlocked" : "locked";
                    lines.add(Component.literal("- " + reward.id() + " (" + status + "): ").append(name));
                }
            }
            for (LeaderboardTracker.LeaderboardCategory category : LeaderboardTracker.LeaderboardCategory.values()) {
                boolean holds = LeaderboardTracker.holdsTitle(player, category);
                String status = holds ? "leader" : "locked";
                lines.add(Component.literal("- leaderboard:" + category.name().toLowerCase(Locale.ROOT) + " (" + status + "): ")
                        .append(LeaderboardTracker.getTitleText(category)));
            }
            if (lines.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("gems.title.admin.list_empty"), false);
            } else {
                for (Component line : lines) {
                    source.sendSuccess(() -> line, false);
                }
            }
        }
        return 1;
    }

    private static int augmentList(CommandSourceStack source, Iterable<ServerPlayer> players, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        for (ServerPlayer player : players) {
            ItemStack gemStack = findGemItem(player, gemId);
            if (gemStack.isEmpty()) {
                source.sendFailure(Component.translatable("gems.augment.admin.no_item", player.getName().getString(), gemId.name()));
                continue;
            }
            List<AugmentInstance> augments = AugmentRuntime.getGemAugments(gemStack);
            source.sendSuccess(() -> Component.translatable("gems.augment.admin.list_header", player.getName().getString(), gemId.name()), false);
            if (augments.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("gems.augment.admin.list_empty"), false);
            } else {
                for (int i = 0; i < augments.size(); i++) {
                    int slot = i + 1;
                    AugmentInstance inst = augments.get(i);
                    source.sendSuccess(() -> formatAugmentEntry(slot, inst), false);
                }
            }
        }
        return 1;
    }

    private static int augmentRemove(CommandSourceStack source, Iterable<ServerPlayer> players, String rawGem, int slot) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        int index = slot - 1;
        for (ServerPlayer player : players) {
            ItemStack gemStack = findGemItem(player, gemId);
            if (gemStack.isEmpty()) {
                source.sendFailure(Component.translatable("gems.augment.admin.no_item", player.getName().getString(), gemId.name()));
                continue;
            }
            boolean removed = AugmentRuntime.removeGemAugment(gemStack, index);
            if (removed) {
                if (GemPlayerState.getActiveGem(player) == gemId) {
                    AugmentRuntime.captureActiveGemAugments(player, gemId, gemStack);
                    com.feel.gems.net.GemCooldownSync.send(player);
                }
                source.sendSuccess(() -> Component.translatable("gems.augment.admin.removed", player.getName().getString(), slot, gemId.name()), true);
            } else {
                source.sendFailure(Component.translatable("gems.augment.admin.remove_failed", player.getName().getString(), slot, gemId.name()));
            }
        }
        return 1;
    }

    private static int augmentClear(CommandSourceStack source, Iterable<ServerPlayer> players, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        for (ServerPlayer player : players) {
            ItemStack gemStack = findGemItem(player, gemId);
            if (gemStack.isEmpty()) {
                source.sendFailure(Component.translatable("gems.augment.admin.no_item", player.getName().getString(), gemId.name()));
                continue;
            }
            AugmentRuntime.clearGemAugments(gemStack);
            if (GemPlayerState.getActiveGem(player) == gemId) {
                AugmentRuntime.captureActiveGemAugments(player, gemId, gemStack);
                com.feel.gems.net.GemCooldownSync.send(player);
            }
            source.sendSuccess(() -> Component.translatable("gems.augment.admin.cleared", player.getName().getString(), gemId.name()), true);
        }
        return 1;
    }

    private static int bonusList(CommandSourceStack source, Iterable<ServerPlayer> players) {
        return bonusListTargets(source, toBonusTargets(players));
    }

    private static int bonusListOffline(CommandSourceStack source, String rawTargets) {
        List<BonusTarget> targets = resolveBonusTargets(source, rawTargets);
        if (targets == null) {
            return 0;
        }
        return bonusListTargets(source, targets);
    }

    private static int bonusListTargets(CommandSourceStack source, List<BonusTarget> targets) {
        BonusClaimsState claims = BonusClaimsState.get(source.getServer());
        for (BonusTarget target : targets) {
            List<Identifier> abilities = claims.getPlayerAbilityOrder(target.uuid());
            List<Identifier> passives = new ArrayList<>(claims.getPlayerPassives(target.uuid()));
            passives.sort((a, b) -> a.toString().compareTo(b.toString()));

            String abilityList = abilities.isEmpty()
                    ? "-"
                    : abilities.stream().map(GemsCommands::formatAbilityEntry)
                            .reduce((a, b) -> a + ", " + b).orElse("-");
            String passiveList = passives.isEmpty()
                    ? "-"
                    : passives.stream().map(GemsCommands::formatPassiveEntry)
                            .reduce((a, b) -> a + ", " + b).orElse("-");

            String name = target.displayName();
            source.sendSuccess(() -> Component.literal("Bonus abilities for " + name + ": " + abilityList), false);
            source.sendSuccess(() -> Component.literal("Bonus passives for " + name + ": " + passiveList), false);
        }
        return 1;
    }

    private static int bonusReset(CommandSourceStack source, Iterable<ServerPlayer> players) {
        return bonusResetTargets(source, toBonusTargets(players));
    }

    private static int bonusSetAbilities(CommandSourceStack source, Iterable<ServerPlayer> players, String rawAbilities) {
        return bonusSetAbilitiesTargets(source, toBonusTargets(players), rawAbilities);
    }

    private static int bonusSetPassives(CommandSourceStack source, Iterable<ServerPlayer> players, String rawPassives) {
        return bonusSetPassivesTargets(source, toBonusTargets(players), rawPassives);
    }

    private static int bonusResetOffline(CommandSourceStack source, String rawTargets) {
        List<BonusTarget> targets = resolveBonusTargets(source, rawTargets);
        if (targets == null) {
            return 0;
        }
        return bonusResetTargets(source, targets);
    }

    private static int bonusResetTargets(CommandSourceStack source, List<BonusTarget> targets) {
        BonusClaimsState claims = BonusClaimsState.get(source.getServer());
        for (BonusTarget target : targets) {
            claims.releaseAllClaims(target.uuid());
            if (target.player() != null) {
                GemPowers.sync(target.player());
                GemStateSync.send(target.player());
            }
            source.sendSuccess(() -> Component.literal("Cleared bonus abilities/passives for " + target.displayName()), true);
        }
        return 1;
    }

    private static int bonusSetAbilitiesOffline(CommandSourceStack source, String rawTargets, String rawAbilities) {
        List<BonusTarget> targets = resolveBonusTargets(source, rawTargets);
        if (targets == null) {
            return 0;
        }
        return bonusSetAbilitiesTargets(source, targets, rawAbilities);
    }

    private static int bonusSetAbilitiesTargets(CommandSourceStack source, List<BonusTarget> targets, String rawAbilities) {
        List<Identifier> abilities = parseBonusList(source, rawAbilities, true);
        if (abilities == null) {
            return 0;
        }
        if (abilities.size() > 2) {
            source.sendFailure(Component.literal("Too many bonus abilities (max 2)."));
            return 0;
        }

        BonusClaimsState claims = BonusClaimsState.get(source.getServer());
        Set<UUID> affected = new HashSet<>();
        String listText = abilities.isEmpty()
                ? "-"
                : abilities.stream().map(GemsCommands::formatAbilityEntry)
                        .reduce((a, b) -> a + ", " + b).orElse("-");

        for (BonusTarget target : targets) {
            ServerPlayer player = target.player();
            if (player != null) {
                GemPlayerState.initIfNeeded(player);
                if (GemPlayerState.getEnergy(player) < 10) {
                    source.sendFailure(Component.literal("Player " + player.getName().getString() + " must be at 10 energy to hold bonus abilities."));
                    continue;
                }
            }

            for (Identifier id : claims.getPlayerAbilities(target.uuid())) {
                claims.releaseAbility(target.uuid(), id);
            }

            for (Identifier id : abilities) {
                UUID owner = claims.getAbilityClaimant(id);
                if (owner != null && !owner.equals(target.uuid())) {
                    claims.releaseAbility(owner, id);
                    affected.add(owner);
                }
                claims.claimAbility(target.uuid(), id);
            }

            if (player != null) {
                GemStateSync.sendBonusAbilitiesSync(player);
            }
            source.sendSuccess(() -> Component.literal("Set bonus abilities for " + target.displayName() + " to " + listText), true);
        }

        for (UUID owner : affected) {
            ServerPlayer other = source.getServer().getPlayerList().getPlayer(owner);
            if (other != null) {
                GemStateSync.sendBonusAbilitiesSync(other);
            }
        }
        return 1;
    }

    private static int bonusSetPassivesOffline(CommandSourceStack source, String rawTargets, String rawPassives) {
        List<BonusTarget> targets = resolveBonusTargets(source, rawTargets);
        if (targets == null) {
            return 0;
        }
        return bonusSetPassivesTargets(source, targets, rawPassives);
    }

    private static int bonusSetPassivesTargets(CommandSourceStack source, List<BonusTarget> targets, String rawPassives) {
        List<Identifier> passives = parseBonusList(source, rawPassives, false);
        if (passives == null) {
            return 0;
        }
        if (passives.size() > 2) {
            source.sendFailure(Component.literal("Too many bonus passives (max 2)."));
            return 0;
        }

        BonusClaimsState claims = BonusClaimsState.get(source.getServer());
        Set<UUID> affected = new HashSet<>();
        String listText = passives.isEmpty()
                ? "-"
                : passives.stream().map(GemsCommands::formatPassiveEntry)
                        .reduce((a, b) -> a + ", " + b).orElse("-");

        for (BonusTarget target : targets) {
            ServerPlayer player = target.player();
            if (player != null) {
                GemPlayerState.initIfNeeded(player);
                if (GemPlayerState.getEnergy(player) < 10) {
                    source.sendFailure(Component.literal("Player " + player.getName().getString() + " must be at 10 energy to hold bonus passives."));
                    continue;
                }
            }

            for (Identifier id : claims.getPlayerPassives(target.uuid())) {
                claims.releasePassive(target.uuid(), id);
            }

            for (Identifier id : passives) {
                UUID owner = claims.getPassiveClaimant(id);
                if (owner != null && !owner.equals(target.uuid())) {
                    claims.releasePassive(owner, id);
                    affected.add(owner);
                }
                claims.claimPassive(target.uuid(), id);
            }

            if (player != null) {
                GemPowers.sync(player);
                GemStateSync.send(player);
            }
            source.sendSuccess(() -> Component.literal("Set bonus passives for " + target.displayName() + " to " + listText), true);
        }

        for (UUID owner : affected) {
            ServerPlayer other = source.getServer().getPlayerList().getPlayer(owner);
            if (other != null) {
                GemPowers.sync(other);
                GemStateSync.send(other);
            }
        }
        return 1;
    }

    private static List<Identifier> parseBonusList(CommandSourceStack source, String raw, boolean abilities) {
        if (raw == null) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none") || trimmed.equals("-")) {
            return List.of();
        }
        String[] tokens = trimmed.split("[,\\s]+");
        List<Identifier> parsed = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String normalized = token.toLowerCase(Locale.ROOT);
            Identifier id = normalized.contains(":")
                    ? Identifier.tryParse(normalized)
                    : Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, normalized);
            if (id == null) {
                source.sendFailure(Component.literal("Unknown " + (abilities ? "ability" : "passive") + " '" + token + "'."));
                return null;
            }
            boolean ok = abilities ? BonusPoolRegistry.isBonusAbility(id) : BonusPoolRegistry.isBonusPassive(id);
            if (!ok) {
                source.sendFailure(Component.literal("Not a bonus " + (abilities ? "ability" : "passive") + ": " + id));
                return null;
            }
            if (!parsed.contains(id)) {
                parsed.add(id);
            }
        }
        return parsed;
    }

    private static String formatAbilityEntry(Identifier id) {
        return id.getPath() + " (" + abilityName(id) + ")";
    }

    private static String formatPassiveEntry(Identifier id) {
        return id.getPath() + " (" + passiveName(id) + ")";
    }

    private static ItemStack findGemItem(ServerPlayer player, GemId gemId) {
        if (player == null || gemId == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        if (isGemItem(main, gemId)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (isGemItem(off, gemId)) {
            return off;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isGemItem(stack, gemId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isGemItem(ItemStack stack, GemId gemId) {
        return stack.getItem() instanceof GemItem item && item.gemId() == gemId;
    }

    private static Component formatAugmentEntry(int slot, AugmentInstance instance) {
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        String name = def != null ? Component.translatable(def.nameKey()).getString() : instance.augmentId();
        String mag = String.format(Locale.ROOT, "%.2f", instance.magnitude());
        Component label = Component.literal("#" + slot + " ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(name).withStyle(rarityColor(instance.rarity())))
                .append(Component.literal(" (" + instance.rarity().name() + " x" + mag + ")").withStyle(ChatFormatting.DARK_GRAY));
        return label;
    }

    private static ChatFormatting rarityColor(AugmentRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.GRAY;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static void resync(ServerPlayer player) {
        GemPlayerState.applyMaxHearts(player);
        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
    }

    private static List<BonusTarget> toBonusTargets(Iterable<ServerPlayer> players) {
        List<BonusTarget> targets = new ArrayList<>();
        for (ServerPlayer player : players) {
            targets.add(new BonusTarget(player.getUUID(), player.getName().getString(), player));
        }
        return targets;
    }

    private static List<BonusTarget> resolveBonusTargets(CommandSourceStack source, String rawTargets) {
        if (rawTargets == null || rawTargets.isBlank()) {
            source.sendFailure(Component.literal("No players provided."));
            return null;
        }
        List<BonusTarget> targets = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        String[] tokens = rawTargets.trim().split("[,\\s]+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(token);
            if (online != null) {
                if (seen.add(online.getUUID())) {
                    targets.add(new BonusTarget(online.getUUID(), online.getName().getString(), online));
                }
                continue;
            }
            UUID uuid = tryParseUuid(token);
            if (uuid == null) {
                uuid = OldUsersConverter.convertMobOwnerIfNecessary(source.getServer(), token);
            }
            if (uuid == null) {
                source.sendFailure(Component.literal("Unknown player '" + token + "'."));
                return null;
            }
            if (seen.add(uuid)) {
                targets.add(new BonusTarget(uuid, token, null));
            }
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No valid players found."));
            return null;
        }
        return targets;
    }

    private static UUID tryParseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record BonusTarget(UUID uuid, String name, ServerPlayer player) {
        String displayName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            return uuid.toString();
        }
    }

    private static void ensurePlayerHasItem(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return;
            }
        }
        if (player.getOffhandItem().is(item)) {
            return;
        }
        if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(item)
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(item)) {
            return;
        }
        player.addItem(new ItemStack(item));
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandSourceStack source, SuggestionsBuilder builder) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            suggestIfMatching(builder, player.getName().getString());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminHelpTopics(SuggestionsBuilder builder) {
        return suggestOptions(builder, List.of("setup", "players", "powers", "server", "testing"));
    }

    private static CompletableFuture<Suggestions> suggestOptions(SuggestionsBuilder builder, List<String> options) {
        for (String option : options) {
            suggestIfMatching(builder, option);
        }
        return builder.buildFuture();
    }

    private static void suggestIfMatching(SuggestionsBuilder builder, String option) {
        if (option.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
            builder.suggest(option);
        }
    }

    private static CompletableFuture<Suggestions> suggestGems(SuggestionsBuilder builder) {
        for (GemId gemId : GemId.values()) {
            suggestIfMatching(builder, gemId.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminItems(SuggestionsBuilder builder) {
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (GemsMod.MOD_ID.equals(id.getNamespace())) {
                suggestIfMatching(builder, builder.getRemainingLowerCase().contains(":")
                        ? id.toString() : id.getPath());
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestTitles(SuggestionsBuilder builder) {
        for (GemId gem : GemId.values()) {
            for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
                if (reward.type() == MasteryReward.MasteryRewardType.TITLE) {
                    suggestIfMatching(builder, reward.id());
                }
            }
        }
        for (LeaderboardTracker.LeaderboardCategory category : LeaderboardTracker.LeaderboardCategory.values()) {
            suggestIfMatching(builder, "leaderboard:" + category.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBonusAbilities(SuggestionsBuilder builder) {
        for (Identifier id : BonusPoolRegistry.BONUS_ABILITIES) {
            suggestIfMatching(builder, id.getPath());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBonusPassives(SuggestionsBuilder builder) {
        for (Identifier id : BonusPoolRegistry.BONUS_PASSIVES) {
            suggestIfMatching(builder, id.getPath());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestStressMode(SuggestionsBuilder builder) {
        return suggestOptions(builder, List.of("realistic", "force"));
    }
}
