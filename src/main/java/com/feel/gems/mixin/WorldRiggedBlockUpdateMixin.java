package com.feel.gems.mixin;

import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class WorldRiggedBlockUpdateMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void gems$riggedBlockUpdated(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        Level world = (Level) (Object) this;
        if (world.isClientSide()) {
            return;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        if (!TerrorRigRuntime.hasTrap(serverWorld, pos)) {
            return;
        }
        BlockState before = world.getBlockState(pos);
        TerrorRigRuntime.tryTriggerUpdate(serverWorld, pos, before, state);
    }
}
