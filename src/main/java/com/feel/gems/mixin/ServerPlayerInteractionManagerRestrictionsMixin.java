package com.feel.gems.mixin;

import com.feel.gems.power.gem.wealth.WealthFumble;
import com.feel.gems.power.runtime.AbilityRestrictions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerInteractionManagerRestrictionsMixin {
    @Shadow
    @Final
    protected ServerPlayer player;
    private int gems$preUseCount = 0;
    private boolean gems$preUseThrowable = false;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void gems$stunBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrLockItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        gems$preUseCount = stack.getCount();
        gems$preUseThrowable = com.feel.gems.power.gem.trickster.TricksterPassiveRuntime.isThrowable(stack);
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (WealthFumble.isActive(player)) {
            if (hand == InteractionHand.OFF_HAND || stack.has(DataComponents.FOOD)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void gems$tricksterSleightOfHand(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!gems$preUseThrowable) {
            return;
        }
        if (stack.getCount() != gems$preUseCount - 1) {
            return;
        }
        if (com.feel.gems.power.gem.trickster.TricksterPassiveRuntime.shouldNotConsumeThrowable(player)) {
            stack.grow(1);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrFumbleBlock(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (WealthFumble.isActive(player)) {
            if (hand == InteractionHand.OFF_HAND || stack.has(DataComponents.FOOD)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }
    }

}
