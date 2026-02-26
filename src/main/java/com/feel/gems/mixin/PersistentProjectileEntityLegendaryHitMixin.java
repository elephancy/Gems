package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryTargeting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityLegendaryHitMixin {
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void gems$recordHit(EntityHitResult hitResult, CallbackInfo ci) {
        Entity owner = ((AbstractArrow) (Object) this).getOwner();
        if (!(owner instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }
        LegendaryTargeting.recordHit(attacker, target);
    }
}
