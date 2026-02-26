package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;



public final class FluxBeamAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_BEAM;
    }

    @Override
    public String name() {
        return "Flux Beam";
    }

    @Override
    public String description() {
        return "Fires a powerful beam; damage scales with stored charge.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().fluxBeamCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().flux().fluxBeamRangeBlocks());
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }

        int charge = FluxCharge.get(player);
        float minDamage = GemsBalance.v().flux().fluxBeamMinDamage();
        float max100 = GemsBalance.v().flux().fluxBeamMaxDamageAt100();
        float max200 = GemsBalance.v().flux().fluxBeamMaxDamageAt200();
        float t100 = Math.min(charge, 100) / 100.0F;
        float tOver = Math.max(0, charge - 100) / 100.0F;
        float damage = minDamage + t100 * (max100 - minDamage) + tOver * (max200 - max100);

        int perPercent = GemsBalance.v().flux().fluxBeamArmorDamagePerPercent();
        int durabilityDamage = charge >= 100
                ? GemsBalance.v().flux().fluxBeamArmorDamageAt100() + (charge - 100) * perPercent
                : charge * perPercent;

        if (target instanceof ServerPlayer other && GemTrust.isTrusted(player, other) && GemPowers.isPassiveActive(player, PowerIds.FLUX_ALLY_INVERSION)) {
            repairArmor(other, durabilityDamage);
            beamFx(player, other.position().add(0.0D, 1.0D, 0.0D), true);
            player.sendSystemMessage(Component.translatable("gems.ability.flux.beam.repaired", charge), true);
            consumeCharge(player);
            return true;
        }

        if (target instanceof ServerPlayer victim && !VoidImmunity.canBeTargeted(player, victim)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }

        ServerLevel world = player.level();
        DamageSource source = player.damageSources().indirectMagic(player, player);
        target.hurtServer(world, source, damage);
        if (target instanceof ServerPlayer victim) {
            damageArmor(victim, durabilityDamage);
        }
        beamFx(player, target.position().add(0.0D, 1.0D, 0.0D), false);
        player.sendSystemMessage(Component.translatable("gems.ability.flux.beam.fired", charge), true);
        consumeCharge(player);
        return true;
    }

    private static void beamFx(ServerPlayer player, Vec3 hitPos, boolean healing) {
        var world = player.level();
        Vec3 from = player.getEyePosition();
        var core = healing ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ELECTRIC_SPARK;
        var trail = healing ? ParticleTypes.END_ROD : ParticleTypes.END_ROD;
        AbilityFeedback.beam(world, from, hitPos, core, 28);
        AbilityFeedback.beam(world, from, hitPos, trail, 18);
        AbilityFeedback.burstAt(world, hitPos, core, 18, 0.35D);
        AbilityFeedback.burstAt(world, hitPos, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.GUARDIAN_ATTACK, 0.9F, healing ? 1.6F : 1.1F);
        AbilityFeedback.sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.25F, healing ? 1.8F : 1.4F);
    }

    private static void consumeCharge(ServerPlayer player) {
        // Flux charge is a one-shot resource; after firing, it resets.
        FluxCharge.set(player, 0);
        FluxCharge.clearIfBelow100(player);
        GemExtraStateSync.send(player);
    }

    private static void damageArmor(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (var slot : slots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !armor.isDamageableItem()) {
                continue;
            }
            armor.setDamageValue(Math.min(armor.getMaxDamage(), armor.getDamageValue() + amount));
        }
    }

    private static void repairArmor(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (var slot : slots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !armor.isDamageableItem() || !armor.isDamaged()) {
                continue;
            }
            armor.setDamageValue(Math.max(0, armor.getDamageValue() - amount));
        }
    }
}
