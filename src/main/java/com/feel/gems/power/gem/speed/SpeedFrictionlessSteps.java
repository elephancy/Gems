package com.feel.gems.power.gem.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


public final class SpeedFrictionlessSteps {
    private SpeedFrictionlessSteps() {
    }

    public static void tick(MinecraftServer server) {
        if ((server.getTickCount() % 5) != 0) {
            return; // run every 5 ticks to cut per-tick scans
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SPEED_FRICTIONLESS)) {
            return;
        }
        if (!isOnFrictionBlock(player)) {
            return;
        }
        int amp = GemsBalance.v().speed().frictionlessSpeedAmplifier();
        if (amp < 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, amp, true, false, false));
    }

    private static boolean isOnFrictionBlock(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition());
        if (isFrictionBlock(state)) {
            return true;
        }
        BlockState below = player.level().getBlockState(player.blockPosition().below());
        return isFrictionBlock(below);
    }

    private static boolean isFrictionBlock(BlockState state) {
        return state.is(Blocks.COBWEB)
                || state.is(Blocks.HONEY_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }
}
