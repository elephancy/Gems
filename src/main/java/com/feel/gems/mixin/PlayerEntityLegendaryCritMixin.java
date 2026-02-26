package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.ThirdStrikeBladeItem;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.util.GemsTime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityLegendaryCritMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void gems$legendaryCriticals(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        
        if (!gems$isVanillaCritical(attacker)) {
            return;
        }

        if (attacker.getMainHandItem().is(ModItems.THIRD_STRIKE_BLADE)) {
            ThirdStrikeBladeItem.recordCriticalHit(attacker);
        }

        if (attacker.getMainHandItem().is(ModItems.VAMPIRIC_EDGE)) {
            float heal = GemsBalance.v().legendary().vampiricHealAmount();
            if (heal > 0.0F) {
                attacker.heal(heal);
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$legendaryCrits(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }

        long now = GemsTime.now(attacker);
        if (AbilityRuntime.isReaperWitheringStrikesActive(attacker, now)) {
            int duration = GemsBalance.v().reaper().witheringStrikesWitherDurationTicks();
            int amp = GemsBalance.v().reaper().witheringStrikesWitherAmplifier();
            if (duration > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amp, true, false, false));
            }
        }
    }

    private static boolean gems$isVanillaCritical(ServerPlayer attacker) {
        if (attacker.getAbilities().flying) {
            return false;
        }
        if (attacker.onGround()) {
            return false;
        }
        if (attacker.fallDistance <= 0.0F) {
            return false;
        }
        if (attacker.onClimbable()) {
            return false;
        }
        if (attacker.isInWater()) {
            return false;
        }
        if (attacker.isPassenger()) {
            return false;
        }
        if (attacker.hasEffect(MobEffects.BLINDNESS)) {
            return false;
        }
        if (attacker.isSprinting()) {
            return false;
        }
        if (attacker.isUsingItem()) {
            return false;
        }
        return true;
    }
}
