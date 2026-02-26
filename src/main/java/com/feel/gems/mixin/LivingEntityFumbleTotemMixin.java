package com.feel.gems.mixin;

import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.gem.wealth.WealthFumble;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFumbleTotemMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void gems$disableOffhandTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        boolean hasTotem = main.is(Items.TOTEM_OF_UNDYING) || offhand.is(Items.TOTEM_OF_UNDYING);
        if (hasTotem && BonusPassiveRuntime.shouldTriggerSecondWind(player)) {
            BonusPassiveRuntime.consumeSecondWind(player);
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.5f));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4, false, true));
            cir.setReturnValue(true);
            return;
        }
        if (!WealthFumble.isActive(player)) {
            return;
        }
        if (!offhand.is(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        if (main.is(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        cir.setReturnValue(false);
    }
}
