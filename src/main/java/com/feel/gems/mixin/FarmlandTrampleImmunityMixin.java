package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(FarmlandBlock.class)
public abstract class FarmlandTrampleImmunityMixin {
    @Inject(method = "fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;D)V", at = @At("HEAD"), cancellable = true)
    private void gems$preventTrample(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (world.isClientSide()) {
            return;
        }
        if (entity instanceof ServerPlayer player && GemPowers.isPassiveActive(player, PowerIds.CROP_TRAMPLE_IMMUNITY)) {
            entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
            ci.cancel();
        }
    }
}

