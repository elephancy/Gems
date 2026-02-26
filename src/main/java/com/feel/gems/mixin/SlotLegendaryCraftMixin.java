package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Slot.class)
public abstract class SlotLegendaryCraftMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void gems$blockLegendary(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!((Object) this instanceof ResultSlot slot)) {
            return;
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        if (!LegendaryCrafting.canStartCraft(serverPlayer, stack)) {
            cir.setReturnValue(false);
        }
    }
}
