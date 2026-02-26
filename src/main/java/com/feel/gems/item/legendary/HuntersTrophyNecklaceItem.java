package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.net.ServerTrophyNecklaceNetworking;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Hunter's Trophy Necklace - steal passives from other players and keep them permanently.
 */
public final class HuntersTrophyNecklaceItem extends Item implements LegendaryItem {
    private static final String KEY_STOLEN_PASSIVES = "trophy_necklace_passives";
    private static final String KEY_STOLEN_FROM = "trophy_necklace_stolen_from";
    private static final String KEY_LOST_PASSIVES = "trophy_necklace_lost_passives";
    private static final String KEY_DISABLED_PASSIVES = "trophy_necklace_disabled_passives";
    private static final String KEY_LAST_TARGET_NAME = "trophy_necklace_last_target_name";
    private static final String KEY_LAST_TARGET_UUID = "trophy_necklace_last_target_uuid";
    private static final String KEY_LAST_OFFERED = "trophy_necklace_last_offered";
    private static final String KEY_LAST_KILL_TARGET_UUID = "trophy_necklace_last_kill_uuid";
    private static final String KEY_LAST_KILL_USED = "trophy_necklace_last_kill_used";

    private static int maxStolenPassives() {
        return GemsBalance.v().legendary().trophyNecklaceMaxPassives();
    }

