package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpaceOrbitalLaserMiningAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_ORBITAL_LASER_MINING;
    }

    @Override
    public String name() {
        return "Orbital Laser (Mining)";
    }

    @Override
    public String description() {
        return Component.translatable("gems.ability.space.orbital_laser.mining.desc").getString();
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().orbitalLaserMiningCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().space().orbitalLaserRangeBlocks();
        HitResult hit = player.pick(range, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
            player.sendSystemMessage(Component.translatable("gems.message.no_block_target"), true);
            return false;
        }
        BlockPos target = bhr.getBlockPos();
        SpaceAnomalies.scheduleOrbitalLaser(player, target, true);
        player.sendSystemMessage(Component.translatable("gems.ability.space.orbital_laser.mining"), true);
        return true;
    }
}
