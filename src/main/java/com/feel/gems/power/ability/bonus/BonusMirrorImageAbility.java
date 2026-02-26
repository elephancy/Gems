package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Mirror Image - Create 3 illusions that confuse enemies.
 */
public final class BonusMirrorImageAbility implements GemAbility {
    private static final int ILLUSION_COUNT = 3;
    private static final int DURATION = 100; // 5 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MIRROR_IMAGE;
    }

    @Override
    public String name() {
        return "Mirror Image";
    }

    @Override
    public String description() {
        return "Create 3 illusions of yourself that confuse enemies for 5s.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 playerPos = player.position();

        for (int i = 0; i < ILLUSION_COUNT; i++) {
            double angle = (2 * Math.PI / ILLUSION_COUNT) * i;
            double offsetX = Math.cos(angle) * 2;
            double offsetZ = Math.sin(angle) * 2;
            Vec3 illusionPos = playerPos.add(offsetX, 0, offsetZ);

            // Create armor stand as illusion marker
            ArmorStand illusion = new ArmorStand(world, illusionPos.x, illusionPos.y, illusionPos.z);
            illusion.setInvisible(true);
            illusion.setNoGravity(true);
            illusion.setPermanentlyInvulnerable(true);
            illusion.setCustomName(player.getName());
            illusion.addTag("gems_mirror_image");
            illusion.addTag("owner:" + player.getStringUUID());
            world.addFreshEntity(illusion);

            // Spawn particles at illusion location
            world.sendParticles(ParticleTypes.CLOUD, illusionPos.x, illusionPos.y + 1, illusionPos.z,
                    15, 0.3, 0.8, 0.3, 0.02);
        }

        // Grant brief invisibility to player
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.INVISIBILITY, 40, 0, false, false, false));

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
