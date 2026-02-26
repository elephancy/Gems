package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.power.gem.wealth.HotbarLock;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;


public final class HotbarLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HOTBAR_LOCK;
    }

    @Override
    public String name() {
        return "Hotbar Lock";
    }

    @Override
    public String description() {
        return "Hotbar Lock: locks an enemy to their current hotbar slot for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().hotbarLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().wealth().hotbarLockRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.WEALTH, GemsBalance.v().wealth().hotbarLockDurationTicks());
        if (target instanceof ServerPlayer other) {
            if (GemTrust.isTrusted(player, other)) {
                player.sendSystemMessage(Component.translatable("gems.message.target_trusted"), true);
                return false;
            }
            if (!VoidImmunity.canBeTargeted(player, other)) {
                player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
                return false;
            }
            HotbarLock.lock(other, other.getInventory().getSelectedSlot(), duration);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, true, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, false));
        }
        AbilityFeedback.sound(player, SoundEvents.CHAIN_PLACE, 0.8F, 1.1F);
        if (player.level() instanceof ServerLevel world) {
            AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 12, 0.2D);
        }
        player.sendSystemMessage(Component.translatable("gems.ability.wealth.hotbar_lock.activated"), true);
        return true;
    }
}
