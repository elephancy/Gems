package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;



public final class ReaperGraveSteedAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_GRAVE_STEED;
    }

    @Override
    public String name() {
        return "Grave Steed";
    }

    @Override
    public String description() {
        return "Summons a saddled skeleton horse mount that decays over time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().graveSteedCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.REAPER, GemsBalance.v().reaper().graveSteedDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.grave_steed.disabled"), true);
            return false;
        }
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) {
            return false;
        }
        SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (horse == null) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.grave_steed.failed"), true);
            return false;
        }
        horse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.setTrap(false);
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.3375D);
        }
        AttributeInstance maxHealth = horse.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(60.0D);
            horse.setHealth(60.0F);
        }
        horse.hurtTime = 0;
        world.addFreshEntity(horse);
        player.startRiding(horse);

        AbilityRuntime.startReaperGraveSteed(player, horse.getUUID(), duration);
        AbilityFeedback.sound(player, SoundEvents.SKELETON_HORSE_AMBIENT, 0.9F, 1.0F);
        player.sendSystemMessage(Component.translatable("gems.ability.reaper.grave_steed.summoned"), true);
        return true;
    }
}
