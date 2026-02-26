package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

public final class BonusArcaneMissilesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ARCANE_MISSILES;
    }

    @Override
    public String name() {
        return "Arcane Missiles";
    }

    @Override
    public String description() {
        return "Fire a barrage of magical missiles at your target.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        Vec3 direction = player.getLookAngle();
        
        for (int i = 0; i < 5; i++) {
            Vec3 offset = new Vec3(
                    direction.x + (world.getRandom().nextDouble() - 0.5) * 0.2,
                    direction.y + (world.getRandom().nextDouble() - 0.5) * 0.2,
                    direction.z + (world.getRandom().nextDouble() - 0.5) * 0.2);
            SmallFireball fireball = new SmallFireball(world, player, offset);
            fireball.setPos(player.getEyePosition().add(direction.scale(0.5)));
            world.addFreshEntity(fireball);
        }
        return true;
    }
}
