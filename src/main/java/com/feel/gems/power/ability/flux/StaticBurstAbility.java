package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;



public final class StaticBurstAbility implements GemAbility {
    private static final String KEY_STORED_DAMAGE = "fluxStoredDamage";
    private static final String KEY_STORED_AT = "fluxStoredDamageAt";

    @Override
    public Identifier id() {
        return PowerIds.STATIC_BURST;
    }

    @Override
    public String name() {
        return "Static Burst";
    }

    @Override
    public String description() {
        return "Releases damage you've stored recently as an area burst.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().staticBurstCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        float stored = nbt.getFloatOr(KEY_STORED_DAMAGE, 0.0F);
        if (stored <= 0.0F) {
            player.sendSystemMessage(Component.translatable("gems.ability.flux.static_burst.no_stored"), true);
            return false;
        }

        ServerLevel world = player.level();
        int hits = 0;
        float damage = Math.min(GemsBalance.v().flux().staticBurstMaxDamage(), stored);
        int radius = GemsBalance.v().flux().staticBurstRadiusBlocks();
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.hurtServer(world, player.damageSources().indirectMagic(player, player), damage);
            AbilityFeedback.burstAt(world, other.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 14, 0.35D);
            hits++;
        }
        nbt.putFloat(KEY_STORED_DAMAGE, 0.0F);
        nbt.putLong(KEY_STORED_AT, GemsTime.now(world));
        AbilityFeedback.sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.ELECTRIC_SPARK, 24, 0.5D);
        player.sendSystemMessage(Component.translatable("gems.ability.flux.static_burst.hit", hits), true);
        return true;
    }

    public static void onDamaged(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.FLUX_CHARGE_STORAGE)) {
            return;
        }
        CompoundTag nbt = persistent(player);
        long now = GemsTime.now(player);
        long storedAt = nbt.getLongOr(KEY_STORED_AT, 0L);
        if (storedAt <= 0 || now - storedAt > GemsBalance.v().flux().staticBurstStoreWindowTicks()) {
            nbt.putFloat(KEY_STORED_DAMAGE, 0.0F);
            nbt.putLong(KEY_STORED_AT, now);
        }
        nbt.putFloat(KEY_STORED_DAMAGE, nbt.getFloatOr(KEY_STORED_DAMAGE, 0.0F) + amount);

        if (GemPowers.isPassiveActive(player, PowerIds.FLUX_CONDUCTIVITY)) {
            int perDamage = GemsBalance.v().flux().fluxConductivityChargePerDamage();
            int maxPerHit = GemsBalance.v().flux().fluxConductivityMaxChargePerHit();
            int add = Math.min(maxPerHit, Math.round(amount * perDamage));
            if (add > 0) {
                int before = FluxCharge.get(player);
                int next = Math.min(200, before + add);
                if (next != before) {
                    FluxCharge.set(player, next);
                    FluxCharge.clearIfBelow100(player);
                    GemExtraStateSync.send(player);
                }
            }
        }
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