    public HuntersTrophyNecklaceItem(Properties settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "hunters_trophy_necklace").toString();
    }

    /**
     * Called when the holder kills another player.
     * Opens a selection UI to choose passives to steal from the victim.
     */
    public static void onKillPlayer(ServerPlayer killer, ServerPlayer victim) {
        if (!hasNecklace(killer) || victim == null) {
            return;
        }
        markKillSession(killer, victim);
        openScreen(killer, victim, victim.getName().getString());
    }

    public static void openScreenForTarget(ServerPlayer opener, ServerPlayer target) {
        if (opener == null || target == null || opener.level().getServer() == null) {
            return;
        }
        openScreen(opener, target, target.getName().getString());
    }

    @Override
    public InteractionResult use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        if (world.isClientSide() || !(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        // Right click a player to steal their passives.
        ServerPlayer target = com.feel.gems.power.util.Targeting.raycastPlayer(player, 15);
        if (target == null || target == player) {
            player.sendSystemMessage(Component.translatable("gems.message.no_player_target"), true);
            return InteractionResult.FAIL;
        }
        openScreen(player, target, target.getName().getString());
        return InteractionResult.SUCCESS;
    }

    public static boolean hasNecklace(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof HuntersTrophyNecklaceItem) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof HuntersTrophyNecklaceItem;
    }

    public static Set<Identifier> getStolenPassives(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        ListTag list = data.getList(KEY_STOLEN_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getStringOr(i, ""));
            if (id != null) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    public static Set<Identifier> getDisabledPassives(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        ListTag list = data.getList(KEY_DISABLED_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getStringOr(i, ""));
            if (id != null) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    public static boolean isPassiveDisabled(ServerPlayer player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        return getDisabledPassives(player).contains(passiveId);
    }

    public static boolean stealPassive(ServerPlayer player, Identifier passiveId) {
        return stealPassiveFrom(player, passiveId, null, true);
    }

    public static boolean stealPassiveFrom(ServerPlayer player, Identifier passiveId, UUID sourceUuid) {
        return stealPassiveFrom(player, passiveId, sourceUuid, true);
    }

    private static boolean stealPassiveFrom(ServerPlayer player, Identifier passiveId, UUID sourceUuid, boolean enforceLimit) {
        if (player == null || passiveId == null || ModPassives.get(passiveId) == null) {
            return false;
        }
        if (sourceUuid == null) {
            return false;
        }
        if (!canClaimFromKill(player, sourceUuid)) {
            player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.need_kill").withStyle(ChatFormatting.RED), true);
            return false;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Set<Identifier> current = getStolenPassives(player);
        if (current.contains(passiveId)) {
            return false;
        }
        int max = maxStolenPassives();
        if (enforceLimit && max > 0 && current.size() >= max) {
            player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.max", max).withStyle(ChatFormatting.RED), true);
            return false;
        }
        ListTag list = new ListTag();
        for (Identifier id : current) {
            list.add(StringTag.valueOf(id.toString()));
        }
        list.add(StringTag.valueOf(passiveId.toString()));
        data.put(KEY_STOLEN_PASSIVES, list);
        if (sourceUuid != null) {
            CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(new CompoundTag());
            stolenFrom.put(passiveId.toString(), GemsNbt.fromUuid(sourceUuid));
            data.put(KEY_STOLEN_FROM, stolenFrom);
            ServerPlayer victim = player.level().getServer() != null
                    ? player.level().getServer().getPlayerList().getPlayer(sourceUuid)
                    : null;
            if (victim != null) {
                markLostPassive(victim, passiveId, player.getUUID());
                GemPassive passive = ModPassives.get(passiveId);
                if (passive != null) {
                    passive.remove(victim);
                }
                GemPowers.sync(victim);
            }
            markKillUsed(player);
        }
        removeDisabledPassive(data, passiveId);
        com.feel.gems.net.StolenStateSync.send(player);
        player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.stolen", ModPassives.get(passiveId).name()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static boolean unstealPassive(ServerPlayer player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        Set<Identifier> current = getStolenPassives(player);
        if (!current.contains(passiveId)) {
            return false;
        }
        java.util.HashSet<Identifier> next = new java.util.HashSet<>(current);
        next.remove(passiveId);
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID source = findSourceForPassive(data, passiveId);
        ListTag list = new ListTag();
        for (Identifier id : next) {
            list.add(StringTag.valueOf(id.toString()));
        }
        data.put(KEY_STOLEN_PASSIVES, list);
        removeDisabledPassive(data, passiveId);
        // Preserve stolen-from mapping so victims can recover on kill even if the passive is unselected.
        GemPassive passive = ModPassives.get(passiveId);
        if (passive != null) {
            passive.remove(player);
        }
        if (source != null && player.level().getServer() != null) {
            ServerPlayer victim = player.level().getServer().getPlayerList().getPlayer(source);
            if (victim != null) {
                clearLostPassive(victim, passiveId);
                GemPowers.sync(victim);
            }
        }
        com.feel.gems.net.StolenStateSync.send(player);
        player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.unstolen", passive != null ? passive.name() : passiveId.getPath()).withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    public static boolean setPassiveEnabled(ServerPlayer player, Identifier passiveId, boolean enabled) {
        if (player == null || passiveId == null) {
            return false;
        }
        Set<Identifier> stolen = getStolenPassives(player);
        if (!stolen.contains(passiveId)) {
            return false;
        }

        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (enabled) {
            removeDisabledPassive(data, passiveId);
        } else {
            addDisabledPassive(data, passiveId);
        }

        UUID source = findSourceForPassive(data, passiveId);
        if (source != null && player.level().getServer() != null) {
            ServerPlayer victim = player.level().getServer().getPlayerList().getPlayer(source);
            if (victim != null) {
                if (enabled) {
                    markLostPassive(victim, passiveId, player.getUUID());
                } else {
                    clearLostPassive(victim, passiveId);
                }
                GemPowers.sync(victim);
            }
        }

        GemPowers.sync(player);
        com.feel.gems.net.StolenStateSync.send(player);
        return true;
    }

    /**
     * Re-open the last Trophy Necklace UI session, if available (used after claim toggles).
     */
    public static boolean openLastTargetScreen(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return false;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String targetName = data.getStringOr(KEY_LAST_TARGET_NAME, "");
        ListTag offeredList = data.getList(KEY_LAST_OFFERED).orElse(null);
        java.util.HashSet<Identifier> offered = new java.util.HashSet<>();
        if (offeredList != null) {
            for (int i = 0; i < offeredList.size(); i++) {
                Identifier id = Identifier.tryParse(offeredList.getStringOr(i, ""));
                if (id != null) {
                    offered.add(id);
                }
            }
        }
        if (offered.isEmpty() && getStolenPassives(player).isEmpty()) {
            return false;
        }
        sendScreenPayload(player, targetName.isBlank() ? "Unknown" : targetName, offered);
        return true;
    }

    public static UUID getLastTargetUuid(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return GemsNbt.getUuid(data, KEY_LAST_TARGET_UUID);
    }

    public static UUID getLastKillTargetUuid(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return GemsNbt.getUuid(data, KEY_LAST_KILL_TARGET_UUID);
    }

    public static boolean wasLastOffered(ServerPlayer player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        ListTag offeredList = data.getList(KEY_LAST_OFFERED).orElse(null);
        if (offeredList == null || offeredList.isEmpty()) {
            return false;
        }
        String raw = passiveId.toString();
        for (int i = 0; i < offeredList.size(); i++) {
            if (raw.equals(offeredList.getStringOr(i, ""))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Component.translatable("gems.item.trophy_necklace.tooltip.1"),
                Component.translatable("gems.item.trophy_necklace.tooltip.2"),
                Component.translatable("gems.item.trophy_necklace.tooltip.3")
        );
    }

    private static void openScreen(ServerPlayer opener, ServerPlayer target, String targetName) {
        if (opener == null || target == null || opener.level().getServer() == null) {
            return;
        }
        var server = opener.level().getServer();
        java.util.HashSet<Identifier> offered = new java.util.HashSet<>();

        com.feel.gems.state.GemPlayerState.initIfNeeded(target);
        com.feel.gems.core.GemId gem = com.feel.gems.state.GemPlayerState.getActiveGem(target);
        if (gem == com.feel.gems.core.GemId.PRISM && server != null) {
            var prism = com.feel.gems.bonus.PrismSelectionsState.get(server).getSelection(target.getUUID());
            offered.addAll(prism.gemPassives());
        } else {
            offered.addAll(com.feel.gems.core.GemRegistry.definition(gem).passives());
        }

        // Include bonus passives the target currently has (energy 10).
        if (server != null && com.feel.gems.state.GemPlayerState.getEnergy(target) >= 10) {
            offered.addAll(com.feel.gems.bonus.BonusClaimsState.get(server).getPlayerPassives(target.getUUID()));
        }
        // Don't offer passives already stolen from this target.
        offered.removeAll(stolenFromTarget(((GemsPersistentDataHolder) opener).gems$getPersistentData(), target.getUUID()));
        offered.removeIf(id -> ModPassives.get(id) == null);
        if (offered.isEmpty() && getStolenPassives(opener).isEmpty()) {
            opener.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.no_passives").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Snapshot for the session so the target can switch gems without affecting this selection.
        CompoundTag data = ((GemsPersistentDataHolder) opener).gems$getPersistentData();
        data.putString(KEY_LAST_TARGET_NAME, targetName);
        GemsNbt.putUuid(data, KEY_LAST_TARGET_UUID, target.getUUID());
        ListTag list = new ListTag();
        for (Identifier id : offered) {
            list.add(StringTag.valueOf(id.toString()));
        }
        data.put(KEY_LAST_OFFERED, list);

        sendScreenPayload(opener, targetName, offered);
    }

    private static void sendScreenPayload(ServerPlayer opener, String targetName, Set<Identifier> offered) {
        Set<Identifier> stolen = getStolenPassives(opener);
        Set<Identifier> disabled = getDisabledPassives(opener);
        List<TrophyNecklaceScreenPayload.OfferedEntry> entries = new java.util.ArrayList<>(offered.size());
        for (Identifier id : offered.stream().sorted().toList()) {
            GemPassive passive = ModPassives.get(id);
            if (passive == null) continue;
            entries.add(new TrophyNecklaceScreenPayload.OfferedEntry(
                    id,
                    passive.name(),
                    passive.description(),
                    stolen.contains(id)
            ));
        }
        List<TrophyNecklaceScreenPayload.StolenEntry> stolenEntries = new java.util.ArrayList<>(stolen.size());
        for (Identifier id : stolen.stream().sorted().toList()) {
            GemPassive passive = ModPassives.get(id);
            if (passive == null) continue;
            stolenEntries.add(new TrophyNecklaceScreenPayload.StolenEntry(
                    id,
                    passive.name(),
                    passive.description(),
                    !disabled.contains(id)
            ));
        }
        ServerTrophyNecklaceNetworking.setSession(opener, offered);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(opener,
                new TrophyNecklaceScreenPayload(targetName, entries, stolenEntries, maxStolenPassives()));
    }

    public static void restoreStolenOnKill(ServerPlayer killer, ServerPlayer thief) {
        if (killer == null || thief == null) {
            return;
        }
        CompoundTag thiefData = ((GemsPersistentDataHolder) thief).gems$getPersistentData();
        CompoundTag stolenFrom = thiefData.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.keySet().isEmpty()) {
            return;
        }

        java.util.List<Identifier> recovered = new java.util.ArrayList<>();
        for (String key : java.util.List.copyOf(stolenFrom.keySet())) {
            java.util.UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source == null || !source.equals(killer.getUUID())) {
                continue;
            }
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                recovered.add(id);
            }
            stolenFrom.remove(key);
        }

        if (stolenFrom.isEmpty()) {
            thiefData.remove(KEY_STOLEN_FROM);
        } else {
            thiefData.put(KEY_STOLEN_FROM, stolenFrom);
        }

        if (recovered.isEmpty()) {
            return;
        }

        for (Identifier id : recovered) {
            removeStolenPassiveSilent(thiefData, id);
            clearLostPassive(killer, id);
        }

        GemPowers.sync(killer);
        GemPowers.sync(thief);
        com.feel.gems.net.StolenStateSync.send(thief);
    }

    public static void restoreStolenFromThief(ServerPlayer thief) {
        if (thief == null) {
            return;
        }
        var server = thief.level().getServer();
        if (server == null) {
            return;
        }

        CompoundTag thiefData = ((GemsPersistentDataHolder) thief).gems$getPersistentData();
        CompoundTag stolenFrom = thiefData.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.keySet().isEmpty()) {
            // Still clear any stolen passives that were not tied to a source.
            for (Identifier id : getStolenPassives(thief)) {
                removeStolenPassiveSilent(thiefData, id);
            }
            com.feel.gems.net.StolenStateSync.send(thief);
            GemPowers.sync(thief);
            return;
        }

        for (String key : java.util.List.copyOf(stolenFrom.keySet())) {
            Identifier id = Identifier.tryParse(key);
            UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (id != null) {
                removeStolenPassiveSilent(thiefData, id);
            }
            if (source != null) {
                ServerPlayer victim = server.getPlayerList().getPlayer(source);
                if (victim != null && id != null) {
                    clearLostPassive(victim, id);
                    GemPowers.sync(victim);
                }
            }
        }

        com.feel.gems.net.StolenStateSync.send(thief);
        GemPowers.sync(thief);
    }

    private static void removeStolenSource(CompoundTag data, Identifier passiveId) {
        CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null) {
            return;
        }
        stolenFrom.remove(passiveId.toString());
        if (stolenFrom.isEmpty()) {
            data.remove(KEY_STOLEN_FROM);
        } else {
            data.put(KEY_STOLEN_FROM, stolenFrom);
        }
    }

    private static void removeStolenPassiveSilent(CompoundTag data, Identifier passiveId) {
        ListTag list = data.getList(KEY_STOLEN_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return;
        }
        String raw = passiveId.toString();
        ListTag next = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getStringOr(i, "");
            if (!raw.equals(entry)) {
                next.add(StringTag.valueOf(entry));
            }
        }
        if (next.isEmpty()) {
            data.remove(KEY_STOLEN_PASSIVES);
        } else {
            data.put(KEY_STOLEN_PASSIVES, next);
        }
        removeStolenSource(data, passiveId);
        removeDisabledPassive(data, passiveId);
    }

    private static void addStolenPassiveForced(ServerPlayer player, Identifier passiveId) {
        if (player == null || passiveId == null || ModPassives.get(passiveId) == null) {
            return;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Set<Identifier> current = getStolenPassives(player);
        if (current.contains(passiveId)) {
            return;
        }
        ListTag list = new ListTag();
        for (Identifier id : current) {
            list.add(StringTag.valueOf(id.toString()));
        }
        list.add(StringTag.valueOf(passiveId.toString()));
        data.put(KEY_STOLEN_PASSIVES, list);
        CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(new CompoundTag());
        stolenFrom.put(passiveId.toString(), GemsNbt.fromUuid(player.getUUID()));
        data.put(KEY_STOLEN_FROM, stolenFrom);
        removeDisabledPassive(data, passiveId);
    }

    public static boolean isPassiveStolenFrom(ServerPlayer player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag lost = data.getCompound(KEY_LOST_PASSIVES).orElse(null);
        return lost != null && lost.contains(passiveId.toString());
    }

    public static void refreshLostPassives(ServerPlayer player) {
        if (player == null) {
            return;
        }
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        UUID playerId = player.getUUID();
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag lost = data.getCompound(KEY_LOST_PASSIVES).orElse(new CompoundTag());
        boolean changed = false;

        for (ServerPlayer thief : server.getPlayerList().getPlayers()) {
            if (thief == null || thief == player) {
                continue;
            }
            CompoundTag thiefData = ((GemsPersistentDataHolder) thief).gems$getPersistentData();
            CompoundTag stolenFrom = thiefData.getCompound(KEY_STOLEN_FROM).orElse(null);
            if (stolenFrom == null || stolenFrom.keySet().isEmpty()) {
                continue;
            }
            Set<Identifier> disabled = getDisabledPassives(thief);
            for (String key : stolenFrom.keySet()) {
                UUID source = GemsNbt.toUuid(stolenFrom.get(key));
                if (source == null || !source.equals(playerId) || lost.contains(key)) {
                    continue;
                }
                Identifier id = Identifier.tryParse(key);
                if (id == null || disabled.contains(id)) {
                    continue;
                }
                if (!lost.contains(key)) {
                    lost.put(key, GemsNbt.fromUuid(thief.getUUID()));
                    changed = true;
                }
            }
        }

        if (changed) {
            data.put(KEY_LOST_PASSIVES, lost);
        }
    }

    private static void markKillSession(ServerPlayer killer, ServerPlayer victim) {
        if (killer == null || victim == null) {
            return;
        }
        CompoundTag data = ((GemsPersistentDataHolder) killer).gems$getPersistentData();
        GemsNbt.putUuid(data, KEY_LAST_KILL_TARGET_UUID, victim.getUUID());
        data.putBoolean(KEY_LAST_KILL_USED, false);
    }

    private static void markKillUsed(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_LAST_KILL_USED, true);
    }

    private static boolean canClaimFromKill(ServerPlayer player, UUID targetUuid) {
        if (player == null || targetUuid == null) {
            return false;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID lastKill = GemsNbt.getUuid(data, KEY_LAST_KILL_TARGET_UUID);
        if (lastKill == null || !lastKill.equals(targetUuid)) {
            return false;
        }
        return !data.getBoolean(KEY_LAST_KILL_USED).orElse(false);
    }

    private static void markLostPassive(ServerPlayer victim, Identifier passiveId, UUID thiefUuid) {
        if (victim == null || passiveId == null || thiefUuid == null) {
            return;
        }
        CompoundTag data = ((GemsPersistentDataHolder) victim).gems$getPersistentData();
        CompoundTag lost = data.getCompound(KEY_LOST_PASSIVES).orElse(new CompoundTag());
        lost.put(passiveId.toString(), GemsNbt.fromUuid(thiefUuid));
        data.put(KEY_LOST_PASSIVES, lost);
    }

    private static void clearLostPassive(ServerPlayer victim, Identifier passiveId) {
        if (victim == null || passiveId == null) {
            return;
        }
        CompoundTag data = ((GemsPersistentDataHolder) victim).gems$getPersistentData();
        CompoundTag lost = data.getCompound(KEY_LOST_PASSIVES).orElse(null);
        if (lost == null) {
            return;
        }
        lost.remove(passiveId.toString());
        if (lost.isEmpty()) {
            data.remove(KEY_LOST_PASSIVES);
        } else {
            data.put(KEY_LOST_PASSIVES, lost);
        }
    }

    private static Identifier findStolenFromSource(CompoundTag data, UUID sourceUuid) {
        if (data == null || sourceUuid == null) {
            return null;
        }
        CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.keySet().isEmpty()) {
            return null;
        }
        for (String key : stolenFrom.keySet()) {
            UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source != null && source.equals(sourceUuid)) {
                return Identifier.tryParse(key);
            }
        }
        return null;
    }

    private static Set<Identifier> stolenFromTarget(CompoundTag data, UUID targetUuid) {
        if (data == null || targetUuid == null) {
            return Set.of();
        }
        CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.keySet().isEmpty()) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (String key : stolenFrom.keySet()) {
            UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source != null && source.equals(targetUuid)) {
                Identifier id = Identifier.tryParse(key);
                if (id != null) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private static UUID findSourceForPassive(CompoundTag data, Identifier passiveId) {
        if (data == null || passiveId == null) {
            return null;
        }
        CompoundTag stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null) {
            return null;
        }
        return GemsNbt.toUuid(stolenFrom.get(passiveId.toString()));
    }

    private static void addDisabledPassive(CompoundTag data, Identifier passiveId) {
        if (data == null || passiveId == null) {
            return;
        }
        ListTag list = data.getList(KEY_DISABLED_PASSIVES).orElse(new ListTag());
        String raw = passiveId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getStringOr(i, ""))) {
                data.put(KEY_DISABLED_PASSIVES, list);
                return;
            }
        }
        list.add(StringTag.valueOf(raw));
        data.put(KEY_DISABLED_PASSIVES, list);
    }

    private static void removeDisabledPassive(CompoundTag data, Identifier passiveId) {
        if (data == null || passiveId == null) {
            return;
        }
        ListTag list = data.getList(KEY_DISABLED_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return;
        }
        String raw = passiveId.toString();
        ListTag next = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getStringOr(i, "");
            if (!raw.equals(entry)) {
                next.add(StringTag.valueOf(entry));
            }
        }
        if (next.isEmpty()) {
            data.remove(KEY_DISABLED_PASSIVES);
        } else {
            data.put(KEY_DISABLED_PASSIVES, next);
        }
    }
}
