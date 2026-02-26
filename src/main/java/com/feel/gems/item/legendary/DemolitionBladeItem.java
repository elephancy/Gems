package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;




public final class DemolitionBladeItem extends Item implements LegendaryItem {
    public DemolitionBladeItem(ToolMaterial material, Properties settings) {
        super(settings.sword(material, 3.0F, -2.4F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "demolition_blade").toString();
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(held) && !GemsAdmin.noLegendaryCooldowns(player)) {
            return InteractionResult.SUCCESS;
        }
        int cooldown = GemsBalance.v().legendary().demolitionCooldownTicks();
        int cooldownScale = GemsBalance.v().legendary().demolitionCooldownScalePercent();
        if (cooldown > 0 && cooldownScale != 100) {
            cooldown = Math.max(0, Math.round(cooldown * (cooldownScale / 100.0F)));
        }
        if (cooldown > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getCooldowns().addCooldown(held, cooldown);
        }
        int range = GemsBalance.v().legendary().demolitionRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        Vec3 spawnPos = null;
        if (target != null) {
            spawnPos = target.position().add(0.0D, 0.2D, 0.0D);
        } else {
            HitResult hit = player.pick(range, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                Vec3 pos = blockHit.getLocation();
                spawnPos = new Vec3(pos.x, pos.y + 0.1D, pos.z);
            }
        }
        if (spawnPos == null) {
            player.sendSystemMessage(Component.translatable("gems.item.demolition_blade.no_target"), true);
            return InteractionResult.SUCCESS;
        }
        int fuse = Math.max(1, GemsBalance.v().legendary().demolitionFuseTicks());
        int tntCount = GemsBalance.v().legendary().demolitionTntCount();
        for (int i = 0; i < tntCount; i++) {
            PrimedTnt tnt = new PrimedTnt(world, spawnPos.x, spawnPos.y, spawnPos.z, player);
            tnt.setFuse(fuse);
            world.addFreshEntity(tnt);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        GemsTooltipFormat.appendDescription(tooltip, Component.translatable("item.gems.demolition_blade.desc"));
    }
}
