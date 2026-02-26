package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityChadStrengthMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$chadStrength(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer attacker)) {
            return;
        }
        if (!AbilityRuntime.isChadStrengthActive(attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        int hit = AbilityRuntime.incrementChadHit(attacker);
        int every = Math.max(1, GemsBalance.v().strength().chadEveryHits());
        if (hit % every != 0) {
            return;
        }
        living.hurtServer(attacker.level(), attacker.damageSources().playerAttack(attacker), GemsBalance.v().strength().chadBonusDamage());
    }
}
