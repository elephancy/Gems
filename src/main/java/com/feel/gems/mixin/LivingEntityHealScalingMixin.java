package com.feel.gems.mixin;

import com.feel.gems.power.gem.space.SpaceLunarScaling;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityHealScalingMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float gems$scaleHeal(float amount) {
        if (amount <= 0.0F) {
            return amount;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }

        if (!(self instanceof ServerPlayer player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.SPACE_LUNAR_SCALING)) {
            return amount;
        }
        return amount * SpaceLunarScaling.multiplier(player.level());
    }
}
