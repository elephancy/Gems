package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HunterHuntingTrapAbility implements GemAbility {
    public static final Map<TrapKey, TrapData> ACTIVE_TRAPS = new HashMap<>();

    public record TrapKey(String worldId, BlockPos pos) {}

    @Override
    public Identifier id() {
        return PowerIds.HUNTER_HUNTING_TRAP;
    }

    @Override
    public String name() {
        return "Hunting Trap";
    }

    @Override
    public String description() {
        return "Place an invisible trap that roots and damages the first enemy who walks over it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().huntingTrapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        BlockPos trapPos = player.blockPosition();
        String worldId = world.dimension().identifier().toString();

        int lifetimeTicks = GemsBalance.v().hunter().huntingTrapLifetimeTicks();
        long expireTime = world.getGameTime() + lifetimeTicks;

        TrapData trap = new TrapData(player.getUUID(), expireTime, worldId);
        ACTIVE_TRAPS.put(new TrapKey(worldId, trapPos), trap);

        AbilityFeedback.burstAt(world, Vec3.atCenterOf(trapPos), ParticleTypes.ENCHANT, 15, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.TRIPWIRE_ATTACH, 0.5F, 1.2F);
        return true;
    }

    public static void checkStep(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerLevel world = player.level();
        String worldId = world.dimension().identifier().toString();
        BlockPos pos = player.blockPosition();
        TrapKey key = new TrapKey(worldId, pos);
        TrapData trap = ACTIVE_TRAPS.get(key);
        if (trap == null) {
            return;
        }
        if (player.getUUID().equals(trap.ownerId)) {
            return;
        }

        ServerPlayer owner = null;
        MinecraftServer server = world.getServer();
        if (server != null) {
            owner = server.getPlayerList().getPlayer(trap.ownerId);
            if (owner != null) {
                if (com.feel.gems.trust.GemTrust.isTrusted(owner, player)) {
                    return;
                }
                if (VoidImmunity.shouldBlockEffect(owner, player)) {
                    ACTIVE_TRAPS.remove(key);
                    return;
                }
            }
        }

        TrapData consumed = ACTIVE_TRAPS.remove(key);
        if (consumed == null) {
            return;
        }
        triggerTrap(consumed, player, pos, world, owner);
    }

    public static void tickWorld(ServerLevel world) {
        if (world == null || ACTIVE_TRAPS.isEmpty()) {
            return;
        }

        String worldId = world.dimension().identifier().toString();
        MinecraftServer server = world.getServer();
        long now = world.getGameTime();

        Iterator<Map.Entry<TrapKey, TrapData>> it = ACTIVE_TRAPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TrapKey, TrapData> entry = it.next();
            TrapKey trapKey = entry.getKey();
            if (!worldId.equals(trapKey.worldId)) {
                continue;
            }
            BlockPos pos = trapKey.pos;
            TrapData trap = entry.getValue();
            if (trap.expireTime <= now) {
                it.remove();
                continue;
            }

            // Trigger on mobs that walk over the trap too, but keep the per-player fast-path in checkStep().
            AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            var mobs = world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
                    e -> e.isAlive() && !(e instanceof ServerPlayer));
            if (mobs.isEmpty()) {
                continue;
            }

            ServerPlayer owner = server != null ? server.getPlayerList().getPlayer(trap.ownerId) : null;
            net.minecraft.world.entity.LivingEntity victim = mobs.getFirst();

            // Consume trap.
            it.remove();

            triggerTrap(trap, victim, pos, world, owner);
        }
    }

    private static void triggerTrap(TrapData trap, net.minecraft.world.entity.LivingEntity victim, BlockPos pos, ServerLevel world, ServerPlayer owner) {
        float damage = GemsBalance.v().hunter().huntingTrapDamage();
        int rootTicks = GemsBalance.v().hunter().huntingTrapRootTicks();

        if (owner != null) {
            victim.hurtServer(world, owner.damageSources().indirectMagic(owner, owner), damage);
        } else {
            victim.hurtServer(world, world.damageSources().magic(), damage);
        }
        // Max slowness = rooted
        if (owner != null && victim instanceof ServerPlayer playerVictim) {
            // Respect Void immunity / trust for players; mobs are always valid victims.
            if (com.feel.gems.trust.GemTrust.isTrusted(owner, playerVictim) || VoidImmunity.shouldBlockEffect(owner, playerVictim)) {
                return;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, rootTicks, 127, true, false, false), owner);
        } else {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, rootTicks, 127, true, false, false));
        }

        AbilityFeedback.burstAt(world, Vec3.atCenterOf(pos).add(0, 1, 0), ParticleTypes.CRIT, 20, 0.5D);
        if (victim instanceof ServerPlayer playerVictim) {
            AbilityFeedback.sound(playerVictim, SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.5F);
        } else {
            world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.IRON_GOLEM_HURT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F, 0.5F);
        }
    }

    public static void cleanExpiredTraps(ServerLevel world) {
        long currentTime = world.getGameTime();
        String worldId = world.dimension().identifier().toString();
        ACTIVE_TRAPS.entrySet().removeIf(entry ->
                entry.getKey().worldId.equals(worldId) && currentTime > entry.getValue().expireTime
        );
    }

    public record TrapData(UUID ownerId, long expireTime, String worldId) {}
}
