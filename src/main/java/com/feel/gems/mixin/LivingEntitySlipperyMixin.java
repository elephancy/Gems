package com.feel.gems.mixin;

import com.feel.gems.power.gem.trickster.TricksterPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySlipperyMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void gems$slipperyIgnoreSlow(MobEffectInstance effect, net.minecraft.world.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        if (effect.getEffect() == MobEffects.POISON
                && GemPowers.isPassiveActive(player, PowerIds.BONUS_POISON_IMMUNITY)) {
            cir.setReturnValue(false);
            return;
        }
        if (effect.getEffect() == MobEffects.SLOWNESS && TricksterPassiveRuntime.shouldIgnoreSlow(player)) {
            cir.setReturnValue(false);
        }
    }
}
