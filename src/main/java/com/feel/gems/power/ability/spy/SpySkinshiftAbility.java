package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.Targeting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;


public final class SpySkinshiftAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_SKINSHIFT;
    }

    @Override
    public String name() {
        return "Skinshift";
    }

    @Override
    public String description() {
        return "Steal a targeted player's appearance for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().skinshiftCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().spy().skinshiftRangeBlocks();
        ServerPlayer other = Targeting.raycastPlayer(player, range);
        if (other == null || other == player) {
            player.sendSystemMessage(Component.translatable("gems.message.no_player_target"), true);
            return false;
        }
        if (!VoidImmunity.canBeTargeted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPY, GemsBalance.v().spy().skinshiftDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.spy.skinshift.disabled"), true);
            return false;
        }
        return SpySystem.startSkinshift(player, other, duration);
    }
}
