package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Thorns Nova - Explode thorns outward, damaging all nearby enemies.
 */
public final class BonusThornsNovaAbility implements GemAbility {
    private static final double RANGE = 8.0;
    private static final float DAMAGE = 10.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_THORNS_NOVA;
    }

    @Override
    public String name() {
        return "Thorns Nova";
    }

    @Override
    public String description() {
        return "Explode thorns outward, dealing 10 damage to all enemies within 8 blocks.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        AABB box = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer p && p.isCreative()));

        if (entities.isEmpty()) {
            return false;
        }

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayer otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            entity.hurtServer(world, world.damageSources().thorns(player), DAMAGE);
            
            // Knockback
            Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(0.8);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
            entity.needsSync = true;
        }

        // Thorn particle explosion
        for (int i = 0; i < 50; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double radius = world.getRandom().nextDouble() * RANGE;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            world.sendParticles(ParticleTypes.CRIT, x, player.getY() + 0.5, z, 2, 0.1, 0.3, 0.1, 0.1);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
