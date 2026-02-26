package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class DuelistFlourishAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DUELIST_FLOURISH;
    }

    @Override
    public String name() {
        return "Flourish";
    }

    @Override
    public String description() {
        return "Quick 360 sword sweep that hits all nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().flourishCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int radius = GemsBalance.v().duelist().flourishRadiusBlocks();
        float damage = GemsBalance.v().duelist().flourishDamage();

        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity e : world.getEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            ((LivingEntity) e).hurtServer(world, player.damageSources().playerAttack(player), damage);
            AbilityFeedback.burstAt(world, e.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SWEEP_ATTACK, 8, 0.2D);
        }

        // Ring particle effect
        AbilityFeedback.ring(world, player.position().add(0.0D, 1.0D, 0.0D), radius, ParticleTypes.SWEEP_ATTACK, 24);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        return true;
    }
}
