package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import com.feel.gems.power.gem.life.HeartLockRuntime;



public final class HeartLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEART_LOCK;
    }

    @Override
    public String name() {
        return "Heart Lock";
    }

    @Override
    public String description() {
        return "Temporarily locks an enemy's max health to their current health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().heartLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().heartLockRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.LIFE, GemsBalance.v().life().heartLockDurationTicks());
        if (target instanceof ServerPlayer other) {
            if (GemTrust.isTrusted(player, other)) {
                player.sendSystemMessage(Component.translatable("gems.message.target_trusted"), true);
                return false;
            }
            if (!VoidImmunity.canBeTargeted(player, other)) {
                player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
                return false;
            }
            AbilityRuntime.startHeartLock(player, other, duration);
        } else {
            if (target instanceof net.minecraft.world.entity.Mob mob) {
                HeartLockRuntime.apply(player, mob, duration);
            }
        }
        AbilityFeedback.sound(player, SoundEvents.CHAIN_PLACE, 0.8F, 0.9F);
        AbilityFeedback.burstAt(player.level(), target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 18, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.life.heart_lock.applied"), true);
        return true;
    }
}
