package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;



public final class PillagerFangsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_FANGS;
    }

    @Override
    public String name() {
        return "Fangs";
    }

    @Override
    public String description() {
        return "Conjures evoker fangs in a line at the target zone.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().fangsCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().pillager().fangsRangeBlocks();
        ServerLevel world = player.level();

        LivingEntity living = Targeting.raycastLiving(player, range);
        Vec3 center;
        if (living != null) {
            center = living.position();
        } else {
            HitResult hit = player.pick(range, 1.0F, false);
            if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
                player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
                return false;
            }
            BlockPos pos = bhr.getBlockPos();
            center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D);
        }

        int count = GemsBalance.v().pillager().fangsCount();
        float spacing = GemsBalance.v().pillager().fangsSpacingBlocks();
        int warmupStep = GemsBalance.v().pillager().fangsWarmupStepTicks();

        Vec3 toCenter = center.subtract(player.position());
        double distSq = toCenter.lengthSqr();

        // Close range: evoker-style ring of fangs around the target.
        if (distSq < 9.0D) {
            double radius = Math.max(0.8D, spacing);
            for (int i = 0; i < Math.max(8, count); i++) {
                double angle = (Math.PI * 2.0D * i) / Math.max(8, count);
                double x = center.x + radius * Math.cos(angle);
                double z = center.z + radius * Math.sin(angle);
                EvokerFangs fangs = new EvokerFangs(world, x, center.y, z, (float) Math.toDegrees(angle), i * warmupStep, player);
                world.addFreshEntity(fangs);
            }
        } else {
            // Far: line of fangs marching forward from the impact point.
            Vec3 dir = player.getViewVector(1.0F).scale(Math.max(0.1D, spacing));
            float yaw = player.getYRot();
            for (int i = 0; i < count; i++) {
                Vec3 p = center.add(dir.scale(i));
                EvokerFangs fangs = new EvokerFangs(world, p.x, p.y, p.z, yaw, i * warmupStep, player);
                world.addFreshEntity(fangs);
            }
        }

        AbilityFeedback.burstAt(world, center.add(0.0D, 0.1D, 0.0D), ParticleTypes.CRIT, 18, 0.25D);
        AbilityFeedback.sound(player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
        return true;
    }
}

