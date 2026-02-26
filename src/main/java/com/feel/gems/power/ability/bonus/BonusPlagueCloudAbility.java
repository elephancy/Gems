package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plague Cloud - Create a lingering cloud that poisons and weakens.
 */
public final class BonusPlagueCloudAbility implements GemAbility {
    private static final double RANGE = 30.0;
    private static final int CLOUD_DURATION = 200; // 10 seconds
    private static final float CLOUD_RADIUS = 4.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PLAGUE_CLOUD;
    }

    @Override
    public String name() {
        return "Plague Cloud";
    }

    @Override
    public String description() {
        return "Create a lingering cloud that poisons and weakens enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(RANGE));
        
        HitResult hit = world.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 targetPos = hit.getLocation();

        // Create area effect cloud
        AreaEffectCloud cloud = new AreaEffectCloud(world, targetPos.x, targetPos.y, targetPos.z);
        cloud.setOwner(player);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setDuration(CLOUD_DURATION);
        cloud.setRadiusPerTick(-0.01f);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        
        world.addFreshEntity(cloud);

        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
