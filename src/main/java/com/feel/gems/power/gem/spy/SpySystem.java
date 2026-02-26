package com.feel.gems.power.gem.spy;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.core.GemId;
import com.feel.gems.legendary.GemSeerTracker;
import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;


public final class SpySystem {
    private static final String KEY_DEATHS = "gemsDeathCount";

    private static final String KEY_LAST_KILLED_TYPE = "spyLastKilledType";
    private static final String KEY_LAST_KILLED_MAX_HEALTH = "spyLastKilledMaxHealth";
    private static final String KEY_LAST_KILLED_ATTACK = "spyLastKilledAttack";

    private static final String KEY_LAST_SEEN_ABILITY = "spyLastSeenAbility";
    private static final String KEY_LAST_SEEN_AT = "spyLastSeenAt";
    private static final String KEY_LAST_SEEN_CASTER = "spyLastSeenCaster";
    private static final String KEY_LAST_SEEN_CASTER_ENTITY_ID = "spyLastSeenCasterEntityId";
    private static final String KEY_OBSERVED = "spyObserved";
    private static final String KEY_OBSERVED_ECHO_SELECTED = "spyObservedEchoSelected";
    private static final String KEY_OBSERVED_STEAL_SELECTED = "spyObservedStealSelected";

    private static final String KEY_STOLEN = "spyStolen";
    private static final String KEY_STOLEN_SELECTED = "spyStolenSelected";
    private static final String KEY_STOLEN_CAST_SELECTED = "spyStolenCastSelected";
    private static final String KEY_STOLEN_BY = "spyStolenBy";

    private static final String KEY_MIMIC_UNTIL = "spyMimicUntil";
    private static final String KEY_SKINSHIFT_UNTIL = "spySkinshiftUntil";
    private static final String KEY_SKINSHIFT_TARGET = "spySkinshiftTarget";
    private static final String KEY_SKINSHIFT_NAME = "spySkinshiftName";

    private static final String KEY_STILL_LAST_X = "spyStillX";
    private static final String KEY_STILL_LAST_Y = "spyStillY";
    private static final String KEY_STILL_LAST_Z = "spyStillZ";
    private static final String KEY_STILL_LAST_MOVED = "spyStillLastMoved";

    private static final Identifier MOD_MIMIC_MAX_HEALTH = Identifier.fromNamespaceAndPath("gems", "spy_mimic_max_health");
    private static final Identifier MOD_MIMIC_SPEED = Identifier.fromNamespaceAndPath("gems", "spy_mimic_speed");
    private static final Identifier MOD_MIMIC_ATTACK = Identifier.fromNamespaceAndPath("gems", "spy_mimic_attack");

    private static final Set<Identifier> SPY_ABILITIES = Set.of(
            PowerIds.SPY_MIMIC_FORM,
            PowerIds.SPY_ECHO,
            PowerIds.SPY_STEAL,
            PowerIds.SPY_SMOKE_BOMB,
            PowerIds.SPY_STOLEN_CAST,
            PowerIds.SPY_SKINSHIFT
    );

    public record ObservedAbility(Identifier id, int count, long lastSeen) {}
    private record MimicBonuses(float bonusHealth, float bonusAttack) {}

    // Cache of online players currently eligible for Spy logic to avoid scanning everyone on each observed ability use.
    private static final Set<UUID> ACTIVE_SPIES = new HashSet<>();

    private SpySystem() {
    }

    public static void incrementDeaths(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        int deaths = nbt.getIntOr(KEY_DEATHS, 0);
        nbt.putInt(KEY_DEATHS, deaths + 1);

        // Dying clears observation progress.
        nbt.remove(KEY_LAST_SEEN_ABILITY);
        nbt.remove(KEY_LAST_SEEN_AT);
        nbt.remove(KEY_LAST_SEEN_CASTER);
        nbt.remove(KEY_LAST_SEEN_CASTER_ENTITY_ID);
        nbt.remove(KEY_OBSERVED);
        nbt.remove(KEY_OBSERVED_ECHO_SELECTED);
        nbt.remove(KEY_OBSERVED_STEAL_SELECTED);
    }

    public static int deaths(ServerPlayer player) {
        return persistent(player).getIntOr(KEY_DEATHS, 0);
    }

    public static void recordLastKilledMob(ServerPlayer killer, LivingEntity killed) {
        if (killed instanceof ServerPlayer) {
            return;
        }
        if (SoulSummons.isSoul(killed) || SummonerSummons.isSummon(killed)) {
            return;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (id == null) {
            return;
        }
        persistent(killer).putString(KEY_LAST_KILLED_TYPE, id.toString());
        persistent(killer).putFloat(KEY_LAST_KILLED_MAX_HEALTH, killed.getMaxHealth());
        double attack = 0.0D;
        AttributeInstance attr = killed.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attack = attr.getValue();
        }
        persistent(killer).putFloat(KEY_LAST_KILLED_ATTACK, (float) Math.max(0.0D, attack));
    }

