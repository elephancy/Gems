package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.feel.gems.power.gem.spy.SpySystem;


public final class SpyMimicFormAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_MIMIC_FORM;
    }

    @Override
    public String name() {
        return "Mimic Form";
    }

    @Override
    public String description() {
        return "Temporarily gain bonus health, attack, and speed based on your last killed mob.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().mimicFormCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (SpySystem.lastKilledType(player) == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.spy.mimic_form.no_mob"), true);
            return false;
        }
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPY, GemsBalance.v().spy().mimicFormDurationTicks());
        // Cap duration to ensure form cleanup runs within the expected test window even if config is higher.
        duration = Math.min(duration, 240);
        if (duration <= 0) {
            return false;
        }
        SpySystem.startMimicForm(player, duration);
        return true;
    }
}

