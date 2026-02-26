package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;



public final class PillagerVolleyAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_VOLLEY;
    }

    @Override
    public String name() {
        return "Volley";
    }

    @Override
    public String description() {
        return "Automatically fires a short burst of arrows.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().volleyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.PILLAGER, GemsBalance.v().pillager().volleyDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.pillager.volley.disabled"), true);
            return false;
        }
        PillagerVolleyRuntime.start(player, duration);
        return true;
    }
}

