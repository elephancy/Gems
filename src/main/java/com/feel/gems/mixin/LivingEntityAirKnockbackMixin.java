package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityAirKnockbackMixin {
    @ModifyVariable(method = "knockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double gems$airAerialGuardKnockback(double strength) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) {
            return strength;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return strength;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_AERIAL_GUARD)) {
            return strength;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return strength;
        }
        double mult = GemsBalance.v().air().aerialGuardKnockbackMultiplier();
        return strength * mult;
    }
}

