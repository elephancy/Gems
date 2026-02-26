package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BonusChainLightningAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CHAIN_LIGHTNING;
    }

    @Override
    public String name() {
        return "Chain Lightning";
    }

    @Override
    public String description() {
        return "Strike multiple nearby enemies with chained lightning.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        AABB area = player.getBoundingBox().inflate(15);
        
        List<LivingEntity> targets = world.getEntities(player, area, e -> e instanceof LivingEntity && !(e instanceof ServerPlayer))
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .limit(5)
                .toList();
        
        for (LivingEntity target : targets) {
            LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                Vec3 pos = target.position();
                lightning.snapTo(pos.x, pos.y, pos.z);
                world.addFreshEntity(lightning);
            }
        }
        return true;
    }
}
