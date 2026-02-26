package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.RangeLimitedProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.phys.Vec3;

public final class BonusDoomBoltAbility implements GemAbility {
    private static final int COOLDOWN_TICKS = 800; // 40 seconds
    private static final float DAMAGE = 12.0F;
    private static final float VELOCITY = 0.5F;
    private static final double MAX_RANGE_BLOCKS = 30.0D;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_DOOM_BOLT;
    }

    @Override
    public String name() {
        return "Doom Bolt";
    }

    @Override
    public String description() {
        return "Launch a slow but devastating dark projectile.";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 direction = player.getViewVector(1.0F).normalize();
        Vec3 spawn = player.getEyePosition().add(direction.scale(0.6D));

        WitherSkull bolt = EntityTypes.WITHER_SKULL.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (bolt == null) {
            return false;
        }
        bolt.setOwner(player);
        bolt.snapTo(spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        bolt.shoot(direction.x, direction.y, direction.z, VELOCITY, 0.0F);
        bolt.addTag("gems_doom_bolt");

        if (bolt instanceof AbstractHurtingProjectile explosive) {
            if (explosive instanceof RangeLimitedProjectile limited) {
                limited.gems$setRangeLimit(spawn, MAX_RANGE_BLOCKS);
            }
        }

        world.addFreshEntity(bolt);

        world.sendParticles(ParticleTypes.SMOKE, spawn.x, spawn.y, spawn.z, 12, 0.3, 0.3, 0.3, 0.02);
        world.sendParticles(ParticleTypes.SOUL, spawn.x, spawn.y, spawn.z, 6, 0.2, 0.2, 0.2, 0.01);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.WITHER_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    public static float doomBoltDamage() {
        return DAMAGE;
    }
}
