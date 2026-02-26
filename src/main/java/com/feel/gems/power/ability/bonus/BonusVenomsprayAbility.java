package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BonusVenomsprayAbility implements GemAbility {
    private static final float CONE_ANGLE = 60.0f; // degrees
    private static final float RANGE = 8.0f;
    private static final float DAMAGE = 4.0f; // 2 hearts

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VENOMSPRAY;
    }

    @Override
    public String name() {
        return "Venomspray";
    }

    @Override
    public String description() {
        return "Spray poison in a cone, damaging and poisoning all enemies hit.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 lookDir = player.getViewVector(1.0f).normalize();
        AABB area = player.getBoundingBox().inflate(RANGE);
        
        // Spawn cone-shaped particles in the direction player is looking
        for (int i = 0; i < 30; i++) {
            double dist = 1.0 + world.getRandom().nextDouble() * (RANGE - 1);
            double spread = dist * 0.3;
            double px = player.getX() + lookDir.x * dist + (world.getRandom().nextDouble() - 0.5) * spread;
            double py = player.getEyeY() + lookDir.y * dist + (world.getRandom().nextDouble() - 0.5) * spread;
            double pz = player.getZ() + lookDir.z * dist + (world.getRandom().nextDouble() - 0.5) * spread;
            world.sendParticles(ParticleTypes.ITEM_SLIME, px, py, pz, 2, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Sound effect
        player.playSound(SoundEvents.LLAMA_SPIT, 1.0f, 0.8f);
        
        DamageSource damageSource = player.damageSources().indirectMagic(player, player);
        
        world.getEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        // Skip trusted players
                        if (living instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                            return;
                        }
                        // Skip players with Void immunity
                        if (living instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                            return;
                        }
                        
                        // Check if target is in the cone
                        Vec3 toTarget = living.position().add(0, living.getBbHeight() / 2, 0)
                                .subtract(player.getEyePosition()).normalize();
                        double dot = lookDir.dot(toTarget);
                        double angleRad = Math.acos(Math.max(-1, Math.min(1, dot)));
                        double angleDeg = Math.toDegrees(angleRad);
                        
                        if (angleDeg <= CONE_ANGLE / 2 && player.distanceToSqr(living) <= RANGE * RANGE) {
                            // Apply damage and effects
                            living.hurtServer(world, damageSource, DAMAGE);
                            living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1)); // 10s Poison II
                        }
                    }
                });
        return true;
    }
}
