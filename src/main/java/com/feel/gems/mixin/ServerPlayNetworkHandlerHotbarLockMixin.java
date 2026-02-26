package com.feel.gems.mixin;

import com.feel.gems.power.gem.wealth.HotbarLock;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerHotbarLockMixin {
    @Shadow
    @Final
    public ServerPlayer player;

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void gems$hotbarLock(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        int locked = HotbarLock.lockedSlot(player);
        if (locked < 0) {
            return;
        }

        int requested = packet.getSlot();
        if (requested == locked) {
            return;
        }

        player.getInventory().setSelectedSlot(locked);
        player.connection.send(new ClientboundSetHeldSlotPacket(locked));
        ci.cancel();
    }
}

