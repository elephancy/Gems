package com.feel.gems.mixin.client.accessor;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface InputAccessor {
    @Accessor("keyPresses")
    Input gems$getPlayerInput();

    @Accessor("keyPresses")
    void gems$setPlayerInput(Input input);

    @Accessor("moveVector")
    Vec2 gems$getMovementVector();

    @Accessor("moveVector")
    void gems$setMovementVector(Vec2 movementVector);
}

