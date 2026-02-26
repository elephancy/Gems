package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceScreenHandlerDoubleDebrisMixin {
    private static final String KEY_DOUBLE_DEBRIS = "gems_double_debris";
    private static final int OUTPUT_SLOT = 2;

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void gems$doubleDebrisShiftClick(Player player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot != OUTPUT_SLOT) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (PlayerStateManager.getTemporary(serverPlayer, KEY_DOUBLE_DEBRIS) > 0) {
            return;
        }
        ItemStack moved = cir.getReturnValue();
        if (moved == null || moved.isEmpty() || !moved.is(Items.NETHERITE_SCRAP)) {
            return;
        }
        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.DOUBLE_DEBRIS)) {
            return;
        }
        String ownerName = serverPlayer.getName().getString();
        ItemStack extra = new ItemStack(Items.NETHERITE_SCRAP, moved.getCount());
        AbilityRuntime.setOwnerWithName(extra, serverPlayer.getUUID(), ownerName);
        boolean inserted = serverPlayer.getInventory().add(extra);
        if (!inserted && !extra.isEmpty()) {
            serverPlayer.drop(extra, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        PlayerStateManager.setTemporary(serverPlayer, KEY_DOUBLE_DEBRIS, 1);
    }
}
