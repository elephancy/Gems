package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityWealthDurabilityMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$durabilityPassives(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer attacker)) {
            return;
        }

        boolean chip = GemPowers.isPassiveActive(attacker, PowerIds.DURABILITY_CHIP);
        boolean mend = GemPowers.isPassiveActive(attacker, PowerIds.ARMOR_MEND_ON_HIT);
        if (!chip && !mend) {
            return;
        }

        if (chip && target instanceof ServerPlayer victim) {
            damageArmor(victim, 1);
        }
        if (mend) {
            mendArmor(attacker, 1);
        }
    }

    private static void damageArmor(ServerPlayer player, int amount) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            stack.hurtAndBreak(amount, player, slot);
        }
    }

    private static void mendArmor(ServerPlayer player, int amount) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) {
                continue;
            }
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
        }
    }
}
