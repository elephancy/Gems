package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(PlayerInfo.class)
public abstract class PlayerListEntrySkinshiftNameMixin {
    @Inject(method = "getTabListDisplayName()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideDisplayName(CallbackInfoReturnable<Component> cir) {
        PlayerInfo self = (PlayerInfo) (Object) this;
        GameProfile profile = self.getProfile();
        UUID id = profile == null ? null : profile.id();
        Component override = ClientDisguiseState.overrideName(id);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
