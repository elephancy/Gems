package com.feel.gems.mixin;

import com.feel.gems.power.ability.bonus.BonusCorpseExplosionAbility;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCorpseTrackerMixin {
    @Inject(method = "die", at = @At("TAIL"))
    private void gems$recordCorpse(DamageSource source, CallbackInfo ci) {
        BonusCorpseExplosionAbility.recordCorpse((LivingEntity) (Object) this);
    }
}
