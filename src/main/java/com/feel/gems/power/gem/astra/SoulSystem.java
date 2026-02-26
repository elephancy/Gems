package com.feel.gems.power.gem.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.MobBlacklist;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;


public final class SoulSystem {
    private static final String KEY_SOUL_TYPE = "soulType";
    private static final String KEY_SOUL_DATA = "soulData";

    private SoulSystem() {
    }

    public static void onKilledMob(ServerPlayer player, LivingEntity killed) {
        if (killed instanceof ServerPlayer) {
            return;
        }
        if (SoulSummons.isSoul(killed)) {
            return;
        }
        if (MobBlacklist.isBlacklisted(killed.getType())) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.SOUL_CAPTURE)) {
            return;
        }

        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (typeId == null) {
            return;
        }
        CompoundTag root = persistent(player);
        root.putString(KEY_SOUL_TYPE, typeId.toString());
        var view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
        killed.saveWithoutId(view);
        CompoundTag data = view.buildResult();
        data.remove("UUID");
        data.remove("UUIDMost");
        data.remove("UUIDLeast");
        root.put(KEY_SOUL_DATA, data);
        com.feel.gems.net.GemExtraStateSync.send(player);
        AbilityFeedback.burst(player, ParticleTypes.SCULK_SOUL, 8, 0.25D);

        if (GemPowers.isPassiveActive(player, PowerIds.SOUL_HEALING)) {
            player.heal(GemsBalance.v().astra().soulHealingHearts());
        }
    }

    public static boolean hasSoul(ServerPlayer player) {
        return persistent(player).contains(KEY_SOUL_TYPE);
    }

    public static String soulType(ServerPlayer player) {
        return persistent(player).getStringOr(KEY_SOUL_TYPE, "");
    }

    public static boolean release(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SOUL_CAPTURE)) {
            player.sendSystemMessage(Component.translatable("gems.astra.soul_capture_not_active"), true);
            return false;
        }
        CompoundTag nbt = persistent(player);
        if (!nbt.contains(KEY_SOUL_TYPE)) {
            player.sendSystemMessage(Component.translatable("gems.astra.no_captured_soul"), true);
            return false;
        }
        Identifier id = Identifier.tryParse(nbt.getStringOr(KEY_SOUL_TYPE, ""));
        if (id == null) {
            nbt.remove(KEY_SOUL_TYPE);
            player.sendSystemMessage(Component.translatable("gems.astra.soul_invalid"), true);
            return false;
        }
        if (MobBlacklist.isBlacklisted(id)) {
            nbt.remove(KEY_SOUL_TYPE);
            player.sendSystemMessage(Component.translatable("gems.astra.soul_blacklisted"), true);
            return false;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            nbt.remove(KEY_SOUL_TYPE);
            player.sendSystemMessage(Component.translatable("gems.astra.cannot_release_soul"), true);
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);

        // Spawn clearly above the ground so summons don't clip into the floor (common with foot-position spawning).
        // (Players reported this feeling "too low" at smaller offsets.)
        double forward = GemsBalance.v().astra().soulReleaseForwardBlocks();
        double up = GemsBalance.v().astra().soulReleaseUpBlocks();
        Vec3 pos = player.position()
                .add(player.getViewVector(1.0F).scale(forward))
                .add(0.0D, up, 0.0D);
        Entity entity = null;
        CompoundTag stored = nbt.getCompound(KEY_SOUL_DATA).orElse(null);
        if (stored != null && !stored.isEmpty()) {
            CompoundTag copy = stored.copy();
            copy.putString("id", id.toString());
            entity = EntityType.loadEntityRecursive(type, copy, world, EntitySpawnReason.MOB_SUMMONED, spawned -> {
                spawned.snapTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
                return spawned;
            });
        }
        if (entity == null) {
            entity = type.create(world, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        }
        if (entity == null) {
            nbt.remove(KEY_SOUL_TYPE);
            nbt.remove(KEY_SOUL_DATA);
            player.sendSystemMessage(Component.translatable("gems.astra.cannot_summon", id.toString()), true);
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(net.minecraft.core.BlockPos.containing(pos)),
                    net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            com.feel.gems.power.gem.summoner.SummonerSummons.tuneControlledMob(mob);
        }
        SoulSummons.mark(entity, player.getUUID());
        SoulSummons.trackSpawn(player, entity);
        if (entity instanceof LivingEntity living) {
            living.skipDropExperience();
            living.setHealth(living.getMaxHealth());
        }
        entity.snapTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        world.addFreshEntity(entity);

        if (entity instanceof Monster hostile) {
            addHostileTargeting(hostile, player.getUUID());
        }

        nbt.remove(KEY_SOUL_TYPE);
        nbt.remove(KEY_SOUL_DATA);
        com.feel.gems.net.GemExtraStateSync.send(player);
        if (GemPowers.isPassiveActive(player, PowerIds.SOUL_HEALING)) {
            player.heal(GemsBalance.v().astra().soulHealingHearts());
        }
        AbilityFeedback.sound(player, SoundEvents.TOTEM_USE, 0.8F, 1.4F);
        AbilityFeedback.burstAt(world, pos.add(0.0D, 1.0D, 0.0D), ParticleTypes.SCULK_SOUL, 18, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.astra.released_soul", id.toString()), true);
        return true;
    }

    private static void addHostileTargeting(Monster mob, UUID ownerUuid) {
        if (!(mob.level() instanceof ServerLevel world)) {
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }

        // Best-effort: make hostile summons target untrusted players, while excluding the owner and trusted players.
        ((com.feel.gems.mixin.accessor.MobEntitySelectorsAccessor) mob).gems$getTargetSelector().addGoal(1,
                new NearestAttackableTargetGoal<>(mob, ServerPlayer.class, true, (candidate, candidateWorld) -> {
                    if (!(candidate instanceof ServerPlayer p)) {
                        return false;
                    }
                    if (!VoidImmunity.canBeTargeted(owner, p)) {
                        return false;
                    }
                    return !com.feel.gems.trust.GemTrust.isTrusted(owner, p);
                })
        );
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

}
