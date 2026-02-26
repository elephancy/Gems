package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;




public final class SupremeSetRuntime {
    private static final int EFFECT_REFRESH_TICKS = 60;

    private SupremeSetRuntime() {
    }

    public static void tick(ServerPlayer player) {
        boolean helmet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SUPREME_HELMET);
        boolean chest = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SUPREME_CHESTPLATE);
        boolean legs = player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SUPREME_LEGGINGS);
        boolean boots = player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SUPREME_BOOTS);

        var cfg = GemsBalance.v().legendary();
        maintainEffect(player, MobEffects.NIGHT_VISION, cfg.supremeHelmetNightVisionAmplifier(), helmet);
        maintainEffect(player, MobEffects.WATER_BREATHING, cfg.supremeHelmetWaterBreathingAmplifier(), helmet);
        maintainEffect(player, MobEffects.STRENGTH, cfg.supremeChestStrengthAmplifier(), chest);
        maintainEffect(player, MobEffects.FIRE_RESISTANCE, cfg.supremeLeggingsFireResAmplifier(), legs);
        maintainEffect(player, MobEffects.SPEED, cfg.supremeBootsSpeedAmplifier(), boots);

        boolean full = helmet && chest && legs && boots;
        maintainEffect(player, MobEffects.RESISTANCE, cfg.supremeSetResistanceAmplifier(), full);
    }

    private static void maintainEffect(ServerPlayer player, Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier, boolean shouldHave) {
        if (amplifier < 0) {
            return;
        }
        if (shouldHave) {
            applyIfMissingOrWeaker(player, effect, amplifier);
        }
    }

    private static void applyIfMissingOrWeaker(ServerPlayer player, Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier) {
            return;
        }
        if (current != null && current.getAmplifier() == amplifier && current.isInfiniteDuration()) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_REFRESH_TICKS, amplifier, true, false, false));
    }
}
