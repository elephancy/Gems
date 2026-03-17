package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientTricksterState;
import com.feel.gems.mixin.client.accessor.InputAccessor;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(KeyboardInput.class)
public abstract class KeyboardInputTricksterMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void gems$applyTricksterControl(CallbackInfo ci) {
        InputAccessor input = (InputAccessor) (Object) this;
        if (ClientTricksterState.isPuppeted()) {
            input.gems$setPlayerInput(Input.EMPTY);
            input.gems$setMovementVector(Vec2.ZERO);
            return;
        }
        if (ClientTricksterState.isMindGames()) {
            // Reverse controls: invert movement direction and swap the logical pressed keys.
            Input current = input.gems$getPlayerInput();
            input.gems$setPlayerInput(new Input(
                    current.backward(),
                    current.forward(),
                    current.right(),
                    current.left(),
                    current.jump(),
                    current.shift(),
                    current.sprint()
            ));
            Vec2 movement = input.gems$getMovementVector();
            input.gems$setMovementVector(new Vec2(-movement.x, -movement.y));
        }
    }
}
