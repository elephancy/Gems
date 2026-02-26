package com.feel.gems.power.runtime;

import com.feel.gems.config.GemsBalance;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;




public final class AbilityFeedback {
    private static final double VISUAL_BOOST = 1.5D;

    private AbilityFeedback() {
    }

    /**
     * Sync velocity immediately to the client. Use this after setting player velocity
     * for abilities that move the player (jumps, dashes, etc.).
     */
    public static void syncVelocity(ServerPlayer player) {
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    public static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (!GemsBalance.v().visual().enableSounds()) {
            return;
        }
        ServerLevel world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void soundAt(ServerLevel world, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        if (!GemsBalance.v().visual().enableSounds()) {
            return;
        }
        world.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder.Reference<SoundEvent> sound, float volume, float pitch) {
        sound(player, sound.value(), volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder<SoundEvent> sound, float volume, float pitch) {
        sound(player, sound.value(), volume, pitch);
    }

    public static void soundAt(ServerLevel world, Vec3 pos, Holder.Reference<SoundEvent> sound, float volume, float pitch) {
        soundAt(world, pos, sound.value(), volume, pitch);
    }

    public static void soundAt(ServerLevel world, Vec3 pos, Holder<SoundEvent> sound, float volume, float pitch) {
        soundAt(world, pos, sound.value(), volume, pitch);
    }

    public static void burst(ServerPlayer player, ParticleOptions particle, int count, double spread) {
        burstAt(player.level(), player.position().add(0.0D, 1.0D, 0.0D), particle, count, spread);
    }

    public static void burstAt(ServerLevel world, Vec3 pos, ParticleOptions particle, int count, double spread) {
        int scaled = scaleParticles(count);
        if (scaled <= 0) {
            return;
        }
        if (!GemsBalance.v().visual().enableParticles()) {
            return;
        }
        scaled = Math.min(scaled, GemsBalance.v().visual().maxParticlesPerCall());
        world.sendParticles(
                particle,
                pos.x,
                pos.y,
                pos.z,
                scaled,
                spread,
                spread,
                spread,
                0.01D
        );
    }

    public static void beam(ServerLevel world, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
        int scaledSteps = scaleParticles(steps);
        if (scaledSteps <= 0) {
            return;
        }
        if (!GemsBalance.v().visual().enableParticles()) {
            return;
        }
        scaledSteps = Math.min(scaledSteps, GemsBalance.v().visual().maxBeamSteps());
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        for (int i = 0; i <= scaledSteps; i++) {
            double t = i / (double) scaledSteps;
            double x = from.x + dx * t;
            double y = from.y + dy * t;
            double z = from.z + dz * t;
            world.sendParticles(particle, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    public static void ring(ServerLevel world, Vec3 center, double radius, ParticleOptions particle, int points) {
        int scaledPoints = scaleParticles(points);
        if (scaledPoints <= 0 || radius <= 0.0D) {
            return;
        }
        if (!GemsBalance.v().visual().enableParticles()) {
            return;
        }
        scaledPoints = Math.min(scaledPoints, GemsBalance.v().visual().maxRingPoints());
        double step = (Math.PI * 2.0D) / scaledPoints;
        for (int i = 0; i < scaledPoints; i++) {
            double a = i * step;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            world.sendParticles(particle, x, center.y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private static int scaleParticles(int count) {
        if (count <= 0) {
            return 0;
        }
        int scale = GemsBalance.v().visual().particleScalePercent();
        if (scale <= 0) {
            return 0;
        }
        return (int) Math.round(count * (scale / 100.0D) * VISUAL_BOOST);
    }
}
