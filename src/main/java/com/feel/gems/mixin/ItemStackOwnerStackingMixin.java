package com.feel.gems.mixin;

import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackOwnerStackingMixin {
    private static final Set<String> OWNER_KEYS = Set.of(
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

    @Unique
    private static final ThreadLocal<Boolean> GEMS_IGNORE_OWNER_COMPARE =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "isSameItemSameComponents", at = @At("RETURN"), cancellable = true)
    private static void gems$ignoreOwnerTags(ItemStack left, ItemStack right, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || GEMS_IGNORE_OWNER_COMPARE.get()) {
            return;
        }
        if (left.isEmpty() || right.isEmpty() || !left.is(right.getItem())) {
            return;
        }
        ItemStack leftCopy = left.copy();
        ItemStack rightCopy = right.copy();
        boolean changedLeft = stripOwnerTags(leftCopy);
        boolean changedRight = stripOwnerTags(rightCopy);
        if (!changedLeft && !changedRight) {
            return;
        }
        GEMS_IGNORE_OWNER_COMPARE.set(true);
        try {
            if (ItemStack.isSameItemSameComponents(leftCopy, rightCopy)) {
                cir.setReturnValue(true);
            }
        } finally {
            GEMS_IGNORE_OWNER_COMPARE.set(false);
        }
    }

    @Unique
    private static boolean stripOwnerTags(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        CompoundTag nbt = custom.copyTag();
        boolean changed = false;
        for (String key : OWNER_KEYS) {
            if (nbt.contains(key)) {
                nbt.remove(key);
                changed = true;
            }
        }
        if (!changed) {
            return false;
        }
        if (nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, out -> out.merge(nbt));
        }
        return true;
    }
}
