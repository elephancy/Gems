package com.feel.gems.power.gem.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;


/**
 * Stores Reaper "Blood Charge" charging state and one-shot next-hit multiplier.
 */
public final class ReaperBloodCharge {
    public static final String KEY_CHARGING_UNTIL = "reaperBloodChargeChargingUntil";
    public static final String KEY_CHARGED_TICKS = "reaperBloodChargeChargedTicks";
    public static final String KEY_BUFF_UNTIL = "reaperBloodChargeBuffUntil";
    public static final String KEY_BUFF_MULT = "reaperBloodChargeBuffMultiplier";

    private ReaperBloodCharge() {
    }

    public static boolean isCharging(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getLongOr(KEY_CHARGING_UNTIL, 0L) > 0L;
    }

    public static float consumeMultiplierIfActive(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLongOr(KEY_BUFF_UNTIL, 0L);
        if (until <= 0) {
            return 1.0F;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            clearBuff(nbt);
            return 1.0F;
        }
        float mult = nbt.getFloatOr(KEY_BUFF_MULT, 1.0F);
        clearBuff(nbt);
        return mult <= 0.0F ? 1.0F : mult;
    }

    public static void clearCharging(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.remove(KEY_CHARGING_UNTIL);
        nbt.remove(KEY_CHARGED_TICKS);
    }

    public static void setBuff(ServerPlayer player, float multiplier, int durationTicks) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (durationTicks <= 0) {
            clearBuff(nbt);
            return;
        }
        nbt.putLong(KEY_BUFF_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putFloat(KEY_BUFF_MULT, multiplier);
    }

    public static float computeMultiplier(int chargedTicks, int maxChargeTicks) {
        float maxMult = GemsBalance.v().reaper().bloodChargeMaxMultiplier();
        if (maxChargeTicks <= 0) {
            return 1.0F;
        }
        float t = Math.min(chargedTicks, maxChargeTicks) / (float) maxChargeTicks;
        return 1.0F + t * (maxMult - 1.0F);
    }

    private static void clearBuff(CompoundTag nbt) {
        nbt.remove(KEY_BUFF_UNTIL);
        nbt.remove(KEY_BUFF_MULT);
    }
}
