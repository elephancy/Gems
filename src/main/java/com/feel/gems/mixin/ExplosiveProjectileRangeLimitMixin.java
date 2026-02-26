package com.feel.gems.mixin;

import com.feel.gems.power.util.RangeLimitedProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractHurtingProjectile.class)
public abstract class ExplosiveProjectileRangeLimitMixin implements RangeLimitedProjectile {
    @Unique
    private Vec3 gems$origin;

    @Unique
    private double gems$maxDistSq;

    @Unique
    private boolean gems$limitSet;

    @Override
    public void gems$setRangeLimit(Vec3 origin, double maxDistanceBlocks) {
        this.gems$origin = origin;
        this.gems$maxDistSq = Math.max(0.0D, maxDistanceBlocks) * Math.max(0.0D, maxDistanceBlocks);
        this.gems$limitSet = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void gems$rangeLimitTick(CallbackInfo ci) {
        if (!gems$limitSet || gems$origin == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self.position().distanceToSqr(gems$origin) > gems$maxDistSq) {
            self.discard();
        }
    }
}
