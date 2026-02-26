package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ItemStack.class)
public abstract class PlayerEntityDoubleSaturationMixin {
    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void gems$doubleSaturation(Level world, net.minecraft.world.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClientSide()) {
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.DOUBLE_SATURATION)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        FoodProperties food = self.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        FoodData hunger = player.getFoodData();
        float boosted = hunger.getSaturationLevel() + food.saturation();
        hunger.setSaturation(Math.min(hunger.getFoodLevel(), boosted));
    }
}
