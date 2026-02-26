package com.feel.gems.mixin;

import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Entity.class)
public abstract class EntitySummonDiscardMixin {
    @Inject(method = "discard", at = @At("HEAD"))
    private void gems$applyCooldownOnDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Mob mob)) {
            return;
        }
        if (!SummonerSummons.isSummon(mob)) {
            return;
        }
        if (mob.entityTags().contains("gems_spectral_blade")) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel world)) {
            return;
        }
        UUID ownerUuid = SummonerSummons.ownerUuid(mob);
        if (ownerUuid == null) {
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        SummonerSummons.applyCooldown(owner);
    }
}
