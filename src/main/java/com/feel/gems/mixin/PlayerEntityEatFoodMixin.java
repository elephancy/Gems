package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.feel.gems.state.GemsPersistentDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ItemStack.class)
public abstract class PlayerEntityEatFoodMixin {
    private static final String KEY_ROT_EATER_UNTIL = "reaperRotEaterUntil";

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void gems$reaperRotEater(Level world, net.minecraft.world.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClientSide()) {
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.REAPER_ROT_EATER)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;

        if (self.is(Items.ROTTEN_FLESH)) {
            markRotEater(player);
        } else if (self.is(Items.SPIDER_EYE)) {
            markRotEater(player);
        }
    }

    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void gems$reaperRotEaterTail(Level world, net.minecraft.world.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClientSide()) {
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        // Vanilla applies food status effects during finishUsing; clear again at TAIL to ensure Hunger/Poison
        // doesn't "win" after our HEAD removal.
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = data.getLongOr(KEY_ROT_EATER_UNTIL, 0L);
        if (until <= 0L) {
            return;
        }
        long now = GemsTime.now(player);
        if (now > until) {
            return;
        }
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.POISON);
    }

    private static void markRotEater(ServerPlayer player) {
        long now = GemsTime.now(player);
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putLong(KEY_ROT_EATER_UNTIL, now + 40L);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.POISON);
    }
}
