package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityRotEaterTickMixin {
    private static final String KEY_ROT_EATER_UNTIL = "reaperRotEaterUntil";

    @Inject(method = "tick", at = @At("HEAD"))
    private void gems$clearRotEaterEffects(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = data.getLongOr(KEY_ROT_EATER_UNTIL, 0L);
        if (until <= 0L) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.REAPER_ROT_EATER)) {
            data.remove(KEY_ROT_EATER_UNTIL);
            return;
        }
        long now = GemsTime.now(player);
        if (now > until) {
            data.remove(KEY_ROT_EATER_UNTIL);
            return;
        }
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.POISON);
    }
}
