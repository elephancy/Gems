package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackUnbreakableMixin {
    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), argsOnly = true)
    private int gems$reduceDurability(int amount, int originalAmount, LivingEntity entity, EquipmentSlot slot) {
        if (amount <= 0 || entity == null) {
            return amount;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_UNBREAKABLE)) {
            return amount;
        }
        float reduction = GemsBalance.v().bonusPool().unbreakableDurabilityReductionPercent / 100.0f;
        return Math.max(0, Math.round(amount * (1.0f - reduction)));
    }
}
