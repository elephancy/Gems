package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;



public final class HealthDrainAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEALTH_DRAIN;
    }

    @Override
    public String name() {
        return "Health Drain";
    }

    @Override
    public String description() {
        return "Siphons health from a target to heal you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().healthDrainCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().healthDrainRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_trusted"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }

        float amount = GemsBalance.v().life().healthDrainAmount();
        float before = target.getHealth();
        target.hurtServer(player.level(), player.damageSources().playerAttack(player), amount);
        float dealt = Math.max(0.0F, before - target.getHealth());
        float rounded = (float) (Math.ceil(dealt * 10.0F) / 10.0F);
        if (rounded > 0.0F) {
            player.heal(rounded);
        }
        AbilityFeedback.sound(player, SoundEvents.WARDEN_HEARTBEAT, 0.7F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 10, 0.25D);
        AbilityFeedback.burstAt(player.level(), target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.DAMAGE_INDICATOR, 12, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.life.health_drain.drained", String.format(Locale.ROOT, "%.1f", rounded)), true);
        return true;
    }
}
