package com.feel.gems.power.ability.summoner;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.summoner.SummonerCommanderMark;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.MobBlacklist;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;


public final class SummonSlotAbility implements GemAbility {
    private final int slot;

    public SummonSlotAbility(int slot) {
        this.slot = slot;
    }

    @Override
    public Identifier id() {
        return switch (slot) {
            case 1 -> PowerIds.SUMMON_SLOT_1;
            case 2 -> PowerIds.SUMMON_SLOT_2;
            case 3 -> PowerIds.SUMMON_SLOT_3;
            case 4 -> PowerIds.SUMMON_SLOT_4;
            case 5 -> PowerIds.SUMMON_SLOT_5;
            default -> PowerIds.SUMMON_SLOT_1;
        };
    }

    @Override
    public String name() {
        return "Summon " + slot;
    }

    @Override
    public String description() {
        return "Summons the configured minions for slot " + slot + ".";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getActiveGem(player) != GemId.SUMMONER) {
            if (GemPlayerState.getActiveGem(player) != GemId.PRISM || !PrismSelectionsState.hasAbility(player, id())) {
                return false;
            }
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }

        var cfg = GemsBalance.v().summoner();
        SummonerLoadouts.Loadout loadout = SummonerLoadouts.resolve(player, cfg);
        List<SummonerLoadouts.Entry> specs = switch (slot) {
            case 1 -> loadout.slot1();
            case 2 -> loadout.slot2();
            case 3 -> loadout.slot3();
            case 4 -> loadout.slot4();
            case 5 -> loadout.slot5();
            default -> loadout.slot1();
        };
        if (specs.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.ability.summoner.slot.no_summons", slot), true);
            return false;
        }

        SummonerSummons.SummonStats stats = SummonerSummons.pruneAndPoints(player, cfg);
        int maxPoints = cfg.maxPoints();
        int remaining = maxPoints > 0 ? Math.max(0, maxPoints - stats.points()) : 0;
        if (remaining <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.summoner.slot.cap_reached"), true);
            return false;
        }

        long until = SummonerSummons.computeUntilTick(player, cfg.summonLifetimeTicks());
        boolean bonusHealth = GemPowers.isPassiveActive(player, PowerIds.SUMMONER_FAMILIARS_BLESSING);
        float bonusHealthAmount = cfg.summonBonusHealth();

        UUID markTarget = SummonerCommanderMark.activeTargetUuid(player);

        int spawned = 0;
        for (var spec : specs) {
            if (remaining <= 0) {
                break;
            }
            Identifier typeId = Identifier.tryParse(spec.entityId());
            if (typeId == null) {
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
            if (MobBlacklist.isBlacklisted(type)) {
                continue;
            }
            int cost = SummonerSummons.costForEntity(cfg.costs(), type);
            if (cost <= 0) {
                continue;
            }
            for (int i = 0; i < spec.count() && remaining >= cost; i++) {
                Entity e = type.create(world, EntitySpawnReason.MOB_SUMMONED);
                if (!(e instanceof Mob mob)) {
                    continue;
                }
                Vec3 spawnPos = spawnPos(player, spawned);
                mob.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                mob.finalizeSpawn(world, world.getCurrentDifficultyAt(BlockPos.containing(spawnPos)), EntitySpawnReason.MOB_SUMMONED, null);
                SummonerSummons.tuneControlledMob(mob);
                SummonerSummons.mark(mob, player.getUUID(), until);
                mob.skipDropExperience();
                if (bonusHealth) {
                    SummonerSummons.applyBonusHealth(mob, bonusHealthAmount);
                }
                mob.setHealth(mob.getMaxHealth());
                world.addFreshEntity(mob);
                SummonerSummons.trackSpawn(player, mob);

                if (markTarget != null) {
                    Entity target = SummonerSummons.findEntity(world.getServer(), markTarget);
                    if (target instanceof LivingEntity living && living.level() == mob.level()) {
                        mob.setTarget(living);
                    }
                }

                spawned++;
                remaining -= cost;
            }
        }

        if (spawned <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.summoner.slot.no_valid"), true);
            return false;
        }

        AbilityFeedback.burst(player, net.minecraft.core.particles.ParticleTypes.PORTAL, 18, 0.35D);
        AbilityFeedback.sound(player, net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_SUMMON, 0.8F, 1.2F);
        player.sendSystemMessage(Component.translatable("gems.ability.summoner.slot.summoned", spawned), true);
        return true;
    }

    private static Vec3 spawnPos(ServerPlayer player, int index) {
        var cfg = GemsBalance.v().summoner();
        int ringLayers = Math.max(1, cfg.summonSpawnRingLayers());
        int ringSegments = Math.max(1, cfg.summonSpawnRingSegments());
        Vec3 forward = player.getViewVector(1.0F).normalize();
        Vec3 base = player.position()
                .add(forward.scale(cfg.summonSpawnForwardBlocks()))
                .add(0.0D, cfg.summonSpawnUpBlocks(), 0.0D);
        double radius = cfg.summonSpawnRingBaseBlocks() + (index % ringLayers) * cfg.summonSpawnRingStepBlocks();
        double angle = (Math.PI * 2.0D) * ((index % ringSegments) / (double) ringSegments);
        return base.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }
}
