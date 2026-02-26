package com.feel.gems.power.gem.speed;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.level.ServerPlayer;


public final class SpeedMomentum {
    private SpeedMomentum() {
    }

    public static float normalized(ServerPlayer player) {
        var cfg = GemsBalance.v().speed();
        double minSpeed = cfg.momentumMinSpeed();
        double maxSpeed = cfg.momentumMaxSpeed();
        if (maxSpeed <= minSpeed) {
            return 0.0F;
        }
        double vx = player.getDeltaMovement().x;
        double vz = player.getDeltaMovement().z;
        double speed = Math.sqrt(vx * vx + vz * vz);
        if (speed <= 1.0E-4D) {
            double dx = player.getX() - player.xo;
            double dz = player.getZ() - player.zo;
            speed = Math.sqrt(dx * dx + dz * dz);
        }
        double t = (speed - minSpeed) / (maxSpeed - minSpeed);
        if (t <= 0.0D) {
            return 0.0F;
        }
        if (t >= 1.0D) {
            return 1.0F;
        }
        return (float) t;
    }

    public static float multiplier(ServerPlayer player) {
        var cfg = GemsBalance.v().speed();
        float minMul = cfg.momentumMinMultiplier();
        float maxMul = cfg.momentumMaxMultiplier();
        if (maxMul < minMul) {
            float tmp = minMul;
            minMul = maxMul;
            maxMul = tmp;
        }
        float t = normalized(player);
        return minMul + (maxMul - minMul) * t;
    }

    public static int scaleInt(int base, float multiplier, int minValue) {
        if (base <= 0) {
            return minValue;
        }
        int scaled = Math.round(base * multiplier);
        return Math.max(minValue, scaled);
    }

    public static double scaleDouble(double base, float multiplier) {
        return base * multiplier;
    }

    public static float scaleFloat(float base, float multiplier) {
        return base * multiplier;
    }
}
