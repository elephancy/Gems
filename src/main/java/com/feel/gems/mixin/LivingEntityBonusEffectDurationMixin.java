package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBonusEffectDurationMixin {
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance gems$adjustDebuffDurations(MobEffectInstance effect) {
        if (effect == null) {
            return null;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return effect;
        }
        int duration = effect.getDuration();
        if (duration <= 0) {
            return effect;
        }
        var cfg = GemsBalance.v().bonusPool();

        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_CHAIN_BREAKER)
                && (effect.getEffect() == MobEffects.SLOWNESS || effect.getEffect() == MobEffects.MINING_FATIGUE)) {
            float reduction = cfg.chainBreakerDurationReductionPercent / 100.0f;
            duration = Math.max(1, Math.round(duration * (1.0f - reduction)));
        }

        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_QUICK_RECOVERY)
                && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            float reduction = cfg.quickRecoveryDebuffReductionPercent / 100.0f;
            duration = Math.max(1, Math.round(duration * (1.0f - reduction)));
        }

        if (duration == effect.getDuration()) {
            return effect;
        }
        return new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }
}
