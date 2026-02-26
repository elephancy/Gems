package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityRestrictions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityRestrictionsMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrLockAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        if (AbilityRestrictions.isStunned(player)) {
            ci.cancel();
        }
    }
}
