package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityImpactAbsorbMixin {
    @Unique
    private boolean gems$impactAbsorbTracking;

    @Unique
    private float gems$impactAbsorbPendingGain;

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void gems$trackImpactAbsorbBefore(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        if (amount <= 0.0F) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_IMPACT_ABSORB)) {
            return;
        }
        gems$impactAbsorbTracking = true;
        float percent = GemsBalance.v().bonusPool().impactAbsorbPercent / 100.0F;
        gems$impactAbsorbPendingGain = Math.max(0.0F, amount * percent);
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("TAIL"))
    private void gems$applyImpactAbsorb(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            gems$impactAbsorbTracking = false;
            gems$impactAbsorbPendingGain = 0.0F;
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            gems$impactAbsorbTracking = false;
            gems$impactAbsorbPendingGain = 0.0F;
            return;
        }
        if (!gems$impactAbsorbTracking) {
            return;
        }
        gems$impactAbsorbTracking = false;
        float gain = gems$impactAbsorbPendingGain;
        gems$impactAbsorbPendingGain = 0.0F;
        if (gain <= 0.0F) {
            return;
        }
        
        // Use StatusEffects.ABSORPTION instead of setAbsorptionAmount()
        // The absorption effect grants 4 absorption hearts per amplifier level (0 = 4 hearts, 1 = 8 hearts, etc.)
        // We want to grant `gain` hearts worth of absorption (where 1 heart = 2 HP)
        float maxAbsorb = GemsBalance.v().bonusPool().impactAbsorbMaxAbsorption;
        float targetAbsorb = Math.min(player.getAbsorptionAmount() + gain, maxAbsorb);
        
        // Calculate amplifier needed: absorption HP / 4 - 1 (since level 0 = 4 hearts = 8 HP)
        // But since we're adding to existing, we need the total target
        int amplifier = Math.max(0, (int) Math.ceil(targetAbsorb / 4.0F) - 1);
        
        // Short duration since we're applying it immediately after damage
        // 60 ticks = 3 seconds should be enough for the absorption to persist
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, amplifier, true, false, false));
    }
}
