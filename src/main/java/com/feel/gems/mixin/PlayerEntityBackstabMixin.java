package com.feel.gems.mixin;

import com.feel.gems.power.gem.spy.SpyBackstab;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Player.class)
public abstract class PlayerEntityBackstabMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$backstab(Entity target, CallbackInfo ci) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer attacker)) {
            return;
        }
        SpyBackstab.apply(attacker, living);
    }
}
