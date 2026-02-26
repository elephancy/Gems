package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Starfall - Rain down small meteors in an area.
 */
public final class BonusStarfallAbility implements GemAbility {
    private static final int METEOR_COUNT = 8;
    private static final double RANGE = 30.0;
    private static final double SPREAD = 5.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_STARFALL;
    }

    @Override
    public String name() {
        return "Starfall";
    }

    @Override
    public String description() {
        return "Rain 8 meteors at your crosshair location.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(RANGE));

        HitResult hit = world.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 targetCenter = hit.getLocation();

        for (int i = 0; i < METEOR_COUNT; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * SPREAD * 2;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * SPREAD * 2;
            Vec3 spawnPos = targetCenter.add(offsetX, 15 + world.getRandom().nextDouble() * 5, offsetZ);

            SmallFireball fireball = new SmallFireball(world, player, new Vec3(0, -1, 0));
            fireball.setPos(spawnPos);
            world.addFreshEntity(fireball);

            world.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z,
                    5, 0.2, 0.2, 0.2, 0.05);
        }

        world.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
