package com.feel.gems.power.gem.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


public final class AirGaleSlamRuntime {
    private static final String KEY_GALE_SLAM_UNTIL = "airGaleSlamUntil";

    private AirGaleSlamRuntime() {
    }

    public static void start(ServerPlayer player, int windowTicks) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putLong(KEY_GALE_SLAM_UNTIL, GemsTime.now(player) + windowTicks);
    }

    public static boolean consumeIfActive(ServerPlayer player) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLongOr(KEY_GALE_SLAM_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        nbt.remove(KEY_GALE_SLAM_UNTIL);
        return now <= until;
    }

    public static void trigger(ServerPlayer player, Entity target) {
        int radius = GemsBalance.v().air().galeSlamRadiusBlocks();
        float bonusDamage = GemsBalance.v().air().galeSlamBonusDamage();
        double knockback = GemsBalance.v().air().galeSlamKnockback();
        if (radius <= 0 || bonusDamage <= 0.0F) {
            return;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        Vec3 center = (target != null) ? target.position() : player.position();
        AABB box = new AABB(center, center).inflate(radius);

        for (Entity entity : world.getEntities(player, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (entity instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (entity instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            living.hurtServer(world, player.damageSources().playerAttack(player), bonusDamage);
            if (knockback > 0.0D) {
                Vec3 away = living.position().subtract(center).normalize();
                living.push(away.x * knockback, 0.2D, away.z * knockback);
                living.needsSync = true;
            }
        }

        AbilityFeedback.burstAt(world, center.add(0.0D, 0.6D, 0.0D), ParticleTypes.GUST, 18, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 1.0F);
    }
}
