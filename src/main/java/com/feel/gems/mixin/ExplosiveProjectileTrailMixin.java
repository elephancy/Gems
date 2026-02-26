package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractHurtingProjectile.class)
public abstract class ExplosiveProjectileTrailMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$trail(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }
        if (self.entityTags().contains("gems_meteor")) {
            Vec3 pos = self.position();
            AbilityFeedback.burstAt(world, pos, ParticleTypes.FLAME, 1, 0.05D);
            AbilityFeedback.burstAt(world, pos, ParticleTypes.SMOKE, 1, 0.05D);
        }
        if (self.entityTags().contains("gems_doom_bolt")) {
            Vec3 pos = self.position();
            AbilityFeedback.burstAt(world, pos, ParticleTypes.SMOKE, 2, 0.02D);
            AbilityFeedback.burstAt(world, pos, ParticleTypes.SOUL, 1, 0.02D);
        }
    }
}
