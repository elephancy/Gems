package com.feel.gems.mixin;

import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(AbstractCubeMob.class)
public abstract class SlimeEntitySummonNoSplitMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void gems$skipSummonSplit(Entity.RemovalReason reason, CallbackInfo ci) {
        AbstractCubeMob self = (AbstractCubeMob) (Object) this;
        if (SummonerSummons.isSummon(self) || SoulSummons.isSoul(self) || HypnoControl.isHypno(self)) {
            if (self.getSize() > 1) {
                self.setSize(1, false);
            }
        }
    }
}
