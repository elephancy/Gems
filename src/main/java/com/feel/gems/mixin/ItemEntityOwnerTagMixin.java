package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import com.feel.gems.item.GemOwnership;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ItemEntity.class)
public abstract class ItemEntityOwnerTagMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void gems$tagPickedUp(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        if (GemOwnership.isInvalidForEpoch(serverPlayer.level().getServer(), self.getItem())) {
            self.discard();
            return;
        }
        AbilityRuntime.setOwnerWithName(self.getItem(), serverPlayer.getUUID(), serverPlayer.getName().getString());
    }
}

