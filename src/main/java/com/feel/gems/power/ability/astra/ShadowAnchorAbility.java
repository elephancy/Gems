package com.feel.gems.power.ability.astra;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.particle.ModParticles;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.sound.ModSounds;
import com.feel.gems.util.GemsTeleport;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;



public final class ShadowAnchorAbility implements GemAbility {
    private static final String KEY_ANCHOR_UNTIL = "shadowAnchorUntil";
    private static final String KEY_ANCHOR_DIM = "shadowAnchorDim";
    private static final String KEY_ANCHOR_POS = "shadowAnchorPos";

    @Override
    public Identifier id() {
        return PowerIds.SHADOW_ANCHOR;
    }

    @Override
    public String name() {
        return "Shadow Anchor";
    }

    @Override
    public String description() {
        return "Shadow Anchor: press once to set an anchor; press again quickly to return.";
    }

    @Override
    public int cooldownTicks() {
        return 0; // 2-stage: set anchor then return
    }

    @Override
    public boolean activate(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long now = GemsTime.now(player);
        ServerLevel world = player.level();

        long until = nbt.getLongOr(KEY_ANCHOR_UNTIL, 0L);
        if (until > 0L && now <= until) {
            if (player.isShiftKeyDown()) {
                nbt.remove(KEY_ANCHOR_UNTIL);
                nbt.remove(KEY_ANCHOR_DIM);
                nbt.remove(KEY_ANCHOR_POS);
                player.sendSystemMessage(Component.translatable("gems.ability.astra.shadow_anchor.cleared"), true);
                startPostCooldown(player, now);
                return true;
            }

            String dim = nbt.getStringOr(KEY_ANCHOR_DIM, "");
            if (dim.equals(world.dimension().identifier().toString())) {
                BlockPos pos = readBlockPos(nbt, KEY_ANCHOR_POS);
                if (pos == null) {
                    nbt.remove(KEY_ANCHOR_UNTIL);
                    nbt.remove(KEY_ANCHOR_DIM);
                    nbt.remove(KEY_ANCHOR_POS);
                    player.sendSystemMessage(Component.translatable("gems.ability.astra.shadow_anchor.invalid"), true);
                    startPostCooldown(player, now);
                    return true;
                }
                Vec3 from = player.position();
                GemsTeleport.teleport(player, world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                AbilityFeedback.sound(player, ModSounds.PHASE_SHIFT, 0.75F, 1.05F);
                AbilityFeedback.burstAt(world, from.add(0.0D, 1.0D, 0.0D), ParticleTypes.PORTAL, 20, 0.4D);
                AbilityFeedback.burstAt(world, from.add(0.0D, 1.0D, 0.0D), ModParticles.GEM_RING, 5, 0.2D);
                AbilityFeedback.burst(player, ParticleTypes.PORTAL, 20, 0.4D);
                AbilityFeedback.burst(player, ModParticles.GEM_STAR, 8, 0.25D);
                nbt.remove(KEY_ANCHOR_UNTIL);
                nbt.remove(KEY_ANCHOR_DIM);
                nbt.remove(KEY_ANCHOR_POS);
                player.sendSystemMessage(Component.translatable("gems.ability.astra.shadow_anchor.returned"), true);
                startPostCooldown(player, now);
                return true;
            }
        }

        // If an old anchor expired, clear it and apply post-cooldown.
        if (until > 0 && now > until) {
            clearAnchor(nbt);
            player.sendSystemMessage(Component.translatable("gems.ability.astra.shadow_anchor.expired"), true);
            startPostCooldown(player, now);
            return false;
        }

        Vec3 current = player.position();
        BlockPos anchor = BlockPos.containing(current);
        nbt.putLong(KEY_ANCHOR_UNTIL, now + GemsBalance.v().astra().shadowAnchorWindowTicks());
        nbt.putString(KEY_ANCHOR_DIM, world.dimension().identifier().toString());
        writeBlockPos(nbt, KEY_ANCHOR_POS, anchor);
        AbilityFeedback.sound(player, ModSounds.ARCANE_CHIME, 0.65F, 1.15F);
        AbilityFeedback.burst(player, ParticleTypes.PORTAL, 12, 0.2D);
        AbilityFeedback.burst(player, ModParticles.GEM_RING, 5, 0.15D);
        player.sendSystemMessage(Component.translatable("gems.ability.astra.shadow_anchor.set"), true);
        return true;
    }

    public static void tick(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_ANCHOR_UNTIL, 0L);
        if (until <= 0 || now <= until) {
            return;
        }
        clearAnchor(nbt);
        startPostCooldown(player, now);
    }

    private static void startPostCooldown(ServerPlayer player, long now) {
        if (GemsAdmin.noCooldowns(player)) {
            return;
        }
        int cooldown = GemsBalance.v().astra().shadowAnchorPostCooldownTicks();
        if (cooldown <= 0) {
            return;
        }
        cooldown = applyCooldownModifiers(player, cooldown);
        GemAbilityCooldowns.setNextAllowedTick(player, PowerIds.SHADOW_ANCHOR, now + cooldown);

        // Best-effort client HUD sync.
        GemDefinition def = GemRegistry.definition(GemId.ASTRA);
        int index = def.abilities().indexOf(PowerIds.SHADOW_ANCHOR);
        if (index >= 0) {
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(GemId.ASTRA.ordinal(), index, cooldown));
        }
    }

    private static int applyCooldownModifiers(ServerPlayer player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player) * LegendaryCooldowns.getCooldownMultiplier(player);
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }

    private static void clearAnchor(CompoundTag nbt) {
        nbt.remove(KEY_ANCHOR_UNTIL);
        nbt.remove(KEY_ANCHOR_DIM);
        nbt.remove(KEY_ANCHOR_POS);
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static void writeBlockPos(CompoundTag nbt, String key, BlockPos pos) {
        if (pos == null) {
            nbt.remove(key);
            return;
        }
        nbt.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    private static BlockPos readBlockPos(CompoundTag nbt, String key) {
        int[] values = nbt.getIntArray(key).orElse(null);
        if (values == null || values.length < 3) {
            return null;
        }
        return new BlockPos(values[0], values[1], values[2]);
    }
}
