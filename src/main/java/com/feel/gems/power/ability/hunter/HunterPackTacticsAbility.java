package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
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

public final class HunterPackTacticsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_PACK_TACTICS;
    }

    @Override
    public String name() {
        return "Pack Tactics";
    }

    @Override
    public String description() {
        return "Nearby trusted allies deal 20% more damage to your marked target for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().packTacticsCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int radius = GemsBalance.v().hunter().packTacticsRadiusBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.HUNTER, GemsBalance.v().hunter().packTacticsDurationTicks());

        // Need a marked target
        var markedTarget = HunterPreyMarkRuntime.getMarkedTarget(player);
        if (markedTarget == null) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Find trusted allies in range
        AABB box = player.getBoundingBox().inflate(radius);
        int alliesBuffed = 0;

        for (Entity e : world.getEntities(player, box, ent -> ent instanceof ServerPlayer)) {
            ServerPlayer ally = (ServerPlayer) e;
            if (!GemTrust.isTrusted(player, ally)) continue;
            if (VoidImmunity.shouldBlockEffect(player, ally)) continue;

            // Grant pack tactics buff to ally against the marked target
            HunterPackTacticsRuntime.grantBuff(ally, markedTarget.getUUID(), durationTicks);
            AbilityFeedback.burstAt(world, ally.position().add(0, 1, 0), ParticleTypes.ENCHANT, 15, 0.5D);
            alliesBuffed++;
        }

        // Also grant to self
        HunterPackTacticsRuntime.grantBuff(player, markedTarget.getUUID(), durationTicks);

        AbilityFeedback.burstAt(world, player.position().add(0, 1.5, 0), ParticleTypes.ENCHANT, 25, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
        return true;
    }
}
