package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.gem.speed.SpeedMomentum;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class ArcShotAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ARC_SHOT;
    }

    @Override
    public String name() {
        return "Arc Shot";
    }

    @Override
    public String description() {
        return "Arc Shot: strikes several enemies along a line with lightning and knockback.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().arcShotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        double maxDistance = GemsBalance.v().speed().arcShotRangeBlocks();
        float momentum = SpeedMomentum.multiplier(player);
        double radius = SpeedMomentum.scaleDouble(GemsBalance.v().speed().arcShotRadiusBlocks(), momentum);
        int maxTargets = SpeedMomentum.scaleInt(GemsBalance.v().speed().arcShotMaxTargets(), momentum, 1);
        float damage = SpeedMomentum.scaleFloat(GemsBalance.v().speed().arcShotDamage(), momentum);

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(dir.scale(maxDistance));

        HitResult blockHit = player.pick(maxDistance, 1.0F, false);
        double length = maxDistance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
            length = start.distanceTo(end);
        }

        AABB box = new AABB(start, end).inflate(radius);
        List<Hit> hits = new ArrayList<>();
        for (var e : world.getEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            LivingEntity living = (LivingEntity) e;
            if (living instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (living instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }

            Vec3 pos = living.position();
            double t = pos.subtract(start).dot(dir);
            if (t < 0.0D || t > length) {
                continue;
            }
            Vec3 closest = start.add(dir.scale(t));
            double distSq = pos.distanceToSqr(closest);
            if (distSq > radius * radius) {
                continue;
            }
            hits.add(new Hit(living, t));
        }

        hits.sort(Comparator.comparingDouble(h -> h.t));
        if (hits.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.message.no_targets"), true);
            return false;
        }

        int count = 0;
        AbilityFeedback.burstAt(world, start, ParticleTypes.ELECTRIC_SPARK, 18, 0.2D);
        for (Hit hit : hits) {
            LivingEntity target = hit.target;
            spawnLightning(world, target.position());
            target.hurtServer(world, player.damageSources().lightningBolt(), damage);
            AbilityFeedback.beam(world, start, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 12);

            Vec3 away = target.position().subtract(player.position()).normalize();
            double kb = 0.8D * momentum;
            target.push(away.x * kb, 0.4D * momentum, away.z * kb);
            target.needsSync = true;

            count++;
            if (count >= maxTargets) {
                break;
            }
        }

        AbilityFeedback.sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.2F);
        return true;
    }

    private static void spawnLightning(ServerLevel world, Vec3 pos) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.snapTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(true);
        world.addFreshEntity(bolt);
    }

    private record Hit(LivingEntity target, double t) {
    }
}
