package com.feel.gems.power.util;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.feel.gems.power.gem.voidgem.VoidImmunity;




public final class Targeting {
    private Targeting() {
    }

    public static LivingEntity raycastLiving(ServerPlayer player, double maxDistance) {
        return raycastLiving(player, maxDistance, 1.0D);
    }

    public static LivingEntity raycastLiving(ServerPlayer player, double maxDistance, double viewRange) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(maxDistance));

        HitResult blockHit = player.pick(maxDistance, 1.0F, false);
        double maxDistanceSq = maxDistance * maxDistance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
            maxDistanceSq = start.distanceToSqr(end);
        }

        double expand = Math.max(0.0D, viewRange);
        AABB box = player.getBoundingBox().expandTowards(direction.scale(maxDistance)).inflate(expand);
        Predicate<Entity> predicate = entity -> {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == player) {
                return false;
            }
            if (living instanceof ServerPlayer targetPlayer) {
                return VoidImmunity.canBeTargeted(player, targetPlayer);
            }
            return true;
        };
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, start, end, box, predicate, maxDistanceSq);
        if (entityHit == null) {
            return null;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    public static ServerPlayer raycastPlayer(ServerPlayer player, double maxDistance) {
        return raycastPlayer(player, maxDistance, 1.0D);
    }

    public static ServerPlayer raycastPlayer(ServerPlayer player, double maxDistance, double viewRange) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(maxDistance));

        HitResult blockHit = player.pick(maxDistance, 1.0F, false);
        double maxDistanceSq = maxDistance * maxDistance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
            maxDistanceSq = start.distanceToSqr(end);
        }

        double expand = Math.max(0.0D, viewRange);
        AABB box = player.getBoundingBox().expandTowards(direction.scale(maxDistance)).inflate(expand);
        Predicate<Entity> predicate = entity -> {
            if (!(entity instanceof ServerPlayer target) || !target.isAlive() || target == player) {
                return false;
            }
            return VoidImmunity.canBeTargeted(player, target);
        };
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, start, end, box, predicate, maxDistanceSq);
        if (entityHit == null) {
            return null;
        }
        if (entityHit.getEntity() instanceof ServerPlayer target) {
            return target;
        }
        return null;
    }
}
