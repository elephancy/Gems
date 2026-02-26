package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.config.GemsBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Decoy Trap - Place a fake diamond item that explodes when picked up by enemies.
 */
public final class BonusDecoyTrapAbility implements GemAbility {
    // Track which item entities are traps and who placed them.
    // Maps itemUuid -> trap data, insertion order preserved for eviction.
    private static final Map<UUID, TrapEntry> TRAP_ITEMS = new LinkedHashMap<>();
    // Counter to periodically cleanup stale entries.
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 20; // Every 20 activations

    @Override
    public Identifier id() {
        return PowerIds.BONUS_DECOY_TRAP;
    }

    @Override
    public String name() {
        return "Decoy Trap";
    }

    @Override
    public String description() {
        return "Place a fake valuable item that explodes when picked up by enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().decoyTrapCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        long now = world.getGameTime();
        var cfg = GemsBalance.v().bonusPool();
        
        // Periodic cleanup to prevent memory leaks
        if (++cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupStaleTraps(world);
            pruneExpiredTraps(world, now);
        }
        
        // Enforce max traps limit
        int maxTraps = Math.max(0, cfg.decoyTrapMaxActive);
        if (maxTraps > 0 && TRAP_ITEMS.size() >= maxTraps) {
            // Remove oldest trap (first entry)
            Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
            if (it.hasNext()) {
                UUID toRemove = it.next().getKey();
                it.remove();
                discardTrapEntity(world.getServer(), toRemove);
            }
        }
        
        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 pos = playerPos.add(player.getViewVector(1.0f).scale(2));
        
        // Create a fake diamond item
        ItemStack fakeItem = new ItemStack(Items.DIAMOND);
        fakeItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("§6Shiny Diamond").withStyle(s -> s.withItalic(false)));
        
        ItemEntity trapItem = new ItemEntity(world, pos.x, pos.y + 0.5, pos.z, fakeItem);
        int armTicks = Math.max(0, cfg.decoyTrapArmTimeSeconds) * 20;
        trapItem.setPickUpDelay(armTicks);
        
        world.addFreshEntity(trapItem);
        long despawnTicks = Math.max(0, cfg.decoyTrapDespawnSeconds) * 20L;
        long expiresAt = despawnTicks > 0 ? now + despawnTicks : Long.MAX_VALUE;
        TRAP_ITEMS.put(trapItem.getUUID(), new TrapEntry(player.getUUID(), world.dimension(), expiresAt));
        
        // Visual feedback
        world.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(SoundEvents.ITEM_PICKUP, 0.5f, 0.5f);
        
        return true;
    }

    /**
     * Check if an item entity is a trap and return the owner's UUID, or null if not a trap.
     */
    public static UUID getTrapOwner(ItemEntity item) {
        TrapEntry entry = TRAP_ITEMS.get(item.getUUID());
        return entry == null ? null : entry.ownerUuid;
    }

    /**
     * Trigger the trap explosion when picked up.
     * @return true if this was a trap and exploded
     */
    public static boolean triggerTrap(ItemEntity item, ServerPlayer picker) {
        return triggerTrap(item, (net.minecraft.world.entity.LivingEntity) picker);
    }

    public static boolean triggerTrap(ItemEntity item, net.minecraft.world.entity.LivingEntity picker) {
        TrapEntry entry = TRAP_ITEMS.remove(item.getUUID());
        if (entry == null) {
            return false;
        }
        UUID ownerUuid = entry.ownerUuid;
        
        if (picker instanceof ServerPlayer player) {
            // Don't explode for the owner or trusted players
            if (player.getUUID().equals(ownerUuid)) {
                item.discard();
                return true;
            }
            if (VoidImmunity.hasImmunity(player)) {
                item.discard();
                return true;
            }
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
                if (owner != null && GemTrust.isTrusted(owner, player)) {
                    item.discard();
                    return true;
                }
            }
        }
        
        // Explode!
        if (!(picker.level() instanceof ServerLevel world)) {
            return false;
        }
        double x = item.getX();
        double y = item.getY();
        double z = item.getZ();
        float power = Math.max(0.0F, GemsBalance.v().bonusPool().decoyTrapExplosionDamage);
        world.explode(null, x, y, z, power, Level.ExplosionInteraction.MOB);
        
        // Remove the item
        item.discard();
        return true;
    }

    /**
     * Clean up a trap item (e.g., when it despawns or is removed).
     */
    public static void removeTrap(UUID itemUuid) {
        TRAP_ITEMS.remove(itemUuid);
    }

    public static void tickTrapItem(ItemEntity item) {
        TrapEntry entry = TRAP_ITEMS.get(item.getUUID());
        if (entry == null) {
            return;
        }
        if (!(item.level() instanceof ServerLevel world)) {
            TRAP_ITEMS.remove(item.getUUID());
            return;
        }
        long now = world.getGameTime();
        if (now >= entry.expiresAt) {
            TRAP_ITEMS.remove(item.getUUID());
            item.discard();
        }
    }

    /**
     * Cleanup stale trap entries for items that no longer exist.
     * Called periodically to prevent memory leaks.
     */
    private static void cleanupStaleTraps(ServerLevel world) {
        if (world == null || TRAP_ITEMS.isEmpty()) {
            return;
        }
        
        Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrapEntry> entry = it.next();
            UUID itemUuid = entry.getKey();
            if (!isTrapPresent(world.getServer(), entry.getValue(), itemUuid)) {
                it.remove();
            }
        }
    }

    private static void pruneExpiredTraps(ServerLevel world, long now) {
        if (TRAP_ITEMS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrapEntry> entry = it.next();
            TrapEntry trap = entry.getValue();
            if (now < trap.expiresAt) {
                continue;
            }
            it.remove();
            discardTrapEntity(world.getServer(), entry.getKey());
        }
    }

    private static boolean isTrapPresent(MinecraftServer server, TrapEntry entry, UUID itemUuid) {
        if (server == null || entry == null) {
            return false;
        }
        ServerLevel world = server.getLevel(entry.dimension);
        if (world == null) {
            return false;
        }
        Entity entity = world.getEntity(itemUuid);
        return entity instanceof ItemEntity;
    }

    private static void discardTrapEntity(MinecraftServer server, UUID itemUuid) {
        if (server == null) {
            return;
        }
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(itemUuid);
            if (entity instanceof ItemEntity item) {
                item.discard();
                return;
            }
        }
    }

    /**
     * Clear all trap entries. Called on server shutdown.
     */
    public static void clearAllTraps() {
        TRAP_ITEMS.clear();
    }

    private static final class TrapEntry {
        final UUID ownerUuid;
        final net.minecraft.resources.ResourceKey<Level> dimension;
        final long expiresAt;
        private TrapEntry(UUID ownerUuid, net.minecraft.resources.ResourceKey<Level> dimension, long expiresAt) {
            this.ownerUuid = ownerUuid;
            this.dimension = dimension;
            this.expiresAt = expiresAt;
        }
    }
}
