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


public final class RichRushAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.RICH_RUSH;
    }

    @Override
    public String name() {
        return "Rich Rush";
    }

    @Override
    public String description() {
        return "Temporarily increases ore and mob drops.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().richRushCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.WEALTH, GemsBalance.v().wealth().richRushDurationTicks());
        AbilityRuntime.startRichRush(player, duration);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.1F);
        AbilityFeedback.burst(player, ParticleTypes.HAPPY_VILLAGER, 14, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.wealth.rich_rush.active"), true);
        return true;
    }
}
