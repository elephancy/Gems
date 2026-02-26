package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.power.util.Targeting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityHunterAimMixin {
    @Unique
    private Vec3 gems$aimDir = null;

    @Inject(method = "shoot(DDDFF)V", at = @At("HEAD"))
    private void gems$computeAim(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        gems$aimDir = null;
        AbstractArrow self = (AbstractArrow) (Object) this;
        Entity owner = self.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getMainHandItem().is(ModItems.HUNTERS_SIGHT_BOW) && !player.getOffhandItem().is(ModItems.HUNTERS_SIGHT_BOW)) {
            return;
        }
        float strength = GemsBalance.v().legendary().hunterAimAssistStrength();
        if (strength <= 0.0F) {
            return;
        }
        int range = GemsBalance.v().legendary().hunterAimRangeBlocks();
        int timeout = GemsBalance.v().legendary().hunterAimTimeoutTicks();
        LivingEntity target = LegendaryTargeting.findTarget(player, range, timeout);
        if (target == null) {
            target = Targeting.raycastLiving(player, range);
        }
        if (target == null) {
            return;
        }
        Vec3 targetPos = target.getEyePosition();
        Vec3 targetVel = target.getDeltaMovement();
        double baseSpeed = Math.sqrt(x * x + y * y + z * z);
        double flightSpeed = baseSpeed > 1.0E-4D ? baseSpeed : Math.max(0.01D, speed);
        double distance = player.getEyePosition().distanceTo(targetPos);
        double leadTime = distance / flightSpeed;
        leadTime = Math.min(1.5D, Math.max(0.0D, leadTime));
        Vec3 desired = targetPos.add(targetVel.scale(leadTime)).subtract(player.getEyePosition());
        if (desired.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 original = new Vec3(x, y, z);
        if (original.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 blended = original.normalize().lerp(desired.normalize(), strength).normalize();
        gems$aimDir = blended;
    }

    @Inject(method = "shoot(DDDFF)V", at = @At("RETURN"))
    private void gems$applyAim(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        if (gems$aimDir == null) {
            return;
        }
        AbstractArrow self = (AbstractArrow) (Object) this;
        Vec3 velocity = self.getDeltaMovement();
        double currentSpeed = velocity.length();
        if (currentSpeed <= 0.0D) {
            return;
        }
        self.setDeltaMovement(gems$aimDir.scale(currentSpeed));
        gems$aimDir = null;
    }
}
