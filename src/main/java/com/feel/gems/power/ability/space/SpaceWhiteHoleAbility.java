package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.feel.gems.power.gem.space.SpaceAnomalies;


public final class SpaceWhiteHoleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_WHITE_HOLE;
    }

    @Override
    public String name() {
        return "White Hole";
    }

    @Override
    public String description() {
        return "Creates a white hole at your position that pushes nearby enemies away.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().whiteHoleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPACE, GemsBalance.v().space().whiteHoleDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.space.white_hole.disabled"), true);
            return false;
        }
        Vec3 center = player.position().add(0.0D, 0.2D, 0.0D);
        SpaceAnomalies.spawnWhiteHole(player, center);
        player.sendSystemMessage(Component.translatable("gems.ability.space.white_hole.spawned"), true);
        return true;
    }
}
