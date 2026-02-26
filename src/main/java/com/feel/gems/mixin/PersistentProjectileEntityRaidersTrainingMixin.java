package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityRaidersTrainingMixin {
    @Unique
    private boolean gems$scaled = false;

    @Inject(method = "shoot(DDDFF)V", at = @At("RETURN"))
    private void gems$scaleAfterSetVelocity(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        if (gems$scaled) {
            return;
        }
        AbstractArrow self = (AbstractArrow) (Object) this;
        Entity owner = self.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.PILLAGER_RAIDERS_TRAINING)) {
            return;
        }
        float mult = GemsBalance.v().pillager().raidersTrainingProjectileVelocityMultiplier();
        if (mult <= 1.0F) {
            return;
        }
        var vel = self.getDeltaMovement();
        if (vel.lengthSqr() <= 1.0E-8D) {
            return;
        }
        gems$scaled = true;
        self.setDeltaMovement(vel.scale(mult));
    }
}
