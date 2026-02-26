package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class TerrorBreachChargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_BREACH_CHARGE;
    }

    @Override
    public String name() {
        return "Breach Charge";
    }

    @Override
    public String description() {
        return "Plants a primed charge on a targeted block or entity.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().breachChargeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().terror().breachChargeRangeBlocks();
        if (range <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.breach_charge.disabled"), true);
            return false;
        }
        Vec3 spawnPos = null;
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target != null) {
            spawnPos = target.position().add(0.0D, 0.2D, 0.0D);
        } else {
            HitResult hit = player.pick(range, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                spawnPos = blockHit.getLocation().add(0.0D, 0.1D, 0.0D);
            }
        }
        if (spawnPos == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.breach_charge.no_target"), true);
            return false;
        }

        float power = GemsBalance.v().terror().breachChargeExplosionPower();
        var world = player.level();
        world.explode(player, spawnPos.x, spawnPos.y, spawnPos.z, power, net.minecraft.world.level.Level.ExplosionInteraction.MOB);

        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.SMOKE, 12, 0.2D);
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.SOUL_FIRE_FLAME, 14, 0.15D);
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.LARGE_SMOKE, 8, 0.18D);
        AbilityFeedback.sound(player, SoundEvents.WITHER_SHOOT, 0.9F, 1.0F);
        return true;
    }
}
