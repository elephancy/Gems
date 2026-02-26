package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityVoidImmunityStatusEffectMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void gems$blockAmbientEffectsOnVoid(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer targetPlayer)) {
            return;
        }
        if (!VoidImmunity.hasImmunity(targetPlayer)) {
            return;
        }
        if (!GemsBalance.v().voidGem().blockAllStatusEffects()) {
            return;
        }
        // Void should be immune to all gem-driven effects, and many abilities/passives apply effects without a source.
        // We conservatively block all status effects while Void immunity is active.
        cir.setReturnValue(false);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void gems$blockPlayerAppliedEffectsOnVoid(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer targetPlayer)) {
            return;
        }
        if (!VoidImmunity.hasImmunity(targetPlayer)) {
            return;
        }
        if (!GemsBalance.v().voidGem().blockAllStatusEffects()) {
            return;
        }

        ServerPlayer sourcePlayer = null;
        if (source instanceof ServerPlayer sp) {
            sourcePlayer = sp;
        } else if (source instanceof Projectile projectile) {
            if (projectile.getOwner() instanceof ServerPlayer ownerPlayer) {
                sourcePlayer = ownerPlayer;
            }
        } else if (source instanceof AreaEffectCloud cloud) {
            if (cloud.getOwner() instanceof ServerPlayer ownerPlayer) {
                sourcePlayer = ownerPlayer;
            }
        }

        // If we can attribute the effect to a player (directly or via owned entities),
        // block it unless it is self-applied.
        if (sourcePlayer != null && sourcePlayer == targetPlayer) {
            return;
        }
        cir.setReturnValue(false);
    }
}
