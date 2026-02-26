package com.feel.gems.mixin;

import com.feel.gems.power.ability.bonus.BonusDoomBoltAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Projectile.class)
public abstract class ExplosiveProjectileDoomBoltMixin {
    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void gems$doomBoltCollision(HitResult hitResult, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!self.entityTags().contains("gems_doom_bolt")) {
            return;
        }
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }

        Entity owner = self.getOwner();
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;

        if (hitResult instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof LivingEntity living) {
                if (ownerPlayer != null) {
                    if (living == ownerPlayer) {
                        self.discard();
                        ci.cancel();
                        return;
                    }
                    if (living instanceof ServerPlayer targetPlayer) {
                        if (GemTrust.isTrusted(ownerPlayer, targetPlayer)) {
                            self.discard();
                            ci.cancel();
                            return;
                        }
                        if (!VoidImmunity.canBeTargeted(ownerPlayer, targetPlayer)) {
                            self.discard();
                            ci.cancel();
                            return;
                        }
                    }
                }
                float damage = BonusDoomBoltAbility.doomBoltDamage();
                if (owner != null) {
                    living.hurtServer(world, world.damageSources().indirectMagic(self, owner), damage);
                } else {
                    living.hurtServer(world, world.damageSources().magic(), damage);
                }
            }
        }

        AbilityFeedback.burstAt(world, self.position(), ParticleTypes.SOUL_FIRE_FLAME, 14, 0.2D);
        AbilityFeedback.soundAt(world, self.position(), SoundEvents.WITHER_SKELETON_HURT, 0.8F, 0.7F);
        self.discard();
        ci.cancel();
    }
}