    public static Identifier lastKilledType(ServerPlayer player) {
        String raw = persistent(player).getStringOr(KEY_LAST_KILLED_TYPE, "");
        return Identifier.tryParse(raw);
    }

    public static void onAbilityUsed(MinecraftServer server, ServerPlayer caster, Identifier abilityId) {
        if (server == null) {
            return;
        }
        if (GemsDisables.isAbilityDisabled(abilityId)) {
            return;
        }
        long now = GemsTime.now(caster);
        int range = GemsBalance.v().spy().observeRangeBlocks();
        double rangeSq = range * (double) range;

        // If the cache is empty, do a full scan so cold starts still record observations.
        if (ACTIVE_SPIES.isEmpty()) {
            for (ServerPlayer spy : server.getPlayerList().getPlayers()) {
                if (spy == caster) {
                    continue;
                }
                if (observeIfEligible(spy, caster, abilityId, now, rangeSq)) {
                    ACTIVE_SPIES.add(spy.getUUID());
                }
            }
            return;
        }

        // Maintain the cache: prune stale entries, add newly eligible spies, and fallback to a full scan if everything expired.
        var iter = ACTIVE_SPIES.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            ServerPlayer spy = server.getPlayerList().getPlayer(uuid);
            if (spy == null || spy == caster) {
                iter.remove();
                continue;
            }
            if (!observeIfEligible(spy, caster, abilityId, now, rangeSq)) {
                iter.remove();
            }
        }

        // Add any newly eligible spies that weren't already in the cache.
        for (ServerPlayer spy : server.getPlayerList().getPlayers()) {
            if (spy == caster || ACTIVE_SPIES.contains(spy.getUUID())) {
                continue;
            }
            if (observeIfEligible(spy, caster, abilityId, now, rangeSq)) {
                ACTIVE_SPIES.add(spy.getUUID());
            }
        }

