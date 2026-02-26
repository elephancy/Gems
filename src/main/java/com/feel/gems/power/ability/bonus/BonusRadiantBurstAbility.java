package com.feel.gems.power.ability.bonus;

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

public final class BonusRadiantBurstAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_RADIANT_BURST;
    }

    @Override
    public String name() {
        return "Radiant Burst";
    }

    @Override
    public String description() {
        return "Emit a burst of light that blinds enemies and heals allies.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        AABB area = player.getBoundingBox().inflate(10);
        
        world.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 50, 5, 2, 5, 0.2);
        
        world.getEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof ServerPlayer other) {
                        if (VoidImmunity.shouldBlockEffect(player, other)) {
                            return;
                        }
                        // Heal allies, blind enemies.
                        if (GemTrust.isTrusted(player, other)) {
                            other.heal(6.0f);
                        } else {
                            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                            other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                        }
                    } else if (e instanceof LivingEntity living) {
                        // Blind enemies
                        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    }
                });
        return true;
    }
}
