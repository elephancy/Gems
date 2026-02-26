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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class SentinelRallyCryAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_RALLY_CRY;
    }

    @Override
    public String name() {
        return "Rally Cry";
    }

    @Override
    public String description() {
        return "Heal all nearby trusted allies and grant Resistance I for 8s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().rallyCryCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int radius = GemsBalance.v().sentinel().rallyCryRadiusBlocks();
        float healAmount = GemsBalance.v().sentinel().rallyCryHealHearts() * 2.0F; // Convert to half-hearts
        int resistanceTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.SENTINEL, GemsBalance.v().sentinel().rallyCryResistanceDurationTicks());

        AABB box = player.getBoundingBox().inflate(radius);
        int alliesHealed = 0;

        for (Entity e : world.getEntities(player, box, ent -> ent instanceof ServerPlayer)) {
            ServerPlayer ally = (ServerPlayer) e;
            if (!GemTrust.isTrusted(player, ally)) continue;
            if (VoidImmunity.shouldBlockEffect(player, ally)) continue;

            ally.heal(healAmount);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, resistanceTicks, 0, false, true));
            AbilityFeedback.burstAt(world, ally.position().add(0, 1, 0), ParticleTypes.HEART, 10, 0.5D);
            alliesHealed++;
        }

        // Also affect self
        player.heal(healAmount);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, resistanceTicks, 0, false, true));

        AbilityFeedback.ring(world, player.position().add(0, 1, 0), radius, ParticleTypes.HEART, 20);
        AbilityFeedback.sound(player, SoundEvents.GOAT_HORN_BREAK, 1.0F, 1.0F);
        return true;
    }
}
