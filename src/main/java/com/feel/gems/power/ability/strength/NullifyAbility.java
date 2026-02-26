package com.feel.gems.power.ability.strength;

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


public final class NullifyAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.NULLIFY;
    }

    @Override
    public String name() {
        return "Nullify";
    }

    @Override
    public String description() {
        return "Removes status effects from nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().nullifyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        int radius = GemsBalance.v().strength().nullifyRadiusBlocks();
        int affected = 0;
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.removeAllEffects();
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 0.8F);
        AbilityFeedback.burst(player, ParticleTypes.ENCHANT, 14, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.strength.nullify.affected", affected), true);
        return true;
    }
}
