package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Blink - Instant micro-teleport (5 blocks) with minimal cooldown.
 */
public final class BonusBlinkAbility implements GemAbility {
    private static final double BLINK_DISTANCE = 5.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLINK;
    }

    @Override
    public String name() {
        return "Blink";
    }

    @Override
    public String description() {
        return "Instantly blink 5 blocks in your facing direction.";
    }

    @Override
    public int cooldownTicks() {
        return 60; // 3 seconds - short cooldown
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(BLINK_DISTANCE));

        // Particles at start
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z,
                15, 0.3, 0.5, 0.3, 0.05);

        // Teleport
        GemsTeleport.teleport(player, world, end.x, end.y, end.z, player.getYRot(), player.getXRot());

        // Particles at end
        world.sendParticles(ParticleTypes.PORTAL, end.x, end.y + 1, end.z,
                15, 0.3, 0.5, 0.3, 0.05);

        world.playSound(null, end.x, end.y, end.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.5f);
        return true;
    }
}
