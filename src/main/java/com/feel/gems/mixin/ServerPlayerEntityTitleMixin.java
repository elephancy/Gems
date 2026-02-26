package com.feel.gems.mixin;

import com.feel.gems.mastery.TitleDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityTitleMixin {
    @Inject(method = "getTabListDisplayName()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    private void gems$addTitleToListName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Component base = cir.getReturnValue();
        if (base == null) {
            base = player.getName();
        }
        Component prefix = TitleDisplay.titlePrefix(player);
        if (prefix == null) {
            return;
        }
        cir.setReturnValue(TitleDisplay.withTitlePrefix(player, base));
    }
}
