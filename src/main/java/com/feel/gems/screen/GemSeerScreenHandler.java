package com.feel.gems.screen;

import com.feel.gems.core.GemId;
import com.feel.gems.legendary.GemSeerTracker;
import com.feel.gems.item.GemItem;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.state.GemPlayerState;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Screen handler for the Gem Seer item.
 * Allows selecting a player to view their gem info.
 */
public final class GemSeerScreenHandler extends AbstractContainerMenu {
    private final List<PlayerInfo> playerInfos = new ArrayList<>();
    
    public record OwnedGem(GemId gem, int count) {
    }

    public record PlayerInfo(
            UUID uuid,
            String name,
            boolean online,
            GemId activeGem,
            int energy,
            List<OwnedGem> ownedGems
    ) {}

    public record OpeningData(List<PlayerInfo> playerInfos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpeningData> PACKET_CODEC = StreamCodec.of(
                OpeningData::write,
                OpeningData::read
        );

        private static void write(RegistryFriendlyByteBuf buf, OpeningData data) {
            List<PlayerInfo> players = data.playerInfos == null ? List.of() : data.playerInfos;
            buf.writeVarInt(players.size());
            for (PlayerInfo info : players) {
                buf.writeUUID(info.uuid());
                buf.writeUtf(info.name(), 128);
                buf.writeBoolean(info.online());
                buf.writeUtf(info.activeGem().name(), 32);
                buf.writeVarInt(info.energy());
                List<OwnedGem> owned = info.ownedGems() == null ? List.of() : info.ownedGems();
                buf.writeVarInt(owned.size());
                for (OwnedGem gem : owned) {
                    buf.writeUtf(gem.gem().name(), 32);
                    buf.writeVarInt(Math.max(1, gem.count()));
                }
            }
        }

        private static OpeningData read(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size <= 0) {
                return new OpeningData(List.of());
            }
            List<PlayerInfo> players = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                UUID uuid = buf.readUUID();
                String name = buf.readUtf(128);
                boolean online = buf.readBoolean();
                GemId activeGem = safeGemId(buf.readUtf(32), GemId.PUFF);
                int energy = buf.readVarInt();

                int ownedSize = buf.readVarInt();
                if (ownedSize < 0) ownedSize = 0;
                if (ownedSize > 64) ownedSize = 64;
                List<OwnedGem> owned = new ArrayList<>(ownedSize);
                for (int j = 0; j < ownedSize; j++) {
                    GemId gem = safeGemId(buf.readUtf(32), GemId.PUFF);
                    int count = Math.max(1, buf.readVarInt());
                    owned.add(new OwnedGem(gem, count));
                }
                players.add(new PlayerInfo(uuid, name, online, activeGem, energy, owned));
            }
            return new OpeningData(players);
        }

        private static GemId safeGemId(String raw, GemId fallback) {
            if (raw == null || raw.isEmpty()) {
                return fallback;
            }
            try {
                return GemId.valueOf(raw);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    public GemSeerScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.GEM_SEER, syncId);
    }

    public GemSeerScreenHandler(int syncId, Inventory playerInventory, OpeningData data) {
        super(ModScreenHandlers.GEM_SEER, syncId);
        if (data != null && data.playerInfos != null) {
            playerInfos.addAll(data.playerInfos);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (id < 0 || id >= playerInfos.size()) {
            return false;
        }

        PlayerInfo info = playerInfos.get(id);
        sendPlayerInfo(serverPlayer, info);
        return true;
    }

    private void sendPlayerInfo(ServerPlayer viewer, PlayerInfo info) {
        if (SpySystem.hidesTracking(viewer.level().getServer(), info.uuid())) {
            viewer.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return;
        }
        viewer.sendSystemMessage(Component.empty(), false); // Blank line
        viewer.sendSystemMessage(Component.translatable("gems.screen.gem_seer.header", info.name)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        Component activeText = Component.translatable("gems.screen.gem_seer.active_gem").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formatGemName(info.activeGem)).withStyle(gemColor(info.activeGem)));
        viewer.sendSystemMessage(activeText, false);
        
        Component energyText = Component.translatable("gems.screen.gem_seer.energy").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + info.energy + "/10]").withStyle(energyColor(info.energy)));
        viewer.sendSystemMessage(energyText, false);
        
        if (!info.ownedGems.isEmpty()) {
            StringBuilder owned = new StringBuilder();
            boolean first = true;
            for (OwnedGem gem : info.ownedGems) {
                if (!first) owned.append(", ");
                owned.append(formatGemName(gem.gem()));
                if (gem.count() > 1) {
                    owned.append(" x").append(gem.count());
                }
                first = false;
            }
            viewer.sendSystemMessage(Component.translatable("gems.screen.gem_seer.owned_gems").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(owned.toString()).withStyle(ChatFormatting.WHITE)), false);
        }
    }

    public void setOpeningData(OpeningData data) {
        playerInfos.clear();
        if (data != null && data.playerInfos() != null) {
            playerInfos.addAll(data.playerInfos());
        }
    }

    public List<PlayerInfo> getPlayerInfos() {
        return playerInfos;
    }

    public static String formatGemName(GemId gem) {
        String name = gem.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static ChatFormatting gemColor(GemId gem) {
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

    public static ChatFormatting energyColor(int energy) {
        if (energy >= 10) return ChatFormatting.GOLD;
        if (energy >= 7) return ChatFormatting.YELLOW;
        if (energy >= 4) return ChatFormatting.WHITE;
        return ChatFormatting.GRAY;
    }

    public static OpeningData buildOpeningData(ServerPlayer viewer) {
        if (viewer == null) {
            return new OpeningData(List.of());
        }
        MinecraftServer server = viewer.level().getServer();
        if (server == null) {
            return new OpeningData(List.of());
        }

        List<PlayerInfo> infos = new ArrayList<>();
        List<GemSeerTracker.Snapshot> snapshots = GemSeerTracker.knownSnapshots(server);
        if (snapshots.isEmpty()) {
            for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                if (target.equals(viewer)) {
                    continue;
                }
                if (SpySystem.hidesTracking(target)) {
                    continue;
                }
                boolean online = true;
                GemId activeGem = GemPlayerState.getActiveGem(target);
                int energy = GemPlayerState.getEnergy(target);
                List<OwnedGem> ownedGems = toOwnedList(countGemItems(target));
                infos.add(new PlayerInfo(target.getUUID(), target.getName().getString(), online, activeGem, energy, ownedGems));
            }
            infos.sort(Comparator.comparing(info -> info.name().toLowerCase(Locale.ROOT)));
            return new OpeningData(Collections.unmodifiableList(infos));
        }

        for (GemSeerTracker.Snapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.uuid() == null || snapshot.name() == null) {
                continue;
            }
            if (snapshot.uuid().equals(viewer.getUUID())) {
                continue;
            }
            if (SpySystem.hidesTracking(server, snapshot.uuid())) {
                continue;
            }
            boolean online = server.getPlayerList().getPlayer(snapshot.uuid()) != null;
            GemId activeGem = OpeningData.safeGemId(snapshot.activeGem(), GemId.PUFF);
            Map<GemId, Integer> counts = new EnumMap<>(GemId.class);
            if (snapshot.ownedCounts() != null && !snapshot.ownedCounts().isEmpty()) {
                for (var entry : snapshot.ownedCounts().entrySet()) {
                    GemId gem = OpeningData.safeGemId(entry.getKey(), GemId.PUFF);
                    int count = Math.max(1, entry.getValue());
                    counts.merge(gem, count, Integer::sum);
                }
            } else {
                for (String raw : snapshot.ownedGems()) {
                    counts.merge(OpeningData.safeGemId(raw, GemId.PUFF), 1, Integer::sum);
                }
            }
            infos.add(new PlayerInfo(snapshot.uuid(), snapshot.name(), online, activeGem, snapshot.energy(), toOwnedList(counts)));
        }
        infos.sort(Comparator.comparing(info -> info.name().toLowerCase(Locale.ROOT)));
        return new OpeningData(Collections.unmodifiableList(infos));
    }

    private static Map<GemId, Integer> countGemItems(ServerPlayer player) {
        Map<GemId, Integer> counts = new EnumMap<>(GemId.class);
        if (player == null) {
            return counts;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            addGemCount(counts, stack);
        }
        addGemCount(counts, player.getOffhandItem());
        addGemCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        addGemCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        addGemCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        addGemCount(counts, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        var ender = player.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) {
            addGemCount(counts, ender.getItem(i));
        }
        return counts;
    }

    private static void addGemCount(Map<GemId, Integer> counts, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof GemItem gem) {
            counts.merge(gem.gemId(), stack.getCount(), Integer::sum);
        }
    }

    private static List<OwnedGem> toOwnedList(Map<GemId, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        List<OwnedGem> list = new ArrayList<>(counts.size());
        for (var entry : counts.entrySet()) {
            list.add(new OwnedGem(entry.getKey(), Math.max(1, entry.getValue())));
        }
        list.sort(Comparator.comparing(gem -> gem.gem().name().toLowerCase(Locale.ROOT)));
        return list;
    }
}
