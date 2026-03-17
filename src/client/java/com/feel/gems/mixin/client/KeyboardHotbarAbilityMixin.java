package com.feel.gems.mixin.client;

import com.feel.gems.client.GemsKeybinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(KeyboardHandler.class)
public abstract class KeyboardHotbarAbilityMixin {
    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$consumeHotbarWhenModifierDown(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (action != InputConstants.PRESS) {
            return;
        }

        int key = input.key();
        int scancode = input.keycode();
        int modifiers = input.modifiers();

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) {
            return;
        }
        if (!GemsKeybinds.useChordControls()) {
            return;
        }
        if (!GemsKeybinds.isModifierDown()) {
            return;
        }

        Options options = client.options;
        if (options == null || options.keyHotbarSlots == null) {
            return;
        }

        int limit = Math.min(9, options.keyHotbarSlots.length);
        for (int i = 0; i < limit; i++) {
            KeyMapping hotbar = options.keyHotbarSlots[i];
            if (hotbar.matches(new KeyEvent(key, scancode, modifiers))) {
                GemsKeybinds.activateSlotChord(client, i + 1);
                ci.cancel();
                return;
            }
        }
    }
}
