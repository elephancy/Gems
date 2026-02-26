package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;



public final class SpaceBlackHoleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_BLACK_HOLE;
    }

    @Override
    public String name() {
        return "Black Hole";
    }

    @Override
    public String description() {
        return "Creates a black hole at your position that pulls and damages nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().blackHoleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPACE, GemsBalance.v().space().blackHoleDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.space.black_hole.disabled"), true);
            return false;
        }
        Vec3 center = player.position().add(0.0D, 0.2D, 0.0D);
        SpaceAnomalies.spawnBlackHole(player, center);
        player.sendSystemMessage(Component.translatable("gems.ability.space.black_hole.spawned"), true);
        return true;
    }
}
