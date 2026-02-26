package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class SentinelInterventionAbility implements GemAbility {
    public static final String INTERVENTION_TARGET_KEY = "sentinel_intervention_target";
    public static final String INTERVENTION_ACTIVE_KEY = "sentinel_intervention_active";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_INTERVENTION;
    }

    @Override
    public String name() {
        return "Intervention";
    }

    @Override
    public String description() {
        return "Instantly teleport to a mutually trusted ally and absorb the next hit they would take.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().interventionCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().sentinel().interventionRangeBlocks();

        // Find nearest trusted ally within range
        AABB box = player.getBoundingBox().inflate(range);
        ServerPlayer target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : world.getEntities(player, box, ent -> ent instanceof ServerPlayer)) {
            ServerPlayer ally = (ServerPlayer) e;
            if (!GemTrust.isTrusted(player, ally)) continue;
            if (!GemTrust.isTrusted(ally, player)) continue;
            if (VoidImmunity.shouldBlockEffect(player, ally)) continue;

            double dist = ally.distanceToSqr(player);
            if (dist < closestDist) {
                closestDist = dist;
                target = ally;
            }
        }

        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Teleport to ally
        GemsTeleport.teleport(player, world, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        player.needsSync = true;

        // Set up damage absorption for ally
        SentinelInterventionRuntime.setProtecting(player, target);

        AbilityFeedback.burstAt(world, player.position().add(0, 1, 0), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.burstAt(world, target.position().add(0, 1, 0), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);
        return true;
    }
}
