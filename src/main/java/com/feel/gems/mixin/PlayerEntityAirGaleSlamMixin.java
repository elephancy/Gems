package com.feel.gems.mixin;

import com.feel.gems.power.gem.air.AirGaleSlamRuntime;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.feel.gems.config.GemsBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityAirGaleSlamMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$airGaleSlam(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        if (target instanceof LivingEntity living
                && GemPowers.isPassiveActive(player, PowerIds.AIR_WIND_SHEAR)
                && AirMacePassive.isHoldingMace(player)) {
            double knockback = GemsBalance.v().air().windShearKnockback();
            if (knockback > 0.0D) {
                Vec3 away = living.position().subtract(player.position()).normalize();
                living.push(away.x * knockback, 0.1D, away.z * knockback);
                living.needsSync = true;
            }
            int slowDuration = GemsBalance.v().air().windShearSlownessDurationTicks();
            if (slowDuration > 0) {
                int slowAmp = GemsBalance.v().air().windShearSlownessAmplifier();
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowDuration, slowAmp, true, false, false));
            }
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_WINDBURST_MACE)) {
            return;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return;
        }
        if (!AirGaleSlamRuntime.consumeIfActive(player)) {
            return;
        }
        AirGaleSlamRuntime.trigger(player, target);
    }
}

