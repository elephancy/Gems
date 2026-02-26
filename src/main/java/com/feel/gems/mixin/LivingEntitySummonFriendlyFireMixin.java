package com.feel.gems.mixin;

import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(LivingEntity.class)
public abstract class LivingEntitySummonFriendlyFireMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void gems$preventSummonFriendlyFire(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Mob target)) {
            return;
        }
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Mob attacker)) {
            return;
        }

        UUID targetOwner = ownerFor(target);
        if (targetOwner == null) {
            return;
        }
        UUID attackerOwner = ownerFor(attacker);
        if (attackerOwner == null) {
            return;
        }
        if (!targetOwner.equals(attackerOwner)) {
            return;
        }
        cir.setReturnValue(false);
    }

    private static UUID ownerFor(Mob mob) {
        if (SummonerSummons.isSummon(mob)) {
            return SummonerSummons.ownerUuid(mob);
        }
        if (HypnoControl.isHypno(mob)) {
            return HypnoControl.ownerUuid(mob);
        }
        return null;
    }
}
