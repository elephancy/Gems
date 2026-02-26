package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BonusShadowstepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SHADOWSTEP;
    }

    @Override
    public String name() {
        return "Shadowstep";
    }

    @Override
    public String description() {
        return "Teleport a short distance in the direction you're looking.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(15));
        
        HitResult hit = world.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(direction.scale(0.5));
        
        world.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.1);
        GemsTeleport.teleport(player, world, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        world.sendParticles(ParticleTypes.PORTAL, target.x, target.y + 1, target.z, 30, 0.5, 1, 0.5, 0.1);
        
        return true;
    }
}
