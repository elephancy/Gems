package com.feel.gems.sound;

import com.feel.gems.GemsMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Registry for custom mod sounds.
 */
public final class ModSounds {
    public static final SoundEvent METAL_PIPE = register("metal_pipe");
    public static final SoundEvent ARCANE_CHIME = register("arcane_chime");
    public static final SoundEvent SURGE_PULSE = register("surge_pulse");
    public static final SoundEvent PHASE_SHIFT = register("phase_shift");

    private ModSounds() {
    }

    public static void init() {
        // Triggers static initialization.
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
