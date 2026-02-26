package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(SculkSensorBlockEntity.class)
public abstract class SculkSensorSilentStepMixin {
    @Inject(method = "createVibrationUser", at = @At("RETURN"), cancellable = true)
    private void gems$wrapCallback(CallbackInfoReturnable<VibrationSystem.User> cir) {
        VibrationSystem.User original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue(new SilentStepCallback(original));
    }

    private record SilentStepCallback(VibrationSystem.User delegate) implements VibrationSystem.User {
        @Override
        public int getListenerRadius() {
            return delegate.getListenerRadius();
        }

        @Override
        public net.minecraft.world.level.gameevent.PositionSource getPositionSource() {
            return delegate.getPositionSource();
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return delegate.canTriggerAvoidVibration();
        }

        @Override
        public boolean canReceiveVibration(ServerLevel world, BlockPos pos, Holder<GameEvent> event, GameEvent.Context emitter) {
            if (emitter != null && emitter.sourceEntity() instanceof ServerPlayer player) {
                if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE) || GemPowers.isPassiveActive(player, PowerIds.SPY_SILENT_STEP)) {
                    return false;
                }
            }
            return delegate.canReceiveVibration(world, pos, event, emitter);
        }

        @Override
        public void onReceiveVibration(ServerLevel world, BlockPos pos, Holder<GameEvent> event, Entity entity, Entity sourceEntity, float distance) {
            if (sourceEntity instanceof ServerPlayer player) {
                if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE) || GemPowers.isPassiveActive(player, PowerIds.SPY_SILENT_STEP)) {
                    return;
                }
            } else if (entity instanceof ServerPlayer player) {
                if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE) || GemPowers.isPassiveActive(player, PowerIds.SPY_SILENT_STEP)) {
                    return;
                }
            }
            delegate.onReceiveVibration(world, pos, event, entity, sourceEntity, distance);
        }

        @Override
        public void onDataChanged() {
            delegate.onDataChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return delegate.requiresAdjacentChunksToBeTicking();
        }
    }
}
