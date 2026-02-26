package com.feel.gems.mixin;

import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(WitherBoss.class)
public abstract class WitherEntitySoulTargetingMixin {
    @ModifyVariable(method = "setAlternativeTarget", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int gems$soulPreventHeadTargetingTrusted(int entityId) {
        if (entityId <= 0) {
            return entityId;
        }

        WitherBoss self = (WitherBoss) (Object) this;
        if (!(self.level() instanceof ServerLevel world)) {
            return entityId;
        }
        boolean soul = SoulSummons.isSoul(self);
        boolean summon = SummonerSummons.isSummon(self);
        if (!soul && !summon) {
            return entityId;
        }

        Entity candidate = world.getEntity(entityId);
        if (!(candidate instanceof ServerPlayer player)) {
            return entityId;
        }

        UUID ownerUuid = soul ? SoulSummons.ownerUuid(self) : SummonerSummons.ownerUuid(self);
        if (ownerUuid == null) {
            return 0;
        }
        if (player.getUUID().equals(ownerUuid)) {
            return 0;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null && GemTrust.isTrusted(owner, player)) {
            return 0;
        }
        return entityId;
    }
}
