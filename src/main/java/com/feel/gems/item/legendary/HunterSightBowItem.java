package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsTooltipFormat;
import com.feel.gems.power.util.Targeting;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;




public final class HunterSightBowItem extends BowItem implements LegendaryItem {
    private static final String KEY_LAST_TARGET = "legendaryHuntersSightTarget";
    private static final String KEY_LAST_SEEN = "legendaryHuntersSightLastSeen";
    private static final double ASSIST_RANGE = 50.0D;

    public HunterSightBowItem(Properties settings) {
        super(settings.enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "hunters_sight_bow").toString();
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClientSide() && user instanceof net.minecraft.server.level.ServerPlayer player) {
            LivingEntity target = getAssistTarget(player);
            if (target != null) {
                float oldYaw = player.getYRot();
                float oldPitch = player.getXRot();
                float oldHead = player.getYHeadRot();
                int useTicks = getUseDuration(stack, user) - remainingUseTicks;
                aimAt(player, target, useTicks);
                boolean fired = super.releaseUsing(stack, world, user, remainingUseTicks);
                player.setYRot(oldYaw);
                player.setXRot(oldPitch);
                player.setYHeadRot(oldHead);
                return fired;
            }
        }
        return super.releaseUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.hunters_sight_bow.desc"));
    }

    public static void recordHit(net.minecraft.server.level.ServerPlayer attacker, LivingEntity target) {
        if (attacker == null || target == null || target == attacker) {
            return;
        }
        var data = ((GemsPersistentDataHolder) attacker).gems$getPersistentData();
        UUID uuid = target.getUUID();
        GemsNbt.putUuid(data, KEY_LAST_TARGET, uuid);
        data.putLong(KEY_LAST_SEEN, GemsTime.now(attacker));
    }

    public static LivingEntity getAssistTarget(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return null;
        }
        var data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID uuid = GemsNbt.getUuid(data, KEY_LAST_TARGET);
        if (uuid != null) {
            Entity entity = player.level().getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                if (player.distanceToSqr(living) <= ASSIST_RANGE * ASSIST_RANGE && player.hasLineOfSight(living)) {
                    return living;
                }
            }
        }

        return Targeting.raycastLiving(player, ASSIST_RANGE);
    }

    private static void aimAt(LivingEntity user, LivingEntity target, int useTicks) {
        Vec3 from = user.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 relativeVel = target.getDeltaMovement().subtract(user.getDeltaMovement());
        double speed = Math.max(0.1D, arrowSpeed(useTicks));
        double distance = from.distanceTo(targetPos);
        double time = Math.min(3.0D, distance / speed);
        Vec3 predicted = targetPos.add(relativeVel.scale(time));
        Vec3 to = predicted.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
        user.setYRot(yaw);
        user.setXRot(pitch);
        user.setYHeadRot(yaw);
    }

    private static double arrowSpeed(int useTicks) {
        float pull = Math.max(0.0F, useTicks) / 20.0F;
        pull = (pull * pull + pull * 2.0F) / 3.0F;
        if (pull > 1.0F) {
            pull = 1.0F;
        }
        return pull * 3.0F;
    }
}
