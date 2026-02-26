package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Blinding Flash - blind nearby enemies.
 */
public final class BonusBlindingFlashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLINDING_FLASH;
    }

    @Override
    public String name() {
        return "Blinding Flash";
    }

    @Override
    public String description() {
        return "Blind all nearby enemies with an intense flash.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().blindingFlashCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.blindingFlashBlindSeconds * 20;
        AABB box = player.getBoundingBox().inflate(cfg.blindingFlashRadiusBlocks);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
        }
        world.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF),
                player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        return true;
    }
}
