package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Entangle - vines erupt to root enemies.
 */
public final class BonusEntangleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ENTANGLE;
    }

    @Override
    public String name() {
        return "Entangle";
    }

    @Override
    public String description() {
        return "Vines erupt from the ground to root enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().entangleCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.entangleDurationSeconds * 20;
        AABB box = player.getBoundingBox().inflate(cfg.entangleRadiusBlocks);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 4, false, false));
        }
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(), player.getZ(), 20, 1.0, 0.2, 1.0, 0.02);
        return true;
    }
}
