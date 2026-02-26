package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;



public final class SpaceGravityFieldAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_GRAVITY_FIELD;
    }

    @Override
    public String name() {
        return "Gravity Field";
    }

    @Override
    public String description() {
        return "Creates a field around you that alters gravity for allies and enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().gravityFieldCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPACE, GemsBalance.v().space().gravityFieldDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.space.gravity_field.disabled"), true);
            return false;
        }
        AbilityRuntime.startSpaceGravityField(player, duration);
        player.sendSystemMessage(Component.translatable("gems.ability.space.gravity_field.activated"), true);
        return true;
    }
}
