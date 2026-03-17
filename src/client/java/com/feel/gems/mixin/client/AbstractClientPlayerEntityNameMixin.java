package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Player.class)
public abstract class AbstractClientPlayerEntityNameMixin {
    @Inject(method = "getName()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!(self instanceof AbstractClientPlayer)) {
            return;
        }
        Component override = ClientDisguiseState.overrideName(self.getUUID());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "getDisplayName()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideDisplayName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!(self instanceof AbstractClientPlayer)) {
            return;
        }
        Component override = ClientDisguiseState.overrideName(self.getUUID());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
