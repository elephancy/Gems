package com.feel.gems.particle;

import com.feel.gems.GemsMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {
    public static final SimpleParticleType GEM_STAR = register("gem_star");
    public static final SimpleParticleType GEM_RING = register("gem_ring");

    private ModParticles() {
    }

    public static void init() {
        // Triggers static registration.
    }

    private static SimpleParticleType register(String path) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, path), FabricParticleTypes.simple());
    }
}
