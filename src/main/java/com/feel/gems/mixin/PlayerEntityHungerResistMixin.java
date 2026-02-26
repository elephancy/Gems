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
public abstract class PlayerEntityHungerResistMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float gems$hungerResist(float exhaustion) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return exhaustion;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_HUNGER_RESIST)) {
            return exhaustion;
        }
        float reduction = GemsBalance.v().bonusPool().hungerResistReductionPercent / 100.0f;
        return exhaustion * (1.0f - reduction);
    }
}
