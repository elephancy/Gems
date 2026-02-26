package com.feel.gems.mixin;

import com.feel.gems.power.ability.hunter.HunterNetShotAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




/**
 * Prevents elytra activation when player is netted by Hunter's Net Shot.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraNetMixin {
    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void gems$blockElytraWhenNetted(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player && HunterNetShotAbility.isNetted(player)) {
            cir.setReturnValue(false);
        }
    }
}
