package com.feel.gems.mixin;

import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.util.GemsTime;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Mob.class)
public abstract class MobEntitySummonerLifetimeMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void gems$summonerDespawnIfExpiredOrOwnerOffline(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }
        if (!SummonerSummons.isSummon(self)) {
            return;
        }
        if (self.entityTags().contains("gems_spectral_blade")) {
            return;
        }

        UUID ownerUuid = SummonerSummons.ownerUuid(self);
        if (ownerUuid == null) {
            self.discard();
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            self.discard();
            return;
        }

        long until = SummonerSummons.untilTick(self);
        if (until > 0 && GemsTime.now(world) >= until) {
            SummonerSummons.applyCooldown(owner);
            self.discard();
        }
    }
}

