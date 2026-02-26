package com.feel.gems.power.gem.pillager;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;


public final class PillagerVolleyRuntime {
    private static final String KEY_UNTIL = "pillagerVolleyUntil";
    private static final String KEY_NEXT_SHOT = "pillagerVolleyNextShot";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private PillagerVolleyRuntime() {
    }

    public static void start(ServerPlayer player, int durationTicks) {
        long now = GemsTime.now(player);
        CompoundTag nbt = persistent(player);
        nbt.putLong(KEY_UNTIL, now + durationTicks);
        nbt.putLong(KEY_NEXT_SHOT, now);
        ACTIVE.add(player.getUUID());
        AbilityFeedback.sound(player, SoundEvents.CROSSBOW_QUICK_CHARGE_3, 0.9F, 1.2F);
    }

    public static void stop(ServerPlayer player) {
        stop(player, true);
    }

    private static void stop(ServerPlayer player, boolean removeFromActive) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_UNTIL);
        nbt.remove(KEY_NEXT_SHOT);
        if (removeFromActive) {
            ACTIVE.remove(player.getUUID());
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<UUID> it = ACTIVE.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                it.remove();
                continue;
            }
            GemPlayerState.initIfNeeded(player);
            if (GemPlayerState.getEnergy(player) <= 1) {
                stop(player, false);
                it.remove();
                continue;
            }
            GemId activeGem = GemPlayerState.getActiveGem(player);
            if (activeGem != GemId.PILLAGER) {
                if (activeGem != GemId.PRISM || !PrismSelectionsState.hasAbility(player, PowerIds.PILLAGER_VOLLEY)) {
                    stop(player, false);
                    it.remove();
                    continue;
                }
            }
            CompoundTag nbt = persistent(player);
            long now = GemsTime.now(player);
            long until = nbt.getLongOr(KEY_UNTIL, 0L);
            if (until <= 0 || now >= until) {
                stop(player, false);
                it.remove();
                continue;
            }
            long next = nbt.getLongOr(KEY_NEXT_SHOT, 0L);
            if (now < next) {
                continue;
            }

            int period = Math.max(1, GemsBalance.v().pillager().volleyPeriodTicks());
            nbt.putLong(KEY_NEXT_SHOT, now + period);
            fireShot(player);
        }
    }

    private static void fireShot(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        int arrows = GemsBalance.v().pillager().volleyArrowsPerShot();
        if (arrows <= 0) {
            return;
        }
        float velocity = GemsBalance.v().pillager().volleyArrowVelocity();
        float inaccuracy = GemsBalance.v().pillager().volleyArrowInaccuracy();
        float damage = GemsBalance.v().pillager().volleyArrowDamage();

        Vec3 dir = player.getViewVector(1.0F);
        Vec3 spawn = player.getEyePosition().add(dir.scale(1.2D));

        for (int i = 0; i < arrows; i++) {
            Arrow arrow = new Arrow(world, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPos(spawn.x, spawn.y, spawn.z);
            arrow.shoot(dir.x, dir.y, dir.z, velocity, inaccuracy);
            arrow.setBaseDamage(damage);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            world.addFreshEntity(arrow);
        }

        AbilityFeedback.burstAt(world, spawn, ParticleTypes.CRIT, 3, 0.05D);
        AbilityFeedback.sound(player, SoundEvents.SKELETON_SHOOT, 0.7F, 1.3F);
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
