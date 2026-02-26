package com.feel.gems.power.gem.beacon;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;


public final class BeaconSupportRuntime {
    private static final String KEY_CORE_NEXT = "beaconCoreNext";

    private BeaconSupportRuntime() {
    }

    public static void tickEverySecond(ServerPlayer player) {
        tickCore(player);
        tickStabilize(player);
    }

    public static void applyRally(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_RALLY)) {
            return;
        }
        int radius = GemsBalance.v().beacon().rallyRadiusBlocks();
        int duration = GemsBalance.v().beacon().rallyDurationTicks();
        int hearts = GemsBalance.v().beacon().rallyAbsorptionHearts();
        if (radius <= 0 || duration <= 0 || hearts <= 0) {
            return;
        }

        int amplifier = Math.max(0, (hearts / 2) - 1);
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (ServerPlayer other : world.getEntitiesOfClass(ServerPlayer.class, box, p -> true)) {
            if (other != player && VoidImmunity.shouldBlockEffect(player, other)) {
                continue;
            }
            if (other == player) {
                other.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, true, true, false));
                continue;
            }

            if (GemTrust.isTrusted(player, other)) {
                other.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, true, true, false));
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, true, false));
            }
        }
    }

    private static void tickCore(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_CORE)) {
            return;
        }
        int radius = GemsBalance.v().beacon().coreRadiusBlocks();
        int pulse = GemsBalance.v().beacon().corePulsePeriodTicks();
        int duration = GemsBalance.v().beacon().coreRegenDurationTicks();
        int amp = GemsBalance.v().beacon().coreRegenAmplifier();
        if (radius <= 0 || pulse <= 0 || duration <= 0) {
            return;
        }

        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long now = GemsTime.now(player);
        long next = nbt.getLongOr(KEY_CORE_NEXT, 0L);
        if (next > now) {
            return;
        }
        nbt.putLong(KEY_CORE_NEXT, now + pulse);

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (ServerPlayer other : world.getEntitiesOfClass(ServerPlayer.class, box, p -> true)) {
            if (other != player && VoidImmunity.shouldBlockEffect(player, other)) {
                continue;
            }
            if (GemTrust.isTrusted(player, other) || other == player) {
                other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amp, true, false, false));
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, true, false, false));
            }
        }
    }

    private static void tickStabilize(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_STABILIZE)) {
            return;
        }
        int radius = GemsBalance.v().beacon().stabilizeRadiusBlocks();
        int reduceTicks = GemsBalance.v().beacon().stabilizeReduceTicksPerSecond();
        if (radius <= 0 || reduceTicks <= 0) {
            return;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (ServerPlayer other : world.getEntitiesOfClass(ServerPlayer.class, box, p -> true)) {
            if (other != player && VoidImmunity.shouldBlockEffect(player, other)) {
                continue;
            }
            if (GemTrust.isTrusted(player, other) || other == player) {
                for (MobEffectInstance effect : java.util.List.copyOf(other.getActiveEffects())) {
                    if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
                        continue;
                    }
                    int remaining = effect.getDuration() - reduceTicks;
                    if (remaining <= 0) {
                        other.removeEffect(effect.getEffect());
                        continue;
                    }
                    // Vanilla refuses to replace a longer-duration effect with a shorter one, so explicitly
                    // remove then re-add to ensure the duration is reduced.
                    other.removeEffect(effect.getEffect());
                    other.addEffect(new MobEffectInstance(
                            effect.getEffect(),
                            remaining,
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon()
                    ));
                }
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, reduceTicks + 10, 0, true, false, false));
            }
        }
    }
}
