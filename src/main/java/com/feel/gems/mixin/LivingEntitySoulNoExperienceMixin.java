package com.feel.gems.mixin;

import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(LivingEntity.class)
public abstract class LivingEntitySoulNoExperienceMixin {
    @Inject(method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$soulNoXp(net.minecraft.server.level.ServerLevel world, Entity attacker, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (SoulSummons.isSoul(self) || SummonerSummons.isSummon(self)) {
            ci.cancel();
        }
    }
}
