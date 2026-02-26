package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;



public final class AstralDaggersAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ASTRAL_DAGGERS;
    }

    @Override
    public String name() {
        return "Astral Daggers";
    }

    @Override
    public String description() {
        return "Fires a volley of fast ranged daggers.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().astralDaggersCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int count = GemsBalance.v().astra().astralDaggersCount();
        float spreadAmount = GemsBalance.v().astra().astralDaggersSpread();
        float velocity = GemsBalance.v().astra().astralDaggersVelocity();
        float damage = GemsBalance.v().astra().astralDaggersDamage();

        Vec3 direction = player.getViewVector(1.0F);
        Vec3 spawnPos = player.getEyePosition().add(direction.scale(1.2D));

        ServerLevel world = player.level();
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.END_ROD, Math.min(24, count * 3), 0.12D);

        for (int i = 0; i < count; i++) {
            // 1.21+ validates that the "weapon" stack is a valid projectile weapon for arrows.
            Arrow arrow = new Arrow(world, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            Vec3 spread = direction.add(
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount,
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount,
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount
            );
            arrow.shoot(spread.x, spread.y, spread.z, velocity, 0.0F);
            arrow.setBaseDamage(damage);
            arrow.setCritArrow(false);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            world.addFreshEntity(arrow);
        }

        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
        return true;
    }
}
