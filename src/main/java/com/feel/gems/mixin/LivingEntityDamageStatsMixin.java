package com.feel.gems.mixin;

import com.feel.gems.stats.GemsStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageStatsMixin {
    @Unique
    private float gems$damageBefore;
    @Unique
    private ServerPlayer gems$damageAttacker;

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void gems$captureDamageSource(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        if (attacker instanceof ServerPlayer player && player != self && amount > 0.0F) {
            gems$damageAttacker = player;
            gems$damageBefore = self.getHealth();
        } else {
            gems$damageAttacker = null;
            gems$damageBefore = 0.0F;
        }
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void gems$recordDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && gems$damageAttacker != null) {
            LivingEntity self = (LivingEntity) (Object) this;
            float delta = gems$damageBefore - self.getHealth();
            if (delta > 0.0F) {
                GemsStats.recordDamageDealt(gems$damageAttacker, delta);
            }
        }
        gems$damageAttacker = null;
        gems$damageBefore = 0.0F;
    }
}
