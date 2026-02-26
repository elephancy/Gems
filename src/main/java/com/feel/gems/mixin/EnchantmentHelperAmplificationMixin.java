package com.feel.gems.mixin;

import com.feel.gems.power.gem.wealth.EnchantmentAmplification;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EnchantmentHelper.class)
public final class EnchantmentHelperAmplificationMixin {
    @Inject(
            method = "getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void gems$amplifyGetLevel(
            Holder<Enchantment> enchantment,
            ItemInstance stack,
            CallbackInfoReturnable<Integer> cir
    ) {
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValueI() + bonus);
        }
    }

    @ModifyArgs(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"
            )
    )
    private static void gems$amplifyForEach(
            Args args,
            ItemStack stack,
            EnchantmentHelper.EnchantmentVisitor consumer
    ) {
        Holder<Enchantment> enchantment = args.get(0);
        int level = args.get(1);
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            args.set(1, level + bonus);
        }
    }

    @ModifyArgs(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"
            )
    )
    private static void gems$amplifyForEachContext(
            Args args,
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity,
            EnchantmentHelper.EnchantmentInSlotVisitor consumer
    ) {
        Holder<Enchantment> enchantment = args.get(0);
        int level = args.get(1);
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            args.set(1, level + bonus);
        }
    }
}
