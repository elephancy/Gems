package com.feel.gems.synergy;

import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Registry of all synergy combos.
 * Each synergy defines which gems need to combine and what effect triggers.
 */
public final class SynergyRegistry {
    private static final Map<String, SynergyDefinition> SYNERGIES = new LinkedHashMap<>();

    static {
        registerSynergies();
    }

    private SynergyRegistry() {
    }

    /**
     * Get all registered synergies.
     */
    public static Collection<SynergyDefinition> getAll() {
        return Collections.unmodifiableCollection(SYNERGIES.values());
    }

    /**
     * Get a synergy by ID.
     */
    public static SynergyDefinition get(String id) {
        return SYNERGIES.get(id);
    }

    private static void register(SynergyDefinition synergy) {
        SYNERGIES.put(synergy.id(), synergy);
    }

    // Helper to get player position as Vec3d
    private static Vec3 getPlayerPos(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY(), player.getZ());
    }

    // Helper to get nearby entities
    private static List<LivingEntity> getNearbyLiving(ServerPlayer player, double radius) {
        ServerLevel world = player.level();
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        return world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());
    }

    // ========== Synergy Definitions ==========

    private static void registerSynergies() {
        // ==================== GEM-BASED SYNERGIES ====================
        // These trigger when any ability from each gem is cast within the window

        // FIRE + FLUX = Thunderstorm: Lightning strike + fire explosion
        register(SynergyDefinition.gemBased(
                "thunderstorm",
                "gems.synergy.thunderstorm",
                Set.of(GemId.FIRE, GemId.FLUX),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        // Create lightning at target location (5 blocks ahead)
                        Vec3 playerPos = getPlayerPos(player);
                        Vec3 forward = player.getLookAngle().scale(5);
                        Vec3 targetPos = playerPos.add(forward);
                        var lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.snapTo(targetPos.x, targetPos.y, targetPos.z);
                            world.addFreshEntity(lightning);
                        }
                    }
                }
        ));

        // LIFE + REAPER = Soul Exchange: Participants swap health percentages
        register(SynergyDefinition.gemBased(
                "soul_exchange",
                "gems.synergy.soul_exchange",
                Set.of(GemId.LIFE, GemId.REAPER),
                60, 600,
                participants -> {
                    if (participants.size() >= 2) {
                        var p1 = participants.get(0).player();
                        var p2 = participants.get(1).player();
                        float pct1 = p1.getHealth() / p1.getMaxHealth();
                        float pct2 = p2.getHealth() / p2.getMaxHealth();
                        p1.setHealth(p1.getMaxHealth() * pct2);
                        p2.setHealth(p2.getMaxHealth() * pct1);
                        p1.sendSystemMessage(Component.translatable("gems.synergy.soul_exchange.swapped").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                        p2.sendSystemMessage(Component.translatable("gems.synergy.soul_exchange.swapped").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                    }
                }
        ));

        // SPEED + SPACE = Warp Drive: All participants get massive speed boost and short-range teleport
        register(SynergyDefinition.gemBased(
                "warp_drive",
                "gems.synergy.warp_drive",
                Set.of(GemId.SPEED, GemId.SPACE),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 4));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 100, 2));
                        // Teleport forward 10 blocks in look direction
                        Vec3 playerPos = getPlayerPos(player);
                        Vec3 forward = player.getLookAngle().scale(10);
                        Vec3 targetPos = playerPos.add(forward);
                        GemsTeleport.teleport(player, player.level(), targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());
                    }
                }
        ));

        // STRENGTH + TERROR = Warcry: All participants and nearby allies get strength + enemies get fear
        register(SynergyDefinition.gemBased(
                "warcry",
                "gems.synergy.warcry",
                Set.of(GemId.STRENGTH, GemId.TERROR),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        // Buff self
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 2));
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
                        // Play intimidating sound
                        AbilityFeedback.sound(player, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                        // Fear nearby enemies
                        for (LivingEntity other : getNearbyLiving(player, 15)) {
                            if (other != player && other instanceof ServerPlayer otherPlayer) {
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                            }
                        }
                    }
                }
        ));

        // SPY + ASTRA = Phantom Strike: All participants become invisible + deal bonus damage on next hit
        register(SynergyDefinition.gemBased(
                "phantom_strike",
                "gems.synergy.phantom_strike",
                Set.of(GemId.SPY, GemId.ASTRA),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
                    }
                }
        ));

        // BEACON + LIFE = Radiant Restoration: Massive AoE heal for all allies
        register(SynergyDefinition.gemBased(
                "radiant_restoration",
                "gems.synergy.radiant_restoration",
                Set.of(GemId.BEACON, GemId.LIFE),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
                        // Heal nearby allies
                        for (LivingEntity other : getNearbyLiving(player, 20)) {
                            if (other instanceof ServerPlayer otherPlayer) {
                                if (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.heal(10.0f);
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                                }
                            }
                        }
                    }
                }
        ));

        // AIR + PUFF = Cyclone: Create a vortex that pulls in and launches enemies
        register(SynergyDefinition.gemBased(
                "cyclone",
                "gems.synergy.cyclone",
                Set.of(GemId.AIR, GemId.PUFF),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        Vec3 playerPos = getPlayerPos(player);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other != player) {
                                boolean ally = other instanceof ServerPlayer otherPlayer &&
                                              GemTrust.isTrusted(player, otherPlayer);
                                if (!ally) {
                                    // Pull towards caster then launch up
                                    Vec3 otherPos = new Vec3(other.getX(), other.getY(), other.getZ());
                                    Vec3 direction = playerPos.subtract(otherPos).normalize();
                                    other.push(direction.x * 1.5, 1.5, direction.z * 1.5);
                                    other.needsSync = true;
                                }
                            }
                        }
                    }
                }
        ));

        // SUMMONER + HUNTER = Pack Alpha: All summoned creatures get massive damage boost
        register(SynergyDefinition.gemBased(
                "pack_alpha",
                "gems.synergy.pack_alpha",
                Set.of(GemId.SUMMONER, GemId.HUNTER),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 2));
                        // Give the player night vision to help track prey
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0));
                    }
                }
        ));

        // PILLAGER + WEALTH = Plunder: Enemies drop extra loot for a duration
        register(SynergyDefinition.gemBased(
                "plunder",
                "gems.synergy.plunder",
                Set.of(GemId.PILLAGER, GemId.WEALTH),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        // Give luck effect for increased drops
                        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 300, 2));
                        player.sendSystemMessage(Component.translatable("gems.synergy.plunder.active").withStyle(ChatFormatting.GOLD), true);
                    }
                }
        ));

        // DUELIST + SENTINEL = Guardian's Challenge: Draw all nearby enemies towards you, gain massive defense
        register(SynergyDefinition.gemBased(
                "guardians_challenge",
                "gems.synergy.guardians_challenge",
                Set.of(GemId.DUELIST, GemId.SENTINEL),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        // Gain massive defense
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 3));
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
                        // Taunt nearby enemies (give them glowing so they're visible)
                        for (LivingEntity other : getNearbyLiving(player, 15)) {
                            if (other != player && other instanceof ServerPlayer otherPlayer) {
                                if (!GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                                }
                            }
                        }
                    }
                }
        ));

        // TRICKSTER + CHAOS = Reality Warp: Random crazy effect on all nearby players
        register(SynergyDefinition.gemBased(
                "reality_warp",
                "gems.synergy.reality_warp",
                Set.of(GemId.TRICKSTER, GemId.CHAOS),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        java.util.Random rand = new java.util.Random();
                        // Apply random effects to everyone nearby
                        for (LivingEntity other : getNearbyLiving(player, 20)) {
                            if (other instanceof ServerPlayer otherPlayer) {
                                // Random effect pool
                                var effects = List.of(
                                        MobEffects.SPEED, MobEffects.SLOWNESS,
                                        MobEffects.STRENGTH, MobEffects.WEAKNESS,
                                        MobEffects.JUMP_BOOST, MobEffects.LEVITATION,
                                        MobEffects.INVISIBILITY, MobEffects.GLOWING,
                                        MobEffects.REGENERATION, MobEffects.POISON
                                );
                                var effect = effects.get(rand.nextInt(effects.size()));
                                otherPlayer.addEffect(new MobEffectInstance(effect, 100, rand.nextInt(3)));
                            }
                        }
                    }
                }
        ));

        // ==================== ABILITY-BASED SYNERGIES ====================
        // These trigger when specific abilities are cast within the window

        // FIREBALL + FLUX BEAM = Plasma Lance: Piercing plasma corridor with explosions and shock
        register(SynergyDefinition.abilityBased(
                "plasma_lance",
                "gems.synergy.plasma_lance",
                Set.of(GemId.FIRE, GemId.FLUX),
                Set.of(PowerIds.FIREBALL, PowerIds.FLUX_BEAM),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 start = getPlayerPos(player).add(0, 1.4, 0);
                        Vec3 dir = player.getLookAngle().normalize();

                        AbilityFeedback.sound(player, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.9f);

                        for (int i = 1; i <= 6; i++) {
                            Vec3 point = start.add(dir.scale(i * 3.0));
                            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 20, 0.6, 0.6, 0.6, 0.1);
                            world.explode(player, point.x, point.y, point.z, 2.4f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);

                            AABB box = new AABB(point, point).inflate(2.5);
                            for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive())) {
                                if (other == player) {
                                    continue;
                                }
                                if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                    continue;
                                }
                                other.igniteForSeconds(4);
                                other.hurtServer(world, player.damageSources().indirectMagic(player, player), 6.0f);
                            }
                        }
                    }
                }
        ));

        // METEOR SHOWER + ORBITAL LASER = Stellar Cataclysm: Falling fire rain + lightning impact
        register(SynergyDefinition.abilityBased(
                "stellar_cataclysm",
                "gems.synergy.stellar_cataclysm",
                Set.of(GemId.FIRE, GemId.SPACE),
                Set.of(PowerIds.METEOR_SHOWER, PowerIds.SPACE_ORBITAL_LASER),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 center = getPlayerPos(player).add(player.getLookAngle().scale(6));

                        var lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.snapTo(center.x, center.y, center.z);
                            world.addFreshEntity(lightning);
                        }

                        world.explode(player, center.x, center.y, center.z, 3.2f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
                        world.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 60, 1.5, 1.0, 1.5, 0.08);

                        for (int i = 0; i < 8; i++) {
                            double angle = i * (Math.PI * 2.0 / 8.0);
                            double radius = 6.0;
                            Vec3 spawn = center.add(Math.cos(angle) * radius, 18.0, Math.sin(angle) * radius);
                            SmallFireball fireball = new SmallFireball(world, player, new Vec3(0.0, -1.0, 0.0));
                            fireball.setPos(spawn.x, spawn.y, spawn.z);
                            world.addFreshEntity(fireball);
                        }
                    }
                }
        ));

        // GRAVITY FIELD + SLIPSTREAM = Gravity Weave: Allies surge forward, enemies are pulled and slowed
        register(SynergyDefinition.abilityBased(
                "gravity_weave",
                "gems.synergy.gravity_weave",
                Set.of(GemId.SPACE, GemId.SPEED),
                Set.of(PowerIds.SPACE_GRAVITY_FIELD, PowerIds.SPEED_SLIPSTREAM),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 forward = player.getLookAngle().normalize();

                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) {
                                continue;
                            }
                            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 3));
                                other.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 2));
                                other.push(forward.x * 1.8, 0.2, forward.z * 1.8);
                                other.needsSync = true;
                            } else {
                                Vec3 pull = getPlayerPos(player).subtract(new Vec3(other.getX(), other.getY(), other.getZ())).normalize();
                                other.push(pull.x * 1.4, 0.4, pull.z * 1.4);
                                other.needsSync = true;
                                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
                                other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                            }
                        }

                        world.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 40, 2.0, 1.0, 2.0, 0.03);
                        AbilityFeedback.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.2f);
                    }
                }
        ));

        // SMOKE BOMB + GLITCH STEP = Shadow Relay: Blink behind a nearby enemy and cripple them
        register(SynergyDefinition.abilityBased(
                "shadow_relay",
                "gems.synergy.shadow_relay",
                Set.of(GemId.SPY, GemId.TRICKSTER),
                Set.of(PowerIds.SPY_SMOKE_BOMB, PowerIds.TRICKSTER_GLITCH_STEP),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();

                        LivingEntity target = null;
                        double bestDist = Double.MAX_VALUE;
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) {
                                continue;
                            }
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            double d = other.distanceToSqr(player);
                            if (d < bestDist) {
                                bestDist = d;
                                target = other;
                            }
                        }

                        Vec3 origin = getPlayerPos(player);
                        if (target != null) {
                            Vec3 behind = new Vec3(target.getX(), target.getY(), target.getZ())
                                    .subtract(target.getLookAngle().normalize().scale(2.0));
                            GemsTeleport.teleport(player, world, behind.x, behind.y, behind.z, player.getYRot(), player.getXRot());
                            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                        }

                        world.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y + 1, origin.z, 40, 1.2, 0.6, 1.2, 0.02);
                        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(6), e -> e.isAlive())) {
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            if (other != player) {
                                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                            }
                        }
                    }
                }
        ));

        // BLOOD CHARGE + LIFE CIRCLE = Blood Convergence: Drain enemies, supercharge allies
        register(SynergyDefinition.abilityBased(
                "blood_convergence",
                "gems.synergy.blood_convergence",
                Set.of(GemId.REAPER, GemId.LIFE),
                Set.of(PowerIds.REAPER_BLOOD_CHARGE, PowerIds.LIFE_CIRCLE),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();

                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) {
                                continue;
                            }
                            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.heal(6.0f);
                                other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
                                other.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
                            } else {
                                other.hurtServer(world, player.damageSources().indirectMagic(player, player), 8.0f);
                                other.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                            }
                        }

                        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 45, 1.8, 0.8, 1.8, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.WITHER_AMBIENT, 0.9f, 1.0f);
                    }
                }
        ));

        // FIREBALL + BLACK HOLE = Singularity Ignition: Pull and incinerate enemies at impact
        register(SynergyDefinition.abilityBased(
                "singularity_ignition",
                "gems.synergy.singularity_ignition",
                Set.of(GemId.FIRE, GemId.SPACE),
                Set.of(PowerIds.FIREBALL, PowerIds.SPACE_BLACK_HOLE),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 center = getPlayerPos(player).add(player.getLookAngle().scale(6));

                        world.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 1, center.z, 80, 1.2, 1.2, 1.2, 0.05);
                        world.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 60, 1.0, 0.6, 1.0, 0.03);
                        AbilityFeedback.sound(player, SoundEvents.WITHER_SPAWN, 0.6f, 1.1f);
                        world.explode(player, center.x, center.y, center.z, 2.8f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);

                        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(9), e -> e.isAlive())) {
                            if (other == player) {
                                continue;
                            }
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            Vec3 pull = center.subtract(new Vec3(other.getX(), other.getY(), other.getZ())).normalize();
                            other.push(pull.x * 1.4, 0.5, pull.z * 1.4);
                            other.needsSync = true;
                            other.igniteForSeconds(6);
                            other.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                        }
                    }
                }
        ));

        // FLUX DISCHARGE + CROSSWIND = Ion Burst: Thunderous shockwave
        register(SynergyDefinition.abilityBased(
                "ion_burst",
                "gems.synergy.ion_burst",
                Set.of(GemId.FLUX, GemId.AIR),
                Set.of(PowerIds.FLUX_DISCHARGE, PowerIds.AIR_CROSSWIND),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.3f);
                        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 80, 2.0, 1.0, 2.0, 0.1);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            Vec3 dir = new Vec3(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                            other.push(dir.x * 1.4, 0.6, dir.z * 1.4);
                            other.needsSync = true;
                            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                        }
                    }
                }
        ));

        // VITALITY VORTEX + BEACON REGEN AURA = Sanctuary Pulse: Cleanse and massive heal
        register(SynergyDefinition.abilityBased(
                "sanctuary_pulse",
                "gems.synergy.sanctuary_pulse",
                Set.of(GemId.LIFE, GemId.BEACON),
                Set.of(PowerIds.VITALITY_VORTEX, PowerIds.BEACON_AURA_REGEN),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 40, 1.6, 0.8, 1.6, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);

                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayer otherPlayer && (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer))) {
                                otherPlayer.heal(8.0f);
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
                                otherPlayer.removeEffect(MobEffects.POISON);
                                otherPlayer.removeEffect(MobEffects.WITHER);
                                otherPlayer.removeEffect(MobEffects.WEAKNESS);
                                otherPlayer.removeEffect(MobEffects.SLOWNESS);
                                otherPlayer.removeEffect(MobEffects.BLINDNESS);
                            }
                        }
                    }
                }
        ));

        // SCYTHE SWEEP + NET SHOT = Reaping Snare: Root and wither
        register(SynergyDefinition.abilityBased(
                "reaping_snare",
                "gems.synergy.reaping_snare",
                Set.of(GemId.REAPER, GemId.HUNTER),
                Set.of(PowerIds.REAPER_SCYTHE_SWEEP, PowerIds.HUNTER_NET_SHOT),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.WITHER_HURT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
                            other.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 5.0f);
                        }
                    }
                }
        ));

        // AFTERIMAGE + SMOKE BOMB = Ghostline: Cloaked assault
        register(SynergyDefinition.abilityBased(
                "ghostline",
                "gems.synergy.ghostline",
                Set.of(GemId.SPEED, GemId.SPY),
                Set.of(PowerIds.SPEED_AFTERIMAGE, PowerIds.SPY_SMOKE_BOMB),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2));
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 50, 1.4, 0.6, 1.4, 0.02);
                        for (LivingEntity other : getNearbyLiving(player, 6)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 3.0f);
                        }
                    }
                }
        ));

        // PARRY + SHIELD WALL = Bulwark Riposte: Shockwave counter
        register(SynergyDefinition.abilityBased(
                "bulwark_riposte",
                "gems.synergy.bulwark_riposte",
                Set.of(GemId.DUELIST, GemId.SENTINEL),
                Set.of(PowerIds.DUELIST_PARRY, PowerIds.SENTINEL_SHIELD_WALL),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 2));
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 1));
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1, player.getZ(), 20, 1.2, 0.6, 1.2, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.SHIELD_BLOCK, 0.8f, 0.9f);
                        for (LivingEntity other : getNearbyLiving(player, 6)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            Vec3 dir = new Vec3(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                            other.push(dir.x * 1.1, 0.4, dir.z * 1.1);
                            other.needsSync = true;
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 4.0f);
                        }
                    }
                }
        ));

        // SUMMON SLOT 1 + CALL THE PACK = Alpha Swarm: Pack frenzy
        register(SynergyDefinition.abilityBased(
                "alpha_swarm",
                "gems.synergy.alpha_swarm",
                Set.of(GemId.SUMMONER, GemId.HUNTER),
                Set.of(PowerIds.SUMMON_SLOT_1, PowerIds.HUNTER_CALL_THE_PACK),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayer otherPlayer) {
                                if (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 2));
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                                } else {
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                                    otherPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                                }
                            }
                        }
                    }
                }
        ));

        // BREACH CHARGE + WARHORN = Siegebreak: Blast and debuff
        register(SynergyDefinition.abilityBased(
                "siegebreak",
                "gems.synergy.siegebreak",
                Set.of(GemId.TERROR, GemId.PILLAGER),
                Set.of(PowerIds.TERROR_BREACH_CHARGE, PowerIds.PILLAGER_WARHORN),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 center = getPlayerPos(player).add(player.getLookAngle().scale(4));
                        world.explode(player, center.x, center.y, center.z, 2.6f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
                        world.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 1, center.z, 12, 0.6, 0.6, 0.6, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.RAVAGER_ROAR, 0.9f, 0.9f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
                        }
                    }
                }
        ));

        // FLUX SURGE + TEMPO SHIFT = Overclock: Burst cooldowns for allies
        register(SynergyDefinition.abilityBased(
                "overclock",
                "gems.synergy.overclock",
                Set.of(GemId.FLUX, GemId.SPEED),
                Set.of(PowerIds.FLUX_SURGE, PowerIds.SPEED_TEMPO_SHIFT),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 60, 1.4, 0.7, 1.4, 0.12);
                        AbilityFeedback.sound(player, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.4f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (!(other instanceof ServerPlayer target)) {
                                continue;
                            }
                            boolean ally = target == player || GemTrust.isTrusted(player, target);
                            if (ally) {
                                long now = world.getGameTime();
                                for (Identifier id : com.feel.gems.power.registry.ModAbilities.all().keySet()) {
                                    long next = com.feel.gems.power.runtime.GemAbilityCooldowns.nextAllowedTick(target, id);
                                    if (next > now) {
                                        com.feel.gems.power.runtime.GemAbilityCooldowns.setNextAllowedTick(target, id, Math.max(now, next - 100));
                                    }
                                }
                            } else {
                                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 1));
                            }
                        }
                    }
                }
        ));

        // WHITE HOLE + GUST = Vacuum Reversal: Blast enemies away
        register(SynergyDefinition.abilityBased(
                "vacuum_reversal",
                "gems.synergy.vacuum_reversal",
                Set.of(GemId.SPACE, GemId.PUFF),
                Set.of(PowerIds.SPACE_WHITE_HOLE, PowerIds.PUFF_GUST),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 50, 2.2, 1.0, 2.2, 0.04);
                        AbilityFeedback.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) continue;
                            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
                            } else {
                                Vec3 dir = new Vec3(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                                other.push(dir.x * 1.6, 0.8, dir.z * 1.6);
                                other.needsSync = true;
                                other.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
                            }
                        }
                    }
                }
        ));

        // BREEZY BASH + TERMINAL VELOCITY = Sonic Uppercut
        register(SynergyDefinition.abilityBased(
                "sonic_uppercut",
                "gems.synergy.sonic_uppercut",
                Set.of(GemId.PUFF, GemId.SPEED),
                Set.of(PowerIds.BREEZY_BASH, PowerIds.TERMINAL_VELOCITY),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 7)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.push(0.0, 1.2, 0.0);
                            other.needsSync = true;
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 6.0f);
                        }
                    }
                }
        ));

        // ORBITAL LASER + LOCKDOWN = Judgment Field: Mark and slow enemies
        register(SynergyDefinition.abilityBased(
                "judgment_field",
                "gems.synergy.judgment_field",
                Set.of(GemId.SPACE, GemId.SENTINEL),
                Set.of(PowerIds.SPACE_ORBITAL_LASER, PowerIds.SENTINEL_LOCKDOWN),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        Vec3 center = getPlayerPos(player).add(player.getLookAngle().scale(5));
                        var lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.snapTo(center.x, center.y, center.z);
                            world.addFreshEntity(lightning);
                        }
                        world.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1, center.z, 60, 2.0, 1.0, 2.0, 0.02);
                        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(8), e -> e.isAlive())) {
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            if (other != player) {
                                other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0));
                                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 140, 2));
                            }
                        }
                    }
                }
        ));

        // HEAT HAZE + REMOTE CHARGE = Ember Chain
        register(SynergyDefinition.abilityBased(
                "ember_chain",
                "gems.synergy.ember_chain",
                Set.of(GemId.FIRE, GemId.TERROR),
                Set.of(PowerIds.HEAT_HAZE_ZONE, PowerIds.TERROR_REMOTE_CHARGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.BLAZE_SHOOT, 0.8f, 1.0f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.igniteForSeconds(6);
                            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                        }
                        world.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 70, 2.0, 0.8, 2.0, 0.05);
                    }
                }
        ));

        // SHADOW CLONE + ASTRAL DAGGERS = Shadow Barrage
        register(SynergyDefinition.abilityBased(
                "shadow_barrage",
                "gems.synergy.shadow_barrage",
                Set.of(GemId.REAPER, GemId.ASTRA),
                Set.of(PowerIds.REAPER_SHADOW_CLONE, PowerIds.ASTRAL_DAGGERS),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 50, 1.4, 0.6, 1.4, 0.1);
                        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_SCREAM, 0.6f, 1.3f);
                        for (LivingEntity other : getNearbyLiving(player, 8)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 5.0f);
                            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                        }
                    }
                }
        ));

        // SKINSHIFT + MIRAGE = Mirror Heist: Confuse and vanish
        register(SynergyDefinition.abilityBased(
                "mirror_heist",
                "gems.synergy.mirror_heist",
                Set.of(GemId.SPY, GemId.TRICKSTER),
                Set.of(PowerIds.SPY_SKINSHIFT, PowerIds.TRICKSTER_MIRAGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1));
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
                            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                        }
                    }
                }
        ));

        // HEALTH DRAIN + RETRIBUTION = Harvest Tide
        register(SynergyDefinition.abilityBased(
                "harvest_tide",
                "gems.synergy.harvest_tide",
                Set.of(GemId.LIFE, GemId.REAPER),
                Set.of(PowerIds.HEALTH_DRAIN, PowerIds.REAPER_RETRIBUTION),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.WITHER_SHOOT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 9)) {
                            if (other == player) continue;
                            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.heal(4.0f);
                                other.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
                            } else {
                                other.hurtServer(world, player.damageSources().indirectMagic(player, player), 6.0f);
                                other.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                            }
                        }
                        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                    }
                }
        ));

        // FLUX BEAM + VINDICATOR BREAK = Stormbreaker
        register(SynergyDefinition.abilityBased(
                "stormbreaker",
                "gems.synergy.stormbreaker",
                Set.of(GemId.FLUX, GemId.PILLAGER),
                Set.of(PowerIds.FLUX_BEAM, PowerIds.PILLAGER_VINDICATOR_BREAK),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.7f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 8)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 7.0f);
                            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                        }
                        world.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 30, 1.0, 0.4, 1.0, 0.2);
                    }
                }
        ));

        // GRAVITY FIELD + AIR DASH = Celestial Stride
        register(SynergyDefinition.abilityBased(
                "celestial_stride",
                "gems.synergy.celestial_stride",
                Set.of(GemId.SPACE, GemId.AIR),
                Set.of(PowerIds.SPACE_GRAVITY_FIELD, PowerIds.AIR_DASH),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        Vec3 playerPos = getPlayerPos(player);
                        Vec3 forward = player.getLookAngle().scale(8);
                        Vec3 target = playerPos.add(forward);
                        GemsTeleport.teleport(player, player.level(), target.x, target.y, target.z, player.getYRot(), player.getXRot());
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 140, 1));
                        player.level().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.8, 0.4, 0.8, 0.02);
                    }
                }
        ));

        // BEACON RESISTANCE AURA + RALLY CRY = Iron Chorus
        register(SynergyDefinition.abilityBased(
                "iron_chorus",
                "gems.synergy.iron_chorus",
                Set.of(GemId.BEACON, GemId.SENTINEL),
                Set.of(PowerIds.BEACON_AURA_RESISTANCE, PowerIds.SENTINEL_RALLY_CRY),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        AbilityFeedback.sound(player, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayer otherPlayer && (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer))) {
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 2));
                                otherPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
                                otherPlayer.removeEffect(MobEffects.WITHER);
                                otherPlayer.removeEffect(MobEffects.POISON);
                            }
                        }
                        world.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 50, 1.6, 0.8, 1.6, 0.01);
                    }
                }
        ));

        // POUNCE + LUNGE = Predator Lunge
        register(SynergyDefinition.abilityBased(
                "predator_lunge",
                "gems.synergy.predator_lunge",
                Set.of(GemId.HUNTER, GemId.DUELIST),
                Set.of(PowerIds.HUNTER_POUNCE, PowerIds.DUELIST_LUNGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayer player = p.player();
                        ServerLevel world = player.level();
                        world.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.2);
                        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 7)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.hurtServer(world, player.damageSources().indirectMagic(player, player), 6.0f);
                            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                        }
                    }
                }
        ));
    }
}
