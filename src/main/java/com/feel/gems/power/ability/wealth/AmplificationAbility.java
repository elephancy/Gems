package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import com.feel.gems.power.gem.wealth.EnchantmentAmplification;


public final class AmplificationAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AMPLIFICATION;
    }

    @Override
    public String name() {
        return "Amplification";
    }

    @Override
    public String description() {
        return "Temporarily boosts enchantments on your tools and armor.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().amplificationCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.WEALTH, GemsBalance.v().wealth().amplificationDurationTicks());
        AbilityRuntime.startAmplification(player, duration);
        EnchantmentAmplification.apply(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.ENCHANT, 18, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.wealth.amplification.active"), true);
        return true;
    }
}