        // If pruning removed everyone, fall back to a full scan so steals still register immediately.
        if (ACTIVE_SPIES.isEmpty()) {
            for (ServerPlayer spy : server.getPlayerList().getPlayers()) {
                if (spy == caster) {
                    continue;
                }
                if (observeIfEligible(spy, caster, abilityId, now, rangeSq)) {
                    ACTIVE_SPIES.add(spy.getUUID());
                }
            }
        }
    }

    private static void recordObservation(ServerPlayer spy, ServerPlayer caster, Identifier abilityId, long now) {
        CompoundTag nbt = persistent(spy);
        nbt.putString(KEY_LAST_SEEN_ABILITY, abilityId.toString());
        nbt.putLong(KEY_LAST_SEEN_AT, now);
        GemsNbt.putUuid(nbt, KEY_LAST_SEEN_CASTER, caster.getUUID());
        nbt.putInt(KEY_LAST_SEEN_CASTER_ENTITY_ID, caster.getId());

        int epoch = deaths(spy);
        CompoundTag observed = nbt.getCompound(KEY_OBSERVED).orElse(new CompoundTag());
        String key = abilityId.toString();
        CompoundTag rec = observed.getCompound(key).orElse(new CompoundTag());

        int recEpoch = rec.getIntOr("epoch", 0);
        long first = rec.getLongOr("first", 0L);
        int count = rec.getIntOr("count", 0);
        if (recEpoch != epoch) {
            count = 0;
            first = now;
        }
        if (first <= 0) {
            first = now;
        }
        count++;
        rec.putInt("epoch", epoch);
        rec.putLong("first", first);
        rec.putLong("last", now);
        rec.putInt("count", count);
        GemsNbt.putUuid(rec, "caster", caster.getUUID());
        observed.put(key, rec);
        nbt.put(KEY_OBSERVED, observed);
    }

    public static Identifier lastSeenAbility(ServerPlayer player) {
        return Identifier.tryParse(persistent(player).getStringOr(KEY_LAST_SEEN_ABILITY, ""));
    }

    public static long lastSeenAt(ServerPlayer player) {
        return persistent(player).getLongOr(KEY_LAST_SEEN_AT, 0L);
    }

    public static UUID lastSeenCaster(ServerPlayer player) {
        return GemsNbt.getUuid(persistent(player), KEY_LAST_SEEN_CASTER);
    }

    public static int lastSeenCasterEntityId(ServerPlayer player) {
        return persistent(player).getIntOr(KEY_LAST_SEEN_CASTER_ENTITY_ID, -1);
    }

    public static int witnessedCount(ServerPlayer player, Identifier abilityId) {
        CompoundTag nbt = persistent(player);
        CompoundTag observed = nbt.getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return 0;
        }
        String key = abilityId.toString();
        return observed.getCompound(key).map(rec -> rec.getIntOr("count", 0)).orElse(0);
    }

    public static ObservedAbility observedAbility(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null) {
            return null;
        }
        CompoundTag observed = persistent(player).getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return null;
        }
        CompoundTag rec = observed.getCompound(abilityId.toString()).orElse(null);
        if (rec == null) {
            return null;
        }
        int count = rec.getIntOr("count", 0);
        long last = rec.getLongOr("last", 0L);
        return new ObservedAbility(abilityId, count, last);
    }

    public static java.util.List<ObservedAbility> observedAbilities(ServerPlayer player) {
        CompoundTag observed = persistent(player).getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null || observed.keySet().isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<ObservedAbility> list = new java.util.ArrayList<>();
        for (String key : observed.keySet()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) {
                continue;
            }
            CompoundTag rec = observed.getCompound(key).orElse(null);
            if (rec == null) {
                continue;
            }
            int count = rec.getIntOr("count", 0);
            long last = rec.getLongOr("last", 0L);
            list.add(new ObservedAbility(id, count, last));
        }
        return list;
    }

    public static boolean selectObservedAbility(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null) {
            return false;
        }
        ObservedAbility observed = observedAbility(player, abilityId);
        if (observed == null) {
            return false;
        }
        persistent(player).putString(KEY_OBSERVED_ECHO_SELECTED, abilityId.toString());
        return true;
    }

    public static Identifier selectedEchoAbility(ServerPlayer player) {
        Identifier id = Identifier.tryParse(persistent(player).getStringOr(KEY_OBSERVED_ECHO_SELECTED, ""));
        if (id == null || !canEcho(player, id)) {
            return null;
        }
        return id;
    }

    public static Identifier selectedStealAbility(ServerPlayer player) {
        Identifier id = Identifier.tryParse(persistent(player).getStringOr(KEY_OBSERVED_STEAL_SELECTED, ""));
        if (id == null || !canSteal(player, id) || isAbilityStolen(player, id)) {
            return null;
        }
        return id;
    }

    public static Identifier selectedStolenCastAbility(ServerPlayer player) {
        Identifier id = Identifier.tryParse(persistent(player).getStringOr(KEY_STOLEN_CAST_SELECTED, ""));
        if (id != null && isAbilityStolen(player, id)) {
            return id;
        }
        Identifier fallback = selectedStolenAbility(player);
        if (fallback != null && isAbilityStolen(player, fallback)) {
            return fallback;
        }
        return null;
    }

    public static boolean selectEchoAbility(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null || !canEcho(player, abilityId)) {
            return false;
        }
        persistent(player).putString(KEY_OBSERVED_ECHO_SELECTED, abilityId.toString());
        return true;
    }

    public static boolean selectStealAbility(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null || !canSteal(player, abilityId) || isAbilityStolen(player, abilityId)) {
            return false;
        }
        persistent(player).putString(KEY_OBSERVED_STEAL_SELECTED, abilityId.toString());
        return true;
    }

    public static boolean selectStolenCastAbility(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null || !isAbilityStolen(player, abilityId)) {
            return false;
        }
        persistent(player).putString(KEY_STOLEN_CAST_SELECTED, abilityId.toString());
        int idx = stolenIndex(player, abilityId);
        if (idx >= 0) {
            persistent(player).putInt(KEY_STOLEN_SELECTED, idx);
        }
        return true;
    }

    public static boolean isEchoableAbility(Identifier abilityId) {
        if (abilityId == null) {
            return false;
        }
        if (abilityId.equals(PowerIds.SPY_ECHO) || abilityId.equals(PowerIds.SPY_STEAL) || abilityId.equals(PowerIds.SPY_STOLEN_CAST)) {
            return false;
        }
        return true;
    }

    public static boolean isSpyAbility(Identifier abilityId) {
        return abilityId != null && SPY_ABILITIES.contains(abilityId);
    }

    public static boolean canEcho(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null) {
            return false;
        }
        if (!isEchoableAbility(abilityId)) {
            return false;
        }
        ObservedAbility observed = observedAbility(player, abilityId);
        return observed != null && observed.count() > 0;
    }

    public static boolean canSteal(ServerPlayer player, Identifier abilityId) {
        if (abilityId == null) {
            return false;
        }
        int required = GemsBalance.v().spy().stealRequiredWitnessCount();
        int count = witnessedCount(player, abilityId);
        if (count < required) {
            return false;
        }
        CompoundTag nbt = persistent(player);
        CompoundTag observed = nbt.getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return false;
        }
        String key = abilityId.toString();
        CompoundTag rec = observed.getCompound(key).orElse(null);
        if (rec == null) {
            return false;
        }
        if (rec.getIntOr("epoch", 0) != deaths(player)) {
            return false;
        }
        long last = rec.getLongOr("last", 0L);
        return last > 0;
    }

    public static boolean consumeObservedCount(ServerPlayer player, Identifier abilityId, int amount) {
        if (player == null || abilityId == null || amount <= 0) {
            return false;
        }
        CompoundTag nbt = persistent(player);
        CompoundTag observed = nbt.getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return false;
        }
        String key = abilityId.toString();
        CompoundTag rec = observed.getCompound(key).orElse(null);
        if (rec == null) {
            return false;
        }
        int count = rec.getIntOr("count", 0);
        if (count < amount) {
            return false;
        }
        int remaining = count - amount;
        int required = GemsBalance.v().spy().stealRequiredWitnessCount();
        if (remaining <= 0) {
            observed.remove(key);
            if (observed.isEmpty()) {
                nbt.remove(KEY_OBSERVED);
            } else {
                nbt.put(KEY_OBSERVED, observed);
            }
            if (key.equals(nbt.getStringOr(KEY_OBSERVED_ECHO_SELECTED, ""))) {
                nbt.remove(KEY_OBSERVED_ECHO_SELECTED);
            }
            if (key.equals(nbt.getStringOr(KEY_OBSERVED_STEAL_SELECTED, ""))) {
                nbt.remove(KEY_OBSERVED_STEAL_SELECTED);
            }
        } else {
            rec.putInt("count", remaining);
            observed.put(key, rec);
            nbt.put(KEY_OBSERVED, observed);
            if (remaining < required && key.equals(nbt.getStringOr(KEY_OBSERVED_STEAL_SELECTED, ""))) {
                nbt.remove(KEY_OBSERVED_STEAL_SELECTED);
            }
        }
        return true;
    }

    public static boolean stealLastSeen(ServerPlayer spy) {
        Identifier abilityId = selectedStealAbility(spy);
        if (abilityId == null) {
            abilityId = lastSeenAbility(spy);
        }
        if (abilityId == null) {
            spy.sendSystemMessage(Component.translatable("gems.spy.no_observed_ability"), true);
            return false;
        }
        if (GemsDisables.isAbilityDisabled(abilityId)) {
            spy.sendSystemMessage(Component.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }
        if (!isEchoableAbility(abilityId)) {
            spy.sendSystemMessage(Component.translatable("gems.spy.cannot_steal_spy"), true);
            return false;
        }
        if (!canSteal(spy, abilityId) || isAbilityStolen(spy, abilityId)) {
            spy.sendSystemMessage(Component.translatable("gems.spy.not_enough_observation"), true);
            return false;
        }

        CompoundTag nbt = persistent(spy);
        int max = GemsBalance.v().spy().maxStolenAbilities();
        ListTag list = nbt.getList(KEY_STOLEN).orElse(new ListTag());
        if (list.size() >= max) {
            spy.sendSystemMessage(Component.translatable("gems.spy.stolen_slots_full"), true);
            return false;
        }

        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getString(i))) {
                spy.sendSystemMessage(Component.translatable("gems.spy.already_stole"), true);
                return false;
            }
        }
        list.add(StringTag.valueOf(raw));
        nbt.put(KEY_STOLEN, list);
        if (nbt.getIntOr(KEY_STOLEN_SELECTED, -1) < 0) {
            nbt.putInt(KEY_STOLEN_SELECTED, 0);
        }
        if (nbt.getStringOr(KEY_STOLEN_CAST_SELECTED, "").isEmpty()) {
            nbt.putString(KEY_STOLEN_CAST_SELECTED, raw);
        }

        ServerPlayer victim = null;
        int casterEntityId = lastSeenCasterEntityId(spy);
        ServerLevel world = spy.level();
        if (casterEntityId >= 0) {
            var entity = world.getEntity(casterEntityId);
            if (entity instanceof ServerPlayer candidate && candidate != spy) {
                victim = candidate;
            }
        }
        if (victim == null) {
            UUID casterUuid = lastSeenCaster(spy);
            var server = world.getServer();
            if (casterUuid != null && server != null) {
                ServerPlayer candidate = server.getPlayerList().getPlayer(casterUuid);
                if (candidate != null && candidate != spy) {
                    victim = candidate;
                }
            }
        }
        if (victim != null) {
            AbilityDisables.disable(victim, abilityId);
            recordStolenFrom(victim, spy.getUUID(), abilityId);
            victim.sendSystemMessage(Component.translatable("gems.spy.ability_was_stolen"), false);
        }

        AbilityFeedback.sound(spy, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.3F);
        consumeObservedCount(spy, abilityId, GemsBalance.v().spy().stealRequiredWitnessCount());
        com.feel.gems.net.StolenStateSync.send(spy);
        spy.sendSystemMessage(Component.translatable("gems.spy.stole_ability", raw), true);
        return true;
    }

    public static Identifier selectedStolenAbility(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        int idx = nbt.getIntOr(KEY_STOLEN_SELECTED, 0);
        idx = Math.floorMod(idx, list.size());
        return Identifier.tryParse(list.getStringOr(idx, ""));
    }

    public static List<Identifier> getStolenAbilities(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Identifier> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getStringOr(i, ""));
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static boolean cycleStolen(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null) {
            return false;
        }
        if (list.isEmpty()) {
            return false;
        }
        int next = Math.floorMod(nbt.getIntOr(KEY_STOLEN_SELECTED, 0) + 1, list.size());
        nbt.putInt(KEY_STOLEN_SELECTED, next);
        Identifier selected = Identifier.tryParse(list.getStringOr(next, ""));
        if (selected != null) {
            nbt.putString(KEY_STOLEN_CAST_SELECTED, selected.toString());
            player.sendSystemMessage(Component.translatable("gems.spy.selected_stolen", selected.toString()), true);
        }
        return true;
    }

    public static void clearOnGemSwitchAway(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        clearMimicForm(player);
        clearSkinshift(player);
        ACTIVE_SPIES.remove(player.getUUID());
        com.feel.gems.net.StolenStateSync.send(player);
    }

    public static void restoreStolenFromThief(ServerPlayer thief) {
        var server = thief.level().getServer();
        if (server == null) {
            return;
        }
        UUID thiefId = thief.getUUID();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            restoreStolenBy(player, thiefId);
        }
    }

    public static void restoreStolenOnKill(ServerPlayer killer, ServerPlayer victim) {
        if (killer == null || victim == null) {
            return;
        }
        List<Identifier> recovered = restoreStolenByInternal(killer, victim.getUUID());
        if (recovered.isEmpty()) {
            return;
        }

        for (Identifier abilityId : recovered) {
            removeStolenAbility(victim, abilityId);
        }
        com.feel.gems.net.StolenStateSync.send(killer);
        com.feel.gems.net.StolenStateSync.send(victim);
    }

    public static void startMimicForm(ServerPlayer player, int durationTicks) {
        Identifier lastKilled = lastKilledType(player);
        if (lastKilled == null) {
            return;
        }
        CompoundTag nbt = persistent(player);
        long now = GemsTime.now(player);
        nbt.putLong(KEY_MIMIC_UNTIL, now + durationTicks);

        float baseBonusHp = GemsBalance.v().spy().mimicFormBonusMaxHealth();
        float speedMult = GemsBalance.v().spy().mimicFormSpeedMultiplier();
        MimicBonuses bonuses = mimicBonuses(player, lastKilled, baseBonusHp);
        float bonusHp = bonuses.bonusHealth();
        float bonusAttack = bonuses.bonusAttack();

        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.removeModifier(MOD_MIMIC_MAX_HEALTH);
            if (bonusHp > 0) {
                maxHp.addPermanentModifier(new AttributeModifier(MOD_MIMIC_MAX_HEALTH, bonusHp, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(MOD_MIMIC_ATTACK);
            if (bonusAttack > 0) {
                attack.addPermanentModifier(new AttributeModifier(MOD_MIMIC_ATTACK, bonusAttack, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOD_MIMIC_SPEED);
            if (speedMult != 1.0F) {
                speed.addPermanentModifier(new AttributeModifier(MOD_MIMIC_SPEED, speedMult - 1.0F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, true, false, false));
        AbilityFeedback.sound(player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.8F, 1.1F);
        // Format the entity type identifier as a readable name (e.g. "minecraft:cow" -> "Cow")
        String entityName = lastKilled.getPath().replace('_', ' ');
        entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        player.sendSystemMessage(Component.translatable("gems.spy.mimic_form", entityName), true);
    }

    public static boolean startSkinshift(ServerPlayer player, ServerPlayer target, int durationTicks) {
        if (target == null || target == player || durationTicks <= 0) {
            return false;
        }
        CompoundTag nbt = persistent(player);
        long now = GemsTime.now(player);
        nbt.putLong(KEY_SKINSHIFT_UNTIL, now + durationTicks);
        GemsNbt.putUuid(nbt, KEY_SKINSHIFT_TARGET, target.getUUID());
        nbt.putString(KEY_SKINSHIFT_NAME, target.getGameProfile().name());
        syncSkinshift(player, target.getUUID());
        AbilityFeedback.sound(player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.7F, 0.9F);
        player.sendSystemMessage(Component.translatable("gems.spy.skinshift", target.getName().getString()), true);
        return true;
    }

    public static void tickEverySecond(ServerPlayer player) {
        trackActiveSpy(player);
        long now = GemsTime.now(player);
        tickStillnessCloak(player, now);
        tickMimicFormCleanup(player, now);
        tickSkinshift(player, now);
    }

    private static boolean isSpyActiveInternal(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem == GemId.SPY) {
            return true;
        }
        if (activeGem == GemId.PRISM) {
            return PrismSelectionsState.hasAnyAbility(player, SPY_ABILITIES);
        }
        return false;
    }

    public static boolean isSpyActive(ServerPlayer player) {
        return isSpyActiveInternal(player);
    }

    public static boolean hidesTracking(ServerPlayer target) {
        if (GemPlayerState.getActiveGem(target) != GemId.SPY) {
            return false;
        }
        return GemPowers.isPassiveActive(target, PowerIds.SPY_FALSE_SIGNATURE);
    }

    public static boolean hidesTracking(MinecraftServer server, UUID targetId) {
        if (server == null || targetId == null) {
            return false;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(targetId);
        if (online != null) {
            return hidesTracking(online);
        }
        GemSeerTracker.Snapshot snapshot = GemSeerTracker.snapshot(server, targetId);
        if (snapshot == null) {
            return false;
        }
        GemId active = safeGemId(snapshot.activeGem());
        return active == GemId.SPY && snapshot.energy() > 0;
    }

    private static void trackActiveSpy(ServerPlayer player) {
        boolean active = isSpyActiveInternal(player);
        UUID id = player.getUUID();
        if (active) {
            ACTIVE_SPIES.add(id);
        } else {
            ACTIVE_SPIES.remove(id);
        }
    }

    // Rebuild the active spy cache when it is empty so first-time observations still work.
    private static void seedActiveSpies(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            trackActiveSpy(player);
        }
    }

    private static boolean observeIfEligible(ServerPlayer spy, ServerPlayer caster, Identifier abilityId, long now, double rangeSq) {
        if (!isSpyActiveInternal(spy)) {
            return false;
        }
        if (spy.level() != caster.level()) {
            return false;
        }
        if (spy.distanceToSqr(caster) > rangeSq) {
            return false;
        }

        // "In front of you": require the caster to be in the forward half-space.
        Vec3 toCaster = caster.position().subtract(spy.position());
        Vec3 look = spy.getViewVector(1.0F);
        if (toCaster.dot(look) <= 0.0D) {
            return false;
        }

        recordObservation(spy, caster, abilityId, now);
        return true;
    }

    private static void tickStillnessCloak(ServerPlayer player, long now) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SPY_STILLNESS_CLOAK)) {
            return;
        }

        CompoundTag nbt = persistent(player);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double lx = nbt.getDoubleOr(KEY_STILL_LAST_X, x);
        double ly = nbt.getDoubleOr(KEY_STILL_LAST_Y, y);
        double lz = nbt.getDoubleOr(KEY_STILL_LAST_Z, z);

        float eps = GemsBalance.v().spy().stillnessMoveEpsilonBlocks();
        double epsSq = eps * (double) eps;
        double dx = x - lx;
        double dy = y - ly;
        double dz = z - lz;
        boolean moved = (dx * dx + dy * dy + dz * dz) > epsSq;

        if (!nbt.contains(KEY_STILL_LAST_MOVED)) {
            nbt.putLong(KEY_STILL_LAST_MOVED, now);
        }
        if (moved) {
            nbt.putLong(KEY_STILL_LAST_MOVED, now);
        }

        nbt.putDouble(KEY_STILL_LAST_X, x);
        nbt.putDouble(KEY_STILL_LAST_Y, y);
        nbt.putDouble(KEY_STILL_LAST_Z, z);

        long lastMoved = nbt.getLongOr(KEY_STILL_LAST_MOVED, now);
        int stillness = GemsBalance.v().spy().stillnessTicks();
        if (stillness <= 0) {
            return;
        }
        if (now - lastMoved < stillness) {
            return;
        }

        int refresh = GemsBalance.v().spy().stillnessInvisRefreshTicks();
        if (refresh <= 0) {
            return;
        }
        // No particles, no icon.
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, refresh, 0, true, false, false));
    }

    private static GemId safeGemId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return GemId.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void tickMimicFormCleanup(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_MIMIC_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now < until) {
            return;
        }
        clearMimicForm(player);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.setInvisible(false);
        persistent(player).putLong(KEY_STILL_LAST_MOVED, now);
    }

    private static void tickSkinshift(ServerPlayer player, long now) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        // Skinshift should persist for its full duration once applied; it may be granted via Prism
        // selection or bonus pools and shouldn't immediately cancel if the player switches gems.
        if (now >= until || GemPlayerState.getEnergy(player) <= 0) {
            clearSkinshift(player);
        }
    }

    private static void clearSkinshift(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        if (GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET) == null) {
            nbt.remove(KEY_SKINSHIFT_UNTIL);
            return;
        }
        nbt.remove(KEY_SKINSHIFT_UNTIL);
        nbt.remove(KEY_SKINSHIFT_TARGET);
        nbt.remove(KEY_SKINSHIFT_NAME);
        syncSkinshift(player, null);
    }

    private static void syncSkinshift(ServerPlayer player, UUID target) {
        if (player.level().getServer() == null) {
            return;
        }
        SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUUID(), Optional.ofNullable(target));
        for (ServerPlayer viewer : player.level().getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void clearMimicForm(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_MIMIC_UNTIL);
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.removeModifier(MOD_MIMIC_MAX_HEALTH);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOD_MIMIC_SPEED);
        }
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(MOD_MIMIC_ATTACK);
        }
    }

    private static MimicBonuses mimicBonuses(ServerPlayer player, Identifier entityId, float baseBonusHp) {
        if (entityId == null) {
            return new MimicBonuses(Math.max(0.0F, baseBonusHp), 0.0F);
        }
        float recordedHealth = persistent(player).getFloatOr(KEY_LAST_KILLED_MAX_HEALTH, 0.0F);
        float recordedAttack = persistent(player).getFloatOr(KEY_LAST_KILLED_ATTACK, 0.0F);
        if (recordedHealth > 0.0F) {
            double baseHealth = recordedHealth;
            double baseAttack = Math.max(0.0D, recordedAttack);
            double healthScale = baseHealth / 20.0D;
            float minHealth = Math.max(0.0F, baseBonusHp * 0.25F);
            float maxHealth = Math.max(minHealth, baseBonusHp * 6.0F);
            float scaledHealth = (float) (baseBonusHp * Math.max(0.1D, healthScale));
            float bonusHealth = Mth.clamp(scaledHealth, minHealth, maxHealth);
            float bonusAttack = (float) Math.max(0.0D, Math.min(baseAttack * 0.5D, 10.0D));
            return new MimicBonuses(bonusHealth, bonusAttack);
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        if (type == null || !LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
            return new MimicBonuses(Math.max(0.0F, baseBonusHp), 0.0F);
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        if (!DefaultAttributes.hasSupplier(livingType)) {
            return new MimicBonuses(Math.max(0.0F, baseBonusHp), 0.0F);
        }
        AttributeSupplier attrs = DefaultAttributes.getSupplier(livingType);
        double baseHealth = attrs.getBaseValue(Attributes.MAX_HEALTH);
        double baseAttack = attrs.hasAttribute(Attributes.ATTACK_DAMAGE)
                ? attrs.getBaseValue(Attributes.ATTACK_DAMAGE)
                : 0.0D;
        double healthScale = baseHealth / 20.0D;
        float minHealth = Math.max(0.0F, baseBonusHp * 0.25F);
        float maxHealth = Math.max(minHealth, baseBonusHp * 6.0F);
        float scaledHealth = (float) (baseBonusHp * Math.max(0.1D, healthScale));
        float bonusHealth = Mth.clamp(scaledHealth, minHealth, maxHealth);
        float bonusAttack = (float) Math.max(0.0D, Math.min(baseAttack * 0.5D, 10.0D));
        return new MimicBonuses(bonusHealth, bonusAttack);
    }

    public static boolean canAffect(ServerPlayer caster, ServerPlayer target) {
        if (VoidImmunity.shouldBlockEffect(caster, target)) {
            return false;
        }
        return !GemTrust.isTrusted(caster, target);
    }

    public static void syncSkinshifts(ServerPlayer viewer) {
        if (viewer.level().getServer() == null) {
            return;
        }
        long now = GemsTime.now(viewer);
        for (ServerPlayer player : viewer.level().getServer().getPlayerList().getPlayers()) {
            CompoundTag nbt = persistent(player);
            UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
            if (target == null) {
                continue;
            }
            long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
            if (until <= now) {
                continue;
            }
            SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUUID(), Optional.of(target));
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    public static void syncSkinshiftSelf(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
        if (target == null) {
            return;
        }
        long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= GemsTime.now(player)) {
            clearSkinshift(player);
            return;
        }
        syncSkinshift(player, target);
    }

    public static Component chatDisguiseName(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= GemsTime.now(player)) {
            return null;
        }
        String name = nbt.getStringOr(KEY_SKINSHIFT_NAME, "");
        if (!name.isBlank()) {
            return Component.literal(name);
        }
        UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
        if (target == null) {
            return null;
        }
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) {
            return Component.literal(online.getGameProfile().name());
        }
        return null;
    }

    public static UUID chatDisguiseTargetId(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= GemsTime.now(player)) {
            return null;
        }
        return GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
    }

    public static boolean isSkinshiftTarget(ServerPlayer target) {
        if (target == null) {
            return false;
        }
        var server = target.level().getServer();
        if (server == null) {
            return false;
        }
        UUID targetId = target.getUUID();
        long now = GemsTime.now(target);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag nbt = persistent(player);
            UUID candidate = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
            if (candidate == null) {
                continue;
            }
            long until = nbt.getLongOr(KEY_SKINSHIFT_UNTIL, 0L);
            if (until <= now) {
                continue;
            }
            if (targetId.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void recordStolenFrom(ServerPlayer victim, UUID thief, Identifier abilityId) {
        if (thief == null || abilityId == null) {
            return;
        }
        CompoundTag root = persistent(victim);
        CompoundTag stolenBy = root.getCompound(KEY_STOLEN_BY).orElse(new CompoundTag());
        stolenBy.put(abilityId.toString(), GemsNbt.fromUuid(thief));
        root.put(KEY_STOLEN_BY, stolenBy);
    }

    private static void restoreStolenBy(ServerPlayer victim, UUID thief) {
        restoreStolenByInternal(victim, thief);
    }

    private static List<Identifier> restoreStolenByInternal(ServerPlayer victim, UUID thief) {
        if (thief == null) {
            return List.of();
        }
        CompoundTag root = persistent(victim);
        CompoundTag stolenBy = root.getCompound(KEY_STOLEN_BY).orElse(null);
        if (stolenBy == null) {
            return List.of();
        }
        boolean changed = false;
        List<Identifier> recovered = new ArrayList<>();
        for (String key : java.util.List.copyOf(stolenBy.keySet())) {
            UUID recorded = GemsNbt.toUuid(stolenBy.get(key));
            if (recorded == null) {
                continue;
            }
            if (!thief.equals(recorded)) {
                continue;
            }
            Identifier abilityId = Identifier.tryParse(key);
            if (abilityId != null) {
                AbilityDisables.enable(victim, abilityId);
                recovered.add(abilityId);
            }
            stolenBy.remove(key);
            changed = true;
        }
        if (stolenBy.isEmpty()) {
            root.remove(KEY_STOLEN_BY);
        } else {
            root.put(KEY_STOLEN_BY, stolenBy);
        }
        if (changed) {
            victim.sendSystemMessage(Component.translatable("gems.spy.recovered_stolen"), true);
        }
        return recovered;
    }

    private static void addStolenAbility(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return;
        }
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(new ListTag());
        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getString(i))) {
                return;
            }
        }
        list.add(StringTag.valueOf(raw));
        nbt.put(KEY_STOLEN, list);
        if (nbt.getIntOr(KEY_STOLEN_SELECTED, -1) < 0) {
            nbt.putInt(KEY_STOLEN_SELECTED, 0);
        }
        if (nbt.getStringOr(KEY_STOLEN_CAST_SELECTED, "").isEmpty()) {
            nbt.putString(KEY_STOLEN_CAST_SELECTED, raw);
        }
    }

    private static void removeStolenAbility(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return;
        }
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null || list.isEmpty()) {
            return;
        }
        String raw = abilityId.toString();
        int removedIndex = -1;
        ListTag next = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getStringOr(i, "");
            if (raw.equals(entry)) {
                if (removedIndex < 0) {
                    removedIndex = i;
                }
                continue;
            }
            next.add(StringTag.valueOf(entry));
        }
        if (removedIndex < 0) {
            return;
        }
        if (next.isEmpty()) {
            nbt.remove(KEY_STOLEN);
            nbt.remove(KEY_STOLEN_SELECTED);
            nbt.remove(KEY_STOLEN_CAST_SELECTED);
            return;
        }
        nbt.put(KEY_STOLEN, next);
        int selected = nbt.getIntOr(KEY_STOLEN_SELECTED, 0);
        if (selected > removedIndex) {
            selected--;
        }
        if (selected < 0 || selected >= next.size()) {
            selected = 0;
        }
        nbt.putInt(KEY_STOLEN_SELECTED, selected);
        if (raw.equals(nbt.getStringOr(KEY_STOLEN_CAST_SELECTED, ""))) {
            Identifier nextId = Identifier.tryParse(next.getStringOr(selected, ""));
            if (nextId != null) {
                nbt.putString(KEY_STOLEN_CAST_SELECTED, nextId.toString());
            } else {
                nbt.remove(KEY_STOLEN_CAST_SELECTED);
            }
        }
    }

    private static boolean isAbilityStolen(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return false;
        }
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null || list.isEmpty()) {
            return false;
        }
        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getStringOr(i, ""))) {
                return true;
            }
        }
        return false;
    }

    private static int stolenIndex(ServerPlayer player, Identifier abilityId) {
        if (player == null || abilityId == null) {
            return -1;
        }
        CompoundTag nbt = persistent(player);
        ListTag list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null || list.isEmpty()) {
            return -1;
        }
        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getStringOr(i, ""))) {
                return i;
            }
        }
        return -1;
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
