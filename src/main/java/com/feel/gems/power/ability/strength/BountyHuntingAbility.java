package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;


public final class BountyHuntingAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BOUNTY_HUNTING;
    }

    @Override
    public String name() {
        return "Bounty Hunting";
    }

    @Override
    public String description() {
        return "Consumes an item and tracks its last owner for a short time (if you're the last owner, it tracks the previous owner instead).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().bountyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.ability.strength.bounty.hold_item"), true);
            return false;
        }
        UUID owner = AbilityRuntime.getOwner(stack);
        if (owner == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.strength.bounty.no_owner"), true);
            return false;
        }
        UUID targetOwner = owner;
        if (owner.equals(player.getUUID())) {
            UUID previous = AbilityRuntime.getPreviousOwner(stack);
            if (previous != null && !previous.equals(player.getUUID())) {
                targetOwner = previous;
            } else {
                player.sendSystemMessage(Component.translatable("gems.ability.strength.bounty.own_item"), true);
                return false;
            }
        }
        var server = player.level().getServer();
        if (server == null) {
            return false;
        }
        if (SpySystem.hidesTracking(server, targetOwner)) {
            player.sendSystemMessage(Component.translatable("gems.tracking.hidden"), true);
            return false;
        }
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, targetOwner);
        if (snapshot == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.strength.bounty.owner_offline"), true);
            return false;
        }

        stack.shrink(1);
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.STRENGTH, GemsBalance.v().strength().bountyDurationTicks());
        AbilityRuntime.startBounty(player, targetOwner, duration);
        AbilityFeedback.sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9F, 0.9F);
        AbilityFeedback.burst(player, ParticleTypes.COMPOSTER, 10, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.strength.bounty.tracking", snapshot.name()), true);
        return true;
    }
}
