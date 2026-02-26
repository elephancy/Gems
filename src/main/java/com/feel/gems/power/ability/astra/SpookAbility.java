package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;



public final class SpookAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPOOK;
    }

    @Override
    public String name() {
        return "Spook";
    }

    @Override
    public String description() {
        return "Briefly disorients nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().spookCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().spookDurationTicks());
        int radius = GemsBalance.v().astra().spookRadiusBlocks();
        int affected = 0;
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, true, false, false));
            other.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, 0, true, false, false));
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 12, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_STARE, 0.8F, 0.8F);
        player.sendSystemMessage(Component.translatable("gems.ability.astra.spook.affected", affected), true);
        return true;
    }
}
