package com.feel.gems.power.gem.terror;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;


public final class TerrorBloodPrice {
    private TerrorBloodPrice() {
    }

    public static void onPlayerKill(ServerPlayer killer) {
        int duration = GemsBalance.v().terror().bloodPriceDurationTicks();
        if (duration <= 0) {
            return;
        }
        int strengthAmp = GemsBalance.v().terror().bloodPriceStrengthAmplifier();
        int resistanceAmp = GemsBalance.v().terror().bloodPriceResistanceAmplifier();

        if (strengthAmp >= 0) {
            killer.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, strengthAmp, true, false, false));
        }
        if (resistanceAmp >= 0) {
            killer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, resistanceAmp, true, false, false));
        }
    }
}

