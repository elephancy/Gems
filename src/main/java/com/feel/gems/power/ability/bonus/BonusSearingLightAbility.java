package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Searing Light - Beam of holy light that burns undead extra.
 */
public final class BonusSearingLightAbility implements GemAbility {
    private static final double RANGE = 30.0;
    private static final float BASE_DAMAGE = 8.0f;
    private static final float UNDEAD_BONUS = 8.0f;
    private static final double BEAM_WIDTH = 2.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SEARING_LIGHT;
    }

    @Override
    public String name() {
        return "Searing Light";
    }

    @Override
    public String description() {
        return "Fire a beam of holy light. Deals 8 damage, double to undead.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(RANGE));

        // Get all entities along the beam path
        AABB beamBox = new AABB(start, end).inflate(BEAM_WIDTH);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            // Check if entity is actually in beam path
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(start);
            Vec3 projected = direction.scale(toEntity.dot(direction));
            double distance = toEntity.subtract(projected).length();

            if (distance < BEAM_WIDTH && projected.length() < RANGE && projected.dot(direction) > 0) {
                float damage = BASE_DAMAGE;
                if (entity.typeHolder().is(EntityTypeTags.UNDEAD)) {
                    damage += UNDEAD_BONUS;
                    entity.igniteForSeconds(5);
                }
                entity.hurtServer(world, player.damageSources().indirectMagic(player, player), damage);
            }
        }

        // Beam particle effect
        for (int i = 0; i < 30; i++) {
            Vec3 particlePos = start.add(direction.scale(i));
            world.sendParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.01);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
