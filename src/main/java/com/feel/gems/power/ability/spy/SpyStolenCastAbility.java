package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.feel.gems.power.gem.spy.SpySystem;


public final class SpyStolenCastAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_STOLEN_CAST;
    }

    @Override
    public String name() {
        return "Stolen Cast";
    }

    @Override
    public String description() {
        return "Casts your selected stolen ability.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().stolenCastCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            boolean ok = SpySystem.cycleStolen(player);
            if (!ok) {
                player.sendSystemMessage(Component.translatable("gems.ability.spy.stolen_cast.no_stolen"), true);
            }
            return ok;
        }

        Identifier id = SpySystem.selectedStolenCastAbility(player);
        if (id == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.spy.stolen_cast.no_stolen"), true);
            return false;
        }
        if (GemsDisables.isAbilityDisabled(id)) {
            player.sendSystemMessage(Component.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }
        GemAbility ability = ModAbilities.get(id);
        if (ability == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.spy.stolen_cast.not_found", id.toString()), true);
            return false;
        }
        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendSystemMessage(Component.translatable("gems.ability.spy.stolen_cast.failed"), true);
        }
        return ok;
    }
}
