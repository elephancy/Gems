package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceOutputDoubleDebrisMixin {
    private static final String KEY_DOUBLE_DEBRIS = "gems_double_debris";

    @Inject(method = "onTake", at = @At("TAIL"))
    private void gems$doubleDebris(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String ownerName = serverPlayer.getName().getString();
        AbilityRuntime.setOwnerWithName(stack, serverPlayer.getUUID(), ownerName);

        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.DOUBLE_DEBRIS)) {
            return;
        }
        if (!stack.is(Items.NETHERITE_SCRAP)) {
            return;
        }

        if (AbilityRuntime.isRichRushActive(serverPlayer)) {
            int rolls = Math.max(1, GemsBalance.v().wealth().richRushLootRolls());
            int extraCount = (rolls - 1) * stack.getCount();
            if (extraCount > 0) {
                ItemStack extraRich = new ItemStack(Items.NETHERITE_SCRAP, extraCount);
                AbilityRuntime.setOwnerWithName(extraRich, serverPlayer.getUUID(), ownerName);
                boolean inserted = serverPlayer.getInventory().add(extraRich);
                if (!inserted && !extraRich.isEmpty()) {
                    serverPlayer.drop(extraRich, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }

        // Duplicate the entire stack count, not just one item.
        // This handles both normal clicks and shift-clicks.
        ItemStack extra = new ItemStack(Items.NETHERITE_SCRAP, stack.getCount());
        AbilityRuntime.setOwnerWithName(extra, serverPlayer.getUUID(), ownerName);
        boolean inserted = serverPlayer.getInventory().add(extra);
        if (!inserted && !extra.isEmpty()) {
            serverPlayer.drop(extra, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        PlayerStateManager.setTemporary(serverPlayer, KEY_DOUBLE_DEBRIS, 1);
    }
}
