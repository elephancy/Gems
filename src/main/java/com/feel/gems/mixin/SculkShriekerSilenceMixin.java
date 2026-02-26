package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerSilenceMixin {
    @Inject(method = "tryShriek", at = @At("HEAD"), cancellable = true)
    private void gems$sculkSilence(ServerLevel world, ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE) || GemPowers.isPassiveActive(player, PowerIds.SPY_SILENT_STEP)) {
            ci.cancel();
        }
    }
}
