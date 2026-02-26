package com.feel.gems.mixin;

import com.feel.gems.event.GemsPlayerDeath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void gems$stashGems(DamageSource source, CallbackInfo ci) {
        GemsPlayerDeath.onDeathHead((ServerPlayer) (Object) this, source);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void gems$onDeath(DamageSource source, CallbackInfo ci) {
        GemsPlayerDeath.onDeathTail((ServerPlayer) (Object) this, source);
    }

}
