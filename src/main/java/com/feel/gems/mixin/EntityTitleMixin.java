package com.feel.gems.mixin;

import com.feel.gems.mastery.TitleDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTitleMixin {
    @Inject(method = "getDisplayName()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    private void gems$addTitleToDisplayName(CallbackInfoReturnable<Component> cir) {
        Component base = cir.getReturnValue();
        if (base == null) {
            return;
        }
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        cir.setReturnValue(TitleDisplay.withTitlePrefix(player, base));
    }
}
