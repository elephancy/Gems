package com.feel.gems.power.gem.life;

import com.feel.gems.GemsMod;
import com.feel.gems.util.GemsTime;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;




public final class HeartLockRuntime {
    private static final String TAG_HEART_LOCK = "gems_heart_lock";
    private static final String TAG_UNTIL_PREFIX = "gems_heart_lock_until:";
    private static final String TAG_MAX_PREFIX = "gems_heart_lock_max:";
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "heart_lock");

    private HeartLockRuntime() {
    }

    public static void apply(ServerPlayer caster, Mob mob, int durationTicks) {
        long until = durationTicks > 0 ? GemsTime.now(caster) + durationTicks : 0L;
        float lockedMax = Math.max(2.0F, mob.getHealth());
        clearTags(mob);
        mob.addTag(TAG_HEART_LOCK);
        if (until > 0) {
            mob.addTag(TAG_UNTIL_PREFIX + until);
        }
        mob.addTag(TAG_MAX_PREFIX + lockedMax);
        applyModifier(mob, lockedMax);
    }

    public static void tick(Mob mob) {
        if (!mob.entityTags().contains(TAG_HEART_LOCK)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel world)) {
            return;
        }
        long until = readUntil(mob);
        long now = GemsTime.now(world);
        if (until > 0 && now >= until) {
            clear(mob);
            return;
        }
        float lockedMax = readLockedMax(mob);
        if (lockedMax <= 0.0F) {
            lockedMax = Math.max(2.0F, mob.getHealth());
        }
        applyModifier(mob, lockedMax);
    }

    private static void applyModifier(Mob mob, float lockedMax) {
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(MODIFIER_ID);
        double baseMax = maxHealth.getValue();
        double delta = lockedMax - baseMax;
        if (delta != 0.0D) {
            maxHealth.addTransientModifier(new AttributeModifier(MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));
        }
        if (mob.getHealth() > lockedMax) {
            mob.setHealth(lockedMax);
        }
    }

    private static void clear(Mob mob) {
        clearTags(mob);
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MODIFIER_ID);
        }
    }

    private static long readUntil(Mob mob) {
        for (String tag : mob.entityTags()) {
            if (!tag.startsWith(TAG_UNTIL_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_UNTIL_PREFIX.length());
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static float readLockedMax(Mob mob) {
        for (String tag : mob.entityTags()) {
            if (!tag.startsWith(TAG_MAX_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_MAX_PREFIX.length());
            try {
                return Float.parseFloat(raw);
            } catch (NumberFormatException ignored) {
                return 0.0F;
            }
        }
        return 0.0F;
    }

    private static void clearTags(Mob mob) {
        List<String> tags = List.copyOf(mob.entityTags());
        for (String tag : tags) {
            if (tag.equals(TAG_HEART_LOCK) || tag.startsWith(TAG_UNTIL_PREFIX) || tag.startsWith(TAG_MAX_PREFIX)) {
                mob.removeTag(tag);
            }
        }
    }
}
