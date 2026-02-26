package com.feel.gems.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryRecipeBookMixin {
    private static final java.util.Set<String> OWNER_KEYS = java.util.Set.of(
            "gemsOwner",
            "gemsOwnerName",
            "gemsFirstOwner",
            "gemsFirstOwnerName",
            "gemsPrevOwner",
            "gemsPrevOwnerName",
            "gemsSignature",
            "signature",
            "GemOwner",
            "GemOwnerEpoch"
    );

    @Inject(method = "findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$getSlotWithStackIgnoreOwner(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            cir.setReturnValue(-1);
            return;
        }
        Inventory self = (Inventory) (Object) this;
        var main = self.getNonEquipmentItems();
        boolean targetHasCustomData = stack.has(DataComponents.CUSTOM_DATA);
        for (int i = 0; i < main.size(); i++) {
            ItemStack candidate = main.get(i);
            if (candidate.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(stack, candidate)) {
                if (targetHasCustomData || !matchesOwnerOnly(stack, candidate)) {
                    continue;
                }
            }
            if (candidate.isDamaged() || candidate.isEnchanted() || candidate.has(DataComponents.CUSTOM_NAME)) {
                continue;
            }
            cir.setReturnValue(i);
            return;
        }
        cir.setReturnValue(-1);
    }

    private static boolean matchesOwnerOnly(ItemStack target, ItemStack candidate) {
        if (!candidate.is(target.getItem())) {
            return false;
        }
        CustomData custom = candidate.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        CompoundTag nbt = custom.copyTag();
        boolean hasOwnerKey = false;
        for (String key : nbt.keySet()) {
            if (OWNER_KEYS.contains(key)) {
                hasOwnerKey = true;
                continue;
            }
            return false;
        }
        return hasOwnerKey;
    }
}
