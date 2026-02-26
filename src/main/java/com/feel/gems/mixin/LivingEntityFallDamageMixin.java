package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void gems$handleFallDamage(double fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (GemPowers.isPassiveActive(player, PowerIds.FALL_DAMAGE_IMMUNITY)
                || GemPowers.isPassiveActive(player, PowerIds.SPACE_LOW_GRAVITY)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float gems$airAerialGuardFallDamage(float damageMultiplier) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) {
            return damageMultiplier;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return damageMultiplier;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_AERIAL_GUARD)) {
            return damageMultiplier;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return damageMultiplier;
        }
        float mult = GemsBalance.v().air().aerialGuardFallDamageMultiplier();
        return damageMultiplier * mult;
    }
}
