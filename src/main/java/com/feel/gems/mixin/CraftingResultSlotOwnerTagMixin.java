package com.feel.gems.mixin;

import com.feel.gems.augment.AugmentCrafting;
import com.feel.gems.legendary.LegendaryCrafting;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ResultSlot.class)
public abstract class CraftingResultSlotOwnerTagMixin {
    @Inject(method = "onTake", at = @At("TAIL"))
    private void gems$tagCrafted(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityRuntime.setOwnerWithName(stack, serverPlayer.getUUID(), serverPlayer.getName().getString());
            LegendaryCrafting.onCrafted(serverPlayer, stack);
            AugmentCrafting.onCrafted(serverPlayer, stack);
        }
    }
}

