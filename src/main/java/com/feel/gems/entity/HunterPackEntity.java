package com.feel.gems.entity;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Six-Pack Pain entity - a player clone that shares health with other pack members.
 * When hit, gives owner regeneration + random buff, attacker gets debuff.
 * Targets untrusted players and hostile mobs.
 */
public class HunterPackEntity extends PathfinderMob {
    
    private UUID ownerUuid;
    private String ownerName;
    private UUID packId; // Shared ID for all clones in a pack
    
    public HunterPackEntity(EntityType<? extends HunterPackEntity> type, Level world) {
        super(type, world);
    }
    
    private int ticksAlive = 0;
    private int maxLifetimeTicks = 200; // 10 seconds default, set by ability
    
    /**
     * 6 clones with 120 hearts shared = 240 HP total / 6 = 40 HP each
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D) // 40 HP each, 240 total shared (120 hearts)
                .add(Attributes.MOVEMENT_SPEED, 0.35D) // Fast like wolves
                .add(Attributes.ATTACK_DAMAGE, 4.0D) // 2 hearts
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.ARMOR, 7.5D); // Iron armor equivalent
    }
    
    @Override
    protected void registerGoals() {
        var cfg = GemsBalance.v().hunter();
        int closeRange = cfg.sixPackPainCloseTargetRangeBlocks();
        int wideRange = cfg.sixPackPainWideTargetRangeBlocks();
        
        // Attack goals
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D, 0.001F));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        // Target goals - priority order as specified
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new TargetUntrustedPlayerGoal(this, closeRange)); // Close range first
        this.targetSelector.addGoal(3, new TargetUntrustedPlayerGoal(this, wideRange)); // Then wider range
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }
    
    public void setOwner(ServerPlayer owner, UUID packId) {
        this.ownerUuid = owner.getUUID();
        this.ownerName = owner.getGameProfile().name();
        this.packId = packId;
        this.setCustomName(owner.getDisplayName());
        this.setCustomNameVisible(true); // Always show name tag
        
        // Copy equipment from owner
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack stack = owner.getItemBySlot(slot);
                this.setItemSlot(slot, stack.copy());
            }
        }
    }
    
    public void setMaxLifetime(int ticks) {
        this.maxLifetimeTicks = ticks;
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        
        // Auto-despawn after max lifetime
        if (!this.level().isClientSide() && ticksAlive >= maxLifetimeTicks) {
            this.discard();
        }
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public UUID getPackId() {
        return packId;
    }
    
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        // Apply damage to shared health pool
        boolean result = distributePackDamage(world, source, amount);
        
        // Give owner buffs and attacker debuff
        if (result && source.getEntity() instanceof LivingEntity attacker) {
            applyHitEffects(world, attacker);
        }
        
        return result;
    }
    
    private boolean distributePackDamage(ServerLevel world, DamageSource source, float amount) {
        if (packId == null) {
            return super.hurtServer(world, source, amount);
        }
        
        // Find all pack members
        List<HunterPackEntity> packMembers = getPackMembers(world);
        if (packMembers.isEmpty()) {
            return super.hurtServer(world, source, amount);
        }
        
        // Calculate total pack health (shared 120 hearts = 240 HP pool)
        float totalHealth = 0;
        for (HunterPackEntity member : packMembers) {
            totalHealth += member.getHealth();
        }
        
        // Apply damage to the shared pool
        float newTotalHealth = totalHealth - amount;
        
        if (newTotalHealth <= 0) {
            // Kill all pack members simultaneously
            for (HunterPackEntity member : packMembers) {
                member.setHealth(0);
                if (member != this) {
                    member.discard();
                }
            }
            // Let this one die properly to trigger death events
            return super.hurtServer(world, source, this.getHealth() + 1);
        }
        
        // Distribute remaining health evenly
        float healthPerMember = newTotalHealth / packMembers.size();
        for (HunterPackEntity member : packMembers) {
            member.setHealth(Math.max(0.0F, healthPerMember));
        }
        
        // Play hurt animation on this one
        this.playHurtSound(source);
        return true;
    }
    
    private List<HunterPackEntity> getPackMembers(ServerLevel world) {
        if (packId == null) return List.of(this);
        
        return world.getEntitiesOfClass(
            HunterPackEntity.class,
            this.getBoundingBox().inflate(50),
            e -> packId.equals(e.getPackId()) && e.isAlive()
        );
    }
    
    private void applyHitEffects(ServerLevel world, LivingEntity attacker) {
        var cfg = GemsBalance.v().hunter();
        int buffDuration = cfg.sixPackPainBuffDurationTicks();
        int debuffDuration = cfg.sixPackPainDebuffDurationTicks();
        
        // Give owner regeneration + random buff
        ServerPlayer owner = getOwner(world);
        if (owner != null) {
            // Regeneration for buff duration
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, buffDuration, 1));
            
            // Random positive effect for buff duration
            var positiveEffects = List.of(
                MobEffects.SPEED,
                MobEffects.STRENGTH,
                MobEffects.RESISTANCE,
                MobEffects.FIRE_RESISTANCE,
                MobEffects.ABSORPTION,
                MobEffects.HASTE
            );
            var randomEffect = positiveEffects.get(random.nextInt(positiveEffects.size()));
            owner.addEffect(new MobEffectInstance(randomEffect, buffDuration, 0));
        }
        
        // Give attacker a debuff
        var debuffs = List.of(
            MobEffects.SLOWNESS,
            MobEffects.WEAKNESS,
            MobEffects.MINING_FATIGUE,
            MobEffects.BLINDNESS,
            MobEffects.NAUSEA,
            MobEffects.GLOWING
        );
        var randomDebuff = debuffs.get(random.nextInt(debuffs.size()));
        attacker.addEffect(new MobEffectInstance(randomDebuff, debuffDuration, 0));
    }
    
    private ServerPlayer getOwner(ServerLevel world) {
        if (ownerUuid == null) return null;
        MinecraftServer server = world.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(ownerUuid);
    }
    
    @Override
    protected void dropEquipment(ServerLevel world) {
        // Don't drop anything
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        // Don't drop equipment
    }
    
    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }
    
    /**
     * Goal to follow the owner when no target
     */
    private static class FollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final HunterPackEntity pack;
        private final double speed;
        private final float maxDistance;
        private final float minDistance;
        private ServerPlayer owner;
        private int updateCountdownTicks;
        
