package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;



public final class GroupBreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.GROUP_BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Group Breezy Bash";
    }

    @Override
    public String description() {
        return "Knocks back all untrusted players nearby.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().groupBashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        int radius = GemsBalance.v().puff().groupBashRadiusBlocks();
        double kb = GemsBalance.v().puff().groupBashKnockback();
        double up = GemsBalance.v().puff().groupBashUpVelocityY();
        int affected = 0;
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            Vec3 away = other.position().subtract(player.position()).normalize();
            other.push(away.x * kb, up, away.z * kb);
            other.needsSync = true;
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 12, 0.35D);
            affected++;
        }
        AbilityFeedback.ring(world, player.position().add(0.0D, 0.2D, 0.0D), Math.min(6.0D, radius), ParticleTypes.GUST, 28);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("gems.ability.puff.group_bash.affected", affected), true);
        return true;
    }
}
