package com.feel.gems.mixin;

import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(LivingEntity.class)
public abstract class LivingEntitySummonDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void gems$applySummonCooldown(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Mob mob)) {
            return;
        }
        if (!SummonerSummons.isSummon(mob)) {
            return;
        }
        if (mob.entityTags().contains("gems_spectral_blade")) {
            return;
        }
        UUID ownerUuid = SummonerSummons.ownerUuid(mob);
        if (ownerUuid == null) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel world)) {
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        SummonerSummons.applyCooldown(owner);
    }
}
