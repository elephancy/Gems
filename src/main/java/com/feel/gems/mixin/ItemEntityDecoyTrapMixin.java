package com.feel.gems.mixin;

import com.feel.gems.power.ability.bonus.BonusDecoyTrapAbility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle Decoy Trap explosions when items are picked up.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityDecoyTrapMixin {

    @Shadow
    private int pickupDelay;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void gems$checkDecoyTrap(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Only trigger if the item can actually be picked up (pickup delay expired)
        if (this.pickupDelay > 0) {
            return;
        }
        
        ItemEntity self = (ItemEntity) (Object) this;
        if (BonusDecoyTrapAbility.triggerTrap(self, serverPlayer)) {
            // The trap exploded - cancel the normal pickup
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$checkMobPickupTrap(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }
        BonusDecoyTrapAbility.tickTrapItem(self);
        if (this.pickupDelay > 0) {
            return;
        }
        if (BonusDecoyTrapAbility.getTrapOwner(self) == null) {
            return;
        }
        AABB box = self.getBoundingBox().inflate(0.35);
        for (LivingEntity living : world.getEntitiesOfClass(LivingEntity.class, box, e -> !(e instanceof Player))) {
            if (BonusDecoyTrapAbility.triggerTrap(self, living)) {
                break;
            }
        }
    }
}
