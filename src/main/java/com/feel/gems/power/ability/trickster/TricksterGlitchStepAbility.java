package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Glitch Step - short-range teleport that leaves a damaging afterimage at your origin.
 */
public final class TricksterGlitchStepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_GLITCH_STEP;
    }

    @Override
    public String name() {
        return "Glitch Step";
    }

    @Override
    public String description() {
        return "Short-range teleport that leaves a damaging afterimage at your origin.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().glitchStepCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int distance = GemsBalance.v().trickster().glitchStepDistanceBlocks();
        float afterimgDamage = GemsBalance.v().trickster().glitchStepAfterimgDamage();
        int afterimgRadius = GemsBalance.v().trickster().glitchStepAfterimgRadiusBlocks();

        Vec3 origin = player.position();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 destination = origin.add(dir.scale(distance));

        // Teleport
        GemsTeleport.teleport(player, world, destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.needsSync = true;

        // Damage enemies near origin (afterimage explosion)
        AABB damageBox = new AABB(origin.subtract(afterimgRadius, afterimgRadius, afterimgRadius),
                                 origin.add(afterimgRadius, afterimgRadius, afterimgRadius));
        for (Entity e : world.getEntities(player, damageBox, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            ((LivingEntity) e).hurtServer(world, player.damageSources().indirectMagic(player, player), afterimgDamage);
        }

        // Visual effects
        AbilityFeedback.burstAt(world, origin.add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.8D);
        AbilityFeedback.burstAt(world, destination.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 20, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.5F);
        AbilityFeedback.sound(player, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.8F);
        return true;
    }
}
