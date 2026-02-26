package com.feel.gems.power.ability.fire;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.RangeLimitedProjectile;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;



public final class FireballAbility implements GemAbility {
    private static final String KEY_CHARGE_START = "fireballChargeStart";
    private static final String KEY_LAST_FIRE = "fireballLastFire";
    private static final String KEY_LAST_SHOWN_CHARGE = "fireballLastShownCharge";
    private static final int CHARGE_BAR_SEGMENTS = 20;

    @Override
    public Identifier id() {
        return PowerIds.FIREBALL;
    }

    @Override
    public String name() {
        return "Fireball";
    }

    @Override
    public String description() {
        return "Press once to start charging; press again to launch an explosive fireball.";
    }

    @Override
    public int cooldownTicks() {
        return 0; // internal cooldown (charge vs fire)
    }

    @Override
    public boolean activate(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long now = GemsTime.now(player);
        long lastFire = nbt.getLongOr(KEY_LAST_FIRE, 0L);
        if (now - lastFire < GemsBalance.v().fire().fireballInternalCooldownTicks()) {
            player.sendSystemMessage(Component.translatable("gems.ability.fire.fireball.on_cooldown"), true);
            return true;
        }

        long start = nbt.getLongOr(KEY_CHARGE_START, 0L);
        if (start <= 0) {
            nbt.putLong(KEY_CHARGE_START, now);
            nbt.putInt(KEY_LAST_SHOWN_CHARGE, -1);
            AbilityFeedback.sound(player, SoundEvents.FIRE_AMBIENT, 0.7F, 1.2F);
            AbilityFeedback.burst(player, ParticleTypes.SMALL_FLAME, 10, 0.2D);
            player.sendSystemMessage(chargeBarText(0), true);
            return true;
        }

        int charge = computeCharge(player.level(), player, start, now);
        nbt.remove(KEY_CHARGE_START);
        nbt.remove(KEY_LAST_SHOWN_CHARGE);
        nbt.putLong(KEY_LAST_FIRE, now);
        launch(player, charge);
        AbilityFeedback.burst(player, ParticleTypes.FLAME, 14, 0.25D);
        AbilityFeedback.burst(player, ParticleTypes.SMOKE, 10, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.fire.fireball.launched").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static void tickCharging(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long start = nbt.getLongOr(KEY_CHARGE_START, 0L);
        if (start <= 0) {
            return;
        }
        int charge = computeCharge(player.level(), player, start, now);
        int bucket = chargeBucket(charge);
        int last = nbt.getIntOr(KEY_LAST_SHOWN_CHARGE, -1);
        if (bucket == last) {
            return;
        }
        nbt.putInt(KEY_LAST_SHOWN_CHARGE, bucket);
        player.sendSystemMessage(chargeBarText(charge), true);
    }

    private static int chargeBucket(int chargePercent) {
        int clamped = Math.max(0, Math.min(100, chargePercent));
        return (clamped * CHARGE_BAR_SEGMENTS) / 100;
    }

    private static Component chargeBarText(int chargePercent) {
        int clamped = Math.max(0, Math.min(100, chargePercent));
        int filled = chargeBucket(clamped);

        ChatFormatting fillColor;
        if (clamped >= 100) {
            fillColor = ChatFormatting.GOLD;
        } else if (clamped >= 67) {
            fillColor = ChatFormatting.GREEN;
        } else if (clamped >= 34) {
            fillColor = ChatFormatting.YELLOW;
        } else {
            fillColor = ChatFormatting.RED;
        }

        var bar = Component.literal("[");
        for (int i = 0; i < CHARGE_BAR_SEGMENTS; i++) {
            boolean on = i < filled;
            bar.append(Component.literal(on ? "|" : ".").withStyle(on ? fillColor : ChatFormatting.DARK_GRAY));
        }
        bar.append(Component.literal("]"));

        var percent = Component.literal(" " + clamped + "%").withStyle(ChatFormatting.GRAY);
        var message = Component.translatable("gems.ability.fire.fireball.charge").withStyle(ChatFormatting.GOLD)
                .append(bar)
                .append(percent);
        if (clamped >= 100) {
            message.append(Component.translatable("gems.ability.fire.fireball.ready").withStyle(ChatFormatting.GOLD));
        }
        return message;
    }

    private static int computeCharge(ServerLevel world, ServerPlayer player, long start, long now) {
        int chargeUpTicks = GemsBalance.v().fire().fireballChargeUpTicks();
        int chargeDownTicks = GemsBalance.v().fire().fireballChargeDownTicks();

        long elapsed = now - start;
        if (elapsed <= chargeUpTicks) {
            return (int) Math.round((elapsed * 100.0D) / Math.max(1, chargeUpTicks));
        }

        BlockPos below = player.blockPosition().below();
        boolean onObsidian = world.getBlockState(below).is(Blocks.OBSIDIAN);
        if (onObsidian) {
            return 100;
        }

        long downElapsed = elapsed - chargeUpTicks;
        if (downElapsed >= chargeDownTicks) {
            return 0;
        }
        double remaining = 1.0D - (downElapsed / (double) Math.max(1, chargeDownTicks));
        return (int) Math.round(100.0D * remaining);
    }

    private static void launch(ServerPlayer player, int chargePercent) {
        ServerLevel world = player.level();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 spawnPos = player.getEyePosition().add(direction.scale(1.5D));

        int power = 1 + (chargePercent / 50);
        LargeFireball fireball = new LargeFireball(world, player, direction, power);
        fireball.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
        if (fireball instanceof RangeLimitedProjectile limited) {
            limited.gems$setRangeLimit(spawnPos, GemsBalance.v().fire().fireballMaxDistanceBlocks());
        }
        world.addFreshEntity(fireball);

        AbilityFeedback.sound(player, SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
