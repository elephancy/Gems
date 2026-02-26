package com.feel.gems.power.gem.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;


public final class TerrorRigRuntime {
    private static final Map<TrapKey, RiggedTrap> ACTIVE = new HashMap<>();

    private TerrorRigRuntime() {
    }

    public static boolean arm(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        if (world.getBlockState(pos).isAir()) {
            return false;
        }
        int duration = GemsBalance.v().terror().rigDurationTicks();
        long until = duration > 0 ? GemsTime.now(player) + duration : 0L;
        TrapKey key = new TrapKey(world.dimension().identifier(), pos.immutable());
        RiggedTrap existing = ACTIVE.get(key);
        if (existing != null && !existing.owner().equals(player.getUUID())) {
            return false;
        }
        ACTIVE.put(key, new RiggedTrap(player.getUUID(), until));
        return true;
    }

    public static boolean tryTriggerUse(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        TrapKey key = new TrapKey(world.dimension().identifier(), pos.immutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean tryTriggerBreak(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        TrapKey key = new TrapKey(world.dimension().identifier(), pos.immutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean tryTriggerUpdate(ServerLevel world, BlockPos pos, BlockState before, BlockState after) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        if (before == after || before.equals(after)) {
            return false;
        }
        TrapKey key = new TrapKey(world.dimension().identifier(), pos.immutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean hasTrap(ServerLevel world, BlockPos pos) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        TrapKey key = new TrapKey(world.dimension().identifier(), pos.immutable());
        return ACTIVE.containsKey(key);
    }

    public static void checkStep(ServerPlayer player) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        TrapKey key = new TrapKey(world.dimension().identifier(), below);
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return;
        }
        trigger(world, below, key, trap);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = GemsTime.now(server);
        Iterator<Map.Entry<TrapKey, RiggedTrap>> iter = ACTIVE.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<TrapKey, RiggedTrap> entry = iter.next();
            RiggedTrap trap = entry.getValue();
            if (trap.until() > 0 && trap.until() <= now) {
                iter.remove();
            }
        }
    }

    private static void trigger(ServerLevel world, BlockPos pos, TrapKey key, RiggedTrap trap) {
        ACTIVE.remove(key);
        int fuse = GemsBalance.v().terror().rigFuseTicks();
        int tntCount = GemsBalance.v().terror().rigTntCount();
        for (int i = 0; i < tntCount; i++) {
            PrimedTnt tnt = new PrimedTnt(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, null);
            tnt.setFuse(fuse);
            world.addFreshEntity(tnt);
        }
    }

    private record TrapKey(Identifier dimension, BlockPos pos) {
    }

    private record RiggedTrap(UUID owner, long until) {
    }
}
