package com.feel.gems.client.hud;

import com.feel.gems.client.ClientChaosState;
import com.feel.gems.client.ClientPrismState;
import com.feel.gems.client.ClientStolenState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.GemItem;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.util.GemsNbt;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;




public final class GemsTooltips {
    private static boolean registered = false;

    private GemsTooltips() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
            appendOwnerTooltip(stack, lines);
            appendTooltip(stack, lines);
        });
    }

    private static final String OWNER_NAME_KEY = "gemsOwnerName";
    private static final String OWNER_UUID_KEY = "gemsOwner";
    private static final String PREV_OWNER_NAME_KEY = "gemsPrevOwnerName";

    private static void appendOwnerTooltip(ItemStack stack, List<Component> lines) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        CompoundTag nbt = custom.copyTag();
        if (!nbt.contains(OWNER_NAME_KEY)) {
            return;
        }
        String ownerName = nbt.getString(OWNER_NAME_KEY).orElse("");
        if (!ownerName.isEmpty()) {
            String displayName = ownerName;
            String prevOwnerName = nbt.getString(PREV_OWNER_NAME_KEY).orElse("");
            if (!prevOwnerName.isEmpty()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    var ownerUuid = GemsNbt.getUuid(nbt, OWNER_UUID_KEY);
                    if (ownerUuid != null && ownerUuid.equals(client.player.getUUID())) {
                        displayName = prevOwnerName;
                    } else if (ownerUuid == null && ownerName.equalsIgnoreCase(client.player.getName().getString())) {
                        displayName = prevOwnerName;
                    }
                }
            }
            lines.add(Component.translatable("gems.item.last_owner", displayName).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (!(stack.getItem() instanceof GemItem item)) {
            return;
        }

        GemId gem = item.gemId();
        GemDefinition def = GemRegistry.definition(gem);

        if (gem == GemId.PRISM || gem == GemId.CHAOS) {
            appendDynamicSections(gem, def, lines);
        } else {
            ChatFormatting headerColor = ChatFormatting.LIGHT_PURPLE;
            ChatFormatting abilityColor = ChatFormatting.AQUA;
            ChatFormatting passiveColor = ChatFormatting.GREEN;

            lines.add(Component.translatable("gems.tooltip.passives").withStyle(headerColor));
            for (Identifier id : def.passives()) {
                GemPassive passive = ModPassives.get(id);
                appendEntry(lines, passiveColor, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            }

            lines.add(Component.translatable("gems.tooltip.abilities").withStyle(headerColor));
            for (Identifier id : def.abilities()) {
                GemAbility ability = ModAbilities.get(id);
                appendEntry(lines, abilityColor, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            }
        }

        if (gem == GemId.FLUX) {
            String chord = com.feel.gems.client.GemsKeybinds.chordSlotLabel(2);
            if (!chord.isEmpty()) {
                lines.add(Component.translatable("gems.tooltip.flux.charge_key", chord).withStyle(ChatFormatting.AQUA));
            }
            lines.add(Component.translatable("gems.tooltip.flux.charge_items").withStyle(ChatFormatting.AQUA));
            var flux = GemsBalance.v().flux();
            lines.add(Component.literal(" - Diamond Block: +" + flux.chargeDiamondBlock() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Gold Block: +" + flux.chargeGoldBlock() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Copper Block: +" + flux.chargeCopperBlock() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Emerald Block: +" + flux.chargeEmeraldBlock() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Amethyst Block: +" + flux.chargeAmethystBlock() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Netherite Scrap: +" + flux.chargeNetheriteScrap() + "%").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.literal(" - Enchanted diamond tool/armor: +" + flux.chargeEnchantedDiamondItem() + "%").withStyle(ChatFormatting.DARK_AQUA));
        }

        appendStolenSections(lines);
    }

    private static void appendDynamicSections(GemId gem, GemDefinition def, List<Component> lines) {
        ChatFormatting headerColor = ChatFormatting.LIGHT_PURPLE;
        ChatFormatting abilityColor = ChatFormatting.AQUA;
        ChatFormatting passiveColor = ChatFormatting.GREEN;

        lines.add(Component.translatable("gems.tooltip.passives").withStyle(headerColor));
        boolean anyPassives = false;

        for (Identifier id : def.passives()) {
            GemPassive passive = ModPassives.get(id);
            appendEntry(lines, passiveColor, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            anyPassives = true;
        }

        if (gem == GemId.PRISM) {
            for (ClientPrismState.PrismPassiveEntry entry : ClientPrismState.getPassives()) {
                GemPassive passive = ModPassives.get(entry.id());
                appendEntry(lines, passiveColor, entry.name(), passive != null ? passive.description() : "");
                anyPassives = true;
            }
        } else if (gem == GemId.CHAOS) {
            for (int i = 0; i < ClientChaosState.slotCount(); i++) {
                ClientChaosState.SlotState slot = ClientChaosState.getSlot(i);
                if (!slot.isActive()) {
                    continue;
                }
                String name = slot.passiveName();
                GemPassive passive = slot.passiveId() != null ? ModPassives.get(slot.passiveId()) : null;
                appendEntry(lines, passiveColor, name, passive != null ? passive.description() : "");
                anyPassives = true;
            }
        }

        if (!anyPassives) {
            lines.add(Component.translatable("gems.item.prism.no_selection").withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable("gems.tooltip.abilities").withStyle(headerColor));
        boolean anyAbilities = false;

        for (Identifier id : def.abilities()) {
            GemAbility ability = ModAbilities.get(id);
            appendEntry(lines, abilityColor, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            anyAbilities = true;
        }

        if (gem == GemId.PRISM) {
            for (ClientPrismState.PrismAbilityEntry entry : ClientPrismState.getAbilities()) {
                GemAbility ability = ModAbilities.get(entry.id());
                appendEntry(lines, abilityColor, entry.name(), ability != null ? ability.description() : "");
                anyAbilities = true;
            }
        } else if (gem == GemId.CHAOS) {
            for (int i = 0; i < ClientChaosState.slotCount(); i++) {
                ClientChaosState.SlotState slot = ClientChaosState.getSlot(i);
                if (!slot.isActive()) {
                    continue;
                }
                String name = slot.abilityName();
                GemAbility ability = slot.abilityId() != null ? ModAbilities.get(slot.abilityId()) : null;
                appendEntry(lines, abilityColor, name, ability != null ? ability.description() : "");
                anyAbilities = true;
            }
        }

        if (!anyAbilities) {
            lines.add(Component.translatable("gems.item.prism.no_selection").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void appendStolenSections(List<Component> lines) {
        if (ClientStolenState.hasStolenPassives()) {
            lines.add(Component.translatable("gems.tooltip.stolen_passives").withStyle(ChatFormatting.GOLD));
            for (Identifier id : ClientStolenState.stolenPassives()) {
                GemPassive passive = ModPassives.get(id);
                appendEntry(lines, ChatFormatting.YELLOW, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            }
        }
        if (ClientStolenState.hasStolenAbilities()) {
            lines.add(Component.translatable("gems.tooltip.stolen_abilities").withStyle(ChatFormatting.GOLD));
            for (Identifier id : ClientStolenState.stolenAbilities()) {
                GemAbility ability = ModAbilities.get(id);
                appendEntry(lines, ChatFormatting.YELLOW, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            }
        }
    }

    private static void appendEntry(List<Component> lines, ChatFormatting color, String name, String desc) {
        if (name == null || name.isBlank()) {
            return;
        }
        String suffix = desc != null && !desc.isBlank() ? ": " + desc : "";
        lines.add(Component.literal(" - " + name + suffix).withStyle(color == null ? ChatFormatting.DARK_GRAY : color));
    }
}
