package com.feel.gems.power.gem.space;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.level.ServerLevel;


public final class SpaceLunarScaling {
    private SpaceLunarScaling() {
    }

    public static float multiplier(ServerLevel world) {
        if (world == null) {
            return 1.0F;
        }
        float min = GemsBalance.v().space().lunarMinMultiplier();
        float max = GemsBalance.v().space().lunarMaxMultiplier();
        if (max <= min) {
            return min;
        }

        // Vanilla moon phases: 0 = full moon, 4 = new moon.
        long day = world.getOverworldClockTime() / 24000L;
        int phase = (int) (day % 8L);
        int distToFull = Math.min(phase, 8 - phase); // 0..4
        float fullFactor = 1.0F - (distToFull / 4.0F); // 1 at full, 0 at new

        return min + fullFactor * (max - min);
    }
}
