package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntrySkinshiftSkinMixin {
    @Inject(method = "getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerInfo self = (PlayerInfo) (Object) this;
        GameProfile profile = self.getProfile();
        UUID id = profile == null ? null : profile.id();
        PlayerSkin override = ClientDisguiseState.overrideSkin(id);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
