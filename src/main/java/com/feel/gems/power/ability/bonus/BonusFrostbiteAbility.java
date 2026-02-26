package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

public final class BonusFrostbiteAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_FROSTBITE;
    }

    @Override
    public String name() {
        return "Frostbite";
    }

    @Override
    public String description() {
        return "Freeze nearby enemies with slowness and mining fatigue.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(8);
        player.level().getEntities(player, area, e -> e instanceof net.minecraft.world.entity.LivingEntity)
                .forEach(e -> {
                    if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2));
                        living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 1));
                    }
                });
        return true;
    }
}
