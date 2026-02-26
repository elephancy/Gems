package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;



public final class ReaperWitheringStrikesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_WITHERING_STRIKES;
    }

    @Override
    public String name() {
        return "Withering Strikes";
    }

    @Override
    public String description() {
        return "Temporarily causes your hits to apply Wither.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().witheringStrikesCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.REAPER, GemsBalance.v().reaper().witheringStrikesDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.withering_strikes.disabled"), true);
            return false;
        }
        AbilityRuntime.startReaperWitheringStrikes(player, duration);
        player.sendSystemMessage(Component.translatable("gems.ability.reaper.withering_strikes.active"), true);
        return true;
    }
}
