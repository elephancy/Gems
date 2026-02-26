package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class BonusTimewarpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_TIMEWARP;
    }

    @Override
    public String name() {
        return "Timewarp";
    }

    @Override
    public String description() {
        return "Briefly slow time for enemies in an area.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().timewarpCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.timewarpDurationSeconds * 20;
        int radius = cfg.timewarpRadiusBlocks;
        int amp = cfg.timewarpSlownessAmplifier;
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amp, false, false));
        }
        return true;
    }
}
