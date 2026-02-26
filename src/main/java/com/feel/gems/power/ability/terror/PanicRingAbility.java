package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;


public final class PanicRingAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_PANIC_RING;
    }

    @Override
    public String name() {
        return "Panic Ring";
    }

    @Override
    public String description() {
        return "Spawns primed TNT around you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().panicRingCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int count = GemsBalance.v().terror().panicRingTntCount();
        if (count <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.terror.panic_ring.no_tnt"), true);
            return false;
        }

        var world = player.level();
        Vec3 center = player.position();
        double radius = GemsBalance.v().terror().panicRingRadiusBlocks();
        int fuse = GemsBalance.v().terror().panicRingFuseTicks();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            Vec3 pos = center.add(Math.cos(angle) * radius, 0.1D, Math.sin(angle) * radius);
            PrimedTnt tnt = new PrimedTnt(world, pos.x, pos.y, pos.z, player);
            tnt.setFuse(fuse);
            world.addFreshEntity(tnt);
        }

        AbilityFeedback.burst(player, ParticleTypes.SMOKE, 18, 0.35D);
        AbilityFeedback.sound(player, SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        return true;
    }
}

