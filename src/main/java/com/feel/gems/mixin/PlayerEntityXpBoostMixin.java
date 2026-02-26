package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerEntityXpBoostMixin {
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int gems$xpBoost(int amount) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_XP_BOOST)) {
            return amount;
        }
        float boost = GemsBalance.v().bonusPool().xpBoostPercent / 100.0f;
        return Math.max(0, Math.round(amount * (1.0f + boost)));
    }
}
