package com.feel.gems.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Shadow clone entity that mimics a player's appearance and randomly moves around.
 * Cannot die, doesn't drop items, but can be hit for visual feedback.
 * Note: NBT persistence not needed as clones are temporary and discarded after duration.
 */
public class ShadowCloneEntity extends PathfinderMob {
    private UUID ownerUuid;
    private String ownerName;
    private boolean mirageClone;
    
    private int ticksAlive = 0;
    private int nextActionTick = 0;
    private int maxLifetimeTicks = 200; // 10 seconds default, set by ability
    
    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level world) {
        super(type, world);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)  // Faster movement
                .add(Attributes.FOLLOW_RANGE, 32.0D); // Larger wander range
    }
    
    @Override
    protected void registerGoals() {
        // Random wandering behavior with larger range
        this.goalSelector.addGoal(1, new CloneWanderGoal(this, 0.8D, 0.05F));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    
    public void setOwner(ServerPlayer owner) {
        this.setPersistenceRequired();
        
        this.ownerUuid = owner.getUUID();
        this.ownerName = owner.getGameProfile().name();
        this.setCustomName(owner.getDisplayName());
        this.setCustomNameVisible(true); // Always show name tag
        
        // Copy equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack stack = owner.getItemBySlot(slot);
                this.setItemSlot(slot, stack.copy());
            }
        }
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setMaxLifetime(int ticks) {
        this.maxLifetimeTicks = ticks;
    }

    public void setMirageClone(boolean mirageClone) {
        this.mirageClone = mirageClone;
    }

    public boolean isMirageClone() {
        return mirageClone;
    }
    
    public GameProfile getOwnerProfile() {
        if (ownerUuid == null || ownerName == null) {
            return null;
        }
        return new GameProfile(ownerUuid, ownerName);
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        
        Level world = this.level();
        if (!world.isClientSide()) {
            // Auto-despawn after max lifetime
            if (ticksAlive >= maxLifetimeTicks) {
                this.discard();
                return;
            }
            
            // Random item swinging/using animation
            if (!mirageClone && world instanceof ServerLevel && ticksAlive >= nextActionTick) {
                performRandomAction();
                nextActionTick = ticksAlive + 40 + random.nextInt(80); // 2-6 seconds
            }
        }
    }
    
    private void performRandomAction() {
        int action = random.nextInt(5);
        switch (action) {
            case 0 -> swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
            case 1 -> swing(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
            case 2 -> setShiftKeyDown(!isShiftKeyDown());
            case 3 -> jumpFromGround();
            // case 4: do nothing
        }
    }
    
    @Override
    public boolean isInvulnerable() {
        return false; // Allow damage for visual feedback
    }
    
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (mirageClone) {
            com.feel.gems.power.ability.trickster.TricksterMirageRuntime.onMirageCloneHit(this);
            this.discard();
            return true;
        }
        // Take damage but don't die - just play hurt effect
        this.playHurtSound(source);
        // Reset health to prevent actual death
        this.setHealth(this.getMaxHealth());
        return true;
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
    public void die(DamageSource damageSource) {
        // Don't actually die
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
    
    /**
     * Custom wander goal with larger range for more dynamic movement
     */
    private static class CloneWanderGoal extends WaterAvoidingRandomStrollGoal {
        private final ShadowCloneEntity clone;
        
        public CloneWanderGoal(ShadowCloneEntity clone, double speed, float probability) {
            super(clone, speed, probability);
            this.clone = clone;
        }
        
        @Override
        public boolean canUse() {
            // Higher chance to move around actively
            if (clone.getRandom().nextFloat() > 0.15F) {
                return false;
            }
            return super.canUse();
        }
        
        @Override
        protected net.minecraft.world.phys.Vec3 getPosition() {
            // Get a target further away for more dynamic movement
            net.minecraft.world.phys.Vec3 target = net.minecraft.world.entity.ai.util.LandRandomPos.getPos(this.clone, 16, 7);
            return target != null ? target : super.getPosition();
        }
    }
}
