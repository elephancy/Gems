package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.feel.gems.power.gem.space.SpaceAnomalies;


public final class SpaceOrbitalLaserAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_ORBITAL_LASER;
    }

    @Override
    public String name() {
        return "Orbital Laser (Damage)";
    }

    @Override
    public String description() {
        return Component.translatable("gems.ability.space.orbital_laser.damage.desc").getString();
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().orbitalLaserCooldownTicks();
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
        SpaceAnomalies.scheduleOrbitalLaser(player, target, false);
        player.sendSystemMessage(Component.translatable("gems.ability.space.orbital_laser.damage"), true);
        return true;
    }
}
