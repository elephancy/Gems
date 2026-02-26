package com.feel.gems.power.passive;

import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.power.api.GemPassive;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;




public final class StatusEffectPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final Holder<MobEffect> effect;
    private final int amplifier;

    public StatusEffectPassive(Identifier id, String name, String description, Holder<MobEffect> effect, int amplifier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effect = effect;
        this.amplifier = amplifier;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayer player) {
        int bonus = AugmentRuntime.passiveAmplifierBonus(player, id);
        int amp = Math.max(0, amplifier + bonus);
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() == amp && current.isInfiniteDuration()) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amp, true, false, false));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(effect);
    }
}

