package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SentinelLockdownAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_LOCKDOWN;
    }

    @Override
    public String name() {
        return "Lockdown";
    }

    @Override
    public String description() {
        return "Create a zone where enemies cannot use abilities for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().lockdownCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int radius = GemsBalance.v().sentinel().lockdownRadiusBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.SENTINEL, GemsBalance.v().sentinel().lockdownDurationTicks());

        Vec3 center = player.position();
        long endTime = world.getGameTime() + durationTicks;

        // Create the lockdown zone
        SentinelLockdownRuntime.createZone(player.getUUID(), center, radius, endTime, world.dimension().identifier().toString());

        // Apply effect to enemies currently in the zone
        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity e : world.getEntities(player, box, ent -> ent instanceof ServerPlayer)) {
            ServerPlayer target = (ServerPlayer) e;
            if (GemTrust.isTrusted(player, target)) continue;
            if (!VoidImmunity.canBeTargeted(player, target)) continue;

            AbilityFeedback.burstAt(world, target.position().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 15, 0.3D);
        }

        AbilityFeedback.ring(world, center.add(0, 0.1, 0), radius, ParticleTypes.ENCHANTED_HIT, 32);
        AbilityFeedback.sound(player, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.5F);
        return true;
    }
}
