package com.feel.gems.mixin;

import com.feel.gems.power.gem.life.HeartLockRuntime;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Mob.class)
public abstract class MobEntityHeartLockMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void gems$tickHeartLock(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        HeartLockRuntime.tick(self);
    }
}
