package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.puff.BreezyBashTracker;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;



public final class BreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Breezy Bash";
    }

    @Override
    public String description() {
        return "Uppercut + Impact: launches a target upward; if they land soon, they take bonus impact damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().breezyBashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().puff().breezyBashRangeBlocks());
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

        Vec3 away = target.position().subtract(player.position()).normalize();
        double knockback = GemsBalance.v().puff().breezyBashKnockback();
        target.push(away.x * knockback, GemsBalance.v().puff().breezyBashUpVelocityY(), away.z * knockback);
        target.needsSync = true;
        ServerLevel world = player.level();
        target.hurtServer(world, player.damageSources().playerAttack(player), GemsBalance.v().puff().breezyBashInitialDamage());
        AbilityFeedback.burstAt(world, target.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 16, 0.35D);
        BreezyBashTracker.track(player, target, GemsBalance.v().puff().breezyBashImpactWindowTicks());

        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 1.0F);
        return true;
    }
}
