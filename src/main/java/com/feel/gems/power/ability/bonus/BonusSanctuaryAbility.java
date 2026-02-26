package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BonusSanctuaryAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SANCTUARY;
    }

    @Override
    public String name() {
        return "Sanctuary";
    }

    @Override
    public String description() {
        return "Create a protective zone that heals allies and pushes away enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 1000; // 50 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        AABB area = player.getBoundingBox().inflate(6);
        
        world.sendParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.5, player.getZ(), 100, 3, 0.5, 3, 0.1);
        
        world.getEntities(player, area).forEach(e -> {
            if (e instanceof ServerPlayer otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    return;
                }
                // Trusted players = allies, untrusted = enemies
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
                } else {
                    // Push enemy players away
                    pushAway(player, e);
                }
            } else if (e instanceof LivingEntity) {
                // Push mobs away
                pushAway(player, e);
            }
        });
        
        // Also buff self
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        return true;
    }

    private void pushAway(ServerPlayer player, Entity e) {
        Vec3 direction = e.getEyePosition().subtract(player.getEyePosition()).normalize().scale(1.5);
        e.setDeltaMovement(direction.x, 0.5, direction.z);
        e.needsSync = true;
    }
}
