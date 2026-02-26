package com.feel.gems.power.gem.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;


public final class PillagerDiscipline {
    private static final String KEY_NEXT = "pillagerDisciplineNext";

    private PillagerDiscipline() {
    }

    public static void tick(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.PILLAGER_ILLAGER_DISCIPLINE)) {
            return;
        }
        long now = GemsTime.now(player);
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long next = nbt.getLongOr(KEY_NEXT, 0L);
        if (next > now) {
            return;
        }

        float thresholdHearts = GemsBalance.v().pillager().illagerDisciplineThresholdHearts();
        float thresholdHealth = thresholdHearts * 2.0F;
        if (player.getHealth() > thresholdHealth) {
            return;
        }

        int duration = GemsBalance.v().pillager().illagerDisciplineResistanceDurationTicks();
        int amp = GemsBalance.v().pillager().illagerDisciplineResistanceAmplifier();
        int cooldown = GemsBalance.v().pillager().illagerDisciplineCooldownTicks();
        if (duration <= 0 || cooldown <= 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amp, true, false, false));
        nbt.putLong(KEY_NEXT, now + cooldown);
        AbilityFeedback.sound(player, SoundEvents.PILLAGER_CELEBRATE, 0.5F, 0.9F);
    }
}