        public FollowOwnerGoal(HunterPackEntity pack, double speed, float maxDistance, float minDistance) {
            this.pack = pack;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.minDistance = minDistance;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            if (pack.getTarget() != null) return false;
            if (!(pack.level() instanceof ServerLevel world)) return false;
            
            this.owner = pack.getOwner(world);
            if (owner == null) return false;
            if (pack.distanceToSqr(owner) < minDistance * minDistance) return false;
            
            return true;
        }
        
        @Override
        public boolean canContinueToUse() {
            if (pack.getTarget() != null) return false;
            if (owner == null || !owner.isAlive()) return false;
            return pack.distanceToSqr(owner) > minDistance * minDistance;
        }
        
        @Override
        public void start() {
            this.updateCountdownTicks = 0;
        }
        
        @Override
        public void stop() {
            this.owner = null;
            pack.getNavigation().stop();
        }
        
        @Override
        public void tick() {
            pack.getLookControl().setLookAt(owner, 10.0F, pack.getMaxHeadXRot());
            if (--updateCountdownTicks <= 0) {
                updateCountdownTicks = 10;
                if (!pack.isLeashed() && !pack.isPassenger()) {
                    if (pack.distanceToSqr(owner) >= maxDistance * maxDistance) {
                        // Teleport if too far
                        pack.snapTo(
                            owner.getX() + (pack.getRandom().nextDouble() - 0.5) * 2,
                            owner.getY(),
                            owner.getZ() + (pack.getRandom().nextDouble() - 0.5) * 2,
                            pack.getYRot(),
                            pack.getXRot()
                        );
                    } else {
                        pack.getNavigation().moveTo(owner, speed);
                    }
                }
            }
        }
    }
    
    /**
     * Goal to target untrusted players within a specific range
     */
    private static class TargetUntrustedPlayerGoal extends NearestAttackableTargetGoal<Player> {
        private final HunterPackEntity pack;
        private final int maxRange;
        
        public TargetUntrustedPlayerGoal(HunterPackEntity pack, int maxRange) {
            super(pack, Player.class, true);
            this.pack = pack;
            this.maxRange = maxRange;
        }
        
        @Override
        public boolean canUse() {
            if (pack.ownerUuid == null) return false;
            if (!(pack.level() instanceof ServerLevel world)) return false;
            
            ServerPlayer owner = pack.getOwner(world);
            if (owner == null) return false;
            
            // Find closest untrusted player within range
            Player closest = null;
            double closestDistSq = maxRange * maxRange;
            
            for (Player player : world.players()) {
                if (player.getUUID().equals(pack.ownerUuid)) continue;
                if (player instanceof ServerPlayer sp && GemTrust.isTrusted(owner, sp)) continue;
                if (player.isSpectator() || player.isCreative()) continue;
                
                double distSq = pack.distanceToSqr(player);
                if (distSq < closestDistSq) {
                    closest = player;
                    closestDistSq = distSq;
                }
            }
            
            if (closest != null) {
                this.target = closest;
                return true;
            }
            return false;
        }
    }
}
