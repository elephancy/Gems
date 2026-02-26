package com.feel.gems.power.gem.terror;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;

public final class TerrorRemoteChargeRuntime {
    private static final String KEY_ARM_UNTIL = "terrorRemoteArmUntil";
    private static final String KEY_CHARGE_UNTIL = "terrorRemoteChargeUntil";
    private static final String KEY_CHARGE_POS = "terrorRemoteChargePos";
    private static final String KEY_CHARGE_DIM = "terrorRemoteChargeDim";
    private static final String KEY_COOLDOWN_UNTIL = "terrorRemoteCooldownUntil";

    private TerrorRemoteChargeRuntime() {
    }

    public static boolean startArming(ServerPlayer player) {
        int window = GemsBalance.v().terror().remoteChargeArmWindowTicks();
        if (window <= 0) {
            return false;
        }
        CompoundTag nbt = persistent(player);
        clearExpired(player, nbt);
        // Skip cooldown check if admin has no-cooldowns enabled
        if (!GemsAdmin.noCooldowns(player) && isOnCooldown(player, nbt)) {
            return false;
        }
        if (hasActiveCharge(player, nbt)) {
            return false;
        }
        nbt.putLong(KEY_ARM_UNTIL, GemsTime.now(player) + window);
        return true;
    }

    public static boolean tryArm(ServerPlayer player, BlockPos pos) {
        CompoundTag nbt = persistent(player);
        clearExpired(player, nbt);
        if (!isArming(player, nbt)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel playerWorld)) {
            return false;
        }
        if (playerWorld.getBlockState(pos).isAir()) {
            return false;
        }
        int detonateWindow = GemsBalance.v().terror().remoteChargeDetonateWindowTicks();
        if (detonateWindow <= 0) {
            return false;
        }
        nbt.putLong(KEY_CHARGE_UNTIL, GemsTime.now(player) + detonateWindow);
        nbt.putLong(KEY_CHARGE_POS, pos.asLong());
        nbt.putString(KEY_CHARGE_DIM, playerWorld.dimension().identifier().toString());
        nbt.remove(KEY_ARM_UNTIL);
        return true;
    }

    public static boolean detonate(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        clearExpired(player, nbt);
        if (!hasActiveCharge(player, nbt)) {
            return false;
        }
        BlockPos pos = readChargePos(nbt);
        String dimRaw = nbt.getStringOr(KEY_CHARGE_DIM, "");
        if (pos == null || dimRaw == null || dimRaw.isBlank()) {
            clearCharge(nbt);
            return false;
        }
        Identifier dimId = Identifier.tryParse(dimRaw);
        if (dimId == null) {
            clearCharge(nbt);
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            clearCharge(nbt);
            return false;
        }
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
        ServerLevel world = server.getLevel(key);
        if (world == null) {
            clearCharge(nbt);
            return false;
        }
        int fuse = Math.max(1, GemsBalance.v().terror().remoteChargeFuseTicks());
        PrimedTnt tnt = new PrimedTnt(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player);
        tnt.setFuse(fuse);
        world.addFreshEntity(tnt);
        clearCharge(nbt);
        startCooldown(player, nbt);
        return true;
    }

    public static boolean isArming(ServerPlayer player) {
        return isArming(player, persistent(player));
    }

    public static boolean hasActiveCharge(ServerPlayer player) {
        return hasActiveCharge(player, persistent(player));
    }

    public static boolean isOnCooldown(ServerPlayer player) {
        return isOnCooldown(player, persistent(player));
    }

    private static boolean isArming(ServerPlayer player, CompoundTag nbt) {
        long until = nbt.getLongOr(KEY_ARM_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_ARM_UNTIL);
            return false;
        }
        return true;
    }

    private static boolean hasActiveCharge(ServerPlayer player, CompoundTag nbt) {
        long until = nbt.getLongOr(KEY_CHARGE_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            clearCharge(nbt);
            return false;
        }
        if (!nbt.contains(KEY_CHARGE_POS)) {
            clearCharge(nbt);
            return false;
        }
        if (!nbt.contains(KEY_CHARGE_DIM)) {
            clearCharge(nbt);
            return false;
        }
        return true;
    }

    private static boolean isOnCooldown(ServerPlayer player, CompoundTag nbt) {
        long until = nbt.getLongOr(KEY_COOLDOWN_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_COOLDOWN_UNTIL);
            return false;
        }
        return true;
    }

    private static void clearExpired(ServerPlayer player, CompoundTag nbt) {
        long now = GemsTime.now(player);
        long armUntil = nbt.getLongOr(KEY_ARM_UNTIL, 0L);
        if (armUntil > 0 && now >= armUntil) {
            nbt.remove(KEY_ARM_UNTIL);
        }
        long chargeUntil = nbt.getLongOr(KEY_CHARGE_UNTIL, 0L);
        if (chargeUntil > 0 && now >= chargeUntil) {
            clearCharge(nbt);
        }
        long cooldownUntil = nbt.getLongOr(KEY_COOLDOWN_UNTIL, 0L);
        if (cooldownUntil > 0 && now >= cooldownUntil) {
            nbt.remove(KEY_COOLDOWN_UNTIL);
        }
    }

    private static void clearCharge(CompoundTag nbt) {
        nbt.remove(KEY_CHARGE_UNTIL);
        nbt.remove(KEY_CHARGE_POS);
        nbt.remove(KEY_CHARGE_DIM);
    }

    private static void startCooldown(ServerPlayer player, CompoundTag nbt) {
        // Skip cooldown if admin has no-cooldowns enabled
        if (GemsAdmin.noCooldowns(player)) {
            return;
        }
        int cooldown = GemsBalance.v().terror().remoteChargeCooldownTicks();
        if (cooldown > 0) {
            cooldown = applyCooldownModifiers(player, cooldown);
            long until = GemsTime.now(player) + cooldown;
            nbt.putLong(KEY_COOLDOWN_UNTIL, until);
            GemAbilityCooldowns.setNextAllowedTick(player, PowerIds.TERROR_REMOTE_CHARGE, until);
            GemCooldownSync.send(player);
        }
    }

    private static int applyCooldownModifiers(ServerPlayer player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player) * LegendaryCooldowns.getCooldownMultiplier(player);
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }

    private static BlockPos readChargePos(CompoundTag nbt) {
        long packed = nbt.getLongOr(KEY_CHARGE_POS, Long.MIN_VALUE);
        if (packed == Long.MIN_VALUE) {
            return null;
        }
        return BlockPos.of(packed);
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
